#!/bin/sh
# Starts a PostgreSQL streaming replica. On first start it copies the primary with pg_basebackup;
# -R writes standby.signal and the connection to the primary, so PostgreSQL starts as a read-only standby.
# Runs as the OS user "postgres". Needs PGPASSWORD (the replicator's password) and optionally PRIMARY_HOST.
set -e
PRIMARY_HOST="${PRIMARY_HOST:-postgres}"
if [ ! -s "$PGDATA/PG_VERSION" ]; then
  until pg_basebackup -h "$PRIMARY_HOST" -U replicator -D "$PGDATA" -R -X stream; do
    echo "Waiting for the primary at $PRIMARY_HOST ..."
    sleep 2
  done
  chmod 0700 "$PGDATA"
fi
exec postgres
