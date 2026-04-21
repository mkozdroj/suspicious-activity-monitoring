package com.grad.sam.service.reports.ruleeffectiveness;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEffectivenessReportEmailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private ObjectProvider<SpringTemplateEngine> templateEngineProvider;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine unusedTemplateEngine;

    private RuleEffectivenessReportEmailService service;
    private SpringTemplateEngine templateEngine;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new RuleEffectivenessReportEmailService(mailSenderProvider, templateEngineProvider);
        templateEngine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        templateEngine.setTemplateResolver(resolver);
    }

    @Test
    void sendReportEmail_throws_when_disabled() {
        ReflectionTestUtils.setField(service, "emailEnabled", false);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.sendReportEmail(reportResult(tempDir.resolve("rules.csv"))));

        assertEquals("Rule effectiveness report email is disabled", exception.getMessage());
    }

    @Test
    void sendReportEmail_sends_email_with_attachment() throws Exception {
        Path reportPath = tempDir.resolve("rules.csv");
        Files.writeString(reportPath, "data");
        MimeMessage mimeMessage = new MimeMessage((Session) null);

        ReflectionTestUtils.setField(service, "emailEnabled", true);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@test.com");
        ReflectionTestUtils.setField(service, "recipientsProperty", "a@test.com,b@test.com");
        ReflectionTestUtils.setField(service, "subject", "Rule Effectiveness Report");

        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(templateEngineProvider.getIfAvailable()).thenReturn(templateEngine);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        List<String> recipients = service.sendReportEmail(reportResult(reportPath));

        assertEquals(List.of("a@test.com", "b@test.com"), recipients);
        verify(mailSender).send(mimeMessage);
    }

    private RuleEffectivenessReportResult reportResult(Path reportPath) {
        return RuleEffectivenessReportResult.builder()
                .reportName(reportPath.getFileName().toString())
                .reportPath(reportPath)
                .recordCount(2)
                .ruleCount(1)
                .actionableAlertCount(1)
                .alertsNeedingReviewCount(1)
                .falsePositiveCount(0)
                .generatedAt(LocalDateTime.of(2026, 4, 21, 10, 15, 33))
                .periodStart(LocalDateTime.of(2026, 4, 20, 10, 15, 33))
                .periodEnd(LocalDateTime.of(2026, 4, 21, 10, 15, 33))
                .trigger("manual")
                .rows(List.of(new RuleEffectivenessReportRow("STR-001", "Structuring", "STRUCTURING", 2, 1, 1, 1, 0, 0, 0, "70.00", "50.00")))
                .build();
    }
}
