package com.grad.sam.service;

import com.grad.sam.model.Account;
import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Txn;
import com.grad.sam.repository.AlertRuleRepository;
import com.grad.sam.repository.TxnRepository;
import com.grad.sam.rules.AmlRule;
import com.grad.sam.rules.RuleContext;
import com.grad.sam.rules.RuleMatch;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Validated
public class RuleEngineService {

    private final AlertRuleRepository alertRuleRepository;
    private final TxnRepository txnRepository;
    private final DataSource dataSource;
    private final Map<String, AmlRule> rulesByCategory;
    private final WatchlistScreeningService watchlistScreeningService;

    public RuleEngineService(AlertRuleRepository alertRuleRepository,
                             TxnRepository txnRepository,
                             DataSource dataSource,
                             List<AmlRule> rules,
                             WatchlistScreeningService watchlistScreeningService) {
        this.alertRuleRepository = alertRuleRepository;
        this.watchlistScreeningService = watchlistScreeningService;
        this.txnRepository = txnRepository;
        this.dataSource = dataSource;

        this.rulesByCategory = rules.stream()
                .collect(Collectors.toMap(
                        AmlRule::getSupportedCategory,
                        r -> r,
                        (existing, replacement) -> existing));
    }

    @Transactional
    public List<Long> screenTransaction(@NotNull Txn txn, @NotNull Account account) {

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
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(match -> callRaiseAlert(txn.getTxnId(), match))
                .filter(id -> id > 0)
                .collect(Collectors.toList());

        callScreenTransaction(txn.getTxnId());

        try {
            watchlistScreeningService.screenCustomer(
                    account.getCustomer().getFullName(),
                    WatchlistScreeningService.FUZZY_MATCH_SCORE,
                    txn
            );
        } catch (Exception e) {
            log.error("Watchlist screening failed for txn {}: {}", txn.getTxnId(), e.getMessage(), e);
        }

        log.info("Screened txn {} — {} alert(s) raised", txn.getTxnRef(), alertIds.size());
        return alertIds;
    }

    private Optional<RuleMatch> evaluateRule(RuleContext context, AlertRule rule) {
        if (rule.getRuleCategory() == null) {
            log.warn("Rule category is null for rule_code: {}", rule.getRuleCode());
            return Optional.empty();
        }

        AmlRule impl = rulesByCategory.get(rule.getRuleCategory().name());
        if (impl == null) {
            log.warn("No rule implementation found for category '{}' (rule_code: {})",
                    rule.getRuleCategory(), rule.getRuleCode());
            return Optional.empty();
        }

        try {
            return impl.evaluate(context, rule);
        } catch (Exception e) {
            log.error("Rule evaluation failed for rule_code '{}': {}",
                    rule.getRuleCode(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    private long callRaiseAlert(Integer txnId, RuleMatch match) {
        String notes = match.getReason();
        if (notes != null && notes.length() > 500) {
            notes = notes.substring(0, 497) + "...";
        }

        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL raise_alert(?, ?, ?, ?)}")) {

            cs.setInt(1, txnId);
            cs.setInt(2, match.getRule().getRuleId());
            cs.setString(3, notes);
            cs.registerOutParameter(4, Types.BIGINT);
            cs.execute();

            long alertId = cs.getLong(4);
            if (alertId > 0) {
                log.debug("raise_alert fired: alert_id={} for txn_id={} rule={}",
                        alertId, txnId, match.getRule().getRuleCode());
            } else {
                log.warn("raise_alert returned -1 for txn_id={} rule={}",
                        txnId, match.getRule().getRuleCode());
            }
            return alertId;

        } catch (Exception e) {
            log.error("Failed to call raise_alert for txn_id={}: {}", txnId, e.getMessage(), e);
            return -1L;
        }
    }

    private void callScreenTransaction(Integer txnId) {
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL screen_transaction(?)}")) {

            cs.setInt(1, txnId);
            cs.execute();
            log.debug("screen_transaction completed for txn_id={}", txnId);

        } catch (Exception e) {
            log.error("Failed to call screen_transaction for txn_id={}: {}", txnId, e.getMessage(), e);
        }
    }
}