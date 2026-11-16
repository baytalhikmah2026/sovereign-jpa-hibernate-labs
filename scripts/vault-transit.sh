#!/bin/sh
# Turns Vault into the project's key service: the Transit engine keeps the key "sovereign-fields"
# inside Vault and only encrypts or decrypts data sent to it. Runs inside the Vault container.
# Safe to run more than once.
set -e
export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_TOKEN=local-root-token

echo "1/3 Enabling the Transit engine..."
vault secrets list | grep -q '^transit/' || vault secrets enable transit > /dev/null

echo "2/3 Creating the key sovereign-fields (AES-256-GCM, never leaves Vault)..."
vault read transit/keys/sovereign-fields > /dev/null 2>&1 || \
  vault write -f transit/keys/sovereign-fields type=aes256-gcm96 > /dev/null

echo "3/3 Allowing the application to use the key..."
vault policy write sovereign-app /scripts/sovereign-app-policy.hcl > /dev/null

echo "Vault Transit is ready: key sovereign-fields"
