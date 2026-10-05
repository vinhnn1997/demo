#!/usr/bin/env bash
set -eu

APP_DIR="${1:?Application directory is required}"
IMAGE_REPOSITORY="${2:?Image repository is required}"
IMAGE_TAG="${3:?Image tag is required}"
TARGET_SERVICE="${4:-all}"

cd "$APP_DIR"
export IMAGE_REPOSITORY IMAGE_TAG

# The production .env.production file must already exist on the deployment server.
test -f .env.production

if [ "$TARGET_SERVICE" = all ]; then
	docker compose --env-file .env.production -f docker-compose.prod.yml pull
	docker compose --env-file .env.production -f docker-compose.prod.yml up -d --remove-orphans
else
	case "$TARGET_SERVICE" in
		api-gateway|eureka-server|province-service|organization-service|taxpayer-service|fine-service|payment-service|identity-service|dataplatform) ;;
		*) echo "Unsupported service: $TARGET_SERVICE" >&2; exit 2 ;;
	esac

	override_file="$(mktemp)"
	trap 'rm -f "$override_file"' EXIT
	printf 'services:\n  %s:\n    image: %s/%s:%s\n' \
		"$TARGET_SERVICE" "$IMAGE_REPOSITORY" "$TARGET_SERVICE" "$IMAGE_TAG" > "$override_file"
	docker compose --env-file .env.production -f docker-compose.prod.yml -f "$override_file" pull "$TARGET_SERVICE"
	docker compose --env-file .env.production -f docker-compose.prod.yml -f "$override_file" up -d --no-deps "$TARGET_SERVICE"
fi
docker compose --env-file .env.production -f docker-compose.prod.yml ps
