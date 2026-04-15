package com.grad.sam.dao;

import com.grad.sam.model.Txn;
import com.grad.sam.repository.TxnRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class TxnDao {

    private final TxnRepository txnRepository;

    public TxnDao(TxnRepository txnRepository) {
        this.txnRepository = txnRepository;
    }

    /** Used by RuleEngineService — fetches recent completed transactions
     *  for an account within a lookback window, excluding the current txn. */
    public List<Txn> findRecentByAccount(Integer accountId, Integer excludeTxnId, int lookbackDays) {
        LocalDate from = LocalDate.now().minusDays(lookbackDays);
        return txnRepository.findRecentByAccount(accountId, excludeTxnId, from);
    }

    public Optional<Txn> findById(Integer txnId) {
        return txnRepository.findById(txnId);
    }

    public Optional<Txn> findByTxnRef(String txnRef) {
        return txnRepository.findByTxnRef(txnRef);
    }

    public List<Txn> findByAccount(Integer accountId) {
        return txnRepository.findByAccount_AccountId(accountId);
    }

    public List<Txn> findByAccountAndDateRange(Integer accountId, LocalDate from, LocalDate to) {
        return txnRepository.findByAccount_AccountIdAndTxnDateBetween(accountId, from, to);
    }

    public List<Txn> findLargeTransactions(BigDecimal minAmountUsd) {
        return txnRepository.findByAmountUsdGreaterThanEqualAndStatus(minAmountUsd, "COMPLETED");
    }

    public List<Txn> findByCounterpartyCountry(String country) {
        return txnRepository.findByCounterpartyCountry(country);
    }

    public String getStatus(Integer txnId) {
        return txnRepository.findById(txnId)
                .map(Txn::getStatus)
                .orElseThrow(() -> new IllegalArgumentException("Transaction " + txnId + " not found."));
    }

    public Txn save(Txn txn) {
        Txn saved = txnRepository.save(txn);
        log.info("Saved txn: {} (id: {})", saved.getTxnRef(), saved.getTxnId());
        return saved;
    }

    @Transactional
    public void updateStatus(Integer txnId, String status) {
        int rows = txnRepository.updateStatus(txnId, status);
        if (rows > 0) {
            log.info("Updated txn {} status to {}", txnId, status);
        } else {
            log.warn("Transaction {} not found for status update", txnId);
        }
    }
}