-- inserts a new alert record for a matched transaction
-- sets its initial severity and status

delimiter $$
drop procedure if exists raise_alert$$

create procedure raise_alert(
	in p_transaction_id bigint,
    in p_rule_id bigint,
    in p_assigned_to varchar(60),
    out o_alert_id bigint
)
begin
	declare v_severity varchar(10);
	declare v_account_id bigint;
	declare v_alert_score smallint;
	declare v_next_id bigint;
	declare v_alert_ref varchar(15);
	declare exit handler for sqlexception
	begin
		set o_alert_id = -1;
	end;

	select ar.severity into v_severity
	from alert_rule ar
	where ar.rule_id = p_rule_id
	limit 1;

	select t.account_id into v_account_id
	from transaction t
	where t.transaction_id = p_transaction_id
	limit 1;

	select coalesce(max(alert_id), 0) + 1
	into v_next_id
	from alert;

	set v_alert_ref = concat('ALT', lpad(v_next_id, 6, '0'));

	set v_alert_score = case v_severity
		when 'CRITICAL' then 95
		when 'HIGH' then 75
		when 'MEDIUM' then 55
		else 35
	end;

	insert into alert (alert_id, alert_ref, rule_id, account_id, transaction_id, triggered_at, alert_score, status, assigned_to)
	values (v_next_id, v_alert_ref, p_rule_id, v_account_id, p_transaction_id, now(), v_alert_score, 'OPEN', p_assigned_to);

	set o_alert_id = v_next_id;
end$$
delimiter ;
