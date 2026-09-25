#!/usr/bin/env bash
set -eu

APP_DIR="${1:?Application directory is required}"
IMAGE_REPOSITORY="${2:?Image repository is required}"
IMAGE_TAG="${3:?Image tag is required}"

cd "$APP_DIR"
export IMAGE_REPOSITORY IMAGE_TAG

# The production .env.production file must already exist on the deployment server.
test -f .env.production

docker compose --env-file .env.production -f docker-compose.prod.yml pull
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --remove-orphans
docker compose --env-file .env.production -f docker-compose.prod.yml ps
