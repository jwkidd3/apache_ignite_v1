# Lab 1: Environment Setup and First Ignite Cluster Startup

## Duration: 45 minutes

## Objectives
- Install and configure Apache Ignite
- Start your first Ignite node
- Understand basic cluster operations
- Verify the installation and cluster connectivity
- Explore basic cluster metrics

## Prerequisites
- Java 8 or higher installed
- Maven or Gradle installed
- IDE of your choice (IntelliJ IDEA, Eclipse, or VS Code)
- Internet connection for downloading dependencies

## Part 1: Environment Setup (15 minutes)

### Step 1: Download Apache Ignite

**Option A: Using Maven**

Create a new Maven project and add Ignite dependency to `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>org.apache.ignite</groupId>
        <artifactId>ignite-core</artifactId>
        <version>2.16.0</version>
    </dependency>
    <dependency>
        <groupId>org.apache.ignite</groupId>
        <artifactId>ignite-spring</artifactId>
        <version>2.16.0</version>
    </dependency>
    <dependency>
        <groupId>org.apache.ignite</groupId>
        <artifactId>ignite-indexing</artifactId>
        <version>2.16.0</version>
    </dependency>
</dependencies>
```

**Option B: Using Gradle**

Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'org.apache.ignite:ignite-core:2.16.0'
    implementation 'org.apache.ignite:ignite-spring:2.16.0'
    implementation 'org.apache.ignite:ignite-indexing:2.16.0'
}
```

### Step 2: Verify Java Installation

```bash
java -version
```

Expected output: Java 8 or higher

### Step 3: Create Project Structure

```
ignite-labs/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── example/
│                   └── ignite/
│                       └── Lab01FirstCluster.java
└── pom.xml (or build.gradle)
```

## Part 2: Start Your First Ignite Node (15 minutes)

### Exercise 1: Create a Simple Ignite Node

Create `Lab01FirstCluster.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;

public class Lab01FirstCluster {

    public static void main(String[] args) {
        // Create Ignite configuration
        IgniteConfiguration cfg = new IgniteConfiguration();

        // Set a unique name for this node
        cfg.setIgniteInstanceName("first-ignite-node");

        // Enable peer class loading (useful for development)
        cfg.setPeerClassLoadingEnabled(true);

        System.out.println("Starting Ignite node...");

        // Start Ignite node
        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Ignite node started successfully!");
            System.out.println("Node ID: " + ignite.cluster().localNode().id());
            System.out.println("Node is client: " + ignite.cluster().localNode().isClient());
            System.out.println("Total nodes in cluster: " + ignite.cluster().nodes().size());

            // Keep the node running for observation
            System.out.println("\nNode is running. Press Enter to stop...");
            System.in.read();

            System.out.println("Stopping Ignite node...");
        } catch (Exception e) {
            System.err.println("Error running Ignite node: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Ignite node stopped.");
    }
}
```

### Exercise 2: Run and Observe

1. **Compile and run the program**

2. **Observe the console output:**
   - Node startup messages
   - Topology version
   - Node ID (UUID)
   - Cluster metrics

3. **Expected Console Output:**
   ```
   Starting Ignite node...
   [timestamp][INFO][main] >>> Ignite instance name: first-ignite-node
   [timestamp][INFO][main] Ignite node started OK
   Ignite node started successfully!
   Node ID: [UUID]
   Node is client: false
   Total nodes in cluster: 1

   Node is running. Press Enter to stop...
   ```

### Exercise 3: Start Multiple Nodes

Modify the code to start a second node in a separate terminal/IDE instance:

```java
cfg.setIgniteInstanceName("second-ignite-node");
```

Run both nodes and observe:
- Both nodes should discover each other
- Total nodes in cluster should show: 2
- Check the topology version increments

## Part 3: Cluster Metrics Exploration (5 minutes)

### Exercise 4: Explore Cluster Metrics

Create `Lab01ClusterMetrics.java` to explore the cluster metrics API:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;

public class Lab01ClusterMetrics {

    public static void main(String[] args) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("metrics-explorer-node");
        cfg.setPeerClassLoadingEnabled(true);

        System.out.println("Starting Ignite node for metrics exploration...\n");

        try (Ignite ignite = Ignition.start(cfg)) {
            // Get the local node
            ClusterNode localNode = ignite.cluster().localNode();

            // Get cluster-wide metrics
            ClusterMetrics clusterMetrics = ignite.cluster().metrics();

            System.out.println("=== Cluster-Wide Metrics ===");
            System.out.println("Total nodes in cluster: " + ignite.cluster().nodes().size());
            System.out.println("Total CPUs across cluster: " + clusterMetrics.getTotalCpus());
            System.out.println("Average CPU load: " + String.format("%.2f%%", clusterMetrics.getAverageCpuLoad() * 100));
            System.out.println("Current CPU load: " + String.format("%.2f%%", clusterMetrics.getCurrentCpuLoad() * 100));

            System.out.println("\n=== Memory Metrics ===");
            System.out.println("Heap memory used: " + formatBytes(clusterMetrics.getHeapMemoryUsed()));
            System.out.println("Heap memory maximum: " + formatBytes(clusterMetrics.getHeapMemoryMaximum()));
            System.out.println("Heap memory committed: " + formatBytes(clusterMetrics.getHeapMemoryCommitted()));
            System.out.println("Non-heap memory used: " + formatBytes(clusterMetrics.getNonHeapMemoryUsed()));

            System.out.println("\n=== Job Metrics ===");
            System.out.println("Active jobs: " + clusterMetrics.getCurrentActiveJobs());
            System.out.println("Waiting jobs: " + clusterMetrics.getCurrentWaitingJobs());
            System.out.println("Executed jobs: " + clusterMetrics.getTotalExecutedJobs());
            System.out.println("Rejected jobs: " + clusterMetrics.getTotalRejectedJobs());
            System.out.println("Cancelled jobs: " + clusterMetrics.getTotalCancelledJobs());

            System.out.println("\n=== Thread Metrics ===");
            System.out.println("Current thread count: " + clusterMetrics.getCurrentThreadCount());
            System.out.println("Maximum thread count: " + clusterMetrics.getMaximumThreadCount());
            System.out.println("Current daemon thread count: " + clusterMetrics.getCurrentDaemonThreadCount());

            System.out.println("\n=== Per-Node Metrics ===");
            for (ClusterNode node : ignite.cluster().nodes()) {
                ClusterMetrics nodeMetrics = node.metrics();
                System.out.println("\nNode: " + node.id().toString().substring(0, 8) + "...");
                System.out.println("  CPUs: " + nodeMetrics.getTotalCpus());
                System.out.println("  Heap used: " + formatBytes(nodeMetrics.getHeapMemoryUsed()));
                System.out.println("  Active jobs: " + nodeMetrics.getCurrentActiveJobs());
                System.out.println("  Uptime: " + formatUptime(nodeMetrics.getUpTime()));
            }

            System.out.println("\n\nMetrics exploration complete. Press Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private static String formatUptime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("%d hours, %d minutes, %d seconds",
            hours, minutes % 60, seconds % 60);
    }
}
```

## Part 4: Verify Installation (5 minutes)

### Exercise 5: Cluster Information Display

Add cluster information to your program to verify everything works:

```java
// Add after starting Ignite in Lab01FirstCluster.java
System.out.println("\n=== Cluster Information ===");
System.out.println("Cluster nodes: " + ignite.cluster().nodes().size());

// Display all node IDs in the cluster
System.out.println("\n=== Node IDs in Cluster ===");
ignite.cluster().nodes().forEach(node -> {
    System.out.println("Node ID: " + node.id());
    System.out.println("  - Consistent ID: " + node.consistentId());
    System.out.println("  - Is Local: " + node.isLocal());
    System.out.println("  - Is Client: " + node.isClient());
});

// Display cluster state
System.out.println("\n=== Cluster State ===");
System.out.println("Active: " + ignite.cluster().state().active());
```

---

## Optional Exercises (If Time Permits)

### Challenge 1: Start a 3-Node Cluster

Create a program that programmatically starts 3 Ignite nodes in the same JVM and verifies they form a cluster:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.util.ArrayList;
import java.util.List;

public class Lab01Challenge1MultiNode {

    public static void main(String[] args) {
        List<Ignite> nodes = new ArrayList<>();

        try {
            System.out.println("=== Challenge 1: Starting 3-Node Cluster ===\n");

            // Start 3 nodes
            for (int i = 1; i <= 3; i++) {
                IgniteConfiguration cfg = new IgniteConfiguration();
                cfg.setIgniteInstanceName("node-" + i);
                cfg.setPeerClassLoadingEnabled(true);

                // Set unique attributes for each node
                cfg.setUserAttributes(java.util.Collections.singletonMap("nodeNumber", i));

                System.out.println("Starting node-" + i + "...");
                Ignite ignite = Ignition.start(cfg);
                nodes.add(ignite);

                System.out.println("  Node-" + i + " started. Cluster size: "
                    + ignite.cluster().nodes().size());
            }

            // Verify cluster formation
            System.out.println("\n=== Cluster Verification ===");
            Ignite firstNode = nodes.get(0);

            if (firstNode.cluster().nodes().size() == 3) {
                System.out.println("SUCCESS: All 3 nodes have joined the cluster!");
            } else {
                System.out.println("WARNING: Expected 3 nodes, found: "
                    + firstNode.cluster().nodes().size());
            }

            // Display cluster topology
            System.out.println("\n=== Cluster Topology ===");
            firstNode.cluster().nodes().forEach(node -> {
                Integer nodeNum = node.attribute("nodeNumber");
                System.out.printf("  %s (node-%d): %s%n",
                    node.id().toString().substring(0, 8) + "...",
                    nodeNum != null ? nodeNum : 0,
                    node.isLocal() ? "LOCAL" : "REMOTE");
            });

            System.out.println("\nPress Enter to stop all nodes...");
            System.in.read();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Stop all nodes gracefully
            System.out.println("\nStopping all nodes...");
            for (int i = nodes.size() - 1; i >= 0; i--) {
                String name = nodes.get(i).name();
                nodes.get(i).close();
                System.out.println("  " + name + " stopped.");
            }
            System.out.println("All nodes stopped.");
        }
    }
}
```

### Challenge 2: Implement Graceful Shutdown

Create a program with proper shutdown hooks and graceful termination:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.Event;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class Lab01Challenge2GracefulShutdown {

    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final CountDownLatch shutdownLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("graceful-shutdown-node");
        cfg.setPeerClassLoadingEnabled(true);

        // Enable discovery events
        cfg.setIncludeEventTypes(
            EventType.EVT_NODE_JOINED,
            EventType.EVT_NODE_LEFT,
            EventType.EVT_NODE_FAILED
        );

        System.out.println("=== Challenge 2: Graceful Shutdown Demo ===\n");

        Ignite ignite = null;

        try {
            ignite = Ignition.start(cfg);
            final Ignite finalIgnite = ignite;

            // Register event listeners for node lifecycle
            ignite.events().localListen(new IgnitePredicate<Event>() {
                @Override
                public boolean apply(Event evt) {
                    System.out.println("[EVENT] " + evt.name() + " at " +
                        java.time.LocalTime.now());
                    return true; // Continue listening
                }
            }, EventType.EVT_NODE_JOINED, EventType.EVT_NODE_LEFT, EventType.EVT_NODE_FAILED);

            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[SHUTDOWN HOOK] Initiating graceful shutdown...");
                running.set(false);

                if (finalIgnite != null && !finalIgnite.cluster().nodes().isEmpty()) {
                    System.out.println("[SHUTDOWN HOOK] Performing pre-shutdown tasks...");

                    // Log final metrics before shutdown
                    try {
                        System.out.println("[SHUTDOWN HOOK] Final cluster size: "
                            + finalIgnite.cluster().nodes().size());
                        System.out.println("[SHUTDOWN HOOK] Total jobs executed: "
                            + finalIgnite.cluster().metrics().getTotalExecutedJobs());
                    } catch (Exception e) {
                        System.out.println("[SHUTDOWN HOOK] Could not retrieve final metrics.");
                    }

                    System.out.println("[SHUTDOWN HOOK] Stopping Ignite node...");
                    finalIgnite.close();
                    System.out.println("[SHUTDOWN HOOK] Ignite node stopped successfully.");
                }

                shutdownLatch.countDown();
            }));

            System.out.println("Node started successfully.");
            System.out.println("Shutdown hook registered.");
            System.out.println("\nNode is running. Use Ctrl+C or send SIGTERM for graceful shutdown.");
            System.out.println("Alternatively, press Enter to trigger manual shutdown.\n");

            // Main loop - simulate work
            Thread monitorThread = new Thread(() -> {
                while (running.get()) {
                    try {
                        Thread.sleep(5000);
                        if (running.get()) {
                            System.out.println("[HEARTBEAT] Node active. Cluster size: "
                                + finalIgnite.cluster().nodes().size()
                                + " | Time: " + java.time.LocalTime.now());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            monitorThread.setDaemon(true);
            monitorThread.start();

            // Wait for Enter key for manual shutdown
            System.in.read();
            System.out.println("\n[MAIN] Manual shutdown requested...");
            running.set(false);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (ignite != null) {
                System.out.println("[MAIN] Closing Ignite instance...");
                ignite.close();
                System.out.println("[MAIN] Ignite instance closed.");
            }
        }

        System.out.println("[MAIN] Application terminated gracefully.");
    }
}
```

### Challenge 3: Create a Node Health Check Utility

Build a utility that connects as a CLIENT to an existing cluster and checks the health of ALL server nodes:

**Important:** This utility runs as a client node, so you must first start one or more server nodes (e.g., from Exercise 1 or Challenge 1) before running this health check.

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Lab01Challenge3HealthCheck {

    // Health thresholds
    private static final double CPU_WARNING_THRESHOLD = 0.7;    // 70%
    private static final double CPU_CRITICAL_THRESHOLD = 0.9;   // 90%
    private static final double HEAP_WARNING_THRESHOLD = 0.7;   // 70%
    private static final double HEAP_CRITICAL_THRESHOLD = 0.9;  // 90%
    private static final int MAX_REJECTED_JOBS = 10;

    public static void main(String[] args) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("health-check-client");
        cfg.setPeerClassLoadingEnabled(true);

        // Run as CLIENT node - connects to existing cluster without storing data
        cfg.setClientMode(true);

        // Configure discovery to find existing cluster nodes
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        System.out.println("=== Challenge 3: Node Health Check Utility ===\n");
        System.out.println("Connecting as CLIENT to check health of ALL cluster nodes...\n");

        try (Ignite ignite = Ignition.start(cfg)) {
            int serverCount = ignite.cluster().forServers().nodes().size();
            int clientCount = ignite.cluster().forClients().nodes().size();

            System.out.println("Connected to cluster!");
            System.out.println("  Server nodes: " + serverCount);
            System.out.println("  Client nodes: " + clientCount + " (including this health checker)\n");

            if (serverCount == 0) {
                System.out.println("WARNING: No server nodes found!");
                System.out.println("Start server nodes first, then run this health check.");
                return;
            }

            // Perform health check
            HealthReport report = performHealthCheck(ignite);

            // Display report
            System.out.println(report.toString());

            // Continuous monitoring option
            System.out.println("\nStarting continuous monitoring (5 checks, 10 seconds apart)...\n");
            for (int i = 1; i <= 5; i++) {
                Thread.sleep(10000);
                System.out.println("--- Health Check #" + i + " ---");
                HealthReport r = performHealthCheck(ignite);
                System.out.println("Server nodes monitored: " + r.clusterSize);
                System.out.println("Overall Status: " + r.overallStatus);
                System.out.println("Issues: " + (r.issues.isEmpty() ? "None" : r.issues.size()));
                r.issues.forEach(issue -> System.out.println("  - " + issue));
                System.out.println();
            }

            System.out.println("Health check complete. Press Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static HealthReport performHealthCheck(Ignite ignite) {
        HealthReport report = new HealthReport();
        report.timestamp = java.time.LocalDateTime.now().toString();
        report.clusterSize = ignite.cluster().forServers().nodes().size();

        // Check each SERVER node (not clients)
        for (ClusterNode node : ignite.cluster().forServers().nodes()) {
            ClusterMetrics metrics = node.metrics();
            String nodeId = node.id().toString().substring(0, 8) + "...";

            // Check CPU usage
            double cpuLoad = metrics.getCurrentCpuLoad();
            if (cpuLoad > CPU_CRITICAL_THRESHOLD) {
                report.issues.add("CRITICAL: Node " + nodeId + " CPU at "
                    + String.format("%.1f%%", cpuLoad * 100));
                report.overallStatus = HealthStatus.CRITICAL;
            } else if (cpuLoad > CPU_WARNING_THRESHOLD) {
                report.issues.add("WARNING: Node " + nodeId + " CPU at "
                    + String.format("%.1f%%", cpuLoad * 100));
                if (report.overallStatus != HealthStatus.CRITICAL) {
                    report.overallStatus = HealthStatus.WARNING;
                }
            }

            // Check heap usage
            double heapUsage = (double) metrics.getHeapMemoryUsed() /
                               metrics.getHeapMemoryMaximum();
            if (heapUsage > HEAP_CRITICAL_THRESHOLD) {
                report.issues.add("CRITICAL: Node " + nodeId + " heap at "
                    + String.format("%.1f%%", heapUsage * 100));
                report.overallStatus = HealthStatus.CRITICAL;
            } else if (heapUsage > HEAP_WARNING_THRESHOLD) {
                report.issues.add("WARNING: Node " + nodeId + " heap at "
                    + String.format("%.1f%%", heapUsage * 100));
                if (report.overallStatus != HealthStatus.CRITICAL) {
                    report.overallStatus = HealthStatus.WARNING;
                }
            }

            // Check rejected jobs
            if (metrics.getTotalRejectedJobs() > MAX_REJECTED_JOBS) {
                report.issues.add("WARNING: Node " + nodeId + " has "
                    + metrics.getTotalRejectedJobs() + " rejected jobs");
                if (report.overallStatus == HealthStatus.HEALTHY) {
                    report.overallStatus = HealthStatus.WARNING;
                }
            }

            // Add node summary to report
            report.nodeSummaries.add(new NodeSummary(
                nodeId,
                cpuLoad,
                heapUsage,
                metrics.getCurrentActiveJobs(),
                metrics.getUpTime()
            ));
        }

        return report;
    }

    enum HealthStatus {
        HEALTHY, WARNING, CRITICAL
    }

    static class HealthReport {
        String timestamp;
        int clusterSize;
        HealthStatus overallStatus = HealthStatus.HEALTHY;
        List<String> issues = new ArrayList<>();
        List<NodeSummary> nodeSummaries = new ArrayList<>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("╔══════════════════════════════════════════════════════════════╗\n");
            sb.append("║                    CLUSTER HEALTH REPORT                      ║\n");
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║ Timestamp: %-50s ║%n", timestamp));
            sb.append(String.format("║ Server Nodes: %-47d ║%n", clusterSize));
            sb.append(String.format("║ Overall Status: %-45s ║%n", overallStatus));
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append("║ SERVER NODE DETAILS                                           ║\n");
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");

            for (NodeSummary ns : nodeSummaries) {
                sb.append(String.format("║ Node: %-55s ║%n", ns.nodeId));
                sb.append(String.format("║   CPU: %-6.1f%%  Heap: %-6.1f%%  Active Jobs: %-14d ║%n",
                    ns.cpuLoad * 100, ns.heapUsage * 100, ns.activeJobs));
                sb.append(String.format("║   Uptime: %-51s ║%n", formatUptime(ns.uptime)));
            }

            if (!issues.isEmpty()) {
                sb.append("╠══════════════════════════════════════════════════════════════╣\n");
                sb.append("║ ISSUES                                                        ║\n");
                sb.append("╠══════════════════════════════════════════════════════════════╣\n");
                for (String issue : issues) {
                    sb.append(String.format("║ • %-60s ║%n", issue));
                }
            }

            sb.append("╚══════════════════════════════════════════════════════════════╝");
            return sb.toString();
        }

        private String formatUptime(long millis) {
            long seconds = millis / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        }
    }

    static class NodeSummary {
        String nodeId;
        double cpuLoad;
        double heapUsage;
        int activeJobs;
        long uptime;

        NodeSummary(String nodeId, double cpuLoad, double heapUsage,
                    int activeJobs, long uptime) {
            this.nodeId = nodeId;
            this.cpuLoad = cpuLoad;
            this.heapUsage = heapUsage;
            this.activeJobs = activeJobs;
            this.uptime = uptime;
        }
    }
}
```

## Verification Steps

### Checklist
- [ ] Java version verified (8 or higher)
- [ ] Maven/Gradle project created successfully
- [ ] Ignite dependencies added and downloaded
- [ ] First Ignite node starts without errors
- [ ] Node ID displayed in console
- [ ] Second node started and discovered by first node
- [ ] Cluster shows 2 total nodes
- [ ] Cluster information displayed correctly
- [ ] Cluster metrics exploration completed (CPU, heap, jobs displayed)

### Common Issues and Solutions

**Issue 1: Port Already in Use**
```
Solution: Ignite uses ports 47500-47509 by default. Ensure these ports are available.
```

**Issue 2: Multicast Discovery Not Working**
```
Solution: If on a network that blocks multicast, you'll learn about alternative
discovery mechanisms in Lab 2.
```

**Issue 3: Class Not Found Exception**
```
Solution: Verify all dependencies are properly added and Maven/Gradle has
downloaded them.
```

## Lab Questions

1. What is the default discovery mechanism used when no configuration is specified?
2. What is the difference between a server node and a client node?
3. What happens to the cluster topology when a new node joins?
4. What ClusterMetrics method would you use to determine the total available CPU cores across all nodes in the cluster?

## Answers

1. **Multicast discovery** - Ignite uses IP multicast by default for node discovery
2. **Server nodes** store data and participate in compute operations. **Client nodes** don't store data but can access cluster resources
3. The **topology version increments**, and all nodes are notified of the change through discovery events
4. Use `ignite.cluster().metrics().getTotalCpus()` to get the total CPU count across all cluster nodes

## Next Steps

In Lab 2, you will:
- Configure different discovery mechanisms
- Set up a multi-node cluster with static IP discovery
- Explore cluster topology in depth
- Configure baseline topology

## Additional Resources

- Apache Ignite Documentation: https://ignite.apache.org/docs/latest/
- Ignite Configuration Reference: https://ignite.apache.org/docs/latest/configuration/
- GitHub Examples: https://github.com/apache/ignite/tree/master/examples

## Completion

Once you can successfully:
- Start an Ignite node
- View cluster information
- Run multiple nodes that discover each other
- Access and display cluster metrics (CPU, memory, jobs)

You have completed Lab 1!
