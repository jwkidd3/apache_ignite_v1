# Lab 2: Multi-Node Cluster Setup with Different Discovery Mechanisms

## Duration: 30 minutes

## Objectives
- Configure static IP discovery
- Set up a multi-node cluster with different discovery mechanisms
- Understand baseline topology
- Activate cluster and manage cluster state

## Prerequisites
- Completed Lab 1
- Apache Ignite dependencies configured
- Multiple terminal windows or IDE instances available

## Part 1: Static IP Discovery (15 minutes)

### Exercise 1: Configure Static IP Discovery

Create `Lab02StaticIPDiscovery.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

public class Lab02StaticIPDiscovery {

    public static void main(String[] args) {
        // Check if node number is provided
        if (args.length < 1) {
            System.out.println("Usage: java Lab02StaticIPDiscovery <nodeNumber>");
            System.exit(1);
        }

        int nodeNumber = Integer.parseInt(args[0]);

        // Create Ignite configuration
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("static-node-" + nodeNumber);

        // Configure TCP Discovery SPI
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();

        // Configure static IP finder
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();

        // Set the addresses of all nodes in the cluster
        ipFinder.setAddresses(Arrays.asList(
            "127.0.0.1:47500..47509"  // Port range for local testing
        ));

        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        // Start Ignite node
        System.out.println("Starting Ignite node " + nodeNumber + " with static IP discovery...");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\n=== Node Started Successfully ===");
            System.out.println("Node Name: " + ignite.name());
            System.out.println("Node ID: " + ignite.cluster().localNode().id());
            System.out.println("Cluster Nodes: " + ignite.cluster().nodes().size());

            // Display all nodes in cluster
            System.out.println("\n=== Cluster Topology ===");
            ignite.cluster().nodes().forEach(node -> {
                System.out.println("Node: " + node.consistentId() +
                                 " | ID: " + node.id().toString().substring(0, 8) + "...");
            });

            System.out.println("\nNode running. Press Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### Exercise 2: Run Multiple Nodes

1. **Open 3 terminal windows**

2. **Terminal 1:**
   ```bash
   java Lab02StaticIPDiscovery 1
   ```

3. **Terminal 2:**
   ```bash
   java Lab02StaticIPDiscovery 2
   ```

4. **Terminal 3:**
   ```bash
   java Lab02StaticIPDiscovery 3
   ```

5. **Observe:** All three nodes should discover each other and form a cluster

## Part 2: Cloud Discovery with Kubernetes/Docker (10 minutes)

### Exercise 3: Configure for Containerized Deployment

Create `Lab02CloudDiscovery.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

public class Lab02CloudDiscovery {

    public static void main(String[] args) {
        IgniteConfiguration cfg = new IgniteConfiguration();

        // Configure for containerized environment
        cfg.setIgniteInstanceName("cloud-node");
        cfg.setClientMode(false);

        // Configure discovery for cloud/container deployment
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();

        // In production, these would come from environment variables
        String discoveryAddresses = System.getenv("IGNITE_DISCOVERY_ADDRESSES");
        if (discoveryAddresses == null) {
            discoveryAddresses = "127.0.0.1:47500..47509";
        }

        ipFinder.setAddresses(Arrays.asList(discoveryAddresses.split(",")));
        discoverySpi.setIpFinder(ipFinder);

        // Set local port for containerized environments
        discoverySpi.setLocalPort(47500);
        discoverySpi.setLocalPortRange(10);

        cfg.setDiscoverySpi(discoverySpi);

        System.out.println("Starting cloud-ready Ignite node...");
        System.out.println("Discovery addresses: " + discoveryAddresses);

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Node started: " + ignite.name());
            System.out.println("Cluster size: " + ignite.cluster().nodes().size());

            System.out.println("\nPress Enter to stop...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 3: Baseline Topology and Cluster Activation (15 minutes)

### Exercise 4: Configure Baseline Topology

Create `Lab02BaselineTopology.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

public class Lab02BaselineTopology {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Lab02BaselineTopology <nodeNumber>");
            System.exit(1);
        }

        int nodeNumber = Integer.parseInt(args[0]);

        // Create configuration
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("baseline-node-" + nodeNumber);

        // Configure persistence (required for baseline topology)
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Configure default data region
        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setPersistenceEnabled(true);
        defaultRegion.setMaxSize(100L * 1024 * 1024); // 100 MB

        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);
        storageCfg.setStoragePath("./ignite-data/node" + nodeNumber);
        storageCfg.setWalPath("./ignite-wal/node" + nodeNumber);
        storageCfg.setWalArchivePath("./ignite-wal-archive/node" + nodeNumber);

        cfg.setDataStorageConfiguration(storageCfg);

        // Configure discovery
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        System.out.println("Starting node " + nodeNumber + " with persistence...");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\n=== Node Information ===");
            System.out.println("Node: " + ignite.name());
            System.out.println("Cluster size: " + ignite.cluster().nodes().size());
            System.out.println("Cluster state: " + ignite.cluster().state());

            // If this is node 1 and cluster is inactive, activate it
            if (nodeNumber == 1 && ignite.cluster().state() != ClusterState.ACTIVE) {
                System.out.println("\nActivating cluster...");
                ignite.cluster().state(ClusterState.ACTIVE);
                System.out.println("Cluster activated!");

                // Set baseline topology
                System.out.println("Setting baseline topology...");
                ignite.cluster().setBaselineTopology(ignite.cluster().topologyVersion());
                System.out.println("Baseline topology set!");
            }

            // Display baseline topology
            System.out.println("\n=== Baseline Topology ===");
            if (ignite.cluster().currentBaselineTopology() != null) {
                System.out.println("Baseline nodes: " +
                    ignite.cluster().currentBaselineTopology().size());
                ignite.cluster().currentBaselineTopology().forEach(node -> {
                    System.out.println("  - " + node.consistentId());
                });
            } else {
                System.out.println("Baseline topology not set");
            }

            System.out.println("\nNode running. Press Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Exercise 5: Activate Cluster and Set Baseline

1. **Start Node 1:**
   ```bash
   java Lab02BaselineTopology 1
   ```
   - This will activate the cluster and set baseline topology

2. **Start Node 2:**
   ```bash
   java Lab02BaselineTopology 2
   ```
   - Observe that it joins the baseline topology

3. **Start Node 3:**
   ```bash
   java Lab02BaselineTopology 3
   ```
   - All three nodes should be in the baseline topology

### Exercise 6: Manage Baseline Topology Programmatically

Add this code to check and update baseline:

```java
// Check if node is in baseline
boolean inBaseline = ignite.cluster().currentBaselineTopology()
    .stream()
    .anyMatch(n -> n.consistentId().equals(
        ignite.cluster().localNode().consistentId()));

System.out.println("This node in baseline: " + inBaseline);

// To add new nodes to baseline (from coordinator node):
if (nodeNumber == 1) {
    // Get current topology version
    long currentTopology = ignite.cluster().topologyVersion();
    System.out.println("Current topology version: " + currentTopology);

    // Update baseline to include all current nodes
    // Uncomment to execute:
    // ignite.cluster().setBaselineTopology(currentTopology);
}
```

## Verification Steps

### Checklist
- [ ] Static IP discovery configured successfully
- [ ] Three nodes started with static IP discovery
- [ ] All nodes discovered each other
- [ ] Baseline topology configured with persistence
- [ ] Cluster activated successfully
- [ ] Baseline topology set and displayed
- [ ] All nodes appear in baseline topology

### Testing Discovery

Create a simple test to verify cluster formation:

```java
public class Lab02DiscoveryTest {
    public static void main(String[] args) throws Exception {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("test-node");

        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        spi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(spi);

        try (Ignite ignite = Ignition.start(cfg)) {
            // Wait for topology to stabilize
            Thread.sleep(2000);

            int expectedNodes = 3;
            int actualNodes = ignite.cluster().nodes().size();

            System.out.println("Expected nodes: " + expectedNodes);
            System.out.println("Actual nodes: " + actualNodes);
            System.out.println("Test " +
                (actualNodes >= expectedNodes ? "PASSED" : "FAILED"));
        }
    }
}
```

## Lab Questions

1. What is the difference between multicast and static IP discovery?
2. Why is baseline topology important for persistent clusters?
3. What happens when a new node joins a cluster with baseline topology?
4. Can you activate a cluster without setting baseline topology?

## Answers

1. **Multicast discovery** uses IP multicast for automatic node discovery (may not work on all networks). **Static IP discovery** requires explicit IP addresses, more suitable for production and cloud environments.

2. **Baseline topology** defines which nodes are data-owning nodes in a persistent cluster. It ensures data consistency and proper rebalancing.

3. The new node joins the cluster but is **not automatically added to baseline**. You must explicitly update the baseline topology to include it as a data node.

4. **Yes**, you can activate without baseline for in-memory-only clusters. For persistent clusters, baseline topology is automatically set on activation.

## Common Issues

**Issue: Nodes not discovering each other**
- Check firewall settings
- Verify port range 47500-47509 is available
- Ensure IP addresses are correct

**Issue: Cluster won't activate**
- Check if persistence is properly configured
- Verify storage paths are writable
- Ensure at least one node is available

**Issue: Node not in baseline**
- Must explicitly add nodes to baseline
- Use `setBaselineTopology()` from any server node

## Next Steps

In Lab 3, you will:
- Implement basic cache operations
- Learn about different cache modes
- Perform CRUD operations
- Compare synchronous vs asynchronous operations

## Additional Resources

- Discovery SPI: https://ignite.apache.org/docs/latest/clustering/discovery
- Baseline Topology: https://ignite.apache.org/docs/latest/clustering/baseline-topology
- Cluster Configuration: https://ignite.apache.org/docs/latest/clustering/clustering

## Completion

You have completed Lab 2 when you can:
- Configure and use static IP discovery
- Start a multi-node cluster
- Activate cluster and set baseline topology
- Understand the role of baseline in persistent clusters
