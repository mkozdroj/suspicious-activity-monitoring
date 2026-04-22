package com.grad.sam.service.reports.opencases;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.AlertStatus;
import com.grad.sam.enums.RuleCategory;
import com.grad.sam.enums.RiskRating;
import com.grad.sam.model.Account;
import com.grad.sam.model.Alert;
import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Customer;
import com.grad.sam.model.Investigation;
import com.grad.sam.repository.AlertRepository;
import com.grad.sam.service.reports.ReportCsvWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenCasesReportServiceTest {

    @Mock
    private AlertRepository alertRepository;

    private ReportCsvWriter reportCsvWriter;
    private OpenCasesReportService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        reportCsvWriter = new ReportCsvWriter();
        service = new OpenCasesReportService(alertRepository, reportCsvWriter);
        ReflectionTestUtils.setField(service, "outputDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "filePrefix", "open_cases_report");
    }

    @Test
    void generateReport_creates_csv_and_summary_counts() {
        Alert open = buildAlert("ALT-1", AlertStatus.OPEN, "STR-001", "Structuring", RuleCategory.STRUCTURING, LocalDateTime.of(2026, 4, 20, 10, 0));
        Alert underReview = buildAlert("ALT-2", AlertStatus.UNDER_REVIEW, "GEO-001", "Geography", RuleCategory.GEOGRAPHY, LocalDateTime.of(2026, 4, 20, 11, 0));
        Alert escalated = buildAlert("ALT-3", AlertStatus.ESCALATED, "STR-002", "Structuring 2", RuleCategory.STRUCTURING, LocalDateTime.of(2026, 4, 20, 12, 0));

        when(alertRepository.findByStatusInOrderByTriggeredAtAscAlertIdAsc(anyList())).thenReturn(List.of(open, underReview, escalated));

        OpenCasesReportResult result = service.generateReport("manual");

        assertEquals(3, result.getRecordCount());
        assertEquals(1, result.getOpenCount());
        assertEquals(1, result.getUnderReviewCount());
        assertEquals(1, result.getEscalatedCount());
        assertEquals(LocalDateTime.of(2026, 4, 20, 10, 0), result.getPeriodStart());
        assertEquals(LocalDateTime.of(2026, 4, 20, 12, 0), result.getPeriodEnd());
        assertEquals(3, result.getRows().size());
        assertEquals("ALT-1", result.getRows().getFirst().alertRef());
        assertEquals("CR-1", result.getRows().getFirst().customerRef());
        assertEquals("Analyst A", result.getRows().getFirst().openedBy());
        assertEquals(2L, result.getCategorySummaries().getFirst().count());
        assertEquals("STRUCTURING", result.getCategorySummaries().getFirst().category());
        assertTrue(result.getReportPath().toFile().exists());
    }

    @Test
    void generateReport_handles_empty_alert_list() {
        when(alertRepository.findByStatusInOrderByTriggeredAtAscAlertIdAsc(anyList())).thenReturn(List.of());

        OpenCasesReportResult result = service.generateReport("scheduler");

        assertEquals(0, result.getRecordCount());
        assertEquals(0, result.getOpenCount());
        assertEquals(0, result.getEscalatedCount());
        assertEquals(0, result.getUnderReviewCount());
        assertEquals(null, result.getPeriodStart());
        assertEquals(null, result.getPeriodEnd());
        assertEquals(List.of(), result.getRows());
        assertEquals(List.of(), result.getCategorySummaries());
    }

    @Test
    void generateReport_wraps_csv_writer_failure() throws Exception {
        ReportCsvWriter writer = org.mockito.Mockito.mock(ReportCsvWriter.class);
        OpenCasesReportService failingService = new OpenCasesReportService(alertRepository, writer);
        ReflectionTestUtils.setField(failingService, "outputDir", tempDir.toString());
        ReflectionTestUtils.setField(failingService, "filePrefix", "open_cases_report");
        when(alertRepository.findByStatusInOrderByTriggeredAtAscAlertIdAsc(anyList())).thenReturn(List.of());
        doThrow(new IOException("disk full")).when(writer).writeCsv(any(), anyList(), anyList(), any());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> failingService.generateReport("manual"));

        assertTrue(exception.getMessage().contains("Failed to generate open cases report"));
    }

    private Alert buildAlert(String alertRef,
                             AlertStatus status,
                             String ruleCode,
                             String ruleName,
                             RuleCategory category,
                             LocalDateTime triggeredAt) {
        AlertRule alertRule = new AlertRule();
        alertRule.setRuleCode(ruleCode);
        alertRule.setRuleName(ruleName);
        alertRule.setRuleCategory(category);
        alertRule.setSeverity(AlertSeverity.HIGH);

        Customer customer = new Customer();
        customer.setCustomerRef("CR-1");
        customer.setFullName("Marta Kowalska");
        customer.setRiskRating(RiskRating.HIGH);

        Account account = new Account();
        account.setAccountNumber("ACC-1");
        account.setCustomer(customer);
        account.setBalance(BigDecimal.TEN);

        Investigation investigation = new Investigation();
        investigation.setInvestigationRef("INV-1");
        investigation.setOpenedBy("Analyst A");
        investigation.setOpenedAt(LocalDateTime.of(2026, 4, 20, 9, 0));

        Alert alert = new Alert();
        alert.setAlertRef(alertRef);
        alert.setStatus(status);
        alert.setTriggeredAt(triggeredAt);
        alert.setAlertRule(alertRule);
        alert.setAccount(account);
        alert.setInvestigation(investigation);
        return alert;
    }
}
