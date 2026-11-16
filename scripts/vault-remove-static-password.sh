#!/bin/sh
# Removes the fixed database user name and password (Lab 4) from Vault, keeping only the field key (Lab 91).
# Older versions of the secret are not kept, so the old password cannot be read back from Vault's history.
# Runs inside the Vault container.
set -e
export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_TOKEN=local-root-token

if ! FIELD_KEY=$(vault kv get -mount=secret -field=sovereign.crypto.field-key sovereign-persistence 2> /dev/null); then
  echo "No field key found: run scripts/vault-field-key.sh first (Lab 91)." >&2
  exit 1
fi
vault kv metadata put -mount=secret -max-versions=1 sovereign-persistence > /dev/null
vault kv put -mount=secret sovereign-persistence sovereign.crypto.field-key="$FIELD_KEY" > /dev/null
echo "Vault no longer holds a fixed database password (secret/sovereign-persistence keeps only the field key)."
