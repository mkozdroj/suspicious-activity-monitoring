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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ScreenTransactionService {

    private final TxnRepository txnRepository;
    private final AlertRepository alertRepository;
    private final RuleEngineService ruleEngineService;


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
