package com.grad.sam.repository;

import com.grad.sam.enums.TxnDirection;
import com.grad.sam.enums.TxnStatus;
import com.grad.sam.enums.TxnType;
import com.grad.sam.model.Txn;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TxnRepository extends JpaRepository<Txn, Integer> {

    default List<Txn> findRecentByAccount(Integer accountId, Integer excludeTxnId, int lookbackDays) {
        LocalDate from = LocalDate.now().minusDays(lookbackDays);
        return findRecentByAccount(accountId, excludeTxnId, from);
    }

    Optional<Txn> findByTxnRef(String txnRef);

    boolean existsByTxnRef(String txnRef);

    List<Txn> findByAccount_AccountId(Integer accountId);

    List<Txn> findByAccount_AccountIdAndTxnDateBetween(Integer accountId, LocalDate from, LocalDate to);

    List<Txn> findByAccount_AccountIdAndStatus(Integer accountId, TxnStatus status);

    List<Txn> findByStatus(TxnStatus status);

    List<Txn> findByTxnType(TxnType txnType);

    List<Txn> findByDirection(TxnDirection direction);

    List<Txn> findByCounterpartyCountry(String counterpartyCountry);

    List<Txn> findByAmountUsdGreaterThanEqualAndStatus(BigDecimal minAmountUsd, TxnStatus status);

    @Query("SELECT t FROM Txn t WHERE t.account.accountId = :accountId " +
            "AND t.txnId <> :excludeTxnId " +
            "AND t.txnDate >= :from " +
            "AND t.status = 'COMPLETED' " +
            "ORDER BY t.txnDate DESC")
    List<Txn> findRecentByAccount(@Param("accountId") Integer accountId,
                                  @Param("excludeTxnId") Integer excludeTxnId,
                                  @Param("from") LocalDate from);

    @Modifying
    @Transactional
    @Query("UPDATE Txn t SET t.status = :status WHERE t.txnId = :txnId")
    int updateStatus(@Param("txnId") Integer txnId,
                     @Param("status") TxnStatus status);

    default String getStatus(Integer txnId) {
        return findById(txnId)
                .map(txn -> txn.getStatus().name())
                .orElseThrow(() -> new IllegalArgumentException("Transaction " + txnId + " not found."));
    }

    default Txn saveTxn(Txn txn) {
        Logger log = LoggerFactory.getLogger(TxnRepository.class);
        Txn saved = save(txn);
        log.info("Saved txn: {} (id: {})", saved.getTxnRef(), saved.getTxnId());
        return saved;
    }

    default void updateTxnStatus(Integer txnId, TxnStatus status) {
        Logger log = LoggerFactory.getLogger(TxnRepository.class);
        int rows = updateStatus(txnId, status);
        if (rows > 0) {
            log.info("Updated txn {} status to {}", txnId, status);
        } else {
            log.warn("Transaction {} not found for status update", txnId);
        }
    }
}