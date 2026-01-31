# Lab 13: Apache Ignite 3.x Hands-On - Solutions

## Overview

This lab provides hands-on experience with Apache Ignite 3.x, covering:
- Client connection and cluster topology
- SWIM discovery and RAFT consensus
- HOCON configuration and dynamic updates
- Table API (RecordView, KeyValueView, Tuple, POJO mapping)
- SQL DDL/DML as first-class operations
- Strictly serializable transactions
- Mixed SQL + KV transactions
- Compute grid (JobTarget, JobDescriptor, deployment units)
- Distribution Zones and COLOCATE BY
- Storage profiles (aimem, aipersist, rocksdb)
- REST API, CLI, and OpenMetrics monitoring
- Complete 2.x vs 3.x comparison with migration checklist

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Running Apache Ignite 3.x cluster (Docker or binary)

## Setting Up Ignite 3.x

### Option 1: Docker (Recommended)

```bash
# Pull and start Ignite 3.x
docker pull apacheignite/ignite:3.0.0
docker run -d --name ignite3-node \
  -p 3344:3344 \
  -p 10300:10300 \
  -p 10800:10800 \
  apacheignite/ignite:3.0.0

# Initialize the cluster
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster init \
  --name=myCluster \
  --meta-storage-node=defaultNode

# Verify
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster state
```

### Option 2: Binary Download

```bash
# Download from https://ignite.apache.org/download
# Extract and start
cd apache-ignite-3.x.x
bin/ignite3 node start

# Initialize
bin/ignite3 cluster init --name=myCluster --meta-storage-node=defaultNode

# Verify
bin/ignite3 cluster state
```

## Project Structure

```
lab13_version_differences/
├── pom.xml                    (ignite-client 3.1.0)
├── README.md
└── src/main/java/com/example/ignite/solutions/lab13/
    ├── Lab13Discovery.java          - Exercises 1-2: Connection, SWIM, RAFT
    ├── Lab13Configuration.java      - Exercises 3-5: HOCON, dynamic config, zones
    ├── Lab13CacheAPI.java           - Exercises 6-8: Table API, SQL queries
    ├── Lab13Transactions.java       - Exercises 9-10: Transactions, mixed SQL+KV
    ├── Lab13Compute.java            - Exercises 11-12: Compute grid, gaps
    ├── Lab13DataColocation.java     - Exercises 13-14: Distribution Zones, COLOCATE BY
    ├── Lab13Monitoring.java         - Exercises 15-16: REST, CLI, OpenMetrics
    └── Lab13ComparisonSummary.java  - Exercise 17: Full comparison + live demo
```

## Quick Start

```bash
# Build
mvn clean compile

# Package
mvn clean package

# Run a specific solution (requires running Ignite 3.x cluster)
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab13.Lab13Discovery"
```

## Running the Solutions

All solutions connect to `127.0.0.1:10800` (default Ignite 3.x client port).

### Exercise 1-2: Discovery and Cluster Lifecycle
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab13.Lab13Discovery"
```
Connects to cluster, shows topology, explains SWIM and RAFT.

### Exercise 3-5: Configuration
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab13.Lab13Configuration"
```
Explains HOCON config, dynamic CLI updates, creates distribution zones.

### Exercise 6-8: Table API
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab13.Lab13CacheAPI"
```
Creates tables via DDL, demonstrates RecordView, KeyValueView, POJO mapping, SQL queries.

### Exercise 9-10: Transactions
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab13.Lab13Transactions"
```
Manual transactions, runInTransaction, mixed SQL+KV, rollback.

### Exercise 11-12: Compute Grid
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab13.Lab13Compute"
```
Explains JobTarget, JobDescriptor, colocated compute, deployment units, feature gaps.

### Exercise 13-14: Data Colocation and Distribution Zones
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab13.Lab13DataColocation"
```
Creates zones, COLOCATE BY tables, colocated SQL joins, storage profiles.

### Exercise 15-16: Monitoring
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab13.Lab13Monitoring"
```
REST API endpoints, CLI commands, OpenMetrics, system views.

### Exercise 17: Complete Comparison
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab13.Lab13ComparisonSummary"
```
Live demo of all features, architecture/API comparison tables, migration checklist.

## Key Concepts

### Ignite 3.x Philosophy
- **Schema-first**: Define tables via SQL DDL, access via Table API or SQL
- **Strong consistency**: RAFT consensus, strictly serializable transactions
- **Cloud-native**: REST API, OpenMetrics, dynamic configuration

### Architecture
| Component | Ignite 2.x | Ignite 3.x |
|-----------|-----------|-----------|
| Discovery | Ring topology (TcpDiscoverySpi) | SWIM gossip |
| Consensus | Custom protocol | RAFT |
| Storage | Single B+ tree engine | Pluggable (aimem/aipersist/RocksDB) |
| Configuration | Spring XML / Java | HOCON / CLI / REST |
| SQL Engine | H2 + Calcite | Calcite only |

### API
| Operation | Ignite 2.x | Ignite 3.x |
|-----------|-----------|-----------|
| Connect | Ignition.start(cfg) | IgniteClient.builder().build() |
| Schema | @QuerySqlField | CREATE TABLE (SQL DDL) |
| CRUD | cache.put/get | view.upsert/get |
| Query | SqlFieldsQuery | client.sql().execute() |
| Async | IgniteFuture | CompletableFuture |
| Binary | BinaryObject | Tuple |

## Common Issues

### Connection Refused
Ensure Ignite 3.x is running and cluster is initialized:
```bash
docker ps                                          # Check container
docker exec -it ignite3-node ignite3 cluster state # Check state
```

### Port 10800 in use
```bash
lsof -i :10800
```

### Docker: Cluster not initialized
```bash
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster init \
  --name=myCluster --meta-storage-node=defaultNode
```
