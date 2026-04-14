package com.grad.sam.dao;

import com.grad.sam.model.Txn;
import com.grad.sam.repository.TxnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TxnDaoTest {

    @Mock
    private TxnRepository txnRepository;

    private TxnDao txnDao;

    @BeforeEach
    void setUp() {
        txnDao = new TxnDao(txnRepository);
    }

    @Test
    void findRecentByAccount_calculates_from_date_and_calls_repository() {
        Integer accountId = 1;
        Integer excludeTxnId = 42;
        int lookbackDays = 30;

        List<Txn> expected = List.of(new Txn());
        when(txnRepository.findRecentByAccount(anyInt(), anyInt(), any(LocalDate.class)))
                .thenReturn(expected);

        List<Txn> result = txnDao.findRecentByAccount(accountId, excludeTxnId, lookbackDays);

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);

        verify(txnRepository).findRecentByAccount(eq(accountId), eq(excludeTxnId), fromCaptor.capture());
        assertEquals(LocalDate.now().minusDays(30), fromCaptor.getValue());
        assertEquals(expected, result);
    }

    @Test
    void findById_returns_repository_result() {
        Txn txn = new Txn();
        txn.setTxnId(42);

        when(txnRepository.findById(42)).thenReturn(Optional.of(txn));

        Optional<Txn> result = txnDao.findById(42);

        assertTrue(result.isPresent());
        assertEquals(42, result.get().getTxnId());
        verify(txnRepository).findById(42);
    }

    @Test
    void findByTxnRef_returns_repository_result() {
        Txn txn = new Txn();
        txn.setTxnRef("TXN-001");

        when(txnRepository.findByTxnRef("TXN-001")).thenReturn(Optional.of(txn));

        Optional<Txn> result = txnDao.findByTxnRef("TXN-001");

        assertTrue(result.isPresent());
        assertEquals("TXN-001", result.get().getTxnRef());
        verify(txnRepository).findByTxnRef("TXN-001");
    }

    @Test
    void findByAccount_returns_repository_result() {
        Integer accountId = 1;
        List<Txn> expected = List.of(new Txn(), new Txn());

        when(txnRepository.findByAccount_AccountId(accountId)).thenReturn(expected);

        List<Txn> result = txnDao.findByAccount(accountId);

        assertEquals(expected, result);
        verify(txnRepository).findByAccount_AccountId(accountId);
    }

    @Test
    void findByAccountAndDateRange_returns_repository_result() {
        Integer accountId = 1;
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);
        List<Txn> expected = List.of(new Txn(), new Txn());

        when(txnRepository.findByAccount_AccountIdAndTxnDateBetween(accountId, from, to))
                .thenReturn(expected);

        List<Txn> result = txnDao.findByAccountAndDateRange(accountId, from, to);

        assertEquals(expected, result);
        verify(txnRepository).findByAccount_AccountIdAndTxnDateBetween(accountId, from, to);
    }

    @Test
    void findLargeTransactions_returns_repository_result() {
        BigDecimal minAmountUsd = new BigDecimal("10000.00");
        List<Txn> expected = List.of(new Txn());

        when(txnRepository.findByAmountUsdGreaterThanEqualAndStatus(minAmountUsd, "COMPLETED"))
                .thenReturn(expected);

        List<Txn> result = txnDao.findLargeTransactions(minAmountUsd);

        assertEquals(expected, result);
        verify(txnRepository).findByAmountUsdGreaterThanEqualAndStatus(minAmountUsd, "COMPLETED");
    }

    @Test
    void findByCounterpartyCountry_returns_repository_result() {
        String country = "PL";
        List<Txn> expected = List.of(new Txn());

        when(txnRepository.findByCounterpartyCountry(country)).thenReturn(expected);

        List<Txn> result = txnDao.findByCounterpartyCountry(country);

        assertEquals(expected, result);
        verify(txnRepository).findByCounterpartyCountry(country);
    }

    @Test
    void save_returns_saved_txn() {
        Txn txn = new Txn();
        txn.setTxnId(42);
        txn.setTxnRef("TXN-001");
        txn.setAmountUsd(new BigDecimal("1000.00"));

        when(txnRepository.save(txn)).thenReturn(txn);

        Txn result = txnDao.save(txn);

        assertNotNull(result);
        assertEquals(42, result.getTxnId());
        assertEquals("TXN-001", result.getTxnRef());
        verify(txnRepository).save(txn);
    }
}