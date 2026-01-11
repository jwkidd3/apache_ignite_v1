# Lab 02: Multi-Node Cluster Setup - Solutions

This directory contains the solution files for Lab 2 of the Apache Ignite training course.

## Overview

Lab 2 focuses on:
- Configuring static IP discovery
- Setting up a multi-node cluster
- Understanding and configuring baseline topology
- Activating cluster and managing cluster state

## Solution Files

| File | Description |
|------|-------------|
| `StaticIPDiscovery.java` | Exercise 1-2: Configures static IP discovery for multi-node setup |
| `BaselineTopology.java` | Exercise 3-5: Configures baseline topology with persistence |
| `ClusterStates.java` | Exercise 6: Demonstrates cluster state management (ACTIVE, ACTIVE_READ_ONLY) |
| `CloudDiscovery.java` | Optional: Environment-based discovery configuration |
| `BaselineOperations.java` | Optional: Advanced baseline topology management operations |

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Ignite 2.16.0

## Quick Start

```bash
# Build
mvn clean compile

# Package (create JAR with dependencies)
mvn clean package

# Run a specific solution
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.StaticIPDiscovery" -Dexec.args="1"
```

## All Maven Commands

```bash
# Clean the project
mvn clean

# Compile only
mvn compile

# Package into JAR
mvn package

# Skip tests during package
mvn package -DskipTests

# Download dependencies
mvn dependency:resolve

# Copy dependencies to target/dependency
mvn dependency:copy-dependencies

# Run with custom JVM options
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.StaticIPDiscovery" -Dexec.args="1" \
  -Djava.net.preferIPv4Stack=true
```

## Running the Solutions

### Exercise 1-2: Static IP Discovery

Open 3 terminal windows and run:

Terminal 1:
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.StaticIPDiscovery" -Dexec.args="1"
```

Terminal 2:
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.StaticIPDiscovery" -Dexec.args="2"
```

Terminal 3:
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.StaticIPDiscovery" -Dexec.args="3"
```

### Exercise 3-5: Baseline Topology

Start Node 1 first (this will activate the cluster):
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.BaselineTopology" -Dexec.args="1"
```

Then start additional nodes:
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.BaselineTopology" -Dexec.args="2"
```

### Exercise 6: Cluster States
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.ClusterStates"
```

### Optional: Cloud Discovery
```bash
# Set environment variable (optional)
export IGNITE_DISCOVERY_ADDRESSES="127.0.0.1:47500..47509"

mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.CloudDiscovery"
```

### Optional: Baseline Operations
```bash
# Add all online nodes to baseline
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.BaselineOperations" -Dexec.args="add-all"

# Remove offline nodes from baseline
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.BaselineOperations" -Dexec.args="remove-offline"

# Reset baseline to current topology
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab02.BaselineOperations" -Dexec.args="reset"
```

## Running Without Maven

```bash
# After running 'mvn package dependency:copy-dependencies'
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab02.StaticIPDiscovery 1

# With JVM options
java -Xms512m -Xmx2g -Djava.net.preferIPv4Stack=true \
  -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab02.StaticIPDiscovery 1
```

### All Solutions Without Maven

```bash
# Static IP Discovery (run in separate terminals with node IDs 1, 2, 3)
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab02.StaticIPDiscovery 1
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab02.StaticIPDiscovery 2
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab02.StaticIPDiscovery 3

# Baseline Topology
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab02.BaselineTopology 1
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab02.BaselineTopology 2

# Cluster States
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab02.ClusterStates

# Cloud Discovery
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab02.CloudDiscovery

# Baseline Operations
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab02.BaselineOperations add-all
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab02.BaselineOperations remove-offline
java -cp "target/classes:target/dependency/*" com.example.ignite.solutions.lab02.BaselineOperations reset
```

## Key Concepts Demonstrated

1. **TcpDiscoverySpi**: TCP-based discovery instead of multicast
2. **TcpDiscoveryVmIpFinder**: Static IP address configuration
3. **DataStorageConfiguration**: Persistence configuration with WAL
4. **ClusterState**: ACTIVE, ACTIVE_READ_ONLY, INACTIVE states
5. **Baseline Topology**: Managing data-owning nodes in persistent clusters

## Cleanup

To clean up persistence data between runs:
```bash
rm -rf ./ignite-data ./ignite-wal ./ignite-wal-archive
```

## Troubleshooting

### Nodes not discovering each other
- Check firewall settings
- Verify port range 47500-47509 is available
- Ensure IP addresses are correct

### Cluster won't activate
- Check if persistence is properly configured
- Verify storage paths are writable

### Baseline topology issues
- Ensure cluster is activated before setting baseline
- Check that all nodes have completed joining
