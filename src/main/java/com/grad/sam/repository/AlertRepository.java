package com.grad.sam.repository;

import com.grad.sam.dao.AlertDao;
import com.grad.sam.enums.AlertStatus;
import org.springframework.beans.factory.annotation.Autowired;
import com.grad.sam.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {

    Optional<Alert> findByAlertRef(String alertRef);

    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findByAccount_AccountId(Long accountId);

    List<Alert> findByAssignedTo(String assignedTo);

    List<Alert> findByAlertScoreGreaterThanEqual(Short minScore);

    void raiseAlert(Integer txnId, String alertType, String description);
}
