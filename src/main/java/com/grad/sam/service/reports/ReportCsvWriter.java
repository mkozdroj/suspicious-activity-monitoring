package com.grad.sam.service.reports;

import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

@Component
public class ReportCsvWriter {

    public <T> void writeCsv(Path reportPath,
                             List<String> headers,
                             List<T> rows,
                             Function<T, List<String>> rowMapper) throws IOException {
        Files.createDirectories(reportPath.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8)) {
            writeRow(writer, headers);
            for (T row : rows) {
                writeRow(writer, rowMapper.apply(row));
            }
        }
    }

    private void writeRow(BufferedWriter writer, List<String> values) throws IOException {
        String line = values.stream()
                .map(this::escapeCsv)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        writer.write(line);
        writer.newLine();
    }

    private String escapeCsv(String value) {
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
