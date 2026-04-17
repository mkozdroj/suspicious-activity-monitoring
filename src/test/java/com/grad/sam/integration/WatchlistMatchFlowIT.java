package com.grad.sam.integration;

import com.grad.sam.enums.*;
import com.grad.sam.model.*;
import com.grad.sam.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Integration test: Watchlist → Transaction → WatchlistMatch
// Tests the full watchlist screening flow:
// a transaction is matched against a sanctioned entity and the
// match is stored with a score and status.

@DataJpaTest
@ActiveProfiles("test")
class WatchlistMatchFlowIT {

    @Autowired private CustomerRepository customerRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TxnRepository txnRepository;
    @Autowired private WatchlistRepository watchlistRepository;
    @Autowired private WatchlistMatchRepository watchlistMatchRepository;

    private Account account;
    private Txn txn;
    private Watchlist sanctionedEntity;

    @BeforeEach
    void setUp() {
        // Customer and account setup
        Customer customer = new Customer();
        customer.setCustomerRef("CUST-WL-001");
        customer.setFullName("Test User");
        customer.setNationality("PL");
        customer.setCountryOfResidence("PL");
        customer.setCustomerType(CustomerType.INDIVIDUAL);
        customer.setRiskRating(RiskRating.LOW);
        customer.setKycStatus(KycStatus.VERIFIED);
        customer.setOnboardedDate(LocalDate.now());
        customer.setIsPep(false);
        customer.setIsActive(true);
        customer = customerRepository.save(customer);

        account = new Account();
        account.setAccountNumber("ACC-WL-001");
        account.setAccountType(AccountType.CURRENT);
        account.setCurrency("USD");
        account.setBalance(BigDecimal.ZERO);
        account.setOpenedDate(LocalDate.now());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBranchCode("LDN-01");
        account.setCustomer(customer);
        account = accountRepository.save(account);

        // Transaction going to a sanctioned country
        txn = new Txn();
        txn.setTxnRef("TXN-WL-001");
        txn.setTxnType(TxnType.WIRE);
        txn.setDirection(TxnDirection.DR);
        txn.setAmount(new BigDecimal("25000.00"));
        txn.setCurrency("USD");
        txn.setAmountUsd(new BigDecimal("25000.00"));
        txn.setTxnDate(LocalDate.now());
        txn.setValueDate(LocalDate.now());
        txn.setStatus(TxnStatus.COMPLETED);
        txn.setCounterpartyCountry("IR");
        txn.setCounterpartyAccount("IR-ACC-99999");
        txn.setCounterpartyBank("Bank Melli Iran");
        txn.setAccount(account);
        txn = txnRepository.save(txn);

        // Sanctioned entity on OFAC list
        sanctionedEntity = new Watchlist();
        sanctionedEntity.setListType(ListType.OFAC);
        sanctionedEntity.setEntityName("Bank Melli Iran");
        sanctionedEntity.setEntityType(WatchlistEntityType.INDIVIDUAL);
        sanctionedEntity.setCountry("IR");
        sanctionedEntity.setReason("Designated under Iran sanctions programme");
        sanctionedEntity.setListedDate(LocalDate.of(2010, 6, 16));
        sanctionedEntity.setIsActive(true);
        sanctionedEntity = watchlistRepository.save(sanctionedEntity);
    }

    @Test
    void watchlist_entry_is_persisted_and_active_entries_queryable() {
        // Add an inactive entry
        Watchlist inactive = new Watchlist();
        inactive.setListType(ListType.UN);
        inactive.setEntityName("Old Delisted Entity");
        inactive.setEntityType(WatchlistEntityType.INDIVIDUAL);
        inactive.setReason("Removed from list");
        inactive.setListedDate(LocalDate.of(2005, 1, 1));
        inactive.setIsActive(false);
        watchlistRepository.save(inactive);

        List<Watchlist> active = watchlistRepository.findByIsActive(true);
        List<Watchlist> ofac = watchlistRepository.findByListType(WatchlistListType.OFAC);

        assertEquals(1, active.size());
        assertEquals("Bank Melli Iran", active.get(0).getEntityName());
        assertEquals(1, ofac.size());
    }

    @Test
    void watchlist_partial_name_search_returns_match() {
        List<Watchlist> results = watchlistRepository.findByEntityNameContainingIgnoreCase("bank melli");

        assertEquals(1, results.size());
        assertEquals("OFAC", results.get(0).getListType());
    }

    @Test
    void watchlist_match_is_created_and_linked_to_txn_and_watchlist() {
        WatchlistMatch match = new WatchlistMatch();
        match.setTxn(txn);
        match.setWatchlist(sanctionedEntity);
        match.setMatchType(MatchType.NAME);
        match.setMatchScore(new BigDecimal("95.50"));
        match.setMatchedField("counterparty_bank");
        match.setMatchedValue("Bank Melli Iran");
        match.setStatus(MatchStatus.PENDING);
        match = watchlistMatchRepository.save(match);

        assertNotNull(match.getMatchId());

        List<WatchlistMatch> byTxn = watchlistMatchRepository.findByTxn_TxnId(txn.getTxnId());
        assertEquals(1, byTxn.size());
        assertEquals("NAME", byTxn.get(0).getMatchType());
        assertEquals(0, new BigDecimal("95.50").compareTo(byTxn.get(0).getMatchScore()));
    }

    @Test
    void match_status_can_be_updated_to_confirmed() {
        WatchlistMatch match = new WatchlistMatch();
        match.setTxn(txn);
        match.setWatchlist(sanctionedEntity);
        match.setMatchType(MatchType.ACCOUNT);
        match.setMatchScore(new BigDecimal("88.00"));
        match.setMatchedField("counterparty_account");
        match.setMatchedValue("IR-ACC-99999");
        match.setStatus(MatchStatus.PENDING);
        match = watchlistMatchRepository.save(match);

        // Analyst reviews and confirms
        match.setStatus(MatchStatus.CONFIRMED);
        match.setReviewedBy("analyst@bank.com");
        match.setReviewedAt(LocalDateTime.now());
        watchlistMatchRepository.save(match);

        List<WatchlistMatch> confirmed = watchlistMatchRepository.findByStatus(MatchStatus.CONFIRMED);
        assertEquals(1, confirmed.size());
        assertEquals("analyst@bank.com", confirmed.get(0).getReviewedBy());
    }

    @Test
    void false_positive_match_is_excluded_from_pending_results() {
        WatchlistMatch realMatch = buildMatch("NAME", "95.00", "PENDING");
        WatchlistMatch falsePositive = buildMatch("FUZZY_NAME", "55.00", "FALSE_POSITIVE");
        watchlistMatchRepository.save(realMatch);
        watchlistMatchRepository.save(falsePositive);

        List<WatchlistMatch> pending = watchlistMatchRepository.findByStatus(MatchStatus.PENDING);
        List<WatchlistMatch> fps = watchlistMatchRepository.findByStatus(MatchStatus.FALSE_POSITIVE);

        assertEquals(1, pending.size());
        assertEquals(1, fps.size());
    }

    @Test
    void multiple_matches_for_same_transaction_are_stored() {
        // Same txn can match multiple watchlist entries (e.g. NAME + COUNTRY)
        Watchlist secondEntry = new Watchlist();
        secondEntry.setListType(ListType.EU);
        secondEntry.setEntityName("Iran Central Bank");
        secondEntry.setEntityType(WatchlistEntityType.ENTITY);
        secondEntry.setCountry("IR");
        secondEntry.setReason("EU sanctions");
        secondEntry.setListedDate(LocalDate.of(2012, 3, 23));
        secondEntry.setIsActive(true);
        secondEntry = watchlistRepository.save(secondEntry);

        watchlistMatchRepository.save(buildMatch("NAME", "95.00", "PENDING"));

        WatchlistMatch countryMatch = new WatchlistMatch();
        countryMatch.setTxn(txn);
        countryMatch.setWatchlist(secondEntry);
        countryMatch.setMatchType(MatchType.COUNTRY);
        countryMatch.setMatchScore(new BigDecimal("70.00"));
        countryMatch.setMatchedField("counterparty_country");
        countryMatch.setMatchedValue("IR");
        countryMatch.setStatus(MatchStatus.PENDING);
        watchlistMatchRepository.save(countryMatch);

        List<WatchlistMatch> allForTxn = watchlistMatchRepository.findByTxn_TxnId(txn.getTxnId());
        assertEquals(2, allForTxn.size());
    }

    // Helper methods

    private WatchlistMatch buildMatch(String matchType, String score, String status) {
        WatchlistMatch m = new WatchlistMatch();
        m.setTxn(txn);
        m.setWatchlist(sanctionedEntity);
        m.setMatchType(MatchType.valueOf(matchType));
        m.setMatchScore(new BigDecimal(score));
        m.setMatchedField("counterparty_bank");
        m.setMatchedValue("Bank Melli Iran");
        m.setStatus(MatchStatus.valueOf(status));
        return m;
    }
}
