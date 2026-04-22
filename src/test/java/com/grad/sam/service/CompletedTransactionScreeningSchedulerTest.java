package com.grad.sam.service;

import com.grad.sam.enums.TxnStatus;
import com.grad.sam.model.Txn;
import com.grad.sam.repository.TxnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompletedTransactionScreeningSchedulerTest {

    @Mock
    private TxnRepository txnRepository;

    @Mock
    private TxnService txnService;

    @Mock
    private ScreenTransactionService screenTransactionService;

    private CompletedTransactionScreeningScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new CompletedTransactionScreeningScheduler(
                txnRepository,
                txnService,
                screenTransactionService
        );
    }

    @Test
    void screenCompletedTransactions_returns_early_when_no_completed_transactions_exist() {
        when(txnRepository.findByStatus(TxnStatus.COMPLETED)).thenReturn(List.of());

        scheduler.screenCompletedTransactions();

        verify(txnRepository).findByStatus(TxnStatus.COMPLETED);
        verify(txnService, never()).claimCompletedForScreening(org.mockito.ArgumentMatchers.anyInt());
        verify(screenTransactionService, never()).screenTransaction(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void screenCompletedTransactions_screens_claimed_completed_transaction() {
        Txn txn = buildTxn(41, "TXN-041");
        when(txnRepository.findByStatus(TxnStatus.COMPLETED)).thenReturn(List.of(txn));
        when(txnService.claimCompletedForScreening(41)).thenReturn(true);

        scheduler.screenCompletedTransactions();

        verify(txnService).claimCompletedForScreening(41);
        verify(screenTransactionService).screenTransaction(41);
        verify(txnService, never()).returnClaimToCompleted(41);
    }

    @Test
    void screenCompletedTransactions_skips_transaction_when_claim_fails() {
        Txn txn = buildTxn(42, "TXN-042");
        when(txnRepository.findByStatus(TxnStatus.COMPLETED)).thenReturn(List.of(txn));
        when(txnService.claimCompletedForScreening(42)).thenReturn(false);

        scheduler.screenCompletedTransactions();

        verify(txnService).claimCompletedForScreening(42);
        verify(screenTransactionService, never()).screenTransaction(42);
        verify(txnService, never()).returnClaimToCompleted(42);
    }

    @Test
    void screenCompletedTransactions_returns_claim_when_screening_throws() {
        Txn txn = buildTxn(43, "TXN-043");
        when(txnRepository.findByStatus(TxnStatus.COMPLETED)).thenReturn(List.of(txn));
        when(txnService.claimCompletedForScreening(43)).thenReturn(true);
        doThrow(new RuntimeException("screening failed")).when(screenTransactionService).screenTransaction(43);

        scheduler.screenCompletedTransactions();

        verify(screenTransactionService).screenTransaction(43);
        verify(txnService).returnClaimToCompleted(43);
    }

    @Test
    void screenCompletedTransactions_continues_processing_other_transactions_after_failure() {
        Txn failedTxn = buildTxn(51, "TXN-051");
        Txn successfulTxn = buildTxn(52, "TXN-052");
        when(txnRepository.findByStatus(TxnStatus.COMPLETED)).thenReturn(List.of(failedTxn, successfulTxn));
        when(txnService.claimCompletedForScreening(51)).thenReturn(true);
        when(txnService.claimCompletedForScreening(52)).thenReturn(true);
        doThrow(new RuntimeException("boom")).when(screenTransactionService).screenTransaction(51);

        scheduler.screenCompletedTransactions();

        verify(screenTransactionService).screenTransaction(51);
        verify(txnService).returnClaimToCompleted(51);
        verify(screenTransactionService).screenTransaction(52);
    }

    private Txn buildTxn(int id, String ref) {
        Txn txn = new Txn();
        txn.setTxnId(id);
        txn.setTxnRef(ref);
        txn.setStatus(TxnStatus.COMPLETED);
        return txn;
    }
}
