#!/bin/sh
# Turns Vault into the issuer of short-lived PostgreSQL users (database secrets engine).
# Runs inside the Vault container. Safe to run more than once.
set -e
export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_TOKEN=local-root-token

echo "1/4 Enabling the database secrets engine..."
vault secrets list | grep -q '^database/' || vault secrets enable database > /dev/null

echo "2/4 Connecting Vault to PostgreSQL with the administrator account..."
vault write database/config/sovereign \
  plugin_name=postgresql-database-plugin \
  connection_url='postgresql://{{username}}:{{password}}@postgres:5432/sovereign?sslmode=disable' \
  username=sovereign_admin \
  password=local-admin-only \
  allowed_roles=sovereign-app-dynamic > /dev/null

echo "3/4 Defining the role sovereign-app-dynamic (users live 2 minutes, renewable up to 6)..."
vault write database/roles/sovereign-app-dynamic \
  db_name=sovereign \
  creation_statements=@/scripts/vault-db-create-user.sql \
  revocation_statements=@/scripts/vault-db-drop-user.sql \
  default_ttl=2m \
  max_ttl=6m > /dev/null

echo "4/4 Allowing the application to request users..."
vault policy write sovereign-app /scripts/sovereign-app-policy.hcl > /dev/null

echo "Vault issues database users at database/creds/sovereign-app-dynamic"
