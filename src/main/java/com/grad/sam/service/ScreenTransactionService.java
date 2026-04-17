package com.grad.sam.service;

import com.grad.sam.enums.TxnStatus;
import com.grad.sam.exception.DataNotFoundException;
import com.grad.sam.exception.InvalidInputException;
import com.grad.sam.model.Account;
import com.grad.sam.model.Alert;
import com.grad.sam.model.Txn;
import com.grad.sam.repository.AlertRepository;
import com.grad.sam.repository.TxnRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
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
@Validated
public class ScreenTransactionService {

    private final TxnRepository txnRepository;
    private final AlertRepository alertRepository;
    private final RuleEngineService ruleEngineService;

    public ScreenTransactionService(TxnRepository txnRepository,
                                    AlertRepository alertRepository,
                                    RuleEngineService ruleEngineService) {
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
     * @param txnId the ID of the transaction to screen; must be a non-null positive integer
     * @return list of alerts triggered; empty if no rules fired
     * @throws jakarta.validation.ConstraintViolationException if {@code txnId} is null or non-positive
     * @throws DataNotFoundException                           if no transaction exists for the given ID
     * @throws InvalidInputException                           if the transaction status is not screenable
     *                                                         or {@code amountUsd} is null or non-positive
     */
    @Transactional
    public List<Alert> screenTransaction(@NotNull @Positive Integer txnId) {

        Txn txn = txnRepository.findById(txnId)
                .orElseThrow(() -> new DataNotFoundException(
                        "Transaction not found for id: " + txnId));

        validateTxnStatus(txn);
        validateTxnAmount(txn);

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

    // -------------------------------------------------------------------------
    // Private validation helpers
    // -------------------------------------------------------------------------

    /**
     * Validates that the transaction is in a status eligible for AML screening.
     *
     * <p>Delegates to {@link TxnStatus#isScreenable()} — only {@code COMPLETED}
     * and {@code PENDING} transactions pass this check.  {@code REVERSED},
     * {@code FAILED}, {@code SCREENED}, and {@code BLOCKED} transactions are
     * excluded because they either never moved funds or have already been handled.</p>
     *
     * @param txn the resolved transaction entity
     * @throws InvalidInputException if the transaction status is not screenable
     */
    private void validateTxnStatus(Txn txn) {
        TxnStatus status = txn.getStatus();
        if (status == null || !status.isScreenable()) {
            throw new InvalidInputException(
                    String.format("Transaction %s (id=%d) has status %s and is not eligible for screening. " +
                                    "Only %s and %s transactions may be screened.",
                            txn.getTxnRef(), txn.getTxnId(), status,
                            TxnStatus.COMPLETED, TxnStatus.PENDING));
        }
    }

    /**
     * Validates that the transaction carries a positive USD amount suitable for rule evaluation.
     *
     * <p>A null or zero {@code amountUsd} would cause threshold and velocity rules to produce
     * misleading results, so screening is blocked early.</p>
     *
     * @param txn the resolved transaction entity
     * @throws InvalidInputException if {@code amountUsd} is null or &lt;= 0
     */
    private void validateTxnAmount(Txn txn) {
        BigDecimal amountUsd = txn.getAmountUsd();
        if (amountUsd == null) {
            throw new InvalidInputException(
                    String.format("Transaction %s (id=%d) has a null amountUsd; cannot screen.",
                            txn.getTxnRef(), txn.getTxnId()));
        }
        if (amountUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException(
                    String.format("Transaction %s (id=%d) has a non-positive amountUsd (%s); cannot screen.",
                            txn.getTxnRef(), txn.getTxnId(), amountUsd.toPlainString()));
        }
    }
}
