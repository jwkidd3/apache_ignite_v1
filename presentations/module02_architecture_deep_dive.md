# Module 2: Ignite Architecture Deep Dive

**Duration:** 60 minutes
**Type:** Presentation

---

## Slide 1: Title
**Apache Ignite Architecture Deep Dive**

Understanding the internals of distributed in-memory computing

---

## Slide 2: Module Objectives

By the end of this module, you will understand:
- Ignite cluster topology and node types
- Data distribution and partitioning
- Memory architecture (on-heap, off-heap, persistence)
- Discovery mechanisms
- Baseline topology and cluster activation

---

## Slide 3: Cluster Topology Overview

### What is an Ignite Cluster?

**Definition:**
A group of Ignite nodes connected together, sharing data and computation.

```
┌─────────────────────────────────────┐
│         Ignite Cluster              │
│  ┌──────┐  ┌──────┐  ┌──────┐      │
│  │Node 1│  │Node 2│  │Node 3│      │
│  └──────┘  └──────┘  └──────┘      │
│     ↕          ↕          ↕         │
│  ┌──────────────────────────┐      │
│  │  Distributed Data Grid   │      │
│  └──────────────────────────┘      │
└─────────────────────────────────────┘
```

**Key Characteristics:**
- All nodes equal (peer-to-peer)
- Automatic discovery
- Self-healing
- Dynamic topology (nodes join/leave)

---

## Slide 4: Node Types

### Server Nodes vs Client Nodes

**Server Nodes:**
- Store data
- Participate in compute operations
- Full members of cluster
- Contribute resources (CPU, RAM)
- Participate in baseline topology

**Client Nodes:**
- Do not store data
- Lightweight
- Connect to cluster
- Can execute operations
- Route requests to server nodes
- Good for application servers

**Visual:**
```
Client Nodes          Server Nodes (Data)
┌────────┐           ┌────────────────┐
│  App   │──────────▶│  Node 1: Data  │
│ Client │           ├────────────────┤
└────────┘           │  Node 2: Data  │
                     ├────────────────┤
┌────────┐           │  Node 3: Data  │
│  App   │──────────▶└────────────────┘
│ Client │
└────────┘
```

---

## Slide 5: Node Configuration

### Setting Node Mode

**Server Node (Default):**
```java
IgniteConfiguration cfg = new IgniteConfiguration();
cfg.setClientMode(false); // Server mode
Ignite ignite = Ignition.start(cfg);
```

**Client Node:**
```java
IgniteConfiguration cfg = new IgniteConfiguration();
cfg.setClientMode(true); // Client mode
Ignite ignite = Ignition.start(cfg);
```

**Best Practices:**
- Use server nodes for data storage
- Use client nodes for application servers
- Typical: 3-5+ server nodes, N client nodes
- Separate application tier from data tier

---

## Slide 6: Data Distribution Fundamentals

### How Data is Spread Across Nodes

**Partitioning:**
- Data divided into partitions (default: 1024)
- Each partition assigned to specific nodes
- Even distribution across cluster
- Key → Hash → Partition → Node(s)

**Visual:**
```
Key: "user123"
    ↓ (hash function)
Partition: 456
    ↓ (affinity)
Nodes: [Node 2 (primary), Node 3 (backup)]
```

**Benefits:**
- Parallel processing
- Linear scalability
- Fault tolerance with backups
- Load balancing

---

## Slide 7: Partitioning Example

### Data Distribution Process

```
1024 Partitions distributed across 4 nodes:

Node 1: Partitions [0-255]     (256 partitions)
Node 2: Partitions [256-511]   (256 partitions)
Node 3: Partitions [512-767]   (256 partitions)
Node 4: Partitions [768-1023]  (256 partitions)

When you put key "X":
1. Hash("X") = 1234567890
2. Partition = 1234567890 % 1024 = 442
3. Primary Node = Node 2 (owns partition 442)
4. Backup Node = Node 3 (if backups=1)
```

**Characteristics:**
- Deterministic (same key always same node)
- Consistent hashing
- Rebalancing on topology changes

---

## Slide 8: Backup Strategy

### Ensuring Data Availability

**Backup Configuration:**
```java
CacheConfiguration<K, V> cfg = new CacheConfiguration<>();
cfg.setBackups(1); // One backup copy
```

**Backup Levels:**
- 0 backups: No fault tolerance (fastest)
- 1 backup: Survive 1 node failure (recommended)
- 2 backups: Survive 2 node failures (high availability)
- N backups: Up to N node failures

**Example with 1 Backup:**
```
Primary Data:    Node 1, Node 2, Node 3
Backup Data:     Node 2, Node 3, Node 1

Node 1 fails → Node 2's backup becomes primary
No data loss! ✓
```

**Trade-offs:**
- More backups = better availability
- More backups = more memory used
- More backups = more network traffic

---

## Slide 9: Memory Architecture Overview

### Three-Tier Memory Model

```
┌─────────────────────────────────────┐
│         On-Heap (JVM Heap)          │
│    - Java objects                   │
│    - Fast access                    │
│    - GC overhead                    │
│    - Size limited                   │
└─────────────────────────────────────┘
              ↕ eviction
┌─────────────────────────────────────┐
│        Off-Heap (Native RAM)        │
│    - Binary format                  │
│    - No GC overhead                 │
│    - Large capacity                 │
│    - Serialization cost             │
└─────────────────────────────────────┘
              ↕ checkpointing
┌─────────────────────────────────────┐
│        Disk (Native Persistence)    │
│    - Durable storage                │
│    - Unlimited capacity             │
│    - Slowest access                 │
│    - Write-Ahead Log                │
└─────────────────────────────────────┘
```

---

## Slide 10: On-Heap vs Off-Heap

### Memory Comparison

**On-Heap Memory:**
- ✅ Fast access (Java objects)
- ✅ No serialization needed
- ❌ GC pauses
- ❌ Size limitations (heap size)
- ❌ Memory fragmentation

**Off-Heap Memory:**
- ✅ No GC pauses
- ✅ Large datasets (100s of GB)
- ✅ Efficient memory use
- ❌ Serialization/deserialization overhead
- ❌ Manual memory management

**Recommended Approach:**
```java
DataRegionConfiguration region = new DataRegionConfiguration();
region.setMaxSize(10L * 1024 * 1024 * 1024); // 10 GB off-heap
region.setPersistenceEnabled(false); // In-memory only
```

---

## Slide 11: Data Regions

### Configuring Memory Regions

**What are Data Regions?**
Logical memory areas with specific characteristics.

**Configuration Example:**
```java
DataStorageConfiguration storageCfg = new DataStorageConfiguration();

// Default region - in-memory
DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
defaultRegion.setName("Default_Region");
defaultRegion.setMaxSize(4L * 1024 * 1024 * 1024); // 4 GB

// Persistent region
DataRegionConfiguration persistRegion = new DataRegionConfiguration();
persistRegion.setName("Persistent_Region");
persistRegion.setMaxSize(10L * 1024 * 1024 * 1024); // 10 GB
persistRegion.setPersistenceEnabled(true);

storageCfg.setDefaultDataRegionConfiguration(defaultRegion);
storageCfg.setDataRegionConfigurations(persistRegion);
```

**Use Cases:**
- Hot data: In-memory region
- Cold data: Persistent region
- Temporary data: Smaller region with eviction

---

## Slide 12: Native Persistence

### Durability Without External Database

**What is Native Persistence?**
Ignite's own disk storage mechanism.

**Features:**
- Write-Ahead Log (WAL) for durability
- Checkpointing for crash recovery
- Page-based storage
- Instant cluster restarts
- No external database needed

**Architecture:**
```
┌─────────────────────────────────────┐
│         RAM (Active Data)           │
└─────────────────────────────────────┘
          ↕ (async writes)
┌─────────────────────────────────────┐
│     WAL (Write-Ahead Log)           │
│  - All changes logged first         │
│  - Sequential writes (fast)         │
└─────────────────────────────────────┘
          ↕ (checkpointing)
┌─────────────────────────────────────┐
│     Disk Storage (Data Pages)       │
│  - Permanent storage                │
│  - Random access structure          │
└─────────────────────────────────────┘
```

---

## Slide 13: WAL (Write-Ahead Log)

### Ensuring Durability

**How WAL Works:**
1. Change made in memory
2. Change written to WAL (append-only)
3. WAL fsync'd to disk
4. Acknowledgment returned
5. Eventually checkpointed to data files

**WAL Modes:**
```java
// LOG_ONLY - best performance
storageCfg.setWalMode(WALMode.LOG_ONLY);

// FSYNC - maximum durability
storageCfg.setWalMode(WALMode.FSYNC);

// BACKGROUND - balanced
storageCfg.setWalMode(WALMode.BACKGROUND);
```

**Trade-offs:**
- LOG_ONLY: Fastest, slight risk of data loss
- FSYNC: Safest, slower writes
- BACKGROUND: Middle ground

---

## Slide 14: Discovery Mechanisms

### How Nodes Find Each Other

**Three Main Discovery Methods:**

**1. Multicast Discovery (Default)**
```java
// Automatic, zero configuration
TcpDiscoverySpi spi = new TcpDiscoverySpi();
// Uses IP multicast for node discovery
```

**2. Static IP Discovery**
```java
TcpDiscoverySpi spi = new TcpDiscoverySpi();
TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
ipFinder.setAddresses(Arrays.asList(
    "192.168.1.10:47500",
    "192.168.1.11:47500",
    "192.168.1.12:47500"
));
spi.setIpFinder(ipFinder);
```

**3. Cloud/Kubernetes Discovery**
```java
// Kubernetes IP Finder
TcpDiscoveryKubernetesIpFinder ipFinder =
    new TcpDiscoveryKubernetesIpFinder();
ipFinder.setNamespace("ignite");
ipFinder.setServiceName("ignite-service");
```

---

## Slide 15: Discovery Process Flow

### Node Join Sequence

```
Step 1: New Node Starts
   ↓
Step 2: Discovery SPI Activated
   ↓
Step 3: Sends Discovery Request
   ↓
Step 4: Coordinator Responds
   ↓
Step 5: Topology Exchange
   ↓
Step 6: Node Added to Cluster
   ↓
Step 7: Rebalancing (if needed)
   ↓
Step 8: Node Fully Joined
```

**Key Concepts:**
- Coordinator: Oldest node in cluster
- Topology Version: Increments on changes
- Exchange: Synchronization of node info
- Ring Topology: Nodes form logical ring

---

## Slide 16: Discovery Port Configuration

### Network Settings

**Default Ports:**
- Discovery: 47500-47509
- Communication: 47100-47199
- Thin clients: 10800
- REST API: 8080

**Configuration:**
```java
TcpDiscoverySpi spi = new TcpDiscoverySpi();
spi.setLocalPort(47500);
spi.setLocalPortRange(10); // 47500-47509

cfg.setDiscoverySpi(spi);
```

**Firewall Requirements:**
- Open discovery port range
- Open communication port range
- Allow both TCP and UDP
- Consider cloud security groups

---

## Slide 17: Baseline Topology

### Stable Server Node Configuration

**What is Baseline Topology?**
A set of server nodes that store data in a persistent cluster.

**Why Baseline?**
- Defines which nodes are data nodes
- Ensures data consistency
- Controls rebalancing
- Required for persistent clusters

**Visual:**
```
All Nodes:     [Node1] [Node2] [Node3] [Client1] [Client2]
Baseline:      [Node1] [Node2] [Node3]
                 ↑        ↑        ↑
           (Only these store data)
```

**Key Points:**
- New nodes don't automatically join baseline
- Must explicitly add nodes to baseline
- Prevents unexpected rebalancing
- Controls data distribution

---

## Slide 18: Cluster Activation

### Starting a Persistent Cluster

**Cluster States:**
- INACTIVE: Cluster started but not ready
- ACTIVE: Cluster ready for operations
- READ_ONLY: Can read but not write

**Activation Process:**
```java
// Check state
ClusterState state = ignite.cluster().state();

// Activate cluster
ignite.cluster().state(ClusterState.ACTIVE);

// Set baseline (first time)
ignite.cluster().setBaselineTopology(
    ignite.cluster().topologyVersion()
);
```

**Command Line:**
```bash
# Activate cluster
bin/ignite.sh --activate

# Set baseline
bin/ignite.sh --baseline add <nodeId>

# View baseline
bin/ignite.sh --baseline
```

---

## Slide 19: Baseline Topology Management

### Adding/Removing Nodes

**Adding Node to Baseline:**
```bash
# Start new node (joins cluster but not baseline)
bin/ignite.sh

# Add to baseline
bin/ignite.sh --baseline add <consistentId>
```

**Removing Node from Baseline:**
```bash
# Stop node gracefully
kill -SIGTERM <pid>

# Remove from baseline
bin/ignite.sh --baseline remove <consistentId>
```

**Auto-Adjust (Ignite 2.4+):**
```java
IgniteConfiguration cfg = new IgniteConfiguration();
cfg.setDataStorageConfiguration(
    new DataStorageConfiguration()
        .setBaselineAutoAdjustEnabled(true)
        .setBaselineAutoAdjustTimeout(30000) // 30 seconds
);
```

**Best Practices:**
- Manually manage in production
- Use auto-adjust cautiously
- Always backup before topology changes

---

## Slide 20: Communication SPI

### Node-to-Node Communication

**Purpose:**
Handles all inter-node communication (data, messages).

**Configuration:**
```java
TcpCommunicationSpi commSpi = new TcpCommunicationSpi();
commSpi.setLocalPort(47100);
commSpi.setLocalPortRange(100); // 47100-47199

// Connection pooling
commSpi.setConnectionsPerNode(1);

// Message queue size
commSpi.setMessageQueueLimit(1024);

cfg.setCommunicationSpi(commSpi);
```

**Tuning Parameters:**
- Connections per node: More = higher throughput
- Message queue: Larger = handle bursts better
- Socket buffer sizes: Match network capacity

---

## Slide 21: Cluster Membership

### Node Lifecycle

**Node States:**

```
START → JOINING → JOINED → ACTIVE → LEAVING → LEFT

START:    Node process started
JOINING:  Discovery in progress
JOINED:   Part of topology
ACTIVE:   Fully operational
LEAVING:  Graceful shutdown
LEFT:     No longer in cluster
```

**Segmentation:**
When network split causes multiple sub-clusters.

**Split-Brain Prevention:**
- Segmentation resolvers
- Majority-based decisions
- Network partition detection

---

## Slide 22: Rebalancing

### Data Redistribution

**When Rebalancing Occurs:**
- Node joins cluster
- Node leaves cluster
- Baseline topology changes
- Cache configuration changes

**Rebalancing Process:**
```
1. Detect topology change
   ↓
2. Calculate new partition mapping
   ↓
3. Transfer partitions to new locations
   ↓
4. Update partition counters
   ↓
5. Verify data consistency
```

**Configuration:**
```java
CacheConfiguration cfg = new CacheConfiguration();

// Rebalancing mode
cfg.setRebalanceMode(CacheRebalanceMode.ASYNC);

// Batch size
cfg.setRebalanceBatchSize(512 * 1024); // 512 KB

// Thread pool size
cfg.setRebalanceThreadPoolSize(2);

// Throttle
cfg.setRebalanceThrottle(100); // 100ms pause
```

---

## Slide 23: Affinity Function

### Controlling Data Placement

**Default Affinity:**
Rendezvous Affinity (consistent hashing)

```java
// Customize partitions
AffinityFunction aff = new RendezvousAffinityFunction(
    false,  // exclude neighbors
    2048    // number of partitions
);
cfg.setAffinity(aff);
```

**Affinity Key:**
Control which node stores related data.

```java
public class Order {
    private int orderId;

    @AffinityKeyMapped
    private int customerId; // Orders colocated with customer
}
```

**Benefits:**
- Colocation of related data
- Reduced network hops
- Faster joins and computations

---

## Slide 24: Topology Events

### Monitoring Cluster Changes

**Event Types:**
- Node joined
- Node left
- Node failed
- Node segmented

**Listening for Events:**
```java
ignite.events().localListen(event -> {
    if (event instanceof DiscoveryEvent) {
        DiscoveryEvent de = (DiscoveryEvent) event;

        System.out.println("Event: " + de.name());
        System.out.println("Node: " + de.eventNode().id());
    }
    return true;
}, EventType.EVT_NODE_JOINED, EventType.EVT_NODE_LEFT);
```

**Use Cases:**
- Monitoring
- Alerting
- Custom rebalancing logic
- Application coordination

---

## Slide 25: Architecture Best Practices

### Design Recommendations

**1. Cluster Sizing:**
- Start with 3-5 server nodes
- Use odd numbers (3, 5, 7) for quorum
- Plan for growth

**2. Memory Allocation:**
- 60-70% of RAM to Ignite
- Leave room for OS and other processes
- Use off-heap for large datasets

**3. Network:**
- Low-latency network critical
- 10 Gbps recommended for large clusters
- Minimize network hops

**4. Backups:**
- 1 backup for most cases
- 2 backups for critical data
- Consider trade-offs

**5. Persistence:**
- Use native persistence for durability
- Configure WAL mode appropriately
- Plan for storage capacity

---

## Slide 26: Common Architecture Patterns

### Deployment Topologies

**1. All-in-One:**
```
[Application + Ignite Server Node]
```
- Simplest deployment
- Good for small apps
- Tight coupling

**2. Separate Data Tier:**
```
[Application (Client)] → [Ignite Cluster (Servers)]
```
- Better separation
- Independent scaling
- Production recommended

**3. Multi-Datacenter:**
```
[DC1: Ignite Cluster] ←→ [DC2: Ignite Cluster]
```
- Geographic distribution
- Disaster recovery
- Application-level replication

---

## Slide 27: Monitoring the Cluster

### Key Metrics to Watch

**Topology Metrics:**
- Number of nodes
- Baseline topology
- Coordinator node
- Topology version

**Memory Metrics:**
- Heap usage
- Off-heap usage
- Data region utilization
- Page replacement rate

**Performance Metrics:**
- Cache hit ratio
- Query execution times
- Rebalancing progress
- Network throughput

**Tools:**
- JMX/MBeans
- Ignite Web Console
- Prometheus + Grafana
- Custom monitoring

---

## Slide 28: Troubleshooting Architecture Issues

### Common Problems

**Problem 1: Nodes Not Discovering**
- ✓ Check network connectivity
- ✓ Verify discovery configuration
- ✓ Check firewall rules
- ✓ Ensure port availability

**Problem 2: Segmentation**
- ✓ Network stability
- ✓ Segmentation resolver config
- ✓ Failure detection timeout

**Problem 3: Slow Rebalancing**
- ✓ Increase rebalance threads
- ✓ Adjust batch sizes
- ✓ Check network bandwidth
- ✓ Review throttling settings

**Problem 4: Memory Issues**
- ✓ Increase data region size
- ✓ Enable eviction
- ✓ Review data model
- ✓ Check for memory leaks

---

## Slide 29: Key Takeaways

### Remember These Points

1. **Cluster is peer-to-peer**
   - All nodes equal (no master)
   - Self-healing and elastic

2. **Data is partitioned and distributed**
   - Consistent hashing
   - Automatic rebalancing

3. **Three-tier memory model**
   - On-heap, off-heap, disk
   - Each has trade-offs

4. **Baseline topology for persistence**
   - Controls which nodes store data
   - Manual management recommended

5. **Discovery is critical**
   - Must be configured correctly
   - Different options for different environments

---

## Slide 30: Questions?

### Discussion Topics

- Cluster sizing for your use case
- Memory configuration strategies
- Discovery mechanism selection
- Persistence requirements

**Next:** Lab 2 - Multi-Node Cluster Setup

---

## Instructor Notes

### Timing Guide
- Slides 1-10: 20 minutes (topology and nodes)
- Slides 11-15: 15 minutes (memory and discovery)
- Slides 16-25: 20 minutes (baseline, rebalancing, best practices)
- Slides 26-30: 5 minutes (wrap-up and Q&A)

### Key Points to Emphasize
1. Difference between server and client nodes (Slide 4)
2. Three-tier memory model (Slide 9)
3. Baseline topology concept (Slide 17)
4. Rebalancing process (Slide 22)

### Demo Opportunities
- Start multi-node cluster
- Show topology view
- Demonstrate node join/leave
- Show rebalancing in action

### Common Questions
1. "How many nodes do we need?"
2. "What happens when a node fails?"
3. "How much memory should we allocate?"
4. "Do we need persistence?"
5. "How long does rebalancing take?"
