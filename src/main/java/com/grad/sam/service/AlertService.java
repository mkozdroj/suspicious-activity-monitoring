package com.grad.sam.service;

import com.grad.sam.exception.InvalidInputException;
import com.grad.sam.repository.AlertRepository;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
public class AlertService {
    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }


    public void raiseAlert(@NotNull Integer txnId, @NotNull String alertType, @NotNull String description) {
        log.info("Raising alert for txnId {}: {} - {}", txnId, alertType, description);

        alertRepository.raiseAlert(txnId, alertType, description);
    }
}
