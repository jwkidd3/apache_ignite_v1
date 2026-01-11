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

## Building the Project

```bash
cd lab02_multinode_cluster
mvn clean compile
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
