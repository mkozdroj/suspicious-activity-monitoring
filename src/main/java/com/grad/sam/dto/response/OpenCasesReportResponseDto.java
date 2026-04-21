package com.grad.sam.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OpenCasesReportResponseDto {

    private String reportName;
    private String reportPath;
    private int recordCount;
    private LocalDateTime generatedAt;
    private String trigger;
    private boolean emailSent;
    private List<String> emailedTo;
}
