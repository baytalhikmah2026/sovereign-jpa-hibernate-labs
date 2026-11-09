#!/bin/sh
# Runs inside the Vault container. Progress goes to the screen (stderr);
# the SQL for PostgreSQL goes to stdout, straight into the next command.
set -e
export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_TOKEN=local-root-token

echo "1/5 Generating a random password for the application user..." >&2
APP_PASSWORD=$(head -c 48 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 32)

echo "2/5 Storing the database credentials in Vault..." >&2
vault kv put -mount=secret sovereign-persistence \
  spring.datasource.username=sovereign_app \
  spring.datasource.password="$APP_PASSWORD" > /dev/null

echo "3/5 Writing the read-only policy and the AppRole identity..." >&2
vault policy write sovereign-app /scripts/sovereign-app-policy.hcl > /dev/null
vault auth list | grep -q '^approle/' || vault auth enable approle > /dev/null
vault write auth/approle/role/sovereign-app \
  token_policies=sovereign-app token_ttl=1h token_max_ttl=4h > /dev/null

echo "4/5 Issuing the application's badge (role-id + secret-id)..." >&2
ROLE_ID=$(vault read -field=role_id auth/approle/role/sovereign-app/role-id)
SECRET_ID=$(vault write -f -field=secret_id auth/approle/role/sovereign-app/secret-id)
cat > /config/vault-approle.yaml <<YAML
spring:
  cloud:
    vault:
      app-role:
        role-id: $ROLE_ID
        secret-id: $SECRET_ID
YAML

echo "5/5 Sending the database user command to PostgreSQL..." >&2
cat <<SQL
SELECT 'CREATE ROLE sovereign_app LOGIN'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sovereign_app') \gexec
ALTER ROLE sovereign_app PASSWORD '$APP_PASSWORD';
GRANT ALL ON SCHEMA public TO sovereign_app;
SQL

echo "Vault is ready. The password is stored only in Vault." >&2
