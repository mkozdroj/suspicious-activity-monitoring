package com.grad.sam.repository;

import com.grad.sam.enums.*;
import com.grad.sam.model.Account;
import com.grad.sam.model.Customer;
import com.grad.sam.model.Txn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class TxnRepositoryTest {

    @Autowired
    private TxnRepository txnRepository;

    @Autowired
    private TestEntityManager em;

    private Account account;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setCustomerRef("CUST-001");
        customer.setFullName("John Doe");
        customer.setNationality("PL");
        customer.setCountryOfResidence("PL");
        customer.setCustomerType(CustomerType.INDIVIDUAL);
        customer.setRiskRating(RiskRating.LOW);
        customer.setKycStatus(KycStatus.VERIFIED);
        customer.setOnboardedDate(LocalDate.now().minusYears(1));
        customer.setIsActive(true);
        customer.setIsPep(false);
        customer = em.persistFlushFind(customer);

        account = new Account();
        account.setAccountNumber("ACC-001");
        account.setCustomer(customer);
        account.setCurrency("USD");
        account.setAccountType(AccountType.CURRENT);
        account.setStatus(AccountStatus.ACTIVE);
        account.setBranchCode("WAR001");
        account.setOpenedDate(LocalDate.now().minusMonths(6));
        account = em.persistFlushFind(account);
    }

    @Test
    void findRecentByAccount_should_return_only_completed_txns_from_lookback_window_and_exclude_current_txn() {
        Txn oldTxn = buildTxn("TXN-OLD", LocalDate.now().minusDays(40), TxnStatus.COMPLETED);
        em.persist(oldTxn);

        Txn includedTxn = buildTxn("TXN-IN", LocalDate.now().minusDays(5), TxnStatus.COMPLETED);
        em.persist(includedTxn);

        Txn excludedCurrent = buildTxn("TXN-CURRENT", LocalDate.now().minusDays(1), TxnStatus.COMPLETED);
        em.persist(excludedCurrent);

        Txn pendingTxn = buildTxn("TXN-PENDING", LocalDate.now().minusDays(2), TxnStatus.PENDING);
        em.persist(pendingTxn);

        em.flush();

        List<Txn> result = txnRepository.findRecentByAccount(
                account.getAccountId(),
                excludedCurrent.getTxnId(),
                30
        );

        assertEquals(1, result.size());
        assertEquals("TXN-IN", result.get(0).getTxnRef());
    }

    @Test
    void updateStatus_should_update_txn_status_and_return_affected_rows() {
        Txn txn = buildTxn("TXN-001", LocalDate.now(), TxnStatus.PENDING);
        txn = em.persistFlushFind(txn);

        int rows = txnRepository.updateStatus(txn.getTxnId(), TxnStatus.SCREENED);
        em.flush();
        em.clear();

        Txn updated = em.find(Txn.class, txn.getTxnId());

        assertEquals(1, rows);
        assertEquals(TxnStatus.SCREENED, updated.getStatus());
    }

    @Test
    void getStatus_should_return_status_name_for_existing_txn() {
        Txn txn = buildTxn("TXN-002", LocalDate.now(), TxnStatus.COMPLETED);
        txn = em.persistFlushFind(txn);

        String status = txnRepository.findById(txn.getTxnId())
                .map(found -> found.getStatus().name())
                .orElseThrow();

        assertEquals("COMPLETED", status);
    }

    @Test
    void getStatus_should_throw_when_txn_not_found() {
        assertTrue(txnRepository.findById(99999).isEmpty());
    }

    private Txn buildTxn(String ref, LocalDate txnDate, TxnStatus status) {
        Txn txn = new Txn();
        txn.setTxnRef(ref);
        txn.setAccount(account);
        txn.setTxnDate(txnDate);
        txn.setValueDate(txnDate);
        txn.setStatus(status);
        txn.setTxnType(TxnType.CARD);
        txn.setDirection(TxnDirection.DR);
        txn.setAmount(new BigDecimal("1000.00"));
        txn.setAmountUsd(new BigDecimal("1000.00"));
        txn.setCurrency("USD");
        return txn;
    }

    @Test
    void saveTxn_should_persist_transaction() {
        Txn txn = buildTxn("TXN-SAVE-001", LocalDate.now(), TxnStatus.PENDING);

        Txn saved = txnRepository.saveAndFlush(txn);

        assertNotNull(saved.getTxnId());
        assertEquals("TXN-SAVE-001", saved.getTxnRef());
        assertEquals(TxnStatus.PENDING, saved.getStatus());
    }

    @Test
    void updateTxnStatus_should_update_status_when_txn_exists() {
        Txn txn = buildTxn("TXN-UPD-001", LocalDate.now(), TxnStatus.PENDING);
        txn = em.persistFlushFind(txn);

        txnRepository.updateStatus(txn.getTxnId(), TxnStatus.SCREENED);
        em.flush();
        em.clear();

        Txn updated = em.find(Txn.class, txn.getTxnId());
        assertEquals(TxnStatus.SCREENED, updated.getStatus());
    }
}
