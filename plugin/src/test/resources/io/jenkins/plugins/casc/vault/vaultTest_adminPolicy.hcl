path "secret/*" {
    capabilities = ["create", "read", "list"]
}

path "kv-v1/*" {
    capabilities = ["create", "read", "list"]
}

path "kv-v2/*" {
    capabilities = ["create", "read", "list"]
}
