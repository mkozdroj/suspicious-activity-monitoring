package com.grad.sam.repository;

import com.grad.sam.enums.AlertStatus;
import com.grad.sam.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {

    Optional<Alert> findByAlertRef(String alertRef);

    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findByAccount_AccountId(Long accountId);

    List<Alert> findByAssignedTo(String assignedTo);

    List<Alert> findByAlertScoreGreaterThanEqual(Short minScore);

    void raiseAlert(Integer txnId, String alertType, String description);

    default void updateStatus(Integer alertId, AlertStatus newStatus) {
        Logger log = LoggerFactory.getLogger(AlertRepository.class);
        findById(alertId).ifPresentOrElse(alert -> {
            alert.setStatus(newStatus);
            save(alert);
            log.info("Updated alert {} status to: {}", alertId, newStatus);
        }, () -> log.warn("Cannot update status - alert not found: {}", alertId));
    }

    default void assignTo(Integer alertId, String analystEmail) {
        Logger log = LoggerFactory.getLogger(AlertRepository.class);
        findById(alertId).ifPresentOrElse(alert -> {
            alert.setAssignedTo(analystEmail);
            if (AlertStatus.OPEN.equals(alert.getStatus())) {
                alert.setStatus(AlertStatus.UNDER_REVIEW);
            }
            save(alert);
            log.info("Assigned alert {} to: {}", alertId, analystEmail);
        }, () -> log.warn("Cannot assign - alert not found: {}", alertId));
    }

    default void delete(Integer alertId) {
        Logger log = LoggerFactory.getLogger(AlertRepository.class);
        deleteById(alertId);
        log.info("Deleted alert: {}", alertId);
    }
}
