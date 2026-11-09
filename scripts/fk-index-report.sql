-- Foreign keys whose first column is not the first column of any index.
-- Every row is a join or a delete that may scan the whole child table.
select c.conrelid::regclass::text as child_table,
       a.attname                  as fk_column,
       c.confrelid::regclass::text as parent_table
from pg_constraint c
join pg_attribute a on a.attrelid = c.conrelid and a.attnum = c.conkey[1]
where c.contype = 'f'
  and not exists (
      select 1
      from pg_index i
      where i.indrelid = c.conrelid
        and i.indkey[0] = c.conkey[1])
order by child_table, fk_column;
