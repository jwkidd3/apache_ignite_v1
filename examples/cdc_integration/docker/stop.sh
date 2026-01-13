#!/bin/bash
#
# Stop and clean up the CDC Integration Environment
#

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║      Stopping CDC Integration Environment                      ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

echo "Stopping Docker services..."
docker-compose down

echo ""
read -p "Do you want to remove volumes (database data)? (y/N) " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Removing volumes..."
    docker-compose down -v
    echo "Volumes removed."
fi

echo ""
echo "Environment stopped."
