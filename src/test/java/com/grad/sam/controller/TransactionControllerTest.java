package com.grad.sam.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.grad.sam.enums.AccountStatus;
import com.grad.sam.enums.AccountType;
import com.grad.sam.enums.CustomerType;
import com.grad.sam.enums.KycStatus;
import com.grad.sam.enums.RiskRating;
import com.grad.sam.enums.TxnDirection;
import com.grad.sam.enums.TxnStatus;
import com.grad.sam.enums.TxnType;
import com.grad.sam.exception.GlobalExceptionHandler;
import com.grad.sam.model.Account;
import com.grad.sam.model.Customer;
import com.grad.sam.model.Txn;
import com.grad.sam.repository.AccountRepository;
import com.grad.sam.repository.CustomerRepository;
import com.grad.sam.repository.TxnRepository;
import com.grad.sam.repository.WatchlistMatchRepository;
import com.grad.sam.service.ScreenTransactionService;
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
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock private ScreenTransactionService screenTransactionService;
    @Mock private CustomerRepository customerRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private TxnRepository txnRepository;
    @Mock private WatchlistMatchRepository watchlistMatchRepository;

    @InjectMocks private TransactionController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

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

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void shouldReturnCustomerWhenFound() throws Exception {
        Customer customer = buildCustomer(5, "Maria Kowalska");
        when(customerRepository.findById(5)).thenReturn(Optional.of(customer));

        mockMvc.perform(get("/api/v1/customers/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(5))
                .andExpect(jsonPath("$.fullName").value("Maria Kowalska"))
                .andExpect(jsonPath("$.customerRef").value("CUST-00005"));
    }

    @Test
    void shouldReturnNotFoundWhenCustomerDoesNotExist() throws Exception {
        when(customerRepository.findById(404)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/customers/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Customer not found for id: 404"));
    }

    @Test
    void shouldReturnBadRequestWhenCustomerIdIsNotInteger() throws Exception {
        mockMvc.perform(get("/api/v1/customers/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }

    @Test
    void shouldReturnTransactionsForExistingAccount() throws Exception {
        Account account = buildAccount(7);
        when(accountRepository.findById(7)).thenReturn(Optional.of(account));
        when(txnRepository.findByAccount_AccountId(7))
                .thenReturn(List.of(buildTxn(101, "TXN-000101"), buildTxn(102, "TXN-000102")));

        mockMvc.perform(get("/api/v1/accounts/7/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].txnId").value(101))
                .andExpect(jsonPath("$[0].txnRef").value("TXN-000101"))
                .andExpect(jsonPath("$[1].txnId").value(102));
    }

    @Test
    void shouldReturnEmptyListWhenAccountHasNoTransactions() throws Exception {
        Account account = buildAccount(7);
        when(accountRepository.findById(7)).thenReturn(Optional.of(account));
        when(txnRepository.findByAccount_AccountId(7)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/accounts/7/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
        when(accountRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/accounts/999/transactions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Account not found for id: 999"));
    }

    @Test
    void shouldReturnBadRequestWhenAccountIdIsNotInteger() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/xyz/transactions"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }

    private Customer buildCustomer(Integer id, String fullName) {
        Customer c = new Customer();
        c.setCustomerId(id);
        c.setCustomerRef(String.format("CUST-%05d", id));
        c.setFullName(fullName);
        c.setNationality("PL");
        c.setCountryOfResidence("PL");
        c.setCustomerType(CustomerType.INDIVIDUAL);
        c.setRiskRating(RiskRating.LOW);
        c.setKycStatus(KycStatus.VERIFIED);
        c.setOnboardedDate(LocalDate.of(2024, 1, 15));
        c.setIsPep(false);
        c.setIsActive(true);
        return c;
    }

    private Account buildAccount(Integer id) {
        Account a = new Account();
        a.setAccountId(id);
        a.setAccountNumber("PL-ACC-" + id);
        a.setAccountType(AccountType.CURRENT);
        a.setCurrency("PLN");
        a.setBalance(new BigDecimal("1000.00"));
        a.setOpenedDate(LocalDate.of(2024, 3, 1));
        a.setStatus(AccountStatus.ACTIVE);
        a.setBranchCode("BR-001");
        return a;
    }

    private Txn buildTxn(Integer id, String ref) {
        Txn t = new Txn();
        t.setTxnId(id);
        t.setTxnRef(ref);
        t.setTxnType(TxnType.WIRE);
        t.setDirection(TxnDirection.CR);
        t.setAmount(new BigDecimal("150.00"));
        t.setCurrency("USD");
        t.setAmountUsd(new BigDecimal("150.00"));
        t.setTxnDate(LocalDate.of(2026, 4, 1));
        t.setValueDate(LocalDate.of(2026, 4, 1));
        t.setStatus(TxnStatus.COMPLETED);
        return t;
    }
}
