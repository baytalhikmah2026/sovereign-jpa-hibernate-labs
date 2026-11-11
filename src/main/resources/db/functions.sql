-- Logic that lives inside PostgreSQL, next to the data (Lab 33).
-- Runs after Hibernate has created the tables. Every statement ends with a semicolon,
-- and the bodies between the $$ markers contain none, so the script splits cleanly.

create or replace function sov_customer_lifetime_value(p_customer_id uuid)
returns numeric
language sql
stable
as $$
    select coalesce(sum(l.line_total_amount), 0.00)
    from sov_purchase_order o
    join sov_order_line l on l.order_id = o.id
    where o.customer_id = p_customer_id
      and o.status <> 'CX'
$$;

create or replace function sov_top_products(p_sku_prefix text, p_limit integer)
returns table (sku varchar, units bigint, revenue numeric)
language sql
stable
as $$
    select p.sku, sum(l.quantity), sum(l.line_total_amount)
    from sov_order_line l
    join sov_product p on p.id = l.product_id
    where p.sku like p_sku_prefix || '%'
    group by p.sku
    order by sum(l.line_total_amount) desc, p.sku
    limit p_limit
$$;

create or replace procedure sov_restock(p_warehouse_code varchar, p_sku varchar, p_quantity integer)
language sql
as $$
    insert into sov_inventory_item (warehouse_code, sku, quantity_on_hand, version)
    values (p_warehouse_code, p_sku, p_quantity, 0)
    on conflict (warehouse_code, sku)
    do update set quantity_on_hand = sov_inventory_item.quantity_on_hand + excluded.quantity_on_hand,
                  version = sov_inventory_item.version + 1
$$;
