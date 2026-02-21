#!/usr/bin/env bash
# Deploy to Ubuntu production server.
# Run this from the project root on your laptop.
# Usage: ./scripts/deploy-prod.sh [user@host]
#
# Requirements on the server (one-time setup):
#   - Docker + Docker Compose installed
#   - The following files already present (copy once manually):
#       ~/pdf-management-app/.env
#       ~/pdf-management-app/docker-compose-infra.yml
#       ~/pdf-management-app/docker-compose-prod.yml
#       ~/pdf-management-app/services/Dockerfiles/api/Dockerfile.prod
#   - Infra already started: docker compose -f docker-compose-infra.yml up -d

set -euo pipefail

SERVER="${1:-stojani@192.168.2.107}"
REMOTE_DIR="~/pdf-management-app"
JAR="pdf-inbound-api/target/pdf-inbound-api-1.0-SNAPSHOT.jar"

echo "==> Building JAR..."
mvn clean package -DskipTests -q

echo "==> Copying JAR to $SERVER:$REMOTE_DIR ..."
ssh "$SERVER" "mkdir -p $REMOTE_DIR/pdf-inbound-api/target"
scp "$JAR" "$SERVER:$REMOTE_DIR/pdf-inbound-api/target/"

echo "==> Deploying on server..."
ssh "$SERVER" "
  cd $REMOTE_DIR
  docker compose -f docker-compose-prod.yml up -d --build
  docker compose -f docker-compose-prod.yml ps
"

echo "==> Done. Verifying..."
sleep 3
curl -sf "http://192.168.2.107:8080/api/documents/ping" && echo "OK" || echo "WARN: ping failed — app may still be starting"
