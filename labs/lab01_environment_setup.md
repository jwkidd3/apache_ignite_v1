# Lab 1: Environment Setup and First Ignite Cluster Startup

## Duration: 30 minutes

## Objectives
- Install and configure Apache Ignite
- Start your first Ignite node
- Understand basic cluster operations
- Verify the installation and cluster connectivity

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

## Part 3: Basic Cluster Operations (Bonus)

### Exercise 4: Add Cluster Information

Extend your program to display more cluster information:

```java
// Add after starting Ignite
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

## Answers

1. **Multicast discovery** - Ignite uses IP multicast by default for node discovery
2. **Server nodes** store data and participate in compute operations. **Client nodes** don't store data but can access cluster resources
3. The **topology version increments**, and all nodes are notified of the change through discovery events

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

You have completed Lab 1!
