#!/bin/sh
# Usage (inside the PostgreSQL container): sh /scripts/db-snapshot.sh <name>
set -e
NAME="$1"
if [ -z "$NAME" ]; then
  echo "Please give the snapshot a name, for example: before-change" >&2
  exit 1
fi

pg_dump -U sovereign_admin -d sovereign --format=custom --file="/snapshots/$NAME.dump"
sh /scripts/db-fingerprint.sh > "/snapshots/$NAME.fingerprint"

echo "Snapshot '$NAME' saved:"
cat "/snapshots/$NAME.fingerprint"
