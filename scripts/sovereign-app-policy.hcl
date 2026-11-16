path "secret/data/sovereign-persistence" {
  capabilities = ["read"]
}

path "secret/data/sovereign-persistence/*" {
  capabilities = ["read"]
}

path "secret/data/application" {
  capabilities = ["read"]
}

path "secret/data/application/*" {
  capabilities = ["read"]
}

path "transit/encrypt/sovereign-fields" {
  capabilities = ["update"]
}

path "transit/decrypt/sovereign-fields" {
  capabilities = ["update"]
}

path "database/creds/sovereign-app-dynamic" {
  capabilities = ["read"]
}

path "sys/leases/renew" {
  capabilities = ["update"]
}

path "sys/leases/revoke" {
  capabilities = ["update"]
}
