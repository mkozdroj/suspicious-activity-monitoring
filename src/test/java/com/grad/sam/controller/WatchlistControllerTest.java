package com.grad.sam.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.grad.sam.enums.ListType;
import com.grad.sam.enums.MatchStatus;
import com.grad.sam.enums.MatchType;
import com.grad.sam.enums.WatchlistEntityType;
import com.grad.sam.exception.GlobalExceptionHandler;
import com.grad.sam.model.Txn;
import com.grad.sam.model.Watchlist;
import com.grad.sam.model.WatchlistMatch;
import com.grad.sam.repository.WatchlistMatchRepository;
import com.grad.sam.repository.WatchlistRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WatchlistControllerTest {

    @Mock private WatchlistRepository watchlistRepository;
    @Mock private WatchlistMatchRepository watchlistMatchRepository;

    @InjectMocks private WatchlistController controller;

    private MockMvc mockMvc;

    private static Level originalHandlerLogLevel;

    @BeforeAll
    static void silenceHandlerLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        originalHandlerLogLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
    }

    @AfterAll
    static void restoreHandlerLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        logger.setLevel(originalHandlerLogLevel);
    }

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnWatchlistMatchesByName() throws Exception {
        when(watchlistRepository.findByEntityNameContainingIgnoreCase("kim"))
                .thenReturn(List.of(
                        buildWatchlist(1, "Kim Jong Un", ListType.OFAC, WatchlistEntityType.INDIVIDUAL),
                        buildWatchlist(2, "Kim Enterprises", ListType.UN, WatchlistEntityType.ENTITY)));

        mockMvc.perform(get("/api/v1/watchlist/search").param("name", "kim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].watchlistId").value(1))
                .andExpect(jsonPath("$[0].entityName").value("Kim Jong Un"))
                .andExpect(jsonPath("$[1].entityName").value("Kim Enterprises"));
    }

    @Test
    void shouldTrimNameQueryParameterBeforeSearch() throws Exception {
        when(watchlistRepository.findByEntityNameContainingIgnoreCase("kim"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/watchlist/search").param("name", "   kim   "))
                .andExpect(status().isOk());

        verify(watchlistRepository).findByEntityNameContainingIgnoreCase("kim");
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        mockMvc.perform(get("/api/v1/watchlist/search").param("name", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("Query parameter 'name' is required and must not be blank"));
    }

    @Test
    void shouldReturnBadRequestWhenNameParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/watchlist/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("Required parameter 'name' is missing"));
    }

    @Test
    void shouldReturnAllWatchlistMatchesWhenTxnIdAbsent() throws Exception {
        when(watchlistMatchRepository.findAll())
                .thenReturn(List.of(
                        buildMatch(100, 1, 50, "Kim Jong Un", ListType.OFAC),
                        buildMatch(101, 2, 70, "Kim Enterprises", ListType.UN)));

        mockMvc.perform(get("/api/v1/watchlist-matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].matchId").value(100))
                .andExpect(jsonPath("$[0].entityName").value("Kim Jong Un"))
                .andExpect(jsonPath("$[0].listSource").value("OFAC"))
                .andExpect(jsonPath("$[1].listSource").value("UN"));
    }

    @Test
    void shouldReturnFilteredMatchesByTxnId() throws Exception {
        when(watchlistMatchRepository.findByTxn_TxnId(55))
                .thenReturn(List.of(buildMatch(200, 5, 95, "Sanctioned Entity", ListType.HMT)));

        mockMvc.perform(get("/api/v1/watchlist-matches").param("txnId", "55"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].matchId").value(200))
                .andExpect(jsonPath("$[0].watchlistId").value(5))
                .andExpect(jsonPath("$[0].listSource").value("HMT"))
                .andExpect(jsonPath("$[0].matchScore").value(95));
    }

    @Test
    void shouldReturnEmptyListWhenNoMatchesForTxnId() throws Exception {
        when(watchlistMatchRepository.findByTxn_TxnId(7)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/watchlist-matches").param("txnId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldHandleMatchWithoutLinkedWatchlist() throws Exception {
        WatchlistMatch orphan = new WatchlistMatch();
        orphan.setMatchId(300);
        orphan.setMatchType(MatchType.NAME);
        orphan.setMatchScore(new BigDecimal("42.00"));
        orphan.setMatchedField("name");
        orphan.setMatchedValue("unknown");
        orphan.setStatus(MatchStatus.PENDING);
        orphan.setWatchlist(null);

        when(watchlistMatchRepository.findAll()).thenReturn(List.of(orphan));

        mockMvc.perform(get("/api/v1/watchlist-matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchId").value(300))
                .andExpect(jsonPath("$[0].watchlistId").doesNotExist())
                .andExpect(jsonPath("$[0].entityName").doesNotExist())
                .andExpect(jsonPath("$[0].listSource").doesNotExist());
    }

    @Test
    void shouldHandleMatchWithNullListSourceAndNullScore() throws Exception {
        Watchlist watchlist = buildWatchlist(8, "Null Source Entity", null, WatchlistEntityType.ENTITY);
        WatchlistMatch match = new WatchlistMatch();
        match.setMatchId(301);
        match.setTxn(new Txn());
        match.setWatchlist(watchlist);
        match.setMatchType(MatchType.NAME);
        match.setMatchScore(null);
        match.setMatchedField("name");
        match.setMatchedValue("Null Source Entity");
        match.setStatus(MatchStatus.PENDING);

        when(watchlistMatchRepository.findAll()).thenReturn(List.of(match));

        mockMvc.perform(get("/api/v1/watchlist-matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchId").value(301))
                .andExpect(jsonPath("$[0].watchlistId").value(8))
                .andExpect(jsonPath("$[0].entityName").value("Null Source Entity"))
                .andExpect(jsonPath("$[0].listSource").doesNotExist())
                .andExpect(jsonPath("$[0].matchScore").doesNotExist());
    }

    @Test
    void shouldReturnBadRequestWhenTxnIdIsNotInteger() throws Exception {
        mockMvc.perform(get("/api/v1/watchlist-matches").param("txnId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }

    private Watchlist buildWatchlist(Integer id, String entityName, ListType listType, WatchlistEntityType entityType) {
        Watchlist w = new Watchlist();
        w.setWatchlistId(id);
        w.setEntityName(entityName);
        w.setListType(listType);
        w.setEntityType(entityType);
        w.setCountry("US");
        w.setReason("Sanctions programme");
        w.setListedDate(LocalDate.of(2024, 1, 1));
        w.setIsActive(true);
        return w;
    }

    private WatchlistMatch buildMatch(Integer matchId, Integer watchlistId, int score, String entityName, ListType listType) {
        Txn txn = new Txn();
        txn.setTxnId(1);

        WatchlistMatch m = new WatchlistMatch();
        m.setMatchId(matchId);
        m.setTxn(txn);
        m.setWatchlist(buildWatchlist(watchlistId, entityName, listType, WatchlistEntityType.INDIVIDUAL));
        m.setMatchType(MatchType.NAME);
        m.setMatchScore(new BigDecimal(score));
        m.setMatchedField("name");
        m.setMatchedValue(entityName);
        m.setStatus(MatchStatus.PENDING);
        return m;
    }
}
