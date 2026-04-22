package com.grad.sam.controller.reports;

import com.grad.sam.dto.response.OpenCasesReportResponseDto;
import com.grad.sam.service.reports.GeneratedReportResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReportResponseMapper {

    public OpenCasesReportResponseDto toResponse(GeneratedReportResult reportResult) {
        return toResponse(reportResult, false, List.of());
    }

    public OpenCasesReportResponseDto toResponse(GeneratedReportResult reportResult,
                                                 boolean emailSent,
                                                 List<String> recipients) {
        OpenCasesReportResponseDto response = new OpenCasesReportResponseDto();
        response.setReportName(reportResult.getReportName());
        response.setReportPath(reportResult.getReportPath().toString());
        response.setRecordCount(reportResult.getRecordCount());
        response.setGeneratedAt(reportResult.getGeneratedAt());
        response.setTrigger(reportResult.getTrigger());
        response.setEmailSent(emailSent);
        response.setEmailedTo(recipients);
        return response;
    }
}
