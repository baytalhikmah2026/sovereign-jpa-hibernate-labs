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
