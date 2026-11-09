#!/bin/sh
# Usage (inside the PostgreSQL container): sh /scripts/db-restore.sh <name>
set -e
NAME="$1"
if [ ! -f "/snapshots/$NAME.dump" ]; then
  echo "No snapshot named '$NAME'. Available snapshots:" >&2
  ls /snapshots | sed -n 's/\.dump$//p' >&2
  exit 1
fi

pg_restore -U sovereign_admin -d sovereign --clean --if-exists --single-transaction "/snapshots/$NAME.dump"
sh /scripts/db-fingerprint.sh > /tmp/after-restore.fingerprint

if diff "/snapshots/$NAME.fingerprint" /tmp/after-restore.fingerprint > /dev/null; then
  echo "Restore of '$NAME' VERIFIED: every table matches the snapshot."
  cat /tmp/after-restore.fingerprint
else
  echo "Restore of '$NAME' FAILED verification:" >&2
  diff "/snapshots/$NAME.fingerprint" /tmp/after-restore.fingerprint >&2 || true
  exit 2
fi
