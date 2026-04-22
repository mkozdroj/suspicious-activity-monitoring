package com.grad.sam.service.reports.opencases;

import jakarta.mail.internet.MimeMessage;
import com.grad.sam.service.reports.ReportEmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenCasesReportEmailService implements ReportEmailSender<OpenCasesReportResult> {

    private static final DateTimeFormatter EMAIL_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectProvider<SpringTemplateEngine> templateEngineProvider;

    @Value("${sam.reports.open-cases.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${sam.reports.open-cases.email.from:}")
    private String fromAddress;

    @Value("${sam.reports.open-cases.email.recipients:}")
    private String recipientsProperty;

    @Value("${sam.reports.open-cases.email.subject:Open Cases Report}")
    private String subject;

    @Override
    public List<String> sendReportEmail(OpenCasesReportResult reportResult) {
        if (!emailEnabled) {
            throw new IllegalStateException("Open cases report email is disabled");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender is not configured. Set spring.mail.* properties first.");
        }

        SpringTemplateEngine templateEngine = templateEngineProvider.getIfAvailable();
        if (templateEngine == null) {
            throw new IllegalStateException("Thymeleaf template engine is not configured");
        }

        List<String> recipients = Arrays.stream(recipientsProperty.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        if (recipients.isEmpty()) {
            throw new IllegalStateException("No recipients configured for open cases report email");
        }

        if (!Files.exists(reportResult.getReportPath())) {
            throw new IllegalStateException("Report file does not exist: " + reportResult.getReportPath());
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            if (!fromAddress.isBlank()) {
                helper.setFrom(fromAddress);
            }
            helper.setTo(recipients.toArray(String[]::new));
            helper.setSubject(subject + " - " + reportResult.getGeneratedAt().toLocalDate());

            Context context = new Context();
            context.setVariable("generatedAt", formatDateTime(reportResult.getGeneratedAt()));
            context.setVariable("recordCount", reportResult.getRecordCount());
            context.setVariable("openCount", reportResult.getOpenCount());
            context.setVariable("escalatedCount", reportResult.getEscalatedCount());
            context.setVariable("underReviewCount", reportResult.getUnderReviewCount());
            context.setVariable("periodStart", formatDateTime(reportResult.getPeriodStart()));
            context.setVariable("periodEnd", formatDateTime(reportResult.getPeriodEnd()));
            context.setVariable("reportName", reportResult.getReportName());
            context.setVariable("reportPath", reportResult.getReportPath().toString());
            context.setVariable("trigger", reportResult.getTrigger());
            context.setVariable("categorySummaries", reportResult.getCategorySummaries());

            String html = templateEngine.process("email/reports/open-cases-report", context);
            helper.setText(html, true);
            helper.addAttachment(reportResult.getReportName(), new FileSystemResource(reportResult.getReportPath()));

            mailSender.send(message);
            log.info("Open cases report email sent: recipients={}, report={}", recipients, reportResult.getReportPath());
            return recipients;
        } catch (Exception ex) {
            log.error("Failed to send open cases report email: recipients={}, report={}, reason={}",
                    recipients,
                    reportResult.getReportPath(),
                    ex.getMessage(),
                    ex);
            throw new IllegalStateException("Failed to send open cases report email: " + ex.getMessage(), ex);
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(EMAIL_DATE_TIME_FORMAT);
    }
}
