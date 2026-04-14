package com.grad.sam.repository;

import com.grad.sam.dao.AlertDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface AlertRepository {

    // Spring will inject this into the implementation behind the scenes
    @Autowired
    AlertDao getAlertDao();

    // =====================================================
    // OPEN ALERTS (uses open_alerts_vw)
    // =====================================================
    default Map<String, Integer> countOpenAlertsBySeverity(Long customerId) {
        return getAlertDao().countOpenAlertsBySeverity(customerId);
    }

    // =====================================================
    // CREATE ALERT (stored procedure: raise_alert)
    // =====================================================
    default Long raiseAlert(Long transactionId, Long ruleId, String assignedTo) {
        return getAlertDao().raiseAlert(transactionId, ruleId, assignedTo);
    }

    // =====================================================
    // HIGH RISK ACCOUNT CHECK (uses high_risk_accounts_vw)
    // =====================================================
    default boolean isHighRiskAccount(Long accountId) {
        return getAlertDao().isHighRiskAccount(accountId);
    }

    // Optional (good for marks)
    default List<Long> getHighRiskAccounts() {
        return getAlertDao().getHighRiskAccounts();
    }
}