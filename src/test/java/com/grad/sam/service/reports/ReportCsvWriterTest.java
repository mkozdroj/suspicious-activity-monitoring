package com.grad.sam.service.reports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportCsvWriterTest {

    private final ReportCsvWriter writer = new ReportCsvWriter();

    @TempDir
    Path tempDir;

    @Test
    void writeCsv_writes_headers_rows_and_escaped_values() throws Exception {
        Path reportPath = tempDir.resolve("nested").resolve("report.csv");

        writer.writeCsv(
                reportPath,
                List.of("col1", "col2"),
                List.of(
                        List.of("plain", "value,with,comma"),
                        List.of("with\"quote", "with\nnewline")
                ),
                row -> row
        );

        assertTrue(Files.exists(reportPath));
        assertEquals(
                List.of(
                        "col1,col2",
                        "plain,\"value,with,comma\"",
                        "\"with\"\"quote\",\"with",
                        "newline\""
                ),
                Files.readAllLines(reportPath)
        );
    }
}
