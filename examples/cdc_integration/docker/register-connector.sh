#!/bin/bash
#
# Register the Debezium PostgreSQL Connector
#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║      Registering Debezium PostgreSQL Connector                 ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if Debezium Connect is available
echo "Checking Debezium Connect availability..."
if ! curl -s http://localhost:8083/connectors > /dev/null; then
    echo "ERROR: Debezium Connect is not available at http://localhost:8083"
    echo "Please run ./start.sh first"
    exit 1
fi

# Check if connector already exists
EXISTING=$(curl -s http://localhost:8083/connectors | grep -o "inventory-connector" || true)
if [ -n "$EXISTING" ]; then
    echo "Connector 'inventory-connector' already exists."
    echo ""
    read -p "Do you want to delete and recreate it? (y/N) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Deleting existing connector..."
        curl -s -X DELETE http://localhost:8083/connectors/inventory-connector
        sleep 2
    else
        echo "Keeping existing connector."
        exit 0
    fi
fi

# Register the connector
echo "Registering PostgreSQL connector..."
RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
    --data @register-postgres-connector.json \
    http://localhost:8083/connectors)

echo ""
echo "Response: $RESPONSE"
echo ""

# Wait for connector to be running
echo "Waiting for connector to start..."
sleep 5

# Check connector status
STATUS=$(curl -s http://localhost:8083/connectors/inventory-connector/status)
echo ""
echo "Connector Status:"
echo "$STATUS" | python3 -m json.tool 2>/dev/null || echo "$STATUS"

echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo "Connector registration complete!"
echo ""
echo "Kafka topics created by Debezium:"
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | grep dbserver1 || echo "  (waiting for topics...)"
echo ""
echo "Next step: Run the Java CDC consumer"
echo "  cd .. && mvn exec:java"
echo "═══════════════════════════════════════════════════════════════════"
