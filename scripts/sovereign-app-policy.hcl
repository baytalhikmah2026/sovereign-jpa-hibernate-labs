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
