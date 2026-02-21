# Make targets for local development
# - build: fast build (skip tests) to verify compile
# - build-full: start required local services and run full mvn clean install
# - up-local: start postgres via Rancher socket helper
# - down-local: stop services (docker compose down)
# - test: run unit tests only

.PHONY: build build-full up-local down-local test

# Fast compile without tests (useful for quick verification)
build:
	mvn -DskipTests=true -T1C clean install

# Full build: ensure local dependencies are up, then run full build (tests included)
# Uses the scripts/use-rancher-docker.sh helper to point docker to Rancher Desktop socket
build-full: up-local
	mvn -T1C clean install

# Start postgres using the helper script (auto-detects Rancher socket)
up-local:
	./scripts/use-rancher-docker.sh

# Stop compose services (uses same socket detection)
down-local:
	./scripts/use-rancher-docker.sh -- docker compose down

# Run unit tests (no integration tests) - ArchUnit or integration tests can be filtered
# This target runs the surefire unit tests (exclude *IntegrationTest)
test:
	mvn -DskipITs=true -Dtest='!**/*IntegrationTest' test
