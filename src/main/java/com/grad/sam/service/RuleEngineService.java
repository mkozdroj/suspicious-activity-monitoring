package com.grad.sam.service;

import com.grad.sam.enums.TxnStatus;
import com.grad.sam.model.Account;
import com.grad.sam.model.Alert;
import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Txn;
import com.grad.sam.rules.AmlRule;
import com.grad.sam.rules.RuleContext;
import com.grad.sam.rules.RuleMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.grad.sam.repository.AlertRuleRepository;
import com.grad.sam.repository.TxnRepository;

@Slf4j
@Service
public class RuleEngineService {
    private final AlertRuleRepository alertRuleRepository;
    private final TxnRepository txnRepository;
    private final TxnService txnService;
    private final Map<String, AmlRule> rulesByCategory;
    private final WatchlistScreeningService watchlistScreeningService;
    private final AlertService alertService;

    public RuleEngineService(AlertRuleRepository alertRuleRepository,
                             TxnRepository txnRepository,
                             TxnService txnService,
                             List<AmlRule> rules,
                             WatchlistScreeningService watchlistScreeningService,
                             AlertService alertService) {
        this.alertRuleRepository = alertRuleRepository;
        this.txnRepository = txnRepository;
        this.txnService = txnService;
        this.watchlistScreeningService = watchlistScreeningService;
        this.alertService = alertService;
        this.rulesByCategory = rules.stream()
                .collect(Collectors.toMap(
                        AmlRule::getSupportedCategory,
                        r -> r,
                        (existing, replacement) -> existing));
    }

public List<Long> screenTransaction(Txn txn, Account account) {
    List<AlertRule> activeRules = alertRuleRepository.findByIsActiveTrue();

    int maxLookback = activeRules.stream()
            .mapToInt(r -> r.getLookbackDays() != null ? r.getLookbackDays() : 30)
            .max()
            .orElse(30);

    List<Txn> recentTxns = txnRepository.findRecentByAccount(
            account.getAccountId(), txn.getTxnId(), maxLookback);

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
        watchlistScreeningService.screenCustomer(
                account.getCustomer().getFullName(),
                WatchlistScreeningService.FUZZY_MATCH_SCORE,
                txn
        );
    } catch (Exception e) {
        log.error("Watchlist screening failed for txn {}: {}", txn.getTxnId(), e.getMessage(), e);
    }

    return alertIds;
}

    private Optional<RuleMatch> evaluateRule(RuleContext context, AlertRule rule) {
        if (rule.getRuleCategory() == null) {
            return Optional.empty();
        }

        AmlRule impl = rulesByCategory.get(rule.getRuleCategory().name());
        if (impl == null) {
            return Optional.empty();
        }

        try {
            return impl.evaluate(context, rule);
        } catch (Exception e) {
            log.error("Rule evaluation failed for rule_code '{}': {}", rule.getRuleCode(), e.getMessage(), e);
            return Optional.empty();
        }
    }
}
