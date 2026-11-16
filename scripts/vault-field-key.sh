#!/bin/sh
# Creates the field encryption key once and stores it next to the database credentials in Vault, where the
# application reads it at start-up (property sovereign.crypto.field-key). Runs inside the Vault container.
# Safe to run more than once: an existing key is never replaced (that would make encrypted data unreadable).
set -e
export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_TOKEN=local-root-token

if vault kv get -mount=secret -field=sovereign.crypto.field-key sovereign-persistence > /dev/null 2>&1; then
  echo "The field key already exists; nothing changed."
  exit 0
fi
FIELD_KEY=$(head -c 32 /dev/urandom | base64 | tr -d '\n')
vault kv patch -mount=secret sovereign-persistence sovereign.crypto.field-key="$FIELD_KEY" > /dev/null
echo "Field key stored in Vault (secret/sovereign-persistence). It is never printed."
