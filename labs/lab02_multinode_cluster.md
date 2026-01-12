# Lab 2: Multi-Node Cluster Setup with Different Discovery Mechanisms

## Duration: 50 minutes

## Objectives
- Configure static IP discovery
- Set up a multi-node cluster
- Understand and configure baseline topology
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

## Part 2: Baseline Topology and Cluster Activation (15 minutes)

### Exercise 3: Configure Baseline Topology

**IMPORTANT:** All nodes must join the cluster BEFORE activation. Once activated, the baseline is set and new nodes won't automatically be included. This is critical for production deployments.

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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

public class Lab02BaselineTopology {

    public static void main(String[] args) throws Exception {
        int nodeNumber = 1;
        int expectedNodes = 1;

        if (args.length >= 1) {
            nodeNumber = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            expectedNodes = Integer.parseInt(args[1]);
        }

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
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("\n=== Node Information ===");
            System.out.println("Node: " + ignite.name());
            System.out.println("Cluster state: " + ignite.cluster().state());

            // If cluster is inactive, handle activation
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                if (nodeNumber == 1) {
                    // Coordinator node - wait for all nodes then activate
                    System.out.println("\n=== Waiting for Nodes ===");
                    System.out.println("Expected nodes: " + expectedNodes);

                    // Wait for expected nodes to join
                    while (ignite.cluster().forServers().nodes().size() < expectedNodes) {
                        int currentSize = ignite.cluster().forServers().nodes().size();
                        System.out.println("Current nodes: " + currentSize + "/" + expectedNodes);
                        Thread.sleep(2000);
                    }

                    System.out.println("\nAll " + expectedNodes + " node(s) have joined!");
                    System.out.println("Press Enter to ACTIVATE cluster and set baseline...");
                    reader.readLine();

                    System.out.println("Activating cluster...");
                    ignite.cluster().state(ClusterState.ACTIVE);

                    // Set baseline topology with ALL current nodes
                    ignite.cluster().setBaselineTopology(ignite.cluster().topologyVersion());
                    System.out.println("Baseline set with all " +
                        ignite.cluster().forServers().nodes().size() + " nodes!");
                } else {
                    // Non-coordinator node - wait for activation
                    System.out.println("\nWaiting for coordinator (node 1) to activate...");
                    while (ignite.cluster().state() != ClusterState.ACTIVE) {
                        Thread.sleep(1000);
                    }
                    System.out.println("Cluster is now ACTIVE!");
                }
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
            reader.readLine();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Exercise 4: Activate Cluster and Set Baseline

**Correct procedure - start all nodes BEFORE activation:**

1. **Start Node 1 (coordinator) - expects 3 nodes:**
   ```bash
   java Lab02BaselineTopology 1 3
   ```
   - Node 1 will wait for 3 nodes to join before prompting for activation

2. **Start Node 2 (in separate terminal):**
   ```bash
   java Lab02BaselineTopology 2
   ```
   - Node 2 joins and waits for activation

3. **Start Node 3 (in separate terminal):**
   ```bash
   java Lab02BaselineTopology 3
   ```
   - Node 3 joins and waits for activation

4. **Activate on Node 1:**
   - When Node 1 shows "All 3 node(s) have joined!", press Enter
   - This activates the cluster and sets baseline with ALL 3 nodes

5. **Verify:** All three nodes should show they are in the baseline topology

### Exercise 5: Manage Baseline Topology Programmatically

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

## Part 3: Cluster State Management (10 minutes)

### Exercise 6: Understanding Cluster States

Create `Lab02ClusterStates.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

public class Lab02ClusterStates {

    public static void main(String[] args) {
        IgniteConfiguration cfg = createConfiguration("cluster-state-node");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\n=== Cluster State Management Demo ===");
            System.out.println("Initial cluster state: " + ignite.cluster().state());

            // Activate the cluster
            System.out.println("\n--- Setting to ACTIVE state ---");
            ignite.cluster().state(ClusterState.ACTIVE);
            System.out.println("State: " + ignite.cluster().state());

            // Create a test cache while active
            CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>("stateTestCache");
            cacheCfg.setCacheMode(CacheMode.REPLICATED);
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cacheCfg);
            cache.put(1, "test-value");
            System.out.println("Created cache and inserted data in ACTIVE state");

            // Switch to ACTIVE_READ_ONLY
            System.out.println("\n--- Setting to ACTIVE_READ_ONLY state ---");
            ignite.cluster().state(ClusterState.ACTIVE_READ_ONLY);
            System.out.println("State: " + ignite.cluster().state());

            // Try to write (will fail)
            System.out.println("\nAttempting write in ACTIVE_READ_ONLY state...");
            try {
                cache.put(2, "another-value");
                System.out.println("ERROR: Write succeeded (unexpected)");
            } catch (Exception e) {
                System.out.println("Write blocked (expected): " + e.getClass().getSimpleName());
            }

            // Read still works
            String value = cache.get(1);
            System.out.println("Read succeeded: key=1, value=" + value);

            // Return to ACTIVE
            System.out.println("\n--- Returning to ACTIVE state ---");
            ignite.cluster().state(ClusterState.ACTIVE);
            System.out.println("State: " + ignite.cluster().state());

            System.out.println("\n=== State Summary ===");
            System.out.println("ACTIVE: Full read/write access");
            System.out.println("ACTIVE_READ_ONLY: Reads allowed, writes blocked");
            System.out.println("INACTIVE: No cache access (for maintenance)");

            System.out.println("\nPress Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteConfiguration createConfiguration(String nodeName) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName(nodeName);

        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setPersistenceEnabled(true);
        defaultRegion.setMaxSize(100L * 1024 * 1024);
        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);
        storageCfg.setStoragePath("./ignite-data/cluster-state");
        storageCfg.setWalPath("./ignite-wal/cluster-state");
        storageCfg.setWalArchivePath("./ignite-wal-archive/cluster-state");
        cfg.setDataStorageConfiguration(storageCfg);

        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        return cfg;
    }
}
```

---

## Verification Steps

### Checklist
- [ ] Static IP discovery configured successfully
- [ ] Three nodes started with static IP discovery
- [ ] All nodes discovered each other
- [ ] Baseline topology configured with persistence
- [ ] Cluster activated successfully
- [ ] Baseline topology set and displayed
- [ ] Cluster state transitions tested (ACTIVE, ACTIVE_READ_ONLY)

### Common Issues

**Issue: Nodes not discovering each other**
- Check firewall settings
- Verify port range 47500-47509 is available
- Ensure IP addresses are correct

**Issue: Cluster won't activate**
- Check if persistence is properly configured
- Verify storage paths are writable

## Lab Questions

1. What is the difference between multicast and static IP discovery?
2. Why is baseline topology important for persistent clusters?
3. What is the purpose of the ACTIVE_READ_ONLY cluster state?

## Answers

1. **Multicast discovery** uses IP multicast for automatic node discovery (may not work on all networks). **Static IP discovery** requires explicit IP addresses, more suitable for production.

2. **Baseline topology** defines which nodes are data-owning nodes in a persistent cluster. It ensures data consistency and proper rebalancing.

3. **ACTIVE_READ_ONLY** allows read operations while blocking all writes. It is used during maintenance windows.

## Next Steps

In Lab 3, you will:
- Implement basic cache operations
- Learn about different cache modes
- Perform CRUD operations

## Completion

You have completed Lab 2 when you can:
- Configure and use static IP discovery
- Start a multi-node cluster
- Activate cluster and set baseline topology
- Switch between cluster states

---

## Optional Exercises (If Time Permits)

### Optional: Cloud Discovery Configuration

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
        cfg.setIgniteInstanceName("cloud-node");
        cfg.setClientMode(false);

        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();

        // In production, these would come from environment variables
        String discoveryAddresses = System.getenv("IGNITE_DISCOVERY_ADDRESSES");
        if (discoveryAddresses == null) {
            discoveryAddresses = "127.0.0.1:47500..47509";
        }

        ipFinder.setAddresses(Arrays.asList(discoveryAddresses.split(",")));
        discoverySpi.setIpFinder(ipFinder);
        discoverySpi.setLocalPort(47500);
        discoverySpi.setLocalPortRange(10);
        cfg.setDiscoverySpi(discoverySpi);

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

### Optional: Baseline Management Operations

Create `Lab02BaselineOperations.java` for advanced baseline management:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.BaselineNode;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.*;

public class Lab02BaselineOperations {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Lab02BaselineOperations <operation>");
            System.out.println("Operations: add-all | remove-offline | reset");
            System.exit(1);
        }

        String operation = args[0];
        IgniteConfiguration cfg = createConfiguration("baseline-ops-node");

        try (Ignite ignite = Ignition.start(cfg)) {
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                ignite.cluster().state(ClusterState.ACTIVE);
            }

            switch (operation) {
                case "add-all":
                    addAllNodesToBaseline(ignite);
                    break;
                case "remove-offline":
                    removeOfflineFromBaseline(ignite);
                    break;
                case "reset":
                    resetBaseline(ignite);
                    break;
                default:
                    System.out.println("Unknown operation: " + operation);
            }

            // Display updated baseline
            displayBaseline(ignite);

            System.out.println("\nPress Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addAllNodesToBaseline(Ignite ignite) {
        System.out.println("\n=== Adding All Online Nodes to Baseline ===");

        // Get current topology version
        long topologyVersion = ignite.cluster().topologyVersion();
        System.out.println("Current topology version: " + topologyVersion);

        // Set baseline to include all current server nodes
        ignite.cluster().setBaselineTopology(topologyVersion);
        System.out.println("Baseline updated to topology version " + topologyVersion);
    }

    private static void removeOfflineFromBaseline(Ignite ignite) {
        System.out.println("\n=== Removing Offline Nodes from Baseline ===");

        Collection<BaselineNode> currentBaseline = ignite.cluster().currentBaselineTopology();
        if (currentBaseline == null || currentBaseline.isEmpty()) {
            System.out.println("No baseline topology set.");
            return;
        }

        // Get online node IDs
        Set<Object> onlineIds = new HashSet<>();
        ignite.cluster().forServers().nodes()
            .forEach(n -> onlineIds.add(n.consistentId()));

        // Filter baseline to only include online nodes
        List<BaselineNode> newBaseline = new ArrayList<>();
        for (BaselineNode node : currentBaseline) {
            if (onlineIds.contains(node.consistentId())) {
                newBaseline.add(node);
            } else {
                System.out.println("Removing offline node: " + node.consistentId());
            }
        }

        // Update baseline with only online nodes
        ignite.cluster().setBaselineTopology(newBaseline);
        System.out.println("Baseline updated. Removed offline nodes.");
    }

    private static void resetBaseline(Ignite ignite) {
        System.out.println("\n=== Resetting Baseline to Current Topology ===");

        // This effectively recreates baseline from scratch with current nodes
        long currentVersion = ignite.cluster().topologyVersion();
        ignite.cluster().setBaselineTopology(currentVersion);

        System.out.println("Baseline reset to topology version: " + currentVersion);
        System.out.println("Baseline now contains all current server nodes.");
    }

    private static void displayBaseline(Ignite ignite) {
        System.out.println("\n=== Updated Baseline Topology ===");

        Collection<BaselineNode> baseline = ignite.cluster().currentBaselineTopology();
        if (baseline != null) {
            System.out.println("Total nodes: " + baseline.size());
            baseline.forEach(n -> System.out.println("  - " + n.consistentId()));
        }
    }

    private static IgniteConfiguration createConfiguration(String nodeName) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName(nodeName);

        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setPersistenceEnabled(true);
        defaultRegion.setMaxSize(100L * 1024 * 1024);
        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);
        storageCfg.setStoragePath("./ignite-data/baseline-ops");
        storageCfg.setWalPath("./ignite-wal/baseline-ops");
        storageCfg.setWalArchivePath("./ignite-wal-archive/baseline-ops");
        cfg.setDataStorageConfiguration(storageCfg);

        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        return cfg;
    }
}
```
