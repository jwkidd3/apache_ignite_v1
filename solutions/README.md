# Apache Ignite Training - Lab Solutions

This directory contains complete solution code for all 12 labs in the Apache Ignite training course.

## Directory Structure

```
solutions/
├── lab01_environment_setup/      # Environment Setup and First Cluster
├── lab02_multinode_cluster/      # Multi-Node Cluster Setup
├── lab03_basic_cache_operations/ # Basic Cache Operations
├── lab04_configuration_deployment/ # Configuration and Deployment
├── lab05_data_modeling_persistence/ # Data Modeling and Persistence
├── lab06_sql_indexing/           # SQL and Indexing
├── lab07_transactions_acid/      # Transactions and ACID
├── lab08_advanced_caching/       # Advanced Caching
├── lab09_compute_grid/           # Compute Grid
├── lab10_integration_connectivity/ # Integration and Connectivity
├── lab11_performance_tuning/     # Performance Tuning
├── lab12_production_deployment/  # Production Deployment
└── README.md                     # This file
```

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Ignite 2.16.0 (handled by Maven)

## Building Solutions

Each lab directory contains a `pom.xml` file. To build a specific lab:

```bash
cd lab01_environment_setup
mvn clean compile
```

To build all solutions:

```bash
for dir in lab*/; do
    echo "Building $dir..."
    (cd "$dir" && mvn clean compile -q)
done
```

## Running Solutions

Each solution file has a `main()` method. Run using Maven:

```bash
cd lab01_environment_setup
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab01.FirstCluster"
```

Or compile and run with Java:

```bash
cd lab01_environment_setup
mvn clean package -DskipTests
java -cp target/classes:target/dependency/* com.example.ignite.solutions.lab01.FirstCluster
```

## Lab Overview

### Day 1: Fundamentals and Basic Operations

| Lab | Title | Duration | Key Topics |
|-----|-------|----------|------------|
| 01 | Environment Setup | 45 min | Ignite installation, first node, cluster metrics |
| 02 | Multi-Node Cluster | 50 min | Static IP discovery, baseline topology, cluster states |
| 03 | Basic Cache Operations | 55 min | CRUD, cache modes, batch operations, DataStreamer |
| 04 | Configuration & Deployment | 60 min | XML/programmatic config, data regions |

### Day 2: Data Management and Transactions

| Lab | Title | Duration | Key Topics |
|-----|-------|----------|------------|
| 05 | Data Modeling & Persistence | 55 min | Affinity keys, native persistence, cache stores |
| 06 | SQL and Indexing | 55 min | DDL, DQL, indexes, JDBC, distributed joins |
| 07 | Transactions & ACID | 55 min | PESSIMISTIC/OPTIMISTIC, isolation levels, deadlocks |
| 08 | Advanced Caching | 60 min | Eviction, expiration, continuous queries, near cache |

### Day 3: Advanced Features and Production

| Lab | Title | Duration | Key Topics |
|-----|-------|----------|------------|
| 09 | Compute Grid | 50 min | IgniteCompute, affinity computing, MapReduce |
| 10 | Integration & Connectivity | 55 min | REST API, JDBC/ODBC, thin clients |
| 11 | Performance Tuning | 55 min | Memory tuning, query optimization, profiling |
| 12 | Production Deployment | 55 min | Security, SSL/TLS, backup/recovery |

## Solution Files Per Lab

### Lab 01: Environment Setup
- `FirstCluster.java` - Basic node startup
- `ClusterMetrics.java` - Metrics exploration
- `MultiNodeCluster.java` - Multi-node in single JVM
- `GracefulShutdown.java` - Shutdown hooks
- `HealthCheck.java` - Health monitoring utility

### Lab 02: Multi-Node Cluster
- `StaticIPDiscovery.java` - TCP discovery configuration
- `BaselineTopology.java` - Baseline management
- `ClusterStates.java` - Cluster state transitions
- `CloudDiscovery.java` - Environment-based discovery
- `BaselineOperations.java` - Baseline operations

### Lab 03: Basic Cache Operations
- `BasicCacheOps.java` - CRUD operations
- `CacheModes.java` - PARTITIONED, REPLICATED, LOCAL
- `AsyncOperations.java` - Async API
- `BatchOperations.java` - putAll, getAll, removeAll
- `AdvancedCacheOps.java` - EntryProcessors, locks
- `CacheIteration.java` - ScanQuery
- `DataStreamer.java` - Bulk loading
- `BinaryObjects.java` - Binary object API
- `PerformanceBenchmark.java` - Performance testing

### Lab 04: Configuration & Deployment
- `XmlConfig.java` - XML configuration
- `ProgrammaticConfig.java` - Programmatic configuration
- `Monitoring.java` - Metrics and monitoring
- `resources/ignite-config.xml` - Sample XML config

### Lab 05: Data Modeling & Persistence
- `model/Customer.java` - Customer entity
- `model/Order.java` - Order entity with affinity
- `AffinityKeys.java` - Colocation demo
- `Persistence.java` - Native persistence
- `CacheStore.java` - Read/write-through
- `WriteBehind.java` - Write-behind store

### Lab 06: SQL and Indexing
- `BasicSQL.java` - DDL, DML, DQL
- `Indexing.java` - Index creation
- `JDBCConnection.java` - JDBC driver
- `DistributedJoins.java` - Distributed joins
- `QueryOptimization.java` - Query tuning

### Lab 07: Transactions & ACID
- `BasicTransactions.java` - Transaction basics
- `PessimisticTransactions.java` - Pessimistic mode
- `OptimisticTransactions.java` - Optimistic mode
- `IsolationLevels.java` - Isolation levels
- `DeadlockHandling.java` - Deadlock detection

### Lab 08: Advanced Caching
- `EvictionPolicies.java` - LRU, FIFO eviction
- `ExpirationPolicies.java` - TTL expiration
- `ContinuousQueries.java` - Real-time notifications
- `NearCache.java` - Near cache configuration
- `CacheEvents.java` - Cache event listeners

### Lab 09: Compute Grid
- `BasicCompute.java` - Run, call, broadcast
- `AffinityCompute.java` - Affinity-aware computing
- `MapReduceTask.java` - MapReduce pattern
- `AsyncCompute.java` - Async execution
- `LoadBalancing.java` - Load balancing strategies

### Lab 10: Integration & Connectivity
- `RestApiClient.java` - REST API usage
- `ThinClientConnection.java` - Thin client
- `SpringIntegration.java` - Spring Framework
- `JdbcThinDriver.java` - JDBC thin driver

### Lab 11: Performance Tuning
- `MemoryTuning.java` - Memory configuration
- `QueryProfiling.java` - Query analysis
- `DataRegionTuning.java` - Data region optimization
- `BenchmarkSuite.java` - Performance benchmarks

### Lab 12: Production Deployment
- `SecurityOverview.java` - Security configuration
- `ClusterSetup.java` - Production cluster setup
- `SSLConfiguration.java` - SSL/TLS setup
- `Authentication.java` - Authentication/authorization
- `BackupRecovery.java` - Backup and recovery
- `RollingUpdate.java` - Zero-downtime updates

## Troubleshooting

### Common Issues

**Port conflicts:**
```bash
# Check if ports 47500-47509 are in use
netstat -an | grep 475
```

**Persistence directory cleanup:**
```bash
rm -rf ./ignite-data ./ignite-wal ./ignite-wal-archive
```

**Memory issues:**
```bash
# Run with increased heap
java -Xms512m -Xmx2g -cp ... MainClass
```

### Recommended JVM Options

```bash
-Xms1g
-Xmx4g
-XX:+UseG1GC
-XX:+AlwaysPreTouch
-XX:+DisableExplicitGC
-Djava.net.preferIPv4Stack=true
```

## Additional Resources

- [Apache Ignite Documentation](https://ignite.apache.org/docs/latest/)
- [Ignite Examples on GitHub](https://github.com/apache/ignite/tree/master/examples)
- [Ignite JavaDoc](https://ignite.apache.org/releases/latest/javadoc/)

## License

These solutions are provided for educational purposes as part of the Apache Ignite training course.
