package com.grad.sam.service.reports.workload;

import com.grad.sam.enums.InvestigationState;
import com.grad.sam.model.Investigation;
import com.grad.sam.repository.InvestigationRepository;
import com.grad.sam.service.reports.ReportCsvWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestigatorWorkloadReportServiceTest {

    @Mock
    private InvestigationRepository investigationRepository;

    private InvestigatorWorkloadReportService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new InvestigatorWorkloadReportService(investigationRepository, new ReportCsvWriter());
        ReflectionTestUtils.setField(service, "outputDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "filePrefix", "investigator_workload_report");
    }

    @Test
    void generateReport_groups_by_analyst_and_calculates_metrics() {
        LocalDateTime now = LocalDateTime.now();
        Investigation a1 = buildInvestigation("Analyst A", InvestigationState.OPEN, now.minusDays(3), null);
        Investigation a2 = buildInvestigation("Analyst A", InvestigationState.CLOSED, now.minusDays(2), now.minusHours(12));
        Investigation a3 = buildInvestigation("Analyst A", InvestigationState.CLOSED, now.minusDays(10), now.minusDays(2));
        Investigation unassigned = buildInvestigation("   ", InvestigationState.UNDER_REVIEW, now.minusDays(1), null);

        when(investigationRepository.findAll()).thenReturn(List.of(a1, a2, a3, unassigned));

        InvestigatorWorkloadReportResult result = service.generateReport("manual");

        assertEquals(4, result.getRecordCount());
        assertEquals(2, result.getAnalystCount());
        assertEquals(2, result.getRows().size());
        assertEquals("Analyst A", result.getRows().getFirst().analyst());
        assertEquals(3, result.getRows().getFirst().assignedCases());
        assertEquals(1, result.getRows().getFirst().openCases());
        assertEquals(1, result.getRows().getFirst().closedLast24Hours());
        assertEquals(2, result.getRows().getFirst().closedLast7Days());
        assertEquals("UNASSIGNED", result.getRows().get(1).analyst());
        assertTrue(result.getReportPath().toFile().exists());
    }

    @Test
    void generateReport_wraps_csv_writer_failure() throws Exception {
        ReportCsvWriter writer = org.mockito.Mockito.mock(ReportCsvWriter.class);
        InvestigatorWorkloadReportService failingService = new InvestigatorWorkloadReportService(investigationRepository, writer);
        ReflectionTestUtils.setField(failingService, "outputDir", tempDir.toString());
        ReflectionTestUtils.setField(failingService, "filePrefix", "investigator_workload_report");
        when(investigationRepository.findAll()).thenReturn(List.of());
        doThrow(new IOException("disk full")).when(writer).writeCsv(any(), anyList(), anyList(), any());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> failingService.generateReport("manual"));

        assertTrue(exception.getMessage().contains("Failed to generate investigator workload report"));
    }

    private Investigation buildInvestigation(String openedBy,
                                             InvestigationState state,
                                             LocalDateTime openedAt,
                                             LocalDateTime closedAt) {
        Investigation investigation = new Investigation();
        investigation.setOpenedBy(openedBy);
        investigation.setState(state);
        investigation.setOpenedAt(openedAt);
        investigation.setClosedAt(closedAt);
        return investigation;
    }
}
