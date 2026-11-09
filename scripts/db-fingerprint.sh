#!/bin/sh
# Prints one line per table: name, number of rows and a fingerprint of all its data.
# Runs inside the PostgreSQL container.
set -e
PSQL="psql -U sovereign_admin -d sovereign -At -v ON_ERROR_STOP=1"

for TABLE in $($PSQL -c "select tablename from pg_tables where schemaname = 'public' order by tablename"); do
  $PSQL -c "select '$TABLE' || ' rows=' || count(*) || ' fingerprint=' || md5(coalesce(string_agg(t::text, '|' order by t::text), '')) from $TABLE t"
done
