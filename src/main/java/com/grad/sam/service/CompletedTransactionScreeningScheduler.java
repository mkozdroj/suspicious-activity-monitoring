package com.grad.sam.service;

import com.grad.sam.enums.TxnStatus;
import com.grad.sam.model.Txn;
import com.grad.sam.repository.TxnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompletedTransactionScreeningScheduler {

    private final TxnRepository txnRepository;
    private final TxnService txnService;
    private final ScreenTransactionService screenTransactionService;

    @Scheduled(
            fixedDelayString = "${sam.screening.fixed-delay-ms:10000}",
            initialDelayString = "${sam.screening.initial-delay-ms:10000}"
    )
    public void screenCompletedTransactions() {
        log.info("Scheduled screening tick started");

        List<Txn> completedTransactions = txnRepository.findByStatus(TxnStatus.COMPLETED);

        if (completedTransactions.isEmpty()) {
            log.info("No COMPLETED transactions found for scheduled screening");
            return;
        }

        log.info("Scheduled screening picked up {} COMPLETED transaction(s)", completedTransactions.size());

        for (Txn txn : completedTransactions) {
            Integer txnId = txn.getTxnId();

            if (!txnService.claimCompletedForScreening(txnId)) {
                continue;
            }

            try {
                screenTransactionService.screenTransaction(txnId);
            } catch (Exception ex) {
                log.error("Scheduled screening failed for txn {} ({}): {}",
                        txnId,
                        txn.getTxnRef(),
                        ex.getMessage(),
                        ex);
                txnService.returnClaimToCompleted(txnId);
            }
        }

        log.info("Scheduled screening tick finished");
    }
}
