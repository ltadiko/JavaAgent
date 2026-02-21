#!/usr/bin/env bash
# Lightweight helper to point the current shell to Rancher Desktop's Docker socket
# and run docker / docker compose commands without changing global settings.
# Usage:
#   ./scripts/use-rancher-docker.sh [SOCKET_PATH] -- docker compose up -d postgres
# Or let the script auto-detect the socket and run the default command:
#   ./scripts/use-rancher-docker.sh

set -euo pipefail

# Candidate Rancher Desktop socket locations (extended)
CANDIDATES=(
  "$HOME/.rancher-desktop/run/docker.sock"
  "$HOME/.rancher-desktop/docker.sock"
  "$HOME/.rd/run/docker.sock"
  "$HOME/.rd/docker.sock"
  "$HOME/.rd/docker.sock"
  "/var/run/docker.sock"
)

# Allow explicit env var override (useful in CI or shells)
# RANCHER_SOCKET can be set to a unix socket path (no unix:// prefix required)
if [[ -n "${RANCHER_SOCKET:-}" ]]; then
  explicit_socket="$RANCHER_SOCKET"
else
  explicit_socket=""
fi

# If DOCKER_HOST is exported in the environment and points to a unix socket, prefer it
if [[ -n "${DOCKER_HOST:-}" ]]; then
  # Accept formats: unix:///path/to/socket or /path/to/socket
  if [[ "$DOCKER_HOST" == unix://* ]]; then
    docker_host_path="${DOCKER_HOST#unix://}"
    if [[ -S "$docker_host_path" ]]; then
      explicit_socket="$docker_host_path"
    fi
  else
    # If DOCKER_HOST is just a path
    if [[ -S "$DOCKER_HOST" ]]; then
      explicit_socket="$DOCKER_HOST"
    fi
  fi
fi

# CLI arg parsing: if a first arg looks like a socket path, accept it
if [[ $# -gt 0 && $1 != "--" && $1 != "docker" && $1 != "nerdctl" && -S "$1" ]]; then
  explicit_socket="$1"
  shift || true
fi

# If user passes --, respect following args as command
if [[ "$explicit_socket" == "--" ]]; then
  explicit_socket=""
fi

# If the user provided a socket path, use it; otherwise auto-detect a candidate
socket_path=""
if [[ -n "$explicit_socket" ]]; then
  if [[ -S "$explicit_socket" ]]; then
    socket_path="$explicit_socket"
  else
    echo "Provided socket path does not exist or is not a socket: $explicit_socket" >&2
    exit 2
  fi
else
  for p in "${CANDIDATES[@]}"; do
    if [[ -S "$p" ]]; then
      socket_path="$p"
      break
    fi
  done
fi

if [[ -z "$socket_path" ]]; then
  echo "No Docker socket found in common Rancher locations." >&2
  echo "Possible next steps:" >&2
  echo "  * Start Rancher Desktop GUI and enable 'Expose Docker API' (Preferences -> Docker / Container Runtime)" >&2
  echo "  * Run: find \"$HOME\" -name docker.sock -type s 2>/dev/null" >&2
  echo "  * Or explicitly pass the socket path to this script:" >&2
  echo "      ./scripts/use-rancher-docker.sh /path/to/docker.sock -- docker compose up -d postgres" >&2
  exit 3
fi

export DOCKER_HOST="unix://$socket_path"
echo "Using DOCKER_HOST=$DOCKER_HOST (temporary for this shell invocation)"

# If the user supplied a command after '--', run it. Otherwise default to bring up postgres.
cmd=( )
if [[ "$#" -gt 0 ]]; then
  # If user used: ./script.sh /socket -- docker compose up -d
  # then $@ contains the command including 'docker' or 'nerdctl'
  cmd=("$@")
else
  cmd=(docker compose up -d postgres)
fi

# Prefer docker CLI if present and able to talk to the socket
if command -v docker >/dev/null 2>&1; then
  echo "docker CLI found, verifying connection..."
  if docker info >/dev/null 2>&1; then
    echo "Docker reachable. Running: ${cmd[*]}"
    "${cmd[@]}"
    exit $?
  else
    echo "docker CLI not responding via socket. Trying nerdctl fallback..."
  fi
fi

# Fallback: use nerdctl (Rancher Desktop's containerd client)
if command -v nerdctl >/dev/null 2>&1; then
  # Common Rancher namespace is 'k8s.io', but if not present, use default
  NAMESPACE=k8s.io
  echo "Using nerdctl (containerd) with namespace: $NAMESPACE"
  # Translate docker compose to nerdctl compose if command looks like docker compose
  if [[ "${cmd[0]}" == "docker" && "${cmd[1]}" == "compose" ]]; then
    # rebuild args
    args=( )
    for ((i=2;i<${#cmd[@]};i++)); do
      args+=("${cmd[i]}")
    done
    echo "Running: nerdctl --namespace $NAMESPACE compose ${args[*]}"
    nerdctl --namespace "$NAMESPACE" compose "${args[@]}"
    exit $?
  else
    # Generic nerdctl invocation
    echo "Running nerdctl ${cmd[*]}"
    nerdctl --namespace "$NAMESPACE" "${cmd[@]}"
    exit $?
  fi
fi

echo "Neither docker nor nerdctl CLI worked against the socket $socket_path."
echo "Ensure Rancher Desktop is running and the socket is accessible by your user." >&2
exit 4
