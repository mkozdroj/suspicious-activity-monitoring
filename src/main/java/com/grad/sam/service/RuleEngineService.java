package com.grad.sam.service;

import com.grad.sam.dao.AlertRuleDao;
import com.grad.sam.dao.TxnDao;
import com.grad.sam.model.Account;
import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Txn;
import com.grad.sam.rules.AmlRule;
import com.grad.sam.rules.RuleContext;
import com.grad.sam.rules.RuleMatch;
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

@Slf4j
@Service
public class RuleEngineService {

    private final AlertRuleDao alertRuleDao;
    private final TxnDao txnDao;
    private final DataSource dataSource;
    private final Map<String, AmlRule> rulesByCategory;

    public RuleEngineService(AlertRuleDao alertRuleDao,
                             TxnDao txnDao,
                             DataSource dataSource,
                             List<AmlRule> rules) {
        this.alertRuleDao = alertRuleDao;
        this.txnDao = txnDao;
        this.dataSource = dataSource;
        this.rulesByCategory = rules.stream()
                .collect(Collectors.toMap(
                        AmlRule::getSupportedCategory,
                        r -> r,
                        (existing, replacement) -> existing));
    }

    /**
     * Main entry point — screens a single transaction against all active rules.
     * Returns the list of alert IDs raised (empty if no rules fired).
     */
    @Transactional
    public List<Long> screenTransaction(Txn txn, Account account) {

        // 1. load all active rules from DB via DAO
        List<AlertRule> activeRules = alertRuleDao.findActiveRules();

        // 2. find the longest lookback window needed across all rules
        int maxLookback = activeRules.stream()
                .mapToInt(r -> r.getLookbackDays() != null ? r.getLookbackDays() : 30)
                .max()
                .orElse(30);

        // 3. fetch recent transactions for the account within that window
        //    txn.getTxnId() is Integer — passed directly to TxnDao
        List<Txn> recentTxns = txnDao.findRecentByAccount(
                account.getAccountId(), txn.getTxnId(), maxLookback);

        // 4. build the shared context object for all rule evaluations
        RuleContext context = RuleContext.builder()
                .txn(txn)
                .account(account)
                .recentTxns(recentTxns)
                .build();

        // 5. evaluate each rule, raise an alert for each match
        List<Long> alertIds = activeRules.stream()
                .map(rule -> evaluateRule(context, rule))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(match -> callRaiseAlert(txn.getTxnId(), match))
                .filter(id -> id > 0)
                .collect(Collectors.toList());

        // 6. mark the transaction as screened
        callScreenTransaction(txn.getTxnId());

        log.info("Screened txn {} — {} alert(s) raised", txn.getTxnRef(), alertIds.size());
        return alertIds;
    }

    // ----------------------------------------------------------------
    // private helpers
    // ----------------------------------------------------------------

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