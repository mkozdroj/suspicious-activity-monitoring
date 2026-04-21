package com.grad.sam.service.reports.opencases;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
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
class OpenCasesReportEmailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private ObjectProvider<SpringTemplateEngine> templateEngineProvider;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine unusedTemplateEngine;

    private OpenCasesReportEmailService service;
    private SpringTemplateEngine templateEngine;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new OpenCasesReportEmailService(mailSenderProvider, templateEngineProvider);
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

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.sendReportEmail(reportResult(tempDir.resolve("open.csv"))));

        assertEquals("Open cases report email is disabled", exception.getMessage());
    }

    @Test
    void sendReportEmail_sends_email_with_attachment() throws Exception {
        Path reportPath = tempDir.resolve("open.csv");
        Files.writeString(reportPath, "data");
        MimeMessage mimeMessage = new MimeMessage((Session) null);

        ReflectionTestUtils.setField(service, "emailEnabled", true);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@test.com");
        ReflectionTestUtils.setField(service, "recipientsProperty", "a@test.com,b@test.com");
        ReflectionTestUtils.setField(service, "subject", "Open Cases Report");

        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(templateEngineProvider.getIfAvailable()).thenReturn(templateEngine);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        List<String> recipients = service.sendReportEmail(reportResult(reportPath));

        assertEquals(List.of("a@test.com", "b@test.com"), recipients);
        verify(mailSender).send(mimeMessage);
    }

    private OpenCasesReportResult reportResult(Path reportPath) {
        return OpenCasesReportResult.builder()
                .reportName(reportPath.getFileName().toString())
                .reportPath(reportPath)
                .recordCount(1)
                .openCount(1)
                .escalatedCount(0)
                .underReviewCount(0)
                .generatedAt(LocalDateTime.of(2026, 4, 21, 10, 15, 33))
                .trigger("manual")
                .categorySummaries(List.of(new OpenCasesCategorySummary("STRUCTURING", 1)))
                .rows(List.of())
                .build();
    }
}
