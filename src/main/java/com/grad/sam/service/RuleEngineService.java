package com.grad.sam.service;

import com.grad.sam.enums.TxnStatus;
import com.grad.sam.model.Account;
import com.grad.sam.model.Alert;
import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Txn;
import com.grad.sam.repository.AlertRuleRepository;
import com.grad.sam.repository.TxnRepository;
import com.grad.sam.rules.AmlRule;
import com.grad.sam.rules.RuleContext;
import com.grad.sam.rules.RuleMatch;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineService {
    private final AlertRuleRepository alertRuleRepository;
    private final TxnRepository txnRepository;
    private final TxnService txnService;
    private final List<AmlRule> rules;
    private final WatchlistScreeningService watchlistScreeningService;
    private final AlertService alertService;
    private Map<String, AmlRule> rulesByCategory;

    @PostConstruct
    public void initRulesByCategory() {
        this.rulesByCategory = rules.stream()
                .collect(Collectors.toMap(AmlRule::getSupportedCategory, r -> r, (existing, replacement) -> existing));
    }

    public List<Long> screenTransaction(Txn txn, Account account) {
        validateRequiredInput(txn, account);

        List<AlertRule> activeRules = alertRuleRepository.findByIsActiveTrue();
        if (activeRules == null) {
            throw new IllegalStateException("Active rules query returned null.");
        }

        int maxLookback = activeRules.stream()
                .mapToInt(r -> r.getLookbackDays() != null ? r.getLookbackDays() : 30)
                .max()
                .orElse(30);

        List<Txn> recentTxns = txnRepository.findRecentByAccount(
                account.getAccountId(), txn.getTxnId(), maxLookback);
        if (recentTxns == null) {
            throw new IllegalStateException("Recent transactions query returned null.");
        }

        RuleContext context = RuleContext.builder()
                .txn(txn)
                .account(account)
                .recentTxns(recentTxns)
                .build();

        List<Long> alertIds = activeRules.stream()
                .map(rule -> evaluateRule(context, rule))
                .flatMap(Optional::stream)
                .map(match -> alertService.createAlert(txn, account, match.getRule(), match.getReason()))
                .map(Alert::getAlertId)
                .map(Long::valueOf)
                .toList();

        txnService.updateTxnStatus(txn.getTxnId(), TxnStatus.SCREENED);

        try {
            String customerName = account.getCustomer() != null ? account.getCustomer().getFullName() : null;
            if (customerName == null || customerName.isBlank()) {
                throw new IllegalStateException(
                        "Cannot run watchlist screening: customer full name is missing for account " + account.getAccountId());
            }

            watchlistScreeningService.screenCustomer(
                    customerName,
                    WatchlistScreeningService.FUZZY_MATCH_SCORE,
                    txn
            );
        } catch (Exception e) {
            log.error("Watchlist screening failed for txn {}: {}", txn.getTxnId(), e.getMessage(), e);
        }

        return alertIds;
    }

    private Optional<RuleMatch> evaluateRule(RuleContext context, AlertRule rule) {
        if (context == null) {
            throw new IllegalArgumentException("Rule context must not be null.");
        }
        if (rule == null) {
            throw new IllegalArgumentException("Alert rule must not be null.");
        }
        if (rule.getRuleCategory() == null) {
            return Optional.empty();
        }

        AmlRule impl = rulesByCategory.get(rule.getRuleCategory().name());
        if (impl == null) {
            return Optional.empty();
        }

        try {
            Optional<RuleMatch> result = impl.evaluate(context, rule);
            if (result == null) {
                throw new IllegalStateException(
                        "Rule implementation returned null Optional for rule_code '" + rule.getRuleCode() + "'.");
            }
            return result;
        } catch (Exception e) {
            log.error("Rule evaluation failed for rule_code '{}': {}", rule.getRuleCode(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    private void validateRequiredInput(Txn txn, Account account) {
        if (txn == null) {
            throw new IllegalArgumentException("Transaction must not be null.");
        }
        if (account == null) {
            throw new IllegalArgumentException("Account must not be null.");
        }
        if (txn.getTxnId() == null) {
            throw new IllegalArgumentException("Transaction id must not be null.");
        }
        if (account.getAccountId() == null) {
            throw new IllegalArgumentException("Account id must not be null.");
        }
    }
}
