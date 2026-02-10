#!/usr/bin/env bash
set -euo pipefail

docker compose -f docker-compose-infra.yml up -d
exec mvn -pl pdf-inbound-api spring-boot:run -Dspring-boot.run.profiles=dev
