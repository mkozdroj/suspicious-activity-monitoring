package com.grad.sam.repository;

import com.grad.sam.dao.AlertDao;
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

    // =====================================================
    // OPEN ALERTS (uses open_alerts_vw)
    // =====================================================
//    default Map<String, Integer> countOpenAlertsBySeverity(Long customerId) {
//        return getAlertDao().countOpenAlertsBySeverity(customerId);
//    }

    List<Alert> findByStatus(String status);

    List<Alert> findByAccount_AccountId(Integer accountId);
    // =====================================================
    // CREATE ALERT (stored procedure: raise_alert)
    // =====================================================
//    default Long raiseAlert(Long transactionId, Long ruleId, String assignedTo) {
//        return getAlertDao().raiseAlert(transactionId, ruleId, assignedTo);
//    }

    List<Alert> findByAssignedTo(String assignedTo);

    List<Alert> findByAlertScoreGreaterThanEqual(Short minScore);
}
    // =====================================================
    // HIGH RISK ACCOUNT CHECK (uses high_risk_accounts_vw)
    // =====================================================
//    default boolean isHighRiskAccount(Long accountId) {
//        return getAlertDao().isHighRiskAccount(accountId);
//    }
//
//    // Optional (good for marks)
//    default List<Long> getHighRiskAccounts() {
//        return getAlertDao().getHighRiskAccounts();
//    }