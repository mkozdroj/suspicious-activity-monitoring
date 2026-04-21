package com.grad.sam.service;

import com.grad.sam.enums.AlertSeverity;
import com.grad.sam.enums.AlertStatus;
import com.grad.sam.enums.RuleCategory;
import com.grad.sam.exception.DataNotFoundException;
import com.grad.sam.exception.InvalidInputException;
import com.grad.sam.model.Account;
import com.grad.sam.model.Alert;
import com.grad.sam.model.AlertRule;
import com.grad.sam.model.Txn;
import com.grad.sam.repository.AlertRepository;
import com.grad.sam.repository.AlertRuleRepository;
import com.grad.sam.repository.TxnRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class AlertService {
    private static final AtomicInteger ALERT_SEQUENCE = new AtomicInteger(0);

    private final AlertRepository alertRepository;
    private final TxnRepository txnRepository;
    private final AlertRuleRepository alertRuleRepository;

    public Alert createAlert(@NotNull Txn txn,
                             @NotNull Account account,
                             @NotNull AlertRule rule,
                             @NotNull String description) {

        Alert alert = new Alert();
        alert.setAlertRef(generateAlertRef());
        alert.setAlertRule(rule);
        alert.setAccount(account);
        alert.setTxn(txn);
        alert.setStatus(AlertStatus.OPEN);
        alert.setTriggeredAt(LocalDateTime.now());
        alert.setNotes(truncate(description));
        alert.setAlertScore(resolveScore(rule));

        return alertRepository.save(alert);
    }

    public Alert raiseAlert(@NotNull Integer txnId,
                            @NotNull String alertType,
                            @NotNull String description) {

        Txn txn = txnRepository.findById(txnId)
                .orElseThrow(() -> new DataNotFoundException("Transaction not found for id: " + txnId));

        Account account = txn.getAccount();
        if (account == null) {
            throw new IllegalStateException("Account not found for transaction: " + txnId);
        }

        RuleCategory category;
        try {
            category = RuleCategory.valueOf(alertType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Unsupported alert type: " + alertType);
        }

        AlertRule rule = alertRuleRepository.findByRuleCategoryAndIsActiveTrue(category).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active alert rule for category: " + category));

        return createAlert(txn, account, rule, description);
    }

    @Transactional
    public Alert updateStatus(@NotNull @Positive Integer alertId,
                              @NotNull AlertStatus newStatus) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new DataNotFoundException("Alert not found for id: " + alertId));

        alert.setStatus(newStatus);
        Alert saved = alertRepository.save(alert);
        log.info("Updated alert {} status to: {}", alertId, newStatus);
        return saved;
    }

    @Transactional
    public Alert assignTo(@NotNull @Positive Integer alertId,
                          @NotNull String analystEmail) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new DataNotFoundException("Alert not found for id: " + alertId));

        alert.setAssignedTo(analystEmail);
        if (AlertStatus.OPEN.equals(alert.getStatus())) {
            alert.setStatus(AlertStatus.UNDER_REVIEW);
        }

        Alert saved = alertRepository.save(alert);
        log.info("Assigned alert {} to: {}", alertId, analystEmail);
        return saved;
    }

    @Transactional
    public void deleteAlert(@NotNull @Positive Integer alertId) {
        alertRepository.deleteById(alertId);
        log.info("Deleted alert: {}", alertId);
    }

    private String truncate(String description) {
        if (description == null) {
            return null;
        }
        return description.length() > 500
                ? description.substring(0, 497) + "..."
                : description;
    }

    private short resolveScore(AlertRule rule) {
        AlertSeverity severity = rule.getSeverity();
        if (severity == null) {
            return 50;
        }

        return switch (severity) {
            case LOW -> 25;
            case MEDIUM -> 50;
            case HIGH -> 75;
            case CRITICAL -> 100;
        };
    }

    private String generateAlertRef() {
        long timePart = System.currentTimeMillis() % 100_000_000L;
        int sequencePart = ALERT_SEQUENCE.updateAndGet(current -> (current + 1) % 1000);
        return String.format("ALT-%08d%03d", timePart, sequencePart);
    }
}
