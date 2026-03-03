# Make targets for local development
# - build: fast build (skip tests) to verify compile
# - build-full: start required local services and run full mvn clean install
# - up-local: start postgres via Rancher socket helper
# - down-local: stop services (docker compose down)
# - test: run unit tests only

.PHONY: build build-full up-local down-local test e2e e2e-ui e2e-setup

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

# ─── E2E Tests (Playwright — standalone module in /e2e) ──────────────────
# Prerequisites:
#   1. docker compose up -d
#   2. Backend: mvn spring-boot:run -Dspring-boot.run.profiles=local
#   3. Vite dev server starts automatically via playwright.config.ts webServer

# Install Playwright browsers (one-time setup)
e2e-setup:
	cd e2e && npm install && npx playwright install chromium

# Run E2E tests headless
e2e:
	cd e2e && npx playwright test

# Run E2E tests with Playwright UI (interactive)
e2e-ui:
	cd e2e && npx playwright test --ui

# Run E2E tests headed (watch browser)
e2e-headed:
	cd e2e && npx playwright test --headed

