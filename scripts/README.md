use-rancher-docker.sh — helper to run docker compose with Rancher Desktop socket

Purpose
-------
This script locates common Rancher Desktop Docker socket paths (e.g., ~/.rancher-desktop/run/docker.sock) and temporarily sets DOCKER_HOST for the current shell invocation. It then runs a docker or nerdctl command using that socket. This avoids global changes and keeps your normal Docker CLI configuration unchanged.

Usage examples
--------------
# Auto-detect socket and start postgres service from docker-compose.yml
./scripts/use-rancher-docker.sh

# Provide explicit socket path and run docker compose
./scripts/use-rancher-docker.sh /Users/you/.rancher-desktop/run/docker.sock -- docker compose up -d postgres

# Provide full command after --
./scripts/use-rancher-docker.sh -- docker compose ps

Notes
-----
- The script exports DOCKER_HOST only for the script's process and commands it runs.
- If `docker` CLI doesn't talk to the socket, it will attempt a `nerdctl` fallback (Rancher Desktop's containerd client).
- Ensure Rancher Desktop is running before using this script.
