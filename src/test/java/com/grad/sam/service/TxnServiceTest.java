package com.grad.sam.service;

import com.grad.sam.enums.TxnStatus;
import com.grad.sam.exception.DataNotFoundException;
import com.grad.sam.model.Txn;
import com.grad.sam.repository.TxnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TxnServiceTest {

    @Mock
    private TxnRepository txnRepository;

    private TxnService service;

    @BeforeEach
    void setUp() {
        service = new TxnService(txnRepository);
    }

    @Test
    void saveTxn_persists_transaction_via_repository() {
        Txn txn = buildTxn(7, "TXN-007", TxnStatus.PENDING);
        when(txnRepository.save(txn)).thenReturn(txn);

        Txn saved = service.saveTxn(txn);

        assertSame(txn, saved);
        verify(txnRepository).save(txn);
    }

    @Test
    void getStatus_returns_status_name_for_existing_txn() {
        Txn txn = buildTxn(9, "TXN-009", TxnStatus.SCREENED);
        when(txnRepository.findById(9)).thenReturn(Optional.of(txn));

        String status = service.getStatus(9);

        assertEquals("SCREENED", status);
        verify(txnRepository).findById(9);
    }

    @Test
    void getStatus_throws_when_txn_missing() {
        when(txnRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(DataNotFoundException.class, () -> service.getStatus(99));
    }

    @Test
    void updateTxnStatus_updates_status_via_repository_query() {
        when(txnRepository.updateStatus(11, TxnStatus.BLOCKED)).thenReturn(1);

        service.updateTxnStatus(11, TxnStatus.BLOCKED);

        verify(txnRepository).updateStatus(11, TxnStatus.BLOCKED);
    }

    @Test
    void updateTxnStatus_throws_when_no_rows_updated() {
        when(txnRepository.updateStatus(42, TxnStatus.SCREENED)).thenReturn(0);

        assertThrows(DataNotFoundException.class,
                () -> service.updateTxnStatus(42, TxnStatus.SCREENED));
    }

    private Txn buildTxn(Integer id, String ref, TxnStatus status) {
        Txn txn = new Txn();
        txn.setTxnId(id);
        txn.setTxnRef(ref);
        txn.setStatus(status);
        return txn;
    }
}
