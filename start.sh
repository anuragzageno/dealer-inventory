#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# start.sh  —  Bootstrap and run the Dealer Inventory application
#
# Only requirement: Docker Desktop (everything else runs inside containers)
#
# Usage:
#   ./start.sh          # build + start everything (default)
#   ./start.sh --db     # start DB only (run app in IDE against localhost:5412)
#   ./start.sh --stop   # stop all containers
#   ./start.sh --clean  # wipe volumes + rebuild from scratch
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

# ── Colours ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

info()    { echo -e "${CYAN}[INFO]${RESET}  $*"; }
success() { echo -e "${GREEN}[OK]${RESET}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${RESET}  $*"; }
error()   { echo -e "${RED}[ERROR]${RESET} $*" >&2; exit 1; }
step()    { echo -e "\n${BOLD}── $* ──${RESET}"; }

# ── Script location (so it works from any cwd) ────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ── Parse arguments ───────────────────────────────────────────────────────────
MODE="full"
case "${1:-}" in
  --db)    MODE="db"    ;;
  --stop)  MODE="stop"  ;;
  --clean) MODE="clean" ;;
  "")      MODE="full"  ;;
  *)       error "Unknown option '$1'. Use --db | --stop | --clean" ;;
esac

# ── Helper: check a command exists ───────────────────────────────────────────
require() {
  if ! command -v "$1" &>/dev/null; then
    echo -e "${RED}[ERROR]${RESET} '$1' is not installed or not on PATH." >&2
    suggest_install "$1"
    exit 1
  fi
}

# ── OS-aware install suggestions ─────────────────────────────────────────────
suggest_install() {
  local tool="$1"
  local os
  os="$(uname -s)"

  echo -e "${YELLOW}── How to install '$tool' ──${RESET}" >&2

  case "$tool" in
    docker)
      case "$os" in
        Darwin) echo -e "  ${CYAN}brew install --cask docker${RESET}  (then open Docker.app)" >&2 ;;
        Linux)  echo -e "  ${CYAN}curl -fsSL https://get.docker.com | sh${RESET}" >&2
                echo -e "  Then: ${CYAN}sudo usermod -aG docker \$USER && newgrp docker${RESET}" >&2 ;;
        MINGW*|MSYS*|CYGWIN*)
                echo -e "  Download Docker Desktop: ${CYAN}https://www.docker.com/products/docker-desktop${RESET}" >&2 ;;
      esac ;;

    java)
      case "$os" in
        Darwin) echo -e "  ${CYAN}brew install --cask temurin@17${RESET}  (Eclipse Temurin JDK 17)" >&2
                echo -e "  Or via SDKMAN: ${CYAN}sdk install java 17-tem${RESET}" >&2 ;;
        Linux)  echo -e "  Debian/Ubuntu: ${CYAN}sudo apt install -y openjdk-17-jdk${RESET}" >&2
                echo -e "  RHEL/Fedora:   ${CYAN}sudo dnf install -y java-17-openjdk-devel${RESET}" >&2
                echo -e "  Or via SDKMAN: ${CYAN}curl -s \"https://get.sdkman.io\" | bash && sdk install java 17-tem${RESET}" >&2 ;;
        MINGW*|MSYS*|CYGWIN*)
                echo -e "  Download Temurin 17: ${CYAN}https://adoptium.net/temurin/releases/?version=17${RESET}" >&2 ;;
      esac ;;

    mvn)
      case "$os" in
        Darwin) echo -e "  ${CYAN}brew install maven${RESET}" >&2
                echo -e "  Or via SDKMAN: ${CYAN}sdk install maven${RESET}" >&2 ;;
        Linux)  echo -e "  Debian/Ubuntu: ${CYAN}sudo apt install -y maven${RESET}" >&2
                echo -e "  RHEL/Fedora:   ${CYAN}sudo dnf install -y maven${RESET}" >&2
                echo -e "  Or via SDKMAN: ${CYAN}sdk install maven${RESET}" >&2 ;;
        MINGW*|MSYS*|CYGWIN*)
                echo -e "  Download Maven: ${CYAN}https://maven.apache.org/download.cgi${RESET}" >&2
                echo -e "  Or via Scoop:   ${CYAN}scoop install maven${RESET}" >&2 ;;
      esac ;;
  esac
  echo "" >&2
}

# ── Stop function ─────────────────────────────────────────────────────────────
stop_all() {
  step "Stopping containers"
  if docker compose ps --quiet 2>/dev/null | grep -q .; then
    docker compose stop
    success "Containers stopped."
  else
    info "No running containers found."
  fi
}

# ── Handle --stop ─────────────────────────────────────────────────────────────
if [[ "$MODE" == "stop" ]]; then
  require docker
  stop_all
  exit 0
fi

# ── Pre-flight checks ─────────────────────────────────────────────────────────
step "Checking prerequisites"

# Only Docker is needed on the host — Java and Maven run inside the container.
require docker

if ! docker info &>/dev/null; then
  error "Docker daemon is not running. Start Docker Desktop and try again."
fi

success "Docker is running — no other dependencies needed on this machine."

# ── Handle --clean ────────────────────────────────────────────────────────────
if [[ "$MODE" == "clean" ]]; then
  step "Wiping containers and volumes (clean start)"
  docker compose down -v --remove-orphans 2>/dev/null || true
  success "All containers and volumes removed."
fi

# ── Free port 5412 if occupied ────────────────────────────────────────────────
step "Checking port 5412"
OCCUPANT=$(docker ps --filter "publish=5412" --format "{{.Names}}" 2>/dev/null || true)
if [[ -n "$OCCUPANT" ]]; then
  warn "Port 5412 is held by container '$OCCUPANT'. Stopping it..."
  docker stop "$OCCUPANT" >/dev/null
  success "Released port 5412."
else
  success "Port 5412 is free."
fi

# ── Start the database ────────────────────────────────────────────────────────
step "Starting PostgreSQL (dealer-inventory-db)"
docker compose up dealer-inventory-db -d

info "Waiting for database to become healthy..."
TIMEOUT=60
ELAPSED=0
until docker inspect dealer-inventory-db \
      --format '{{.State.Health.Status}}' 2>/dev/null | grep -q "healthy"; do
  if (( ELAPSED >= TIMEOUT )); then
    error "Database did not become healthy within ${TIMEOUT}s. Check: docker logs dealer-inventory-db"
  fi
  sleep 2
  (( ELAPSED += 2 ))
  echo -n "."
done
echo ""
success "Database is healthy (${ELAPSED}s)."

# ── DB-only mode exits here ───────────────────────────────────────────────────
if [[ "$MODE" == "db" ]]; then
  echo ""
  echo -e "${GREEN}${BOLD}Database is ready.${RESET}"
  echo -e "  JDBC URL : ${CYAN}jdbc:postgresql://localhost:5412/dealer_inventory${RESET}"
  echo -e "  User     : dealer_user"
  echo -e "  Password : dealer_pass"
  echo ""
  echo -e "Run the app: ${YELLOW}mvn spring-boot:run${RESET}  or start it in your IDE."
  exit 0
fi

# ── Build image + start everything ───────────────────────────────────────────
step "Building image and starting all services"
info "First run downloads base images and compiles the app — this may take a few minutes."
info "Subsequent runs use Docker layer cache and are much faster."
echo ""

docker compose up --build -d

# Wait for app to be ready
info "Waiting for application to start on port 8080..."
TIMEOUT=120
ELAPSED=0
until curl -sf -o /dev/null http://localhost:8080/actuator/health 2>/dev/null \
   || curl -sf -o /dev/null -w "%{http_code}" http://localhost:8080/dealers \
        -u tenant1_user:password -H "X-Tenant-Id: tenant-1" 2>/dev/null | grep -qE '^(200|401|403)'; do
  # Also accept any HTTP response — means Tomcat is up even if endpoint needs auth
  HTTP=$(curl -o /dev/null -sw "%{http_code}" http://localhost:8080/ 2>/dev/null || true)
  if [[ "$HTTP" =~ ^[0-9]+$ ]] && (( HTTP > 0 )); then
    break
  fi
  if (( ELAPSED >= TIMEOUT )); then
    echo ""
    error "App did not start within ${TIMEOUT}s. Check logs: docker compose logs dealer-inventory-app"
  fi
  sleep 3
  (( ELAPSED += 3 ))
  echo -n "."
done
echo ""

success "Application is up!"
echo ""
echo -e "  ${BOLD}App URL   :${RESET} ${CYAN}http://localhost:8080${RESET}"
echo -e "  ${BOLD}Users     :${RESET} tenant1_user/password  |  tenant2_user/password  |  global_admin/admin"
echo -e "  ${BOLD}API docs  :${RESET} see API.md or import DealerInventory.postman_collection.json"
echo ""
echo -e "  ${BOLD}Logs      :${RESET} docker compose logs -f dealer-inventory-app"
echo -e "  ${BOLD}Stop      :${RESET} ./start.sh --stop"
echo -e "  ${BOLD}Wipe data :${RESET} ./start.sh --clean"
echo ""
