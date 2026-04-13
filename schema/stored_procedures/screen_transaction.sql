-- checks a transaction against all actiove rules,
-- creates alerts for any matches,
-- marks the transacrion as screened

delimiter $$
drop procedure if exists screen_transaction$$

create procedure screen_transaction(in p_txn_id bigint)
begin
    declare v_rule_id bigint;
    declare v_severity varchar(10);
    declare v_alert_id bigint;
    declare done int default 0;

    declare cur_rule cursor for
        select rule_id, severity
        from alert_rule
        where is_active = true;

    declare continue handler for not found set done = 1;

    open cur_rule;

    read_loop: loop
        fetch cur_rule into v_rule_id, v_severity;
        if done then
            leave read_loop;
        end if;

        call raise_alert(p_txn_id, v_rule_id, null, v_alert_id);
    end loop;

    close cur_rule;

    update txn
    set status = 'SCREENED'
    where txn_id = p_txn_id;

    select 'Transaction screened successfully' as message;
end$$
delimiter ;
