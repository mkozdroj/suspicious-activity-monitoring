package com.grad.sam.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.grad.sam.dto.request.CaseNoteDto;
import com.grad.sam.dto.request.OpenCaseRequestDto;
import com.grad.sam.enums.InvestigationState;
import com.grad.sam.enums.Priority;
import com.grad.sam.exception.BusinessConflictException;
import com.grad.sam.exception.DataNotFoundException;
import com.grad.sam.exception.GlobalExceptionHandler;
import com.grad.sam.exception.InvalidInputException;
import com.grad.sam.model.Account;
import com.grad.sam.model.Alert;
import com.grad.sam.model.Customer;
import com.grad.sam.model.Investigation;
import com.grad.sam.repository.InvestigationRepository;
import com.grad.sam.service.InvestigationService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InvestigationControllerTest {

    @Mock private InvestigationService investigationService;
    @Mock private InvestigationRepository investigationRepository;

    @InjectMocks private InvestigationController controller;

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
    void shouldOpenCaseAndReturnCreated() throws Exception {
        OpenCaseRequestDto request = new OpenCaseRequestDto(10, "Jane Smith", Priority.HIGH);
        Investigation inv = buildInvestigation(42, 10, (short) 85, "Jane Smith");

        when(investigationService.openCase(eq(10), eq("Jane Smith"), eq(Priority.HIGH)))
                .thenReturn(inv);

        mockMvc.perform(post("/api/v1/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseId").value(42))
                .andExpect(jsonPath("$.alertId").value(10))
                .andExpect(jsonPath("$.alertSeverity").value("HIGH"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedOfficer").value("Jane Smith"));
    }

    @Test
    void shouldDefaultPriorityToMediumWhenNotProvided() throws Exception {
        OpenCaseRequestDto request = new OpenCaseRequestDto(11, "Anna Nowak", null);
        Investigation inv = buildInvestigation(43, 11, (short) 30, "Anna Nowak");

        when(investigationService.openCase(eq(11), eq("Anna Nowak"), eq(Priority.MEDIUM)))
                .thenReturn(inv);

        mockMvc.perform(post("/api/v1/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alertSeverity").value("LOW"));

        verify(investigationService).openCase(11, "Anna Nowak", Priority.MEDIUM);
    }

    @Test
    void shouldReturnBadRequestForInvalidOpenCaseRequest() throws Exception {
        String body = "{\"alertId\":null,\"assignedOfficer\":\"lowercase name\"}";

        mockMvc.perform(post("/api/v1/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void shouldPropagateBusinessConflictFromService() throws Exception {
        OpenCaseRequestDto request = new OpenCaseRequestDto(10, "Jane Smith", Priority.HIGH);

        when(investigationService.openCase(anyInt(), anyString(), any()))
                .thenThrow(new BusinessConflictException("Investigation already exists for alert id: 10"));

        mockMvc.perform(post("/api/v1/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Investigation already exists for alert id: 10"));
    }

    @Test
    void shouldPropagateNotFoundFromOpenCaseService() throws Exception {
        OpenCaseRequestDto request = new OpenCaseRequestDto(10, "Jane Smith", Priority.HIGH);

        when(investigationService.openCase(anyInt(), anyString(), any()))
                .thenThrow(new DataNotFoundException("Alert not found for id: 10"));

        mockMvc.perform(post("/api/v1/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Alert not found for id: 10"));
    }

    @Test
    void shouldPropagateInvalidInputFromOpenCaseService() throws Exception {
        OpenCaseRequestDto request = new OpenCaseRequestDto(10, "Jane Smith", Priority.HIGH);

        when(investigationService.openCase(anyInt(), anyString(), any()))
                .thenThrow(new InvalidInputException("Assigned officer is invalid"));

        mockMvc.perform(post("/api/v1/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("Assigned officer is invalid"));
    }

    @Test
    void shouldGetCaseWhenFound() throws Exception {
        Investigation inv = buildInvestigation(42, 10, (short) 50, "Jane Smith");
        when(investigationRepository.findById(42)).thenReturn(Optional.of(inv));

        mockMvc.perform(get("/api/v1/cases/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value(42))
                .andExpect(jsonPath("$.alertId").value(10))
                .andExpect(jsonPath("$.alertSeverity").value("MEDIUM"));
    }

    @Test
    void shouldReturnLowSeverityWhenAlertScoreIsNull() throws Exception {
        Investigation inv = buildInvestigation(42, 10, null, "Jane Smith");
        when(investigationRepository.findById(42)).thenReturn(Optional.of(inv));

        mockMvc.perform(get("/api/v1/cases/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertSeverity").value("LOW"));
    }

    @Test
    void shouldReturnCaseWithoutAlertFieldsWhenAlertIsNull() throws Exception {
        Investigation inv = buildInvestigation(42, 10, (short) 50, "Jane Smith");
        inv.setAlert(null);
        when(investigationRepository.findById(42)).thenReturn(Optional.of(inv));

        mockMvc.perform(get("/api/v1/cases/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value(42))
                .andExpect(jsonPath("$.alertId").doesNotExist())
                .andExpect(jsonPath("$.alertSeverity").doesNotExist());
    }

    @Test
    void shouldReturnNotFoundWhenCaseDoesNotExist() throws Exception {
        when(investigationRepository.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/cases/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Investigation not found for id: 99"));
    }

    @Test
    void shouldAddNoteToCaseWithoutExistingFindings() throws Exception {
        Investigation inv = buildInvestigation(42, 10, (short) 50, "Jane Smith");
        inv.setFindings(null);
        when(investigationRepository.findById(42)).thenReturn(Optional.of(inv));
        when(investigationRepository.save(any(Investigation.class))).thenAnswer(i -> i.getArgument(0));

        CaseNoteDto note = new CaseNoteDto("Jane Smith", "Reviewed customer risk profile");

        mockMvc.perform(post("/api/v1/cases/42/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(note)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings", containsString("Jane Smith")))
                .andExpect(jsonPath("$.findings", containsString("Reviewed customer risk profile")));

        ArgumentCaptor<Investigation> captor = ArgumentCaptor.forClass(Investigation.class);
        verify(investigationRepository).save(captor.capture());
        Investigation saved = captor.getValue();
        org.junit.jupiter.api.Assertions.assertTrue(saved.getFindings().contains("Reviewed customer risk profile"));
    }

    @Test
    void shouldAppendNoteToExistingFindings() throws Exception {
        Investigation inv = buildInvestigation(42, 10, (short) 50, "Jane Smith");
        inv.setFindings("[2026-04-01 09:00 | Anna Nowak] Initial review complete");
        when(investigationRepository.findById(42)).thenReturn(Optional.of(inv));
        when(investigationRepository.save(any(Investigation.class))).thenAnswer(i -> i.getArgument(0));

        CaseNoteDto note = new CaseNoteDto("Jane Smith", "Follow-up on counterparty");

        mockMvc.perform(post("/api/v1/cases/42/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(note)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings", containsString("Initial review complete")))
                .andExpect(jsonPath("$.findings", containsString("Follow-up on counterparty")));
    }

    @Test
    void shouldTruncateFindingsWhenCombinedExceedsFiveHundredChars() throws Exception {
        Investigation inv = buildInvestigation(42, 10, (short) 50, "Jane Smith");
        inv.setFindings("A".repeat(495));
        when(investigationRepository.findById(42)).thenReturn(Optional.of(inv));
        when(investigationRepository.save(any(Investigation.class))).thenAnswer(i -> i.getArgument(0));

        CaseNoteDto note = new CaseNoteDto("Jane Smith", "Latest note added after truncation");

        mockMvc.perform(post("/api/v1/cases/42/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(note)))
                .andExpect(status().isOk());

        ArgumentCaptor<Investigation> captor = ArgumentCaptor.forClass(Investigation.class);
        verify(investigationRepository).save(captor.capture());
        String findings = captor.getValue().getFindings();
        org.junit.jupiter.api.Assertions.assertEquals(500, findings.length());
        org.junit.jupiter.api.Assertions.assertTrue(findings.endsWith("..."));
    }

    @Test
    void shouldReturnNotFoundWhenAddingNoteToMissingCase() throws Exception {
        when(investigationRepository.findById(99)).thenReturn(Optional.empty());
        CaseNoteDto note = new CaseNoteDto("Jane Smith", "Any note");

        mockMvc.perform(post("/api/v1/cases/99/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(note)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldReturnBadRequestForInvalidNotePayload() throws Exception {
        String body = "{\"author\":\"\",\"noteText\":\"\"}";

        mockMvc.perform(post("/api/v1/cases/42/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void shouldReturnBadRequestForNotePayloadThatExceedsMaxLength() throws Exception {
        String body = "{\"author\":\"Jane Smith\",\"noteText\":\"" + "x".repeat(401) + "\"}";

        mockMvc.perform(post("/api/v1/cases/42/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void shouldReturnBadRequestWhenCaseIdIsNotInteger() throws Exception {
        mockMvc.perform(get("/api/v1/cases/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }

    @Test
    void shouldReturnBadRequestWhenAddingNoteCaseIdIsNotInteger() throws Exception {
        CaseNoteDto note = new CaseNoteDto("Jane Smith", "Any note");

        mockMvc.perform(post("/api/v1/cases/not-a-number/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(note)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }

    private Investigation buildInvestigation(Integer id, Integer alertId, Short alertScore, String officer) {
        Customer customer = new Customer();
        customer.setCustomerId(1);
        customer.setFullName("Acme Corp");

        Account account = new Account();
        account.setAccountId(7);
        account.setCustomer(customer);

        Alert alert = new Alert();
        alert.setAlertId(alertId);
        alert.setAccount(account);
        alert.setAlertScore(alertScore);

        Investigation inv = new Investigation();
        inv.setInvestigationId(id);
        inv.setInvestigationRef("INV260421-00001");
        inv.setAlert(alert);
        inv.setCustomer(customer);
        inv.setOpenedBy(officer);
        inv.setOpenedAt(LocalDateTime.now());
        inv.setPriority(Priority.MEDIUM);
        inv.setState(InvestigationState.OPEN);
        return inv;
    }
}
