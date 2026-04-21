package com.grad.sam.rules;

import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Txn;
import com.grad.sam.model.Txn;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PatternRule implements AmlRule {

    @Override
    public String getSupportedCategory() {
        return "PATTERN";
    }

    @Override
    public List<String> getSupportedRuleCodes() {
        return List.of("PAT-001", "PAT-002", "PAT-003", "PAT-004", "PAT-005");
    }

    @Override
    public boolean supports(AlertRule rule) {
        return rule != null
                && rule.getRuleCode() != null
                && rule.getRuleCode().startsWith("PAT-");
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
            case "PAT-001" -> evaluateRoundTripping(context, rule);
            case "PAT-002" -> evaluateLayering(context, rule);
            case "PAT-003" -> evaluateDormantAccount(context, rule);
            case "PAT-004" -> Optional.empty();
            case "PAT-005" -> evaluateCharityDiversion(context, rule);
            default -> Optional.empty();
        };
    }

    private Optional<RuleMatch> evaluateRoundTripping(RuleContext context, AlertRule rule) {
        Txn currentTxn = context.getTxn();
        BigDecimal currentAmount = requireAmount(currentTxn);
        long thresholdCount = rule.getThresholdCount() != null ? rule.getThresholdCount() : 2;

        long occurrences = context.getRecentTxns().stream()
                .map(Txn::getAmountUsd)
                .filter(amount -> amount != null && amount.compareTo(currentAmount) == 0)
                .count() + 1;

        if (occurrences < thresholdCount) {
            return Optional.empty();
        }

        String reason = String.format(
                "Repeated amount pattern detected: USD %.2f appeared %d times in %d-day window",
                currentAmount, occurrences, rule.getLookbackDays());
        return Optional.of(new RuleMatch(rule, reason));
    }

    private Optional<RuleMatch> evaluateLayering(RuleContext context, AlertRule rule) {
        Set<String> counterparties = java.util.stream.Stream.concat(
                        context.getRecentTxns().stream().map(Txn::getCounterpartyAccount),
                        java.util.stream.Stream.of(context.getTxn().getCounterpartyAccount()))
                .filter(account -> account != null && !account.isBlank())
                .collect(Collectors.toSet());

        if (context.getTxn().getTxnType() != com.grad.sam.enums.TxnType.WIRE || counterparties.size() < 3) {
            return Optional.empty();
        }

        String reason = String.format(
                "Layering indicator: %d distinct counterparties in %d-day window",
                counterparties.size(), rule.getLookbackDays());
        return Optional.of(new RuleMatch(rule, reason));
    }

    private Optional<RuleMatch> evaluateDormantAccount(RuleContext context, AlertRule rule) {
        if (rule.getThresholdAmount() == null) {
            return Optional.empty();
        }

        BigDecimal currentAmount = requireAmount(context.getTxn());
        if (!context.getRecentTxns().isEmpty() || currentAmount.compareTo(rule.getThresholdAmount()) < 0) {
            return Optional.empty();
        }

        String reason = String.format(
                "Dormant account activation: no completed transactions in %d-day window before USD %.2f credit",
                rule.getLookbackDays(), currentAmount);
        return Optional.of(new RuleMatch(rule, reason));
    }

    private Optional<RuleMatch> evaluateCharityDiversion(RuleContext context, AlertRule rule) {
        if (rule.getThresholdAmount() == null
                || context.getAccount().getCustomer() == null
                || context.getAccount().getCustomer().getCustomerType() != com.grad.sam.enums.CustomerType.CHARITY
                || context.getTxn().getDirection() != com.grad.sam.enums.TxnDirection.DR) {
            return Optional.empty();
        }

        BigDecimal currentAmount = requireAmount(context.getTxn());
        if (currentAmount.compareTo(rule.getThresholdAmount()) < 0) {
            return Optional.empty();
        }

        String country = context.getTxn().getCounterpartyCountry();
        if (country == null || country.isBlank()) {
            return Optional.empty();
        }

        String reason = String.format(
                "Charity fund diversion indicator: charity sent USD %.2f to %s",
                currentAmount, country);
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
