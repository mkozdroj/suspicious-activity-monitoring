package com.grad.sam.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Repository
public class AlertDao {

    private static final Logger log = LoggerFactory.getLogger(AlertDao.class);

    private final DataSource dataSource;

    public AlertDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // =====================================================
    // 1. USE VIEW: open_alerts_vw (REPLACED JOIN)
    // =====================================================
    public Map<String, Integer> countOpenAlertsBySeverity(Long customerId) {

        String sql = """
                SELECT severity, COUNT(*) AS cnt
                FROM open_alerts_vw
                WHERE customer_id = ?
                GROUP BY severity
                """;

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setLong(1, customerId);
            rs = ps.executeQuery();

            Map<String, Integer> counts = new LinkedHashMap<>();
            while (rs.next()) {
                counts.put(rs.getString("severity"), rs.getInt("cnt"));
            }

            return counts;

        } catch (SQLException e) {
            log.error("countOpenAlertsBySeverity() failed — customerId={}", customerId, e);
            throw new RuntimeException(e);

        } finally {
            closeQuietly(rs, ps, conn);
        }
    }

    // =====================================================
    // 2. STORED PROCEDURE: raise_alert
    // =====================================================
    public Long raiseAlert(Long txnId, Long ruleId, String assignedTo) {
        return (Long) execute("raise_alert", Types.BIGINT, txnId, ruleId, assignedTo);
    }

    // =====================================================
    // 3. USE VIEW: high_risk_accounts_vw
    // =====================================================
    public boolean isHighRiskAccount(Long accountId) {

        String sql = """
                SELECT 1
                FROM high_risk_accounts_vw
                WHERE account_id = ?
                """;

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
            ps   = conn.prepareStatement(sql);
            ps.setLong(1, accountId);
            rs = ps.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            log.error("isHighRiskAccount() failed — accountId={}", accountId, e);
            throw new RuntimeException(e);

        } finally {
            closeQuietly(rs, ps, conn);
        }
    }

    public List<Long> getHighRiskAccounts() {

        String sql = "SELECT account_id FROM high_risk_accounts_vw";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
            ps   = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            List<Long> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getLong("account_id"));
            }

            return list;

        } catch (SQLException e) {
            log.error("getHighRiskAccounts() failed", e);
            throw new RuntimeException(e);

        } finally {
            closeQuietly(rs, ps, conn);
        }
    }

    // =====================================================
    // GENERIC STORED PROCEDURE EXECUTOR
    // =====================================================
    private Object execute(String procedureName, int outType, Object... params) {

        Connection conn = null;
        CallableStatement cs = null;

        try {
            conn = dataSource.getConnection();

            StringBuilder call = new StringBuilder("{ call ");
            call.append(procedureName).append("(");

            int totalParams = params.length + 1; // +1 for OUT param
            for (int i = 0; i < totalParams; i++) {
                call.append("?");
                if (i < totalParams - 1) call.append(",");
            }
            call.append(") }");

            cs = conn.prepareCall(call.toString());

            // IN params
            for (int i = 0; i < params.length; i++) {
                cs.setObject(i + 1, params[i]);
            }

            // OUT param
            cs.registerOutParameter(params.length + 1, outType);

            cs.execute();

            return cs.getObject(params.length + 1);

        } catch (SQLException e) {
            log.error("Stored procedure call failed: {}", procedureName, e);
            throw new RuntimeException(e);

        } finally {
            closeQuietly(null, cs, conn);
        }
    }

    // =====================================================
    // CLEANUP
    // =====================================================
    private void closeQuietly(ResultSet rs, Statement stmt, Connection conn) {
        try { if (rs != null) rs.close(); } catch (Exception ignored) {}
        try { if (stmt != null) stmt.close(); } catch (Exception ignored) {}
        try { if (conn != null) conn.close(); } catch (Exception ignored) {}
    }
}