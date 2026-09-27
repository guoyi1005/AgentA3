#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/deploy/compose.submission.yml"
ENV_FILE="${ROOT_DIR}/deploy/.env"
SQL_FILE="${ROOT_DIR}/deploy/seed-campus-activities.sql"
IMAGES_ARCHIVE="${ROOT_DIR}/deploy/campus-activity-images.tar.gz"

compose=(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE")

for required in "$COMPOSE_FILE" "$ENV_FILE" "$SQL_FILE" "$IMAGES_ARCHIVE"; do
  if [[ ! -f "$required" ]]; then
    echo "[FAIL] Missing required file: $required" >&2
    exit 1
  fi
done

cd "$ROOT_DIR"

echo "[1/2] Importing activity records..."
"${compose[@]}" exec -T mysql \
  sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 "$MYSQL_DATABASE"' \
  < "$SQL_FILE"

echo "[2/2] Copying activity cover images..."
backend_id="$("${compose[@]}" ps -q backend)"
if [[ -z "$backend_id" ]]; then
  echo "[FAIL] backend container is not running" >&2
  exit 1
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT
tar -xzf "$IMAGES_ARCHIVE" -C "$tmp_dir"
"${compose[@]}" exec -T backend mkdir -p /app/uploads/smart-campus
docker cp "$tmp_dir/smart-campus/." "$backend_id:/app/uploads/smart-campus/"

echo "[PASS] Campus activities and cover images imported."
echo "Refresh http://8.137.33.56/activities and verify the list."