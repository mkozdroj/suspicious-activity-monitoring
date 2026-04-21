package com.grad.sam.repository;

import com.grad.sam.enums.AlertStatus;
import com.grad.sam.model.Alert;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {

    Optional<Alert> findByAlertRef(String alertRef);

    List<Alert> findByStatus(AlertStatus status);

    @EntityGraph(attributePaths = {"alertRule", "account", "account.customer", "investigation"})
    List<Alert> findByStatusInOrderByTriggeredAtAscAlertIdAsc(List<AlertStatus> statuses);

    @EntityGraph(attributePaths = {"alertRule", "investigation"})
    List<Alert> findAllByOrderByTriggeredAtAscAlertIdAsc();

    List<Alert> findByAccount_AccountId(Long accountId);

    List<Alert> findByAssignedTo(String assignedTo);

    List<Alert> findByAlertScoreGreaterThanEqual(Short minScore);
}
