#!/bin/bash
#
# Start the CDC Integration Environment
#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║      Starting CDC Integration Environment                      ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Start all services
echo "Starting Docker services..."
docker-compose up -d

echo ""
echo "Waiting for services to be ready..."

# Wait for PostgreSQL
echo -n "  PostgreSQL: "
until docker exec postgres pg_isready -U postgres > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo " Ready!"

# Wait for Kafka
echo -n "  Kafka: "
until docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo " Ready!"

# Wait for Debezium Connect
echo -n "  Debezium Connect: "
until curl -s http://localhost:8083/connectors > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo " Ready!"

echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo "All services are running!"
echo ""
echo "Service URLs:"
echo "  PostgreSQL:      localhost:5432 (user: postgres, pass: postgres)"
echo "  Kafka:           localhost:29092"
echo "  Kafka UI:        http://localhost:8080"
echo "  Debezium:        http://localhost:8083"
echo "  Ignite:          localhost:10800 (thin client)"
echo ""
echo "Next step: Register the Debezium connector"
echo "  Run: ./register-connector.sh"
echo "═══════════════════════════════════════════════════════════════════"
