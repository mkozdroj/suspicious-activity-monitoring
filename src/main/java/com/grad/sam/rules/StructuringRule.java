package com.grad.sam.rules;

import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Txn;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class StructuringRule implements AmlRule {

    private static final BigDecimal LOWER_BAND_FACTOR = new BigDecimal("0.75");

    @Override
    public String getSupportedCategory() {
        return "STRUCTURING";
    }

    @Override
    public boolean supports(AlertRule rule) {
        return rule != null
                && rule.getRuleCode() != null
                && rule.getRuleCode().startsWith("STR-");
    }

    @Override
    public List<String> getSupportedRuleCodes() {
        return List.of("STR-001", "STR-002", "STR-003", "STR-004");
    }

    @Override
    public Optional<RuleMatch> evaluate(RuleContext context, AlertRule rule) {
        if (context == null) {
            throw new IllegalArgumentException("Rule context must not be null.");
        }
        if (rule == null) {
            throw new IllegalArgumentException("Alert rule must not be null.");
        }
        if (context.getTxn() == null) {
            throw new IllegalArgumentException("Transaction in context must not be null.");
        }
        if (context.getRecentTxns() == null) {
            throw new IllegalStateException("Recent transactions list must not be null.");
        }

        return switch (rule.getRuleCode()) {
            case "STR-001" -> evaluateCashStructuring(context, rule);
            case "STR-002" -> evaluateSmurfing(context, rule);
            case "STR-003" -> evaluateCryptoOffRamp(context, rule);
            case "STR-004" -> evaluateChequeKiting(context, rule);
            default -> Optional.empty();
        };
    }

    private Optional<RuleMatch> evaluateCashStructuring(RuleContext context, AlertRule rule) {
        if (rule.getThresholdAmount() == null) {
            return Optional.empty();
        }

        Txn currentTxn = context.getTxn();
        BigDecimal currentAmount = requireAmount(currentTxn);
        BigDecimal threshold = rule.getThresholdAmount();
        BigDecimal lowerBand = threshold.multiply(LOWER_BAND_FACTOR);

        BigDecimal windowTotal = context.getRecentTxns().stream()
                .map(Txn::getAmountUsd)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(currentAmount);

        boolean isStructuring = windowTotal.compareTo(lowerBand) >= 0
                && windowTotal.compareTo(threshold) < 0;

        if (!isStructuring) {
            return Optional.empty();
        }

        String reason = String.format(
                "Cash structuring detected: cumulative USD %.2f in %d-day window approaches threshold USD %.2f",
                windowTotal, rule.getLookbackDays(), threshold);
        return Optional.of(new RuleMatch(rule, reason));
    }

    private Optional<RuleMatch> evaluateSmurfing(RuleContext context, AlertRule rule) {
        if (rule.getThresholdAmount() == null || rule.getThresholdCount() == null) {
            return Optional.empty();
        }

        Txn currentTxn = context.getTxn();
        BigDecimal threshold = rule.getThresholdAmount();
        if (currentTxn.getTxnType() != com.grad.sam.enums.TxnType.CASH
                || currentTxn.getDirection() != com.grad.sam.enums.TxnDirection.CR
                || requireAmount(currentTxn).compareTo(threshold) > 0) {
            return Optional.empty();
        }

        long count = context.getRecentTxns().stream()
                .filter(txn -> txn.getTxnType() == com.grad.sam.enums.TxnType.CASH)
                .filter(txn -> txn.getDirection() == com.grad.sam.enums.TxnDirection.CR)
                .filter(txn -> txn.getAmountUsd() != null && txn.getAmountUsd().compareTo(threshold) <= 0)
                .count() + 1;

        if (count < rule.getThresholdCount()) {
            return Optional.empty();
        }

        String reason = String.format(
                "Smurfing detected: %d small cash deposits in %d-day window (threshold: %d)",
                count, rule.getLookbackDays(), rule.getThresholdCount());
        return Optional.of(new RuleMatch(rule, reason));
    }

    private Optional<RuleMatch> evaluateCryptoOffRamp(RuleContext context, AlertRule rule) {
        if (rule.getThresholdAmount() == null || rule.getThresholdCount() == null) {
            return Optional.empty();
        }

        Txn currentTxn = context.getTxn();
        BigDecimal threshold = rule.getThresholdAmount();
        if (currentTxn.getTxnType() != com.grad.sam.enums.TxnType.CRYPTO
                || currentTxn.getDirection() != com.grad.sam.enums.TxnDirection.CR
                || requireAmount(currentTxn).compareTo(threshold) > 0) {
            return Optional.empty();
        }

        long count = context.getRecentTxns().stream()
                .filter(txn -> txn.getTxnType() == com.grad.sam.enums.TxnType.CRYPTO)
                .filter(txn -> txn.getDirection() == com.grad.sam.enums.TxnDirection.CR)
                .filter(txn -> txn.getAmountUsd() != null && txn.getAmountUsd().compareTo(threshold) <= 0)
                .count() + 1;

        if (count < rule.getThresholdCount()) {
            return Optional.empty();
        }

        String reason = String.format(
                "Crypto off-ramp pattern detected: %d crypto credits below USD %.2f in %d-day window",
                count, threshold, rule.getLookbackDays());
        return Optional.of(new RuleMatch(rule, reason));
    }

    private Optional<RuleMatch> evaluateChequeKiting(RuleContext context, AlertRule rule) {
        Txn currentTxn = context.getTxn();
        if (currentTxn.getTxnType() != com.grad.sam.enums.TxnType.CASH
                || currentTxn.getDirection() != com.grad.sam.enums.TxnDirection.DR) {
            return Optional.empty();
        }

        boolean hasRecentChequeDeposit = context.getRecentTxns().stream()
                .anyMatch(txn -> txn.getTxnType() == com.grad.sam.enums.TxnType.CHEQUE
                        && txn.getDirection() == com.grad.sam.enums.TxnDirection.CR);

        if (!hasRecentChequeDeposit) {
            return Optional.empty();
        }

        String reason = String.format(
                "Cheque kiting indicator: cash withdrawal followed a cheque deposit within %d-day window",
                rule.getLookbackDays());
        return Optional.of(new RuleMatch(rule, reason));
    }

    private BigDecimal requireAmount(Txn txn) {
        BigDecimal currentAmount = txn.getAmountUsd();
        if (currentAmount == null) {
            throw new IllegalStateException("Transaction amountUsd must not be null.");
        }
        return currentAmount;
    }
}
