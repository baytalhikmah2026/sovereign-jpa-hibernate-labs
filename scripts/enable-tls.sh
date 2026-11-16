#!/bin/sh
# Makes a private certificate authority (CA), a server certificate for PostgreSQL and a client
# certificate for the application user, switches TLS on and requires the client certificate for
# sovereign_app. Runs inside the postgres container as root. Safe to run more than once.
set -e
TLS_DIR=/var/lib/postgresql/tls
OUT=/certs
mkdir -p "$TLS_DIR" "$OUT"
cd "$TLS_DIR"

if [ ! -f ca.key ]; then
  echo "1/4 Creating the private certificate authority..."
  openssl req -new -x509 -days 825 -nodes -newkey rsa:3072 \
    -subj "/CN=Sovereign Local CA" -keyout ca.key -out ca.crt 2> /dev/null

  echo "2/4 Creating the PostgreSQL server certificate (localhost, postgres)..."
  openssl req -new -nodes -newkey rsa:3072 -subj "/CN=localhost" \
    -keyout server.key -out server.csr 2> /dev/null
  printf "subjectAltName=DNS:localhost,DNS:postgres,IP:127.0.0.1\nextendedKeyUsage=serverAuth\n" > server.ext
  openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
    -days 825 -extfile server.ext -out server.crt 2> /dev/null

  echo "3/4 Creating the client certificate for sovereign_app..."
  openssl req -new -nodes -newkey rsa:3072 -subj "/CN=sovereign_app" \
    -keyout client.key -out client.csr 2> /dev/null
  printf "extendedKeyUsage=clientAuth\n" > client.ext
  openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
    -days 825 -extfile client.ext -out client.crt 2> /dev/null
  openssl pkcs8 -topk8 -inform PEM -outform DER -nocrypt -in client.key -out client.pk8

  chown postgres:postgres server.key server.crt ca.crt
  chmod 600 server.key
else
  echo "1-3/4 Certificates already exist, reusing them."
fi

cp ca.crt client.crt client.pk8 "$OUT"/
chmod 644 "$OUT"/ca.crt "$OUT"/client.crt "$OUT"/client.pk8

echo "4/4 Switching TLS on and requiring the client certificate for sovereign_app..."
HBA="$PGDATA/pg_hba.conf"
if ! grep -q "^hostssl sovereign sovereign_app" "$HBA"; then
  {
    echo "hostnossl sovereign sovereign_app all reject"
    echo "hostssl sovereign sovereign_app all scram-sha-256 clientcert=verify-full"
    cat "$HBA"
  } > "$HBA.new"
  mv "$HBA.new" "$HBA"
  chown postgres:postgres "$HBA"
fi
PSQL="psql -v ON_ERROR_STOP=1 -U $POSTGRES_USER -d $POSTGRES_DB -tA"
$PSQL -c "alter system set ssl_cert_file = '$TLS_DIR/server.crt'" > /dev/null
$PSQL -c "alter system set ssl_key_file = '$TLS_DIR/server.key'" > /dev/null
$PSQL -c "alter system set ssl_ca_file = '$TLS_DIR/ca.crt'" > /dev/null
$PSQL -c "alter system set ssl = on" > /dev/null
$PSQL -c "select pg_reload_conf()" > /dev/null
echo "TLS is on. Client files for the application: certs/ca.crt, certs/client.crt, certs/client.pk8"
