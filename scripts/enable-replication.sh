#!/bin/sh
# Prepares the primary PostgreSQL for a streaming replica: a replication user and a pg_hba.conf rule.
# Runs inside the primary's container as the OS user "postgres". Safe to run more than once.
set -e
PSQL="psql -v ON_ERROR_STOP=1 -U $POSTGRES_USER -d $POSTGRES_DB -tA"
if [ "$($PSQL -c "select count(*) from pg_roles where rolname = 'replicator'")" = "0" ]; then
  $PSQL -c "create role replicator with replication login password 'replica-local-only'"
fi
if ! grep -q "^host replication replicator" "$PGDATA/pg_hba.conf"; then
  echo "host replication replicator all scram-sha-256" >> "$PGDATA/pg_hba.conf"
fi
$PSQL -c "select pg_reload_conf()" > /dev/null
echo "Replication enabled for user replicator"
