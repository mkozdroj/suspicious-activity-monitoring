package com.grad.sam.repository;

import com.grad.sam.model.Txn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TxnRepository extends JpaRepository<Txn, Integer> {

    Optional<Txn> findByTxnRef(String txnRef);

    boolean existsByTxnRef(String txnRef);

    List<Txn> findByAccount_AccountId(Integer accountId);

    List<Txn> findByAccount_AccountIdAndTxnDateBetween(Integer accountId, LocalDate from, LocalDate to);

    List<Txn> findByAccount_AccountIdAndStatus(Integer accountId, String status);

    List<Txn> findByStatus(String status);

    List<Txn> findByTxnType(String txnType);

    List<Txn> findByDirection(String direction);

    List<Txn> findByCounterpartyCountry(String counterpartyCountry);

    List<Txn> findByAmountUsdGreaterThanEqualAndStatus(BigDecimal minAmountUsd, String status);

    @Query("SELECT t FROM Txn t WHERE t.account.accountId = :accountId " +
            "AND t.txnId <> :excludeTxnId " +
            "AND t.txnDate >= :from " +
            "AND t.status = 'COMPLETED' " +
            "ORDER BY t.txnDate DESC")
    List<Txn> findRecentByAccount(@Param("accountId") Integer accountId,
                                  @Param("excludeTxnId") Integer excludeTxnId,
                                  @Param("from") LocalDate from);
}