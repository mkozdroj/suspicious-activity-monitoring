-- test: raise_alert

-- test 1a: high-risk geography (Iran)
-- rule 5: Cash Structuring Below Reporting Threshold (HIGH severity = score 75)
-- Transaction 5: Iran wire, $12,017.50
-- Expected: Alert created with score 75, status OPEN
select 'test 1a' as test;
call raise_alert(5, 5, 'compliance@bank.com', @alert_id_1);
select alert_id, alert_ref, rule_id, alert_score, status, assigned_to
from alert where alert_id = @alert_id_1;

-- test 1b: high-risk geography (Syria)
-- Rule 5: Cash Structuring Below Reporting Threshold (HIGH severity = score 75)
-- Transaction 18: Syria wire, $31,191.20 CHF
-- Expected: Alert created with score 75, status OPEN
select 'test 1b' as test;
call raise_alert(18, 5, 'analyst.john@bank.com', @alert_id_2);
select alert_id, alert_ref, rule_id, alert_score, status, assigned_to
from alert where alert_id = @alert_id_2;

-- test 1c: watchlist match (critical)
-- Rule 7: Watchlist Name Match (CRITICAL severity = score 95)
-- Transaction 1: Customer name might match watchlist
-- Expected: Alert created with score 95, status OPEN
select 'test 1c' as test;
call raise_alert(1, 7, null, @alert_id_3);
select alert_id, alert_ref, rule_id, alert_score, status, assigned_to
from alert where alert_id = @alert_id_3;

-- summary
select 'summary' as result;
select alert_id, alert_ref, rule_id, alert_score, status, assigned_to
from alert
where alert_id in (@alert_id_1, @alert_id_2, @alert_id_3)
order by alert_id;

-- ===============

-- test: screen_transaction

-- test 2a: screen a single transaction
-- Transaction 5 (Iran wire, $12,017.50)
-- Expected: Alerts created for EACH active rule, transaction status = SCREENED
select 'test 2a' as test;
select count(*) as alerts_before from alert where txn_id = 5;
call screen_transaction(5);
select count(*) as alerts_after from alert where txn_id = 5;

select alert_id, alert_ref, rule_id, alert_score, status
from alert
where txn_id = 5
order by alert_id;

select txn_id, txn_ref, status
from txn
where txn_id = 5;

-- test 2b: different transaction
-- Transaction 18 (Syria wire, $31,191.20)
-- Expected: Alerts created for EACH active rule, transaction status = SCREENED
select 'test 2b' as test;
select count(*) as alerts_before from alert where txn_id = 18;
call screen_transaction(18);
select count(*) as alerts_after from alert where txn_id = 18;

select alert_id, alert_ref, rule_id, alert_score, status
from alert
where txn_id = 18
order by alert_id;

select txn_id, txn_ref, status
from txn
where txn_id = 18;

-- test 2c: Screen transaction with low-risk profile
-- Transaction 3 (Low amount, no high-risk indicators)
-- Expected: Alerts created for ALL active rules (screening catches everything), status = SCREENED
select 'test 2c' as test;
select count(*) as alerts_before from alert where txn_id = 3;
call screen_transaction(3);
select count(*) as alerts_after from alert where txn_id = 3;

select alert_id, alert_ref, rule_id, alert_score, status
from alert
where txn_id = 3
order by alert_id;

select txn_id, txn_ref, status
from txn
where txn_id = 3;

-- summary
select 'summary' as result;
select txn_id, count(*) as total_alerts, group_concat(rule_id) as rules_triggered
from alert
where txn_id IN (5, 18, 3)
group by txn_id
order by txn_id;

-- ===================
-- test: match_watchlist

-- test 3a: Exact match
select 'test 3a' as test;
call match_watchlist('Bank Mellat', 100.00, 5);
select match_id, txn_id, watchlist_id, match_type, match_score, matched_value, status
from watchlist_match
where txn_id = 5
order by match_id desc;

-- test 3b: Partial match
select 'test 3b' as test;
call match_watchlist('Bank Mellat Iran', 85.00, 18);
select match_id, txn_id, watchlist_id, match_type, match_score, matched_value, status
from watchlist_match
where txn_id = 18
order by match_id desc;

-- test 3c: No match
select 'test 3c' as test;
call match_watchlist('John Smith Import Export', 100.00, 3);
select match_id, txn_id, watchlist_id, match_type, match_score, matched_value, status
from watchlist_match
where txn_id = 3
order by match_id desc;

-- test 3d: Lower threshold
select 'test 3d' as test;
call match_watchlist('Commercial Bank', 50.00, 1);
select match_id, txn_id, watchlist_id, match_type, match_score, matched_value, status
from watchlist_match
where txn_id = 1
order by match_id desc;