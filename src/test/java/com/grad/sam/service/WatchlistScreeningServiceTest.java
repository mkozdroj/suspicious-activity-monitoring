package com.grad.sam.service;

import com.grad.sam.enums.ListType;
import com.grad.sam.enums.MatchStatus;
import com.grad.sam.enums.MatchType;
import com.grad.sam.enums.TxnStatus;
import com.grad.sam.enums.WatchlistEntityType;
import com.grad.sam.model.Txn;
import com.grad.sam.model.Watchlist;
import com.grad.sam.model.WatchlistMatch;
import com.grad.sam.repository.WatchlistMatchRepository;
import com.grad.sam.repository.WatchlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchlistScreeningServiceTest {

    @Mock private WatchlistRepository watchlistRepository;
    @Mock private WatchlistMatchRepository watchlistMatchRepository;
    @Mock private TxnService txnService;
    @Mock private AlertService alertService;

    private WatchlistScreeningService service;

    private Txn txn;

    private static final BigDecimal THRESHOLD = new BigDecimal("80.00");
    private static final BigDecimal EXACT_SCORE = new BigDecimal("100.00");
    private static final BigDecimal FUZZY_SCORE = new BigDecimal("85.00");

    @BeforeEach
    void setUp() {
        service = new WatchlistScreeningService(
                watchlistRepository, watchlistMatchRepository, txnService, alertService);

        txn = new Txn();
        txn.setTxnId(42);
        txn.setTxnRef("TXN-WL-001");
        txn.setAmount(new BigDecimal("5000.00"));
        txn.setAmountUsd(new BigDecimal("5000.00"));
        txn.setCurrency("USD");
        txn.setTxnDate(LocalDate.now());
        txn.setStatus(TxnStatus.COMPLETED);
    }

    // Happy path: no match

    @Test
    void marks_txn_as_SCREENED_when_no_watchlist_match() {
        when(watchlistRepository.findByIsActive(true)).thenReturn(Collections.emptyList());

        List<WatchlistMatch> result = service.screenCustomer("John Doe", THRESHOLD, txn);

        assertTrue(result.isEmpty());
        verify(txnService).updateTxnStatus(42, TxnStatus.SCREENED);
        verify(txnService, never()).updateTxnStatus(42, TxnStatus.BLOCKED);
        verify(alertService, never()).raiseAlert(any(), anyString(), anyString());
    }

    @Test
    void returns_empty_list_when_no_active_watchlist_entries() {
        when(watchlistRepository.findByIsActive(true)).thenReturn(Collections.emptyList());

        List<WatchlistMatch> result = service.screenCustomer("Anyone", THRESHOLD, txn);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void returns_empty_when_name_does_not_match_any_watchlist_entry() {
        Watchlist unrelated = buildWatchlist(1, "Osama Bin Laden");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(unrelated));

        List<WatchlistMatch> result = service.screenCustomer("Jane Smith", THRESHOLD, txn);

        assertTrue(result.isEmpty());
        verify(txnService).updateTxnStatus(42, TxnStatus.SCREENED);
    }

    // Exact match path

    @Test
    void blocks_transaction_on_exact_watchlist_match() {
        Watchlist sanctioned = buildWatchlist(1, "Vladimir Putin");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(sanctioned));

        List<WatchlistMatch> result = service.screenCustomer("Vladimir Putin", THRESHOLD, txn);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getMatchScore().compareTo(EXACT_SCORE));
        verify(txnService).updateTxnStatus(42, TxnStatus.PENDING);
        verify(txnService).updateTxnStatus(42, TxnStatus.BLOCKED);
        verify(alertService).raiseAlert(eq(42), eq("WATCHLIST"), anyString());
    }

    @Test
    void exact_match_is_case_insensitive() {
        Watchlist sanctioned = buildWatchlist(1, "KIM JONG UN");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(sanctioned));

        List<WatchlistMatch> result = service.screenCustomer("kim jong un", THRESHOLD, txn);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getMatchScore().compareTo(EXACT_SCORE));
        verify(txnService).updateTxnStatus(42, TxnStatus.BLOCKED);
    }

    @Test
    void exact_match_ignores_leading_and_trailing_whitespace() {
        Watchlist sanctioned = buildWatchlist(1, "  Ali Khamenei  ");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(sanctioned));

        List<WatchlistMatch> result = service.screenCustomer("   Ali Khamenei   ", THRESHOLD, txn);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getMatchScore().compareTo(EXACT_SCORE));
    }

    // Fuzzy match path

    @Test
    void fuzzy_match_when_customer_name_contains_watchlist_entry() {
        Watchlist sanctioned = buildWatchlist(1, "Putin");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(sanctioned));

        List<WatchlistMatch> result = service.screenCustomer("Vladimir Putin", THRESHOLD, txn);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getMatchScore().compareTo(FUZZY_SCORE));
        verify(txnService).updateTxnStatus(42, TxnStatus.PENDING);
        // Fuzzy must NOT block
        verify(txnService, never()).updateTxnStatus(42, TxnStatus.BLOCKED);
        verify(alertService, never()).raiseAlert(any(), anyString(), anyString());
    }

    @Test
    void fuzzy_match_when_watchlist_entry_contains_customer_name() {
        // Entry "ACME Trading Corp" contains customer "ACME"
        Watchlist sanctioned = buildWatchlist(1, "ACME Trading Corp");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(sanctioned));

        List<WatchlistMatch> result = service.screenCustomer("ACME", THRESHOLD, txn);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getMatchScore().compareTo(FUZZY_SCORE));
        assertEquals(MatchType.FUZZY_NAME, result.get(0).getMatchType());
    }

    @Test
    void fuzzy_match_substring_sets_PENDING_status_not_BLOCKED() {
        Watchlist entry = buildWatchlist(1, "ACME");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry));

        List<WatchlistMatch> result = service.screenCustomer("ACME Trading Corp", THRESHOLD, txn);

        assertEquals(1, result.size());
        assertEquals(MatchType.FUZZY_NAME, result.get(0).getMatchType());
        verify(txnService).updateTxnStatus(42, TxnStatus.PENDING);
        verify(txnService, never()).updateTxnStatus(42, TxnStatus.BLOCKED);
    }

    // Threshold filtering

    @Test
    void does_not_return_match_when_score_below_threshold() {
        // Threshold above fuzzy score (85) → fuzzy hits are filtered out
        Watchlist entry = buildWatchlist(1, "ACME");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry));

        List<WatchlistMatch> result =
                service.screenCustomer("ACME Corp", new BigDecimal("90.00"), txn);

        assertTrue(result.isEmpty());
        verify(txnService).updateTxnStatus(42, TxnStatus.SCREENED);
    }

    @Test
    void exact_match_still_returned_when_threshold_is_100() {
        Watchlist entry = buildWatchlist(1, "Exact Name");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry));

        List<WatchlistMatch> result =
                service.screenCustomer("Exact Name", new BigDecimal("100.00"), txn);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getMatchScore().compareTo(EXACT_SCORE));
    }

    @Test
    void fuzzy_match_filtered_out_when_threshold_equals_100() {
        Watchlist entry = buildWatchlist(1, "ACME");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry));

        List<WatchlistMatch> result =
                service.screenCustomer("ACME Trading", new BigDecimal("100.00"), txn);

        assertTrue(result.isEmpty());
    }

    // Multiple matches

    @Test
    void returns_multiple_matches_when_multiple_entries_hit() {
        Watchlist entry1 = buildWatchlist(1, "ACME");
        Watchlist entry2 = buildWatchlist(2, "ACME Trading Corp");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry1, entry2));

        List<WatchlistMatch> result = service.screenCustomer("ACME Trading Corp", THRESHOLD, txn);

        assertEquals(2, result.size());
        // entry2 is exact → BLOCKED should fire
        verify(txnService).updateTxnStatus(42, TxnStatus.BLOCKED);
        verify(alertService).raiseAlert(eq(42), eq("WATCHLIST"), anyString());
    }

    @Test
    void blocks_when_any_match_is_exact_even_if_others_are_fuzzy() {
        Watchlist fuzzyEntry = buildWatchlist(1, "ACME");
        Watchlist exactEntry = buildWatchlist(2, "ACME Trading Corp");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(fuzzyEntry, exactEntry));

        service.screenCustomer("ACME Trading Corp", THRESHOLD, txn);

        verify(txnService).updateTxnStatus(42, TxnStatus.BLOCKED);
    }

    // Persistence & field population

    @Test
    void saves_matches_via_repository() {
        Watchlist entry = buildWatchlist(1, "Vladimir Putin");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry));

        service.screenCustomer("Vladimir Putin", THRESHOLD, txn);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WatchlistMatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(watchlistMatchRepository).saveAll(captor.capture());

        List<WatchlistMatch> saved = captor.getValue();
        assertEquals(1, saved.size());
        WatchlistMatch m = saved.get(0);
        assertEquals(txn, m.getTxn());
        assertEquals(entry, m.getWatchlist());
        assertEquals(MatchType.FUZZY_NAME, m.getMatchType());
        assertEquals("entity_name", m.getMatchedField());
        assertEquals("Vladimir Putin", m.getMatchedValue());
        assertEquals(MatchStatus.PENDING, m.getStatus());
        assertEquals(0, m.getMatchScore().compareTo(EXACT_SCORE));
    }

    @Test
    void save_is_called_even_when_no_matches_found() {
        when(watchlistRepository.findByIsActive(true)).thenReturn(Collections.emptyList());

        service.screenCustomer("Nobody Famous", THRESHOLD, txn);

        verify(watchlistMatchRepository).saveAll(anyList());
    }

    // Active-entries-only contract

    @Test
    void only_queries_active_watchlist_entries() {
        when(watchlistRepository.findByIsActive(true)).thenReturn(Collections.emptyList());

        service.screenCustomer("John Doe", THRESHOLD, txn);

        verify(watchlistRepository).findByIsActive(true);
        verify(watchlistRepository, never()).findByIsActive(false);
        verify(watchlistRepository, never()).findAll();
    }

    // blockIfSanctioned direct tests

    @Test
    void blockIfSanctioned_blocks_when_exact_match_present() {
        WatchlistMatch exact = buildMatch(EXACT_SCORE);

        service.blockIfSanctioned(txn, List.of(exact));

        verify(txnService).updateTxnStatus(42, TxnStatus.BLOCKED);
        verify(alertService).raiseAlert(eq(42), eq("WATCHLIST"), anyString());
    }

    @Test
    void blockIfSanctioned_does_nothing_when_only_fuzzy_matches() {
        WatchlistMatch fuzzy = buildMatch(FUZZY_SCORE);

        service.blockIfSanctioned(txn, List.of(fuzzy));

        verify(txnService, never()).updateTxnStatus(any(), eq(TxnStatus.BLOCKED));
        verify(alertService, never()).raiseAlert(any(), anyString(), anyString());
    }

    @Test
    void blockIfSanctioned_does_nothing_on_empty_match_list() {
        service.blockIfSanctioned(txn, Collections.emptyList());

        verify(txnService, never()).updateTxnStatus(any(), eq(TxnStatus.BLOCKED));
        verify(alertService, never()).raiseAlert(any(), anyString(), anyString());
    }

    @Test
    void blockIfSanctioned_wraps_unexpected_errors_as_RuntimeException() {
        WatchlistMatch exact = buildMatch(EXACT_SCORE);

        doThrow(new RuntimeException("DB down"))
                .when(txnService).updateTxnStatus(42, TxnStatus.BLOCKED);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.blockIfSanctioned(txn, List.of(exact)));
        assertTrue(ex.getMessage().contains("Failed to evaluate sanction status"));
    }

    // AML domain scenarios

    @Test
    void sanctions_list_exact_hit_triggers_alert_with_WATCHLIST_type() {
        Watchlist ofacEntry = buildWatchlist(1, "BANK SADERAT IRAN");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(ofacEntry));

        service.screenCustomer("BANK SADERAT IRAN", THRESHOLD, txn);

        verify(alertService).raiseAlert(eq(42), eq("WATCHLIST"), anyString());
    }

    @Test
    void clean_customer_generates_no_alerts_and_no_blocks() {
        Watchlist entry = buildWatchlist(1, "Osama Bin Laden");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry));

        service.screenCustomer("Marta Kowalska", THRESHOLD, txn);

        verify(txnService).updateTxnStatus(42, TxnStatus.SCREENED);
        verify(alertService, never()).raiseAlert(any(), anyString(), anyString());
    }

    //  Additional edge cases & boundary conditions

    @Test
    void exact_score_at_threshold_boundary_is_included() {
        // Threshold equals exact score → exact match must still be included (>= comparison)
        Watchlist entry = buildWatchlist(1, "Name");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry));

        List<WatchlistMatch> result =
                service.screenCustomer("Name", new BigDecimal("100.00"), txn);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getMatchScore().compareTo(EXACT_SCORE));
    }

    @Test
    void fuzzy_score_at_threshold_boundary_is_included() {
        // Threshold equals fuzzy score → fuzzy match should still be included
        Watchlist entry = buildWatchlist(1, "ACME");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry));

        List<WatchlistMatch> result =
                service.screenCustomer("ACME Corp", new BigDecimal("85.00"), txn);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getMatchScore().compareTo(FUZZY_SCORE));
    }

    @Test
    void fuzzy_score_just_above_threshold_is_excluded() {
        Watchlist entry = buildWatchlist(1, "ACME");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry));

        List<WatchlistMatch> result =
                service.screenCustomer("ACME Corp", new BigDecimal("85.01"), txn);

        assertTrue(result.isEmpty());
    }

    // Current service behaviour on matchType & matchedValue

    @Test
    void match_type_is_always_FUZZY_NAME_even_for_exact_match() {
        // Documents current behaviour: service labels every match as FUZZY_NAME
        Watchlist entry = buildWatchlist(1, "Sanctioned Name");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry));

        List<WatchlistMatch> result = service.screenCustomer("Sanctioned Name", THRESHOLD, txn);

        assertEquals(1, result.size());
        assertEquals(MatchType.FUZZY_NAME, result.get(0).getMatchType());
        assertEquals(0, result.get(0).getMatchScore().compareTo(EXACT_SCORE));
    }

    @Test
    void matched_value_stores_original_customer_name_not_normalized() {
        // Customer name "  vLaDiMiR pUtIn  " after uppercasing+trim should match, but
        // matchedValue should preserve original formatting for audit trail
        Watchlist entry = buildWatchlist(1, "Vladimir Putin");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry));

        String original = "  vLaDiMiR pUtIn  ";
        List<WatchlistMatch> result = service.screenCustomer(original, THRESHOLD, txn);

        assertEquals(1, result.size());
        assertEquals(original, result.get(0).getMatchedValue(),
                "matchedValue should store the raw customer name for auditability");
    }

    @Test
    void matched_field_is_always_entity_name() {
        Watchlist entry = buildWatchlist(1, "ACME");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry));

        List<WatchlistMatch> result = service.screenCustomer("ACME Corp", THRESHOLD, txn);

        assertEquals("entity_name", result.get(0).getMatchedField());
    }

    @Test
    void all_matches_have_PENDING_status() {
        Watchlist entry1 = buildWatchlist(1, "ACME");
        Watchlist entry2 = buildWatchlist(2, "ACME Trading Corp");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry1, entry2));

        List<WatchlistMatch> result = service.screenCustomer("ACME Trading Corp", THRESHOLD, txn);

        assertTrue(result.stream().allMatch(m -> m.getStatus() == MatchStatus.PENDING));
    }

    // Ordering & interaction verification

    @Test
    void pending_status_is_set_before_block_for_exact_match() {
        Watchlist sanctioned = buildWatchlist(1, "Sanctioned");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(sanctioned));

        service.screenCustomer("Sanctioned", THRESHOLD, txn);

        InOrder inOrder = inOrder(txnService, alertService);
        inOrder.verify(txnService).updateTxnStatus(42, TxnStatus.PENDING);
        inOrder.verify(txnService).updateTxnStatus(42, TxnStatus.BLOCKED);
        inOrder.verify(alertService).raiseAlert(eq(42), eq("WATCHLIST"), anyString());
    }

    @Test
    void saveAll_is_invoked_exactly_once_per_screening() {
        Watchlist e1 = buildWatchlist(1, "ACME");
        Watchlist e2 = buildWatchlist(2, "ACME Trading Corp");
        Watchlist e3 = buildWatchlist(3, "Unrelated Entity");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(e1, e2, e3));

        service.screenCustomer("ACME Trading Corp", THRESHOLD, txn);

        verify(watchlistMatchRepository, times(1)).saveAll(anyList());
    }

    @Test
    void status_update_is_called_exactly_once_for_non_blocking_result() {
        when(watchlistRepository.findByIsActive(true)).thenReturn(Collections.emptyList());

        service.screenCustomer("Anybody", THRESHOLD, txn);

        verify(txnService, times(1)).updateTxnStatus(eq(42), any(TxnStatus.class));
    }

    // Mixed active/inactive entries
    @Test
    void only_matches_active_entries_returned_by_repository() {
        // Repository contract: findByIsActive(true) should only ever return active entries.
        // This test verifies the service trusts that contract and matches whatever is returned.
        Watchlist activeOne = buildWatchlist(1, "Active Target");
        Watchlist activeTwo = buildWatchlist(2, "Unrelated Entity");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(activeOne, activeTwo));

        List<WatchlistMatch> result = service.screenCustomer("Active Target", THRESHOLD, txn);

        assertEquals(1, result.size());
        assertEquals("Active Target", result.get(0).getWatchlist().getEntityName());
    }

    // Alert message content

    @Test
    void block_alert_description_mentions_exact_match() {
        Watchlist sanctioned = buildWatchlist(1, "Hostile Actor");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(sanctioned));

        service.screenCustomer("Hostile Actor", THRESHOLD, txn);

        ArgumentCaptor<String> desc = ArgumentCaptor.forClass(String.class);
        verify(alertService).raiseAlert(eq(42), eq("WATCHLIST"), desc.capture());
        assertTrue(desc.getValue().toLowerCase().contains("exact"),
                "Alert description should reference 'exact' match for traceability");
    }

    // Screening against large lists

    @Test
    void screens_against_many_entries_and_returns_only_matching_ones() {
        List<Watchlist> many = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) {
            many.add(buildWatchlist(i, "Entity-" + i));
        }
        // Insert one that will match
        many.add(buildWatchlist(9999, "Target X"));
        when(watchlistRepository.findByIsActive(true)).thenReturn(many);

        List<WatchlistMatch> result = service.screenCustomer("Target X", THRESHOLD, txn);

        assertEquals(1, result.size());
        assertEquals("Target X", result.get(0).getWatchlist().getEntityName());
    }

    // blockIfSanctioned additional scenarios

    @Test
    void blockIfSanctioned_only_blocks_once_even_with_multiple_exact_matches() {
        WatchlistMatch exact1 = buildMatch(EXACT_SCORE);
        WatchlistMatch exact2 = buildMatch(EXACT_SCORE);

        service.blockIfSanctioned(txn, List.of(exact1, exact2));

        verify(txnService, times(1)).updateTxnStatus(42, TxnStatus.BLOCKED);
        verify(alertService, times(1)).raiseAlert(eq(42), eq("WATCHLIST"), anyString());
    }

    @Test
    void blockIfSanctioned_blocks_when_mix_of_fuzzy_and_exact() {
        WatchlistMatch fuzzy = buildMatch(FUZZY_SCORE);
        WatchlistMatch exact = buildMatch(EXACT_SCORE);

        service.blockIfSanctioned(txn, List.of(fuzzy, exact));

        verify(txnService).updateTxnStatus(42, TxnStatus.BLOCKED);
        verify(alertService).raiseAlert(eq(42), eq("WATCHLIST"), anyString());
    }

    @Test
    void blockIfSanctioned_wraps_alertService_failure_as_RuntimeException() {
        WatchlistMatch exact = buildMatch(EXACT_SCORE);

        doThrow(new RuntimeException("mail server down"))
                .when(alertService).raiseAlert(eq(42), anyString(), anyString());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.blockIfSanctioned(txn, List.of(exact)));
        assertTrue(ex.getMessage().contains("Failed to evaluate sanction status"));
        assertNotNull(ex.getCause());
    }

    // Identity / reference propagation

    @Test
    void match_references_correct_txn_and_watchlist_entry() {
        Watchlist entry = buildWatchlist(77, "Vladimir Putin");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry));

        List<WatchlistMatch> result = service.screenCustomer("Vladimir Putin", THRESHOLD, txn);

        assertSame(txn, result.get(0).getTxn(),
                "Match must reference the same txn instance passed in");
        assertSame(entry, result.get(0).getWatchlist(),
                "Match must reference the exact watchlist entry that caused it");
    }

    // Customer-name edge shapes

    @Test
    void special_characters_in_name_do_not_break_screening() {
        Watchlist entry = buildWatchlist(1, "O'Brien-Müller & Co.");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry));

        List<WatchlistMatch> result =
                service.screenCustomer("O'Brien-Müller & Co.", THRESHOLD, txn);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getMatchScore().compareTo(EXACT_SCORE));
    }

    @Test
    void unicode_name_exact_match_is_detected() {
        Watchlist entry = buildWatchlist(1, "Żółć Łódź");
        when(watchlistRepository.findByIsActive(true)).thenReturn(List.of(entry));

        List<WatchlistMatch> result = service.screenCustomer("żółć łódź", THRESHOLD, txn);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getMatchScore().compareTo(EXACT_SCORE));
    }

    // Helpers methods

    private Watchlist buildWatchlist(Integer id, String entityName) {
        Watchlist w = new Watchlist();
        w.setWatchlistId(id);
        w.setEntityName(entityName);
        w.setEntityType(WatchlistEntityType.INDIVIDUAL);
        w.setListType(ListType.OFAC);
        w.setCountry("US");
        w.setReason("Sanctioned");
        w.setListedDate(LocalDate.now().minusYears(1));
        w.setIsActive(true);
        return w;
    }

    private WatchlistMatch buildMatch(BigDecimal score) {
        WatchlistMatch m = new WatchlistMatch();
        m.setTxn(txn);
        m.setMatchType(MatchType.FUZZY_NAME);
        m.setMatchScore(score);
        m.setMatchedField("entity_name");
        m.setMatchedValue("Test");
        m.setStatus(MatchStatus.PENDING);
        return m;
    }
}
