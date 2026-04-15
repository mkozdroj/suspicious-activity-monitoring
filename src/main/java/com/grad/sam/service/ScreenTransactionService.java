package com.grad.sam.service;


import com.grad.sam.model.Account;
import com.grad.sam.model.Alert;
import com.grad.sam.model.Txn;
import com.grad.sam.repository.AlertRepository;
import com.grad.sam.repository.TxnRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for screening a single transaction against all active AML alert rules.
 *
 * <p>Orchestrates the rule engine to evaluate THRESHOLD, VELOCITY, PATTERN (round-number),
 * and WATCHLIST rules, collecting every alert raised.  Acts as the primary entry point for
 * automated transaction surveillance.</p>
 *
 * <p>Domain context: In AML compliance, every transaction must be screened before settlement.
 * This service implements the "screen_transaction" stored-procedure contract at the Java layer,
 * delegating rule evaluation to {@link RuleEngineService}.</p>
 */
@Data
@Slf4j
@Service
public class ScreenTransactionService {

    private final TxnRepository txnRepository;
    private final AlertRepository alertRepository;
    private final RuleEngineService ruleEngineService;

    public ScreenTransactionService(TxnRepository txnRepository, AlertRepository alertRepository, RuleEngineService ruleEngineService) {
        this.txnRepository = txnRepository;
        this.alertRepository = alertRepository;
        this.ruleEngineService = ruleEngineService;
    }

    /**
     * Screens a transaction against all active alert rules and returns any triggered alerts.
     *
     * <p>The engine evaluates rules in the following order:
     * <ol>
     *   <li>THRESHOLD — single-transaction amount breach</li>
     *   <li>VELOCITY   — transaction count within a rolling window</li>
     *   <li>PATTERN    — round-number / structuring indicators</li>
     *   <li>WATCHLIST  — name fuzzy-match against sanctioned entities</li>
     * </ol>
     * Each matching rule raises an alert via the {@code raise_alert} stored procedure.</p>
     *
     * @param txnId the ID of the transaction to screen
     * @return list of alerts triggered; empty if no rules fired or transaction not found
     */
    @Transactional
    public List<Alert> screenTransaction(Integer txnId) {

        Txn txn = txnRepository.findById(txnId).orElse(null);
        if (txn == null) {
            log.warn("Transaction not found: {}", txnId);
            return List.of();
        }

        Account account = txn.getAccount();

        log.info("Screening txn {} ({}) on account {}",
                txn.getTxnRef(), txnId, account.getAccountNumber());

        List<Long> alertIds = ruleEngineService.screenTransaction(txn, account);

        if (alertIds.isEmpty()) {
            log.info("Txn {} passed all rules — no alerts raised", txn.getTxnRef());
            return List.of();
        }

        List<Alert> triggeredAlerts = alertIds.stream()
                .map(id -> alertRepository.findById(id.intValue()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();

        log.info("Txn {} triggered {} alert(s): {}",
                txn.getTxnRef(),
                triggeredAlerts.size(),
                alertIds);

        return triggeredAlerts;
    }
}
