#!/bin/sh
# Runs inside the postgres container as the postgres user. Requires TLS for every member of sovereign_app
# (the short-lived Vault users): plain connections are refused, TLS connections must show a client certificate
# signed by the project's CA. The rules go right below the two sovereign_app rules from Lab 86.
set -e
HBA="$PGDATA/pg_hba.conf"
if grep -q "+sovereign_app" "$HBA"; then
  echo "The rules for Vault users already exist; nothing changed."
  exit 0
fi
if ! head -n 1 "$HBA" | grep -q "^hostnossl sovereign sovereign_app"; then
  echo "Run scripts/enable-tls.sh first (Lab 86)." >&2
  exit 1
fi
{
  head -n 2 "$HBA"
  echo "hostnossl sovereign +sovereign_app all reject"
  echo "hostssl sovereign +sovereign_app all scram-sha-256 clientcert=verify-ca"
  tail -n +3 "$HBA"
} > "$HBA.new"
mv "$HBA.new" "$HBA"
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tA -c "select pg_reload_conf()" > /dev/null
echo "Vault users must now connect with TLS and a client certificate."
