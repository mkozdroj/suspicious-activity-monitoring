package com.grad.sam.service.reports;

import java.nio.file.Path;
import java.time.LocalDateTime;

public interface GeneratedReportResult {

    String getReportName();

    Path getReportPath();

    int getRecordCount();

    LocalDateTime getGeneratedAt();

    String getTrigger();
}
