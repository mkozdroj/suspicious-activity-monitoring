delimiter $$
drop procedure if exists match_watchlist$$

create procedure match_watchlist(
    in p_checked_name varchar(100),
    in p_threshold    decimal(5,2),
    in p_txn_id       bigint
)
begin
    declare v_normalized_name varchar(100);
    declare v_match_id        bigint;

    set v_normalized_name = upper(trim(p_checked_name));

    select coalesce(max(match_id), 0) + 1 into v_match_id from watchlist_match;

    insert into watchlist_match (match_id, txn_id, watchlist_id, match_type,
                                 match_score, matched_field, matched_value, status)
    select v_match_id + row_number() over (order by w.watchlist_id),
           p_txn_id,
           w.watchlist_id,
           'FUZZY_NAME',
           case
               when upper(trim(w.entity_name)) = v_normalized_name then 100.00
               when locate(upper(trim(w.entity_name)), v_normalized_name) > 0 then 85.00
               when locate(v_normalized_name, upper(trim(w.entity_name))) > 0 then 85.00
               else 0.00
    end as match_score,
           'customer_name',
           p_checked_name,
           'PENDING'
    from watchlist w
    where w.is_active = true
      and (upper(trim(w.entity_name)) = v_normalized_name
           or locate(upper(trim(w.entity_name)), v_normalized_name) > 0
           or locate(v_normalized_name, upper(trim(w.entity_name))) > 0)
    having match_score >= p_threshold;
    end$$
        delimiter ;