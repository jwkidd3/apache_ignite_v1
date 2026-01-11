# Apache Ignite Training - Lab Solutions

This directory contains complete solution code for all 12 labs plus a bonus lab in the Apache Ignite training course.

**Total: 81 Java solution files (75 core + 6 bonus)**

## Directory Structure

```
solutions/
├── lab01_environment_setup/        # 5 files - Environment Setup and First Cluster
├── lab02_multinode_cluster/        # 5 files - Multi-Node Cluster Setup
├── lab03_basic_cache_operations/   # 9 files - Basic Cache Operations
├── lab04_configuration_deployment/ # 5 files - Configuration and Deployment
├── lab05_data_modeling_persistence/# 6 files - Data Modeling and Persistence
├── lab06_sql_indexing/             # 6 files - SQL and Indexing
├── lab07_transactions_acid/        # 8 files - Transactions and ACID
├── lab08_advanced_caching/         # 6 files - Advanced Caching
├── lab09_compute_grid/             # 7 files - Compute Grid
├── lab10_integration_connectivity/ # 4 files - Integration and Connectivity
├── lab11_performance_tuning/       # 6 files - Performance Tuning
├── lab12_production_deployment/    # 8 files - Production Deployment
├── lab13_bonus_cdc_integration/    # 6 files - BONUS: CDC with Kafka/Debezium
└── README.md                       # This file
```

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Ignite 2.16.0 (handled by Maven)
- Docker & Docker Compose (for Lab 13 bonus only)

---

## Quick Start - Building & Running

### Build a Single Lab

```bash
cd lab01_environment_setup
mvn clean compile
```

### Build All Labs

```bash
# From solutions directory
for dir in lab*/; do
    echo "Building $dir..."
    (cd "$dir" && mvn clean compile -q)
done
```

### Run a Solution

```bash
cd lab01_environment_setup
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab01.FirstCluster"
```

---

## All Maven Commands

### Build Commands

```bash
# Clean and compile
mvn clean compile

# Package into JAR (skip tests for speed)
mvn clean package -DskipTests

# Full build with tests
mvn clean package

# Install to local repository
mvn clean install -DskipTests

# Compile with verbose output
mvn clean compile -X
```

### Dependency Management

```bash
# Download dependencies to target/dependency
mvn dependency:copy-dependencies

# Display dependency tree
mvn dependency:tree

# Check for dependency updates
mvn versions:display-dependency-updates
```

### Running Solutions

```bash
# Run with exec:java plugin
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab01.FirstCluster"

# Run with additional JVM arguments
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab01.FirstCluster" \
    -Dexec.args="" \
    -Dexec.jvmArgs="-Xms512m -Xmx2g"
```

### Running Without Maven

```bash
# First, package and copy dependencies
mvn clean package -DskipTests
mvn dependency:copy-dependencies

# Then run with java directly
java -cp "target/classes:target/dependency/*" \
    com.example.ignite.solutions.lab01.FirstCluster

# With JVM tuning
java -Xms512m -Xmx2g -XX:+UseG1GC \
    -cp "target/classes:target/dependency/*" \
    com.example.ignite.solutions.lab01.FirstCluster
```

### Testing Commands

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=FirstClusterTest

# Run tests with output
mvn test -Dsurefire.useFile=false
```

---

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

### Bonus Lab

| Lab | Title | Duration | Key Topics |
|-----|-------|----------|------------|
| 13 | CDC Integration | 60-90 min | Kafka, Debezium, PostgreSQL, real-time sync, Docker |

---

## Solution Files Per Lab

### Lab 01: Environment Setup (5 files)
```
com.example.ignite.solutions.lab01
├── FirstCluster.java       # Basic node startup
├── ClusterMetrics.java     # Metrics exploration
├── MultiNodeCluster.java   # Multi-node in single JVM
├── GracefulShutdown.java   # Shutdown hooks
└── HealthCheck.java        # Health monitoring utility
```

**Run commands:**
```bash
cd lab01_environment_setup
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab01.FirstCluster"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab01.ClusterMetrics"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab01.MultiNodeCluster"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab01.GracefulShutdown"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab01.HealthCheck"
```

### Lab 02: Multi-Node Cluster (5 files)
```
com.example.ignite.solutions.lab02
├── StaticIPDiscovery.java   # TCP discovery configuration
├── BaselineTopology.java    # Baseline management
├── ClusterStates.java       # Cluster state transitions
├── CloudDiscovery.java      # Environment-based discovery
└── BaselineOperations.java  # Baseline operations
```

**Run commands:**
```bash
cd lab02_multinode_cluster
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.StaticIPDiscovery"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.BaselineTopology"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.ClusterStates"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.CloudDiscovery"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.BaselineOperations"
```

### Lab 03: Basic Cache Operations (9 files)
```
com.example.ignite.solutions.lab03
├── BasicCacheOps.java        # CRUD operations
├── CacheModes.java           # PARTITIONED, REPLICATED modes
├── AsyncOperations.java      # Async API
├── BatchOperations.java      # putAll, getAll, removeAll
├── AdvancedCacheOps.java     # EntryProcessors, locks
├── CacheIteration.java       # ScanQuery iteration
├── DataStreamer.java         # Bulk loading
├── BinaryObjects.java        # Binary object API
└── PerformanceBenchmark.java # Performance testing
```

**Run commands:**
```bash
cd lab03_basic_cache_operations
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.BasicCacheOps"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.CacheModes"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.AsyncOperations"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.BatchOperations"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.AdvancedCacheOps"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.CacheIteration"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.DataStreamer"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.BinaryObjects"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab03.PerformanceBenchmark"
```

### Lab 04: Configuration & Deployment (5 files)
```
com.example.ignite.solutions.lab04
├── XmlConfig.java              # XML configuration loading
├── ProgrammaticConfig.java     # Programmatic configuration
├── Monitoring.java             # Metrics and monitoring
├── config/IgniteConfig.java    # Spring @Bean config
└── controller/CacheController.java  # REST controller
```

**Run commands:**
```bash
cd lab04_configuration_deployment
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab04.XmlConfig"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab04.ProgrammaticConfig"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab04.Monitoring"
```

### Lab 05: Data Modeling & Persistence (6 files)
```
com.example.ignite.solutions.lab05
├── model/Customer.java    # Customer entity
├── model/Order.java       # Order entity with affinity
├── AffinityKeys.java      # Colocation demonstration
├── Persistence.java       # Native persistence
├── CacheStore.java        # Read/write-through
└── WriteBehind.java       # Write-behind store
```

**Run commands:**
```bash
cd lab05_data_modeling_persistence
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab05.AffinityKeys"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab05.Persistence"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab05.CacheStore"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab05.WriteBehind"
```

### Lab 06: SQL and Indexing (6 files)
```
com.example.ignite.solutions.lab06
├── StartIgniteNode.java     # Server node for JDBC
├── BasicSQL.java            # DDL, DML, DQL
├── Indexing.java            # Index creation
├── JDBCConnection.java      # JDBC driver usage
├── DistributedJoins.java    # Distributed joins
└── QueryOptimization.java   # Query tuning
```

**Run commands:**
```bash
cd lab06_sql_indexing
# Start server node first (in separate terminal)
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab06.StartIgniteNode"

# Then run other solutions
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab06.BasicSQL"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab06.Indexing"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab06.JDBCConnection"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab06.DistributedJoins"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab06.QueryOptimization"
```

### Lab 07: Transactions & ACID (8 files)
```
com.example.ignite.solutions.lab07
├── Lab07BasicTransactions.java      # Transaction basics
├── Lab07TransactionModels.java      # PESSIMISTIC/OPTIMISTIC
├── Lab07IsolationComparison.java    # Isolation levels
├── Lab07DeadlockHandling.java       # Deadlock detection
├── Lab07BankTransfer.java           # Bank transfer example
├── Lab07CrossCacheTransactions.java # Cross-cache transactions
├── Lab07TransactionMonitor.java     # Transaction monitoring
└── Lab07BestPractices.java          # Best practices demo
```

**Run commands:**
```bash
cd lab07_transactions_acid
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07BasicTransactions"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07TransactionModels"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07IsolationComparison"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07DeadlockHandling"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07BankTransfer"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07CrossCacheTransactions"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07TransactionMonitor"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab07.Lab07BestPractices"
```

### Lab 08: Advanced Caching (6 files)
```
com.example.ignite.solutions.lab08
├── Lab08Eviction.java           # LRU, FIFO eviction
├── Lab08ExpiryPolicies.java     # TTL expiration policies
├── Lab08ContinuousQueries.java  # Real-time notifications
├── Lab08NearCache.java          # Near cache configuration
├── Lab08CacheEvents.java        # Cache event listeners
└── Lab08EntryProcessors.java    # Entry processor patterns
```

**Run commands:**
```bash
cd lab08_advanced_caching
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab08.Lab08Eviction"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab08.Lab08ExpiryPolicies"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab08.Lab08ContinuousQueries"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab08.Lab08NearCache"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab08.Lab08CacheEvents"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab08.Lab08EntryProcessors"
```

### Lab 09: Compute Grid (7 files)
```
com.example.ignite.solutions.lab09
├── Lab09BasicCompute.java     # Run, call, broadcast
├── Lab09ClosureCompute.java   # Closure-based compute
├── Lab09AffinityCompute.java  # Affinity-aware computing
├── Lab09MapReduce.java        # MapReduce pattern
├── Lab09AsyncCompute.java     # Async execution
├── Lab09FailoverCompute.java  # Failover handling
└── Lab09ClusterGroups.java    # Cluster group operations
```

**Run commands:**
```bash
cd lab09_compute_grid
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09BasicCompute"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09ClosureCompute"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09AffinityCompute"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09MapReduce"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09AsyncCompute"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09FailoverCompute"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab09.Lab09ClusterGroups"
```

### Lab 10: Integration & Connectivity (4 files)
```
com.example.ignite.solutions.lab10
├── Lab10RestAPI.java          # REST API usage
├── Lab10ThinClient.java       # Thin client connection
├── Lab10JDBC.java             # JDBC driver usage
└── Lab10SpringIntegration.java # Spring Framework
```

**Run commands:**
```bash
cd lab10_integration_connectivity
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab10.Lab10RestAPI"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab10.Lab10ThinClient"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab10.Lab10JDBC"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab10.Lab10SpringIntegration"
```

### Lab 11: Performance Tuning (6 files)
```
com.example.ignite.solutions.lab11
├── Lab11DataRegions.java        # Data region configuration
├── Lab11QueryOptimization.java  # Query analysis and tuning
├── Lab11Monitoring.java         # Performance monitoring
├── Lab11JVMTuning.java          # JVM configuration
├── Lab11Benchmark.java          # Performance benchmarks
└── Lab11AntiPatterns.java       # Common anti-patterns
```

**Run commands:**
```bash
cd lab11_performance_tuning
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab11.Lab11DataRegions"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab11.Lab11QueryOptimization"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab11.Lab11Monitoring"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab11.Lab11JVMTuning"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab11.Lab11Benchmark"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab11.Lab11AntiPatterns"
```

### Lab 12: Production Deployment (8 files)
```
com.example.ignite.solutions.lab12
├── Lab12SecurityOverview.java     # Security configuration
├── Lab12ClusterSetup.java         # Production cluster setup
├── Lab12SSLConfiguration.java     # SSL/TLS setup
├── Lab12Authentication.java       # Authentication/authorization
├── Lab12BackupRecovery.java       # Backup and recovery
├── Lab12RollingUpdate.java        # Zero-downtime updates
├── Lab12HealthChecker.java        # Health checking
└── Lab12DeploymentValidator.java  # Deployment validation
```

**Run commands:**
```bash
cd lab12_production_deployment
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab12.Lab12SecurityOverview"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab12.Lab12ClusterSetup"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab12.Lab12SSLConfiguration"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab12.Lab12Authentication"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab12.Lab12BackupRecovery"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab12.Lab12RollingUpdate"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab12.Lab12HealthChecker"
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab12.Lab12DeploymentValidator"
```

### Lab 13: Bonus - CDC Integration (6 files)
```
com.example.ignite.solutions.lab13
├── Lab13CDCIntegration.java    # Client mode (Docker Ignite)
├── Lab13CDCStandalone.java     # Standalone mode (embedded)
├── cdc/
│   ├── CDCEvent.java           # Debezium event model
│   └── IgniteCDCConsumer.java  # Kafka consumer
└── model/
    ├── Customer.java           # Customer entity
    ├── Product.java            # Product entity
    ├── Order.java              # Order entity
    └── OrderItem.java          # Order item entity
```

**Prerequisites:** Docker & Docker Compose

**Run commands:**
```bash
cd lab13_bonus_cdc_integration

# Start Docker environment
cd docker && ./start.sh && ./register-connector.sh && cd ..

# Run standalone mode
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab13.Lab13CDCStandalone"

# Or run client mode (with Docker Ignite)
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab13.Lab13CDCIntegration"
```

---

## Troubleshooting

### Common Issues

**Port conflicts:**
```bash
# Check if ports 47500-47509 are in use
netstat -an | grep 475
lsof -i :47500

# Kill process using port
kill -9 $(lsof -t -i:47500)
```

**Persistence directory cleanup:**
```bash
rm -rf ./ignite-data ./ignite-wal ./ignite-wal-archive ./work
```

**Memory issues:**
```bash
# Run with increased heap
java -Xms512m -Xmx2g -cp ... MainClass

# Check current Java memory settings
java -XshowSettings:vm -version
```

**Compilation issues:**
```bash
# Clean and rebuild
mvn clean compile -U

# Force dependency update
mvn dependency:purge-local-repository
mvn clean compile
```

### Recommended JVM Options

```bash
# Development
-Xms512m
-Xmx2g
-XX:+UseG1GC
-Djava.net.preferIPv4Stack=true

# Production
-Xms8g
-Xmx8g
-XX:+UseG1GC
-XX:+AlwaysPreTouch
-XX:+DisableExplicitGC
-XX:MaxDirectMemorySize=4g
-Djava.net.preferIPv4Stack=true
-DIGNITE_QUIET=false
```

### Build All Labs Script

```bash
#!/bin/bash
# save as build-all.sh

echo "Building all Apache Ignite lab solutions..."
cd "$(dirname "$0")"

for dir in lab*/; do
    echo ""
    echo "=== Building $dir ==="
    (cd "$dir" && mvn clean compile -q)
    if [ $? -eq 0 ]; then
        echo "SUCCESS: $dir"
    else
        echo "FAILED: $dir"
        exit 1
    fi
done

echo ""
echo "All labs built successfully!"
```

---

## Additional Resources

- [Apache Ignite Documentation](https://ignite.apache.org/docs/latest/)
- [Ignite Examples on GitHub](https://github.com/apache/ignite/tree/master/examples)
- [Ignite JavaDoc](https://ignite.apache.org/releases/latest/javadoc/)
- [Ignite SQL Reference](https://ignite.apache.org/docs/latest/SQL/sql-reference/index)

---

## License

These solutions are provided for educational purposes as part of the Apache Ignite training course.
