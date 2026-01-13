# Lab 13 (Bonus): CDC Integration

## Ignite + Kafka + Debezium + PostgreSQL

This bonus lab demonstrates real-time cache synchronization using Change Data Capture (CDC) to keep Apache Ignite caches automatically updated when data changes in PostgreSQL.

**Duration:** 60-90 minutes (self-paced)

---

## Architecture

```
┌──────────────┐    ┌──────────────┐    ┌─────────┐    ┌──────────────┐    ┌──────────┐
│  PostgreSQL  │───>│   Debezium   │───>│  Kafka  │───>│ CDC Consumer │───>│  Ignite  │
│  (Source DB) │    │   (CDC)      │    │(Broker) │    │   (Java)     │    │ (Cache)  │
└──────────────┘    └──────────────┘    └─────────┘    └──────────────┘    └──────────┘
       │                   │                                   │
       │     Captures      │         Publishes                │
       │     WAL changes   │         events                   │
       └───────────────────┴──────────────────────────────────┘
                    Change Data Capture Flow
```

### Components

| Component | Purpose | Port |
|-----------|---------|------|
| **PostgreSQL** | Source database with inventory data | 5432 |
| **Debezium** | Captures database changes via WAL | 8083 |
| **Apache Kafka** | Message broker for CDC events | 29092 |
| **Kafka UI** | Web interface for monitoring | 8080 |
| **Apache Ignite** | In-memory data grid (cache) | 10800, 47500 |

---

## Prerequisites

- Docker and Docker Compose
- Java 11+
- Maven 3.6+
- ~4GB RAM available for Docker

---

## Quick Start

### Step 1: Start Docker Environment

```bash
cd docker

# Make scripts executable
chmod +x *.sh

# Start all services
./start.sh
```

Wait for all services to be ready (about 30-60 seconds).

### Step 2: Register Debezium Connector

```bash
./register-connector.sh
```

This registers a PostgreSQL connector that will:
- Connect to the `inventory` database
- Monitor tables: `customers`, `products`, `orders`, `order_items`
- Publish changes to Kafka topics

### Step 3: Build and Run the Java Consumer

```bash
cd ..  # Back to lab13 directory

# Build
mvn clean compile

# Run (standalone mode - Ignite embedded in JVM)
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab13.Lab13CDCStandalone"
```

### Step 4: Test CDC

In a new terminal:

```bash
cd docker
./test-cdc.sh
```

Or manually connect to PostgreSQL:

```bash
docker exec -it postgres psql -U postgres inventory
```

Then run SQL commands:

```sql
-- INSERT
INSERT INTO inventory.customers (first_name, last_name, email, city)
VALUES ('New', 'Customer', 'new@example.com', 'Boston');

-- UPDATE
UPDATE inventory.customers SET city = 'Seattle' WHERE id = 1;

-- DELETE
DELETE FROM inventory.customers WHERE email = 'new@example.com';

-- Update product inventory
UPDATE inventory.products SET quantity = 999 WHERE id = 1;
```

Watch the Java console - you'll see CDC events being processed in real-time!

---

## Detailed Setup Guide

### Docker Services

Start individual services if needed:

```bash
cd docker

# Start only infrastructure (no Ignite)
docker-compose up -d postgres zookeeper kafka debezium-connect kafka-ui

# Start everything including Ignite
docker-compose up -d
```

### Verify Services

```bash
# Check all containers are running
docker-compose ps

# Check PostgreSQL
docker exec postgres pg_isready -U postgres

# Check Kafka topics
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check Debezium connectors
curl http://localhost:8083/connectors

# Check connector status
curl http://localhost:8083/connectors/inventory-connector/status
```

### View Kafka UI

Open http://localhost:8080 in your browser to:
- See all Kafka topics
- Monitor CDC events in real-time
- View consumer groups

---

## Maven Commands

```bash
# Compile
mvn clean compile

# Run standalone mode (embedded Ignite)
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab13.Lab13CDCStandalone"

# Run client mode (connects to Docker Ignite)
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab13.Lab13CDCIntegration"

# Package uber-jar
mvn clean package

# Run uber-jar
java -jar target/lab13-bonus-cdc-integration-1.0-SNAPSHOT-uber.jar
```

---

## Application Commands

Once the Java application is running:

| Command | Description |
|---------|-------------|
| `stats`, `s` | Show cache statistics and CDC metrics |
| `customers`, `c` | List all customers in Ignite |
| `products`, `p` | List all products in Ignite |
| `orders`, `o` | List all orders in Ignite |
| `items`, `i` | List all order items in Ignite |
| `sql` | Show PostgreSQL test commands |
| `help`, `h` | Show available commands |
| `quit`, `q` | Exit application |

---

## How CDC Works

### 1. PostgreSQL WAL

PostgreSQL uses Write-Ahead Logging (WAL) for durability. Debezium reads the WAL to capture changes.

```sql
-- Enable logical replication (already configured in Docker)
ALTER SYSTEM SET wal_level = logical;
```

### 2. Debezium Connector

The connector configuration (`register-postgres-connector.json`):

```json
{
  "name": "inventory-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres",
    "database.dbname": "inventory",
    "topic.prefix": "dbserver1",
    "table.include.list": "inventory.customers,inventory.products,..."
  }
}
```

### 3. Kafka Topics

Debezium creates topics per table:
- `dbserver1.inventory.customers`
- `dbserver1.inventory.products`
- `dbserver1.inventory.orders`
- `dbserver1.inventory.order_items`

### 4. CDC Event Format

```json
{
  "before": { "id": 1, "name": "Old Name", ... },
  "after": { "id": 1, "name": "New Name", ... },
  "op": "u",
  "ts_ms": 1699999999999,
  "source": {
    "table": "customers",
    "schema": "inventory"
  }
}
```

Operations:
- `c` = CREATE (insert)
- `u` = UPDATE
- `d` = DELETE
- `r` = READ (initial snapshot)

### 5. Java Consumer

The `IgniteCDCConsumer` class:
1. Subscribes to Kafka CDC topics
2. Parses Debezium JSON events
3. Maps events to domain objects
4. Updates Ignite caches

---

## Solution Files

```
lab13_bonus_cdc_integration/
├── docker/
│   ├── docker-compose.yml       # All services
│   ├── init-db.sql              # PostgreSQL schema & data
│   ├── ignite-config.xml        # Ignite configuration
│   ├── register-postgres-connector.json
│   ├── start.sh                 # Start environment
│   ├── stop.sh                  # Stop environment
│   ├── register-connector.sh    # Register Debezium
│   └── test-cdc.sh              # Test CDC changes
├── src/main/java/.../lab13/
│   ├── Lab13CDCIntegration.java # Client mode (Docker Ignite)
│   ├── Lab13CDCStandalone.java  # Standalone mode (embedded)
│   ├── cdc/
│   │   ├── CDCEvent.java        # Debezium event model
│   │   └── IgniteCDCConsumer.java # Kafka consumer
│   └── model/
│       ├── Customer.java
│       ├── Product.java
│       ├── Order.java
│       └── OrderItem.java
├── pom.xml
└── README.md
```

---

## Key Learning Points

### 1. Change Data Capture Pattern
- Captures all database changes without polling
- Uses database transaction log (WAL)
- Maintains order of changes

### 2. Event-Driven Architecture
- Loose coupling between systems
- Async communication via Kafka
- Scalable event processing

### 3. Cache Synchronization
- Real-time cache updates
- No application code changes needed in source
- Eventual consistency model

### 4. Production Considerations
- Handle out-of-order events
- Implement idempotent processing
- Monitor lag and throughput
- Handle schema evolution

---

## Troubleshooting

### Debezium connector not starting

```bash
# Check connector status
curl http://localhost:8083/connectors/inventory-connector/status

# Check Debezium logs
docker logs debezium-connect

# Restart connector
curl -X POST http://localhost:8083/connectors/inventory-connector/restart
```

### No CDC events appearing

```bash
# Check Kafka topics exist
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check topic has messages
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic dbserver1.inventory.customers \
  --from-beginning --max-messages 5
```

### Java consumer not connecting

```bash
# Verify Kafka is accessible
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check consumer group
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group ignite-cdc-consumer
```

### Reset everything

```bash
cd docker
./stop.sh  # Select 'y' to remove volumes
./start.sh
./register-connector.sh
```

---

## Extensions (Optional Challenges)

1. **Add Schema Evolution**: Modify the PostgreSQL schema and update the consumer to handle new fields

2. **Implement Exactly-Once**: Use Kafka transactions and Ignite transactions for exactly-once processing

3. **Add Monitoring**: Integrate Prometheus/Grafana for CDC lag monitoring

4. **Scale Consumers**: Run multiple consumer instances with Kafka consumer groups

5. **Bi-directional Sync**: Implement changes from Ignite back to PostgreSQL

---

## Cleanup

```bash
cd docker

# Stop all services
./stop.sh

# Remove all data (select 'y' when prompted)
docker-compose down -v

# Remove images (optional)
docker-compose down --rmi all
```

---

## Additional Resources

- [Debezium Documentation](https://debezium.io/documentation/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Apache Ignite Kafka Streamer](https://ignite.apache.org/docs/latest/extensions-and-integrations/streaming/kafka-streamer)
- [CDC Patterns](https://www.confluent.io/blog/using-logs-to-build-a-solid-data-infrastructure-or-why-dual-writes-are-a-bad-idea/)
