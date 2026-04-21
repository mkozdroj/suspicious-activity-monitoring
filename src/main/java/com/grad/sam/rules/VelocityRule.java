package com.grad.sam.rules;

import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Txn;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class VelocityRule implements AmlRule {

    @Override
    public String getSupportedCategory() {
        return "VELOCITY";
    }

    @Override
    public boolean supports(AlertRule rule) {
        return rule != null
                && rule.getRuleCode() != null
                && rule.getRuleCode().startsWith("VEL-");
    }

    @Override
    public List<String> getSupportedRuleCodes() {
        return List.of("VEL-001", "VEL-002", "VEL-003", "VEL-004");
    }

    @Override
    public Optional<RuleMatch> evaluate(RuleContext context, AlertRule rule) {
        if (context == null) {
            throw new IllegalArgumentException("Rule context must not be null.");
        }
        if (rule == null) {
            throw new IllegalArgumentException("Alert rule must not be null.");
        }
        if (context.getRecentTxns() == null) {
            throw new IllegalStateException("Recent transactions list must not be null.");
        }
        if (context.getAccount() == null) {
            throw new IllegalArgumentException("Account in context must not be null.");
        }

        return switch (rule.getRuleCode()) {
            case "VEL-001" -> evaluateHighValueWireVelocity(context, rule);
            case "VEL-002" -> evaluateRapidTurnover(context, rule);
            case "VEL-003" -> evaluateForeignExchangeActivity(context, rule);
            case "VEL-004" -> Optional.empty();
            default -> Optional.empty();
        };
    }

    private Optional<RuleMatch> evaluateHighValueWireVelocity(RuleContext context, AlertRule rule) {
        if (rule.getThresholdCount() == null) {
            return Optional.empty();
        }

        requireAmount(rule, context.getTxn());
        long total = context.getRecentTxns().size() + 1L;

        if (total <= rule.getThresholdCount()) {
            return Optional.empty();
        }

        String reason = String.format(
                "High velocity detected: %d transactions in %d-day window on account %s",
                total, rule.getLookbackDays(), context.getAccount().getAccountNumber());
        return Optional.of(new RuleMatch(rule, reason));
    }

    private Optional<RuleMatch> evaluateRapidTurnover(RuleContext context, AlertRule rule) {
        Txn currentTxn = context.getTxn();
        if (currentTxn.getDirection() != com.grad.sam.enums.TxnDirection.DR) {
            return Optional.empty();
        }

        BigDecimal recentCredits = context.getRecentTxns().stream()
                .filter(txn -> txn.getDirection() == com.grad.sam.enums.TxnDirection.CR)
                .map(Txn::getAmountUsd)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (recentCredits.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        BigDecimal debitAmount = requireAmount(rule, currentTxn);
        BigDecimal turnoverThreshold = recentCredits.multiply(new BigDecimal("0.90"));
        if (debitAmount.compareTo(turnoverThreshold) < 0) {
            return Optional.empty();
        }

        String reason = String.format(
                "Rapid turnover detected: debit USD %.2f consumed at least 90%% of recent credits USD %.2f",
                debitAmount, recentCredits);
        return Optional.of(new RuleMatch(rule, reason));
    }

    private Optional<RuleMatch> evaluateForeignExchangeActivity(RuleContext context, AlertRule rule) {
        if (rule.getThresholdCount() == null) {
            return Optional.empty();
        }

        long distinctCurrencies = Stream.concat(
                        context.getRecentTxns().stream().map(Txn::getCurrency),
                        Stream.of(context.getTxn().getCurrency()))
                .filter(currency -> currency != null && !currency.isBlank())
                .collect(Collectors.toSet())
                .size();

        if (distinctCurrencies < rule.getThresholdCount()) {
            return Optional.empty();
        }

        String reason = String.format(
                "Unusual FX activity detected: %d distinct currencies in %d-day window",
                distinctCurrencies, rule.getLookbackDays());
        return Optional.of(new RuleMatch(rule, reason));
    }

    private BigDecimal requireAmount(AlertRule rule, Txn txn) {
        if (txn.getAmountUsd() == null) {
            throw new IllegalStateException("Transaction amountUsd must not be null for rule " + rule.getRuleCode());
        }
        return txn.getAmountUsd();
    }
}
