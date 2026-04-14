package com.grad.sam.dao;

import com.grad.sam.model.Alert;
import com.grad.sam.repository.AlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class AlertDao {

    private final AlertRepository alertRepository;

    public AlertDao(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public List<Alert> findAll() {
        return alertRepository.findAll();
    }

    public List<Alert> findOpenAlerts() {
        return alertRepository.findByStatus("OPEN");
    }

    public List<Alert> findByStatus(String status) {
        return alertRepository.findByStatus(status);
    }

    public List<Alert> findByAccountId(Long accountId) {
        return alertRepository.findByAccount_AccountId(accountId);
    }

    public List<Alert> findByAssignedTo(String assignedTo) {
        return alertRepository.findByAssignedTo(assignedTo);
    }

    public Optional<Alert> findById(Integer alertId) {
        return alertRepository.findById(alertId);
    }

    public Optional<Alert> findByAlertRef(String alertRef) {
        return alertRepository.findByAlertRef(alertRef);
    }

    public Alert save(Alert alert) {
        Alert saved = alertRepository.save(alert);
        log.info("Saved alert: {} ({})", saved.getAlertRef(), saved.getAlertId());
        return saved;
    }

    public void updateStatus(Integer alertId, String newStatus) {
        alertRepository.findById(alertId).ifPresentOrElse(alert -> {
            alert.setStatus(newStatus);
            alertRepository.save(alert);
            log.info("Updated alert {} status to: {}", alertId, newStatus);
        }, () -> log.warn("Cannot update status — alert not found: {}", alertId));
    }

    public void assignTo(Integer alertId, String analystEmail) {
        alertRepository.findById(alertId).ifPresentOrElse(alert -> {
            alert.setAssignedTo(analystEmail);
            if ("OPEN".equals(alert.getStatus())) {
                alert.setStatus("UNDER_REVIEW");
            }
            alertRepository.save(alert);
            log.info("Assigned alert {} to: {}", alertId, analystEmail);
        }, () -> log.warn("Cannot assign — alert not found: {}", alertId));
    }

    public void delete(Integer alertId) {
        alertRepository.deleteById(alertId);
        log.info("Deleted alert: {}", alertId);
    }
}