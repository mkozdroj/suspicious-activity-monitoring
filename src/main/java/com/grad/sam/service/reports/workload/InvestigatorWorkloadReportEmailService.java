package com.grad.sam.service.reports.workload;

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
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestigatorWorkloadReportEmailService implements ReportEmailSender<InvestigatorWorkloadReportResult> {

    private static final DateTimeFormatter EMAIL_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectProvider<SpringTemplateEngine> templateEngineProvider;

    @Value("${sam.reports.investigator-workload.email.enabled:${sam.reports.open-cases.email.enabled:false}}")
    private boolean emailEnabled;

    @Value("${sam.reports.investigator-workload.email.from:${sam.reports.open-cases.email.from:}}")
    private String fromAddress;

    @Value("${sam.reports.investigator-workload.email.recipients:${sam.reports.open-cases.email.recipients:}}")
    private String recipientsProperty;

    @Value("${sam.reports.investigator-workload.email.subject:Investigator Workload Report}")
    private String subject;

    @Override
    public List<String> sendReportEmail(InvestigatorWorkloadReportResult reportResult) {
        if (!emailEnabled) {
            throw new IllegalStateException("Investigator workload report email is disabled");
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
            throw new IllegalStateException("No recipients configured for investigator workload report email");
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
            context.setVariable("analystCount", reportResult.getAnalystCount());
            context.setVariable("closedLast24HoursSince", formatDateTime(reportResult.getClosedLast24HoursSince()));
            context.setVariable("closedLast7DaysSince", formatDateTime(reportResult.getClosedLast7DaysSince()));
            context.setVariable("reportName", reportResult.getReportName());
            context.setVariable("trigger", reportResult.getTrigger());
            context.setVariable("rows", reportResult.getRows());

            String html = templateEngine.process("email/reports/investigator-workload-report", context);
            helper.setText(html, true);
            helper.addAttachment(reportResult.getReportName(), new FileSystemResource(reportResult.getReportPath()));

            mailSender.send(message);
            log.info("Investigator workload report email sent: recipients={}, report={}",
                    recipients,
                    reportResult.getReportPath());
            return recipients;
        } catch (Exception ex) {
            log.error("Failed to send investigator workload report email: recipients={}, report={}, reason={}",
                    recipients,
                    reportResult.getReportPath(),
                    ex.getMessage(),
                    ex);
            throw new IllegalStateException("Failed to send investigator workload report email: " + ex.getMessage(), ex);
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(EMAIL_DATE_TIME_FORMAT);
    }
}
