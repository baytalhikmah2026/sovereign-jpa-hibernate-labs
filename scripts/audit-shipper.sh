#!/bin/sh
# Sidecar: copies new lines of every write-log segment from /audit (read-only) to /worm, every 2 seconds.
# It only ever appends: a line that reached /worm is never changed or removed, even if the original is edited.
set -e
mkdir -p /worm
echo "audit-shipper: watching /audit, shipping to /worm"
while true; do
  for source in /audit/*.jsonl; do
    [ -e "$source" ] || continue
    target="/worm/$(basename "$source")"
    shipped=$(cat "$target" 2>/dev/null | wc -l)
    total=$(wc -l < "$source")
    if [ "$total" -gt "$shipped" ]; then
      tail -n +"$((shipped + 1))" "$source" | head -n "$((total - shipped))" >> "$target"
      echo "audit-shipper: $(basename "$source") +$((total - shipped)) lines (now $total)"
    fi
  done
  sleep 2
done
