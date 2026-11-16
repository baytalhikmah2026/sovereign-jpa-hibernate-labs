#!/bin/sh
# Rollback of Lab 93: gives the fixed user sovereign_app a new password and puts it back into Vault, next to the
# field key (kv patch keeps the other keys). Runs inside the Vault container; the SQL for PostgreSQL goes to stdout.
set -e
export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_TOKEN=local-root-token

APP_PASSWORD=$(head -c 48 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 32)
vault kv patch -mount=secret sovereign-persistence \
  spring.datasource.username=sovereign_app \
  spring.datasource.password="$APP_PASSWORD" > /dev/null
echo "ALTER ROLE sovereign_app LOGIN PASSWORD '$APP_PASSWORD';"
echo "The fixed password is back in Vault; sovereign_app may log in again." >&2
