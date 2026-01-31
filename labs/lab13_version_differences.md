# Lab 13: Apache Ignite 3.x -- Architecture, APIs, and Migration from 2.x

## Duration: 120-150 minutes

## Objectives
- Set up and connect to an Apache Ignite 3.x cluster using the thin client API
- Explore the SWIM discovery protocol and RAFT-based cluster lifecycle
- Configure Ignite 3.x using HOCON and the CLI (dynamic configuration)
- Use the Table API: RecordView with Tuples, RecordView with POJOs, and KeyValueView
- Execute SQL as a first-class operation and combine SQL with KV in transactions
- Run colocated compute jobs using JobTarget and JobDescriptor
- Create Distribution Zones with COLOCATE BY for data affinity
- Monitor the cluster via REST API, CLI, and system views
- Build a comprehensive comparison matrix between Ignite 2.x and 3.x

## Prerequisites

### Software Requirements
- **Java 11+** (Ignite 3.x minimum; Java 17 recommended)
- **Maven 3.6+**
- **Docker** and **Docker Compose** installed and running
- **curl** (for REST API exercises)
- Internet connection for pulling Docker images and Maven dependencies

### Start the Ignite 3.x Cluster

Pull and start a three-node Ignite 3.x cluster:

```bash
# Pull the image
docker pull apacheignite/ignite:3.1.0

# Start node 1
docker run -d --name ignite3-node1 \
  -p 10300:10300 \
  -p 10800:10800 \
  -p 3344:3344 \
  apacheignite/ignite:3.1.0

# Wait for startup
sleep 20

# Initialize the cluster (one-time operation)
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 cluster init \
  --name=lab13cluster \
  --meta-storage-node=defaultNode

# Verify the cluster is running
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 cluster state
```

**Expected Output:**
```
Cluster was initialized successfully
```

### Maven Project Setup

Create a project directory and `pom.xml`:

```bash
mkdir -p lab13-ignite3/src/main/java/com/example/ignite3
cd lab13-ignite3
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.ignite3</groupId>
    <artifactId>lab13-ignite3</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <ignite3.version>3.1.0</ignite3.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.apache.ignite</groupId>
            <artifactId>ignite-client</artifactId>
            <version>${ignite3.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Part 1: Discovery and Cluster Lifecycle (15 minutes)

### Exercise 1: Explore the SWIM Protocol and RAFT Consensus

**Objective:** Understand how Ignite 3.x nodes discover each other using the SWIM gossip protocol and how RAFT manages cluster metadata.

**Step 1:** Inspect the running node and its network configuration.

```bash
# View the network section of the cluster configuration
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 cluster config show \
  --selector=network

# View physical topology -- lists every node in the cluster
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 cluster topology physical

# View logical topology -- lists nodes that have joined the logical cluster
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 cluster topology logical
```

**Expected Output (physical topology):**
```
╔══════════╤══════════════════════════════════════╤══════════╤═══════╗
║ name     │ id                                   │ host     │ port  ║
╠══════════╪══════════════════════════════════════╪══════════╪═══════╣
║ defaultN │ xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx │ hostname │ 3344  ║
╚══════════╧══════════════════════════════════════╧══════════╧═══════╝
```

**Step 2:** Query the cluster state via the REST API.

```bash
# Cluster state via REST (port 10300)
curl -s http://localhost:10300/management/v1/cluster/state | python3 -m json.tool
```

**Expected Output:**
```json
{
    "cmgNodes": ["defaultNode"],
    "msNodes": ["defaultNode"],
    "clusterTag": {
        "clusterName": "lab13cluster",
        "clusterId": "..."
    }
}
```

**Step 3:** Examine how SWIM differs from the Ignite 2.x ring topology.

```bash
echo "=== Ignite 3.x Discovery Summary ==="
echo "Protocol:            SWIM gossip (Scalable Weakly-consistent Infection-style Membership)"
echo "Inter-node port:     3344 (single port for discovery AND communication)"
echo "Client port:         10800"
echo "REST port:           10300"
echo "Failure detection:   O(log n) convergence via randomised probing"
echo ""
echo "=== vs Ignite 2.x ==="
echo "2.x Protocol:        Ring-based TcpDiscoverySpi"
echo "2.x Discovery port:  47500"
echo "2.x Comm port:       47100"
echo "2.x Failure detect:  O(n) -- message must traverse the ring"
```

**Key Takeaway (vs 2.x):** Ignite 2.x uses a ring topology where failure detection is O(n). Ignite 3.x uses SWIM gossip, which converges in O(log n), making it significantly more scalable for large clusters.

---

### Exercise 2: Cluster Initialization and Client Connection

**Objective:** Connect a Java thin client to the running Ignite 3.x cluster and inspect cluster metadata.

**Step 1:** Create `Ex02_ClusterConnection.java`:

```java
package com.example.ignite3;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;

public class Ex02_ClusterConnection {

    public static void main(String[] args) {
        System.out.println("=== Exercise 2: Connecting to Ignite 3.x ===\n");

        // IgniteClient is the single entry point -- no thick/thin distinction
        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            System.out.println("Connected to Ignite 3.x cluster.");

            // Query cluster nodes through the system view
            System.out.println("\nCluster nodes (from SYSTEM.NODES view):");
            try (ResultSet<SqlRow> rs = client.sql().execute(
                    null, "SELECT * FROM SYSTEM.NODES")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.println("  Node: " + row.stringValue("NAME")
                        + "  ID: " + row.stringValue("ID"));
                }
            }

            // List existing tables
            System.out.println("\nExisting tables (from SYSTEM.TABLES view):");
            try (ResultSet<SqlRow> rs = client.sql().execute(
                    null, "SELECT * FROM SYSTEM.TABLES")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.println("  " + row.stringValue("SCHEMA_NAME")
                        + "." + row.stringValue("NAME"));
                }
            }

            System.out.println("\nConnection test complete.");

        } catch (Exception e) {
            System.err.println("Failed to connect: " + e.getMessage());
            System.err.println("Ensure the cluster is running:");
            System.err.println("  docker exec ignite3-node1 /opt/ignite/bin/ignite3 cluster state");
        }
    }
}
```

**Step 2:** Compile and run:

```bash
cd lab13-ignite3
mvn compile exec:java -Dexec.mainClass="com.example.ignite3.Ex02_ClusterConnection"
```

**Expected Output:**
```
=== Exercise 2: Connecting to Ignite 3.x ===

Connected to Ignite 3.x cluster.

Cluster nodes (from SYSTEM.NODES view):
  Node: defaultNode  ID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx

Existing tables (from SYSTEM.TABLES view):
  (empty if no tables created yet)

Connection test complete.
```

**Key Takeaway (vs 2.x):** In Ignite 2.x you had to choose between a thick client (`Ignition.start()`, which joins the cluster ring) and a thin client (`Ignition.startClient()`). In Ignite 3.x there is a single `IgniteClient` that always connects via the client protocol on port 10800. Clients never join the cluster topology.

---

### Q&A -- Part 1

**Q1:** Why does Ignite 3.x require an explicit `cluster init` command instead of auto-forming a cluster when nodes discover each other?

**A1:** Explicit initialization prevents accidental cluster formation. In 2.x, any nodes on the same network segment using multicast could silently merge into one cluster. In 3.x, `cluster init` designates which nodes hold the metastorage (RAFT group for schema and configuration), giving operators full control over cluster membership from the start.

**Q2:** What happens if the metastorage RAFT leader fails?

**A2:** RAFT automatically elects a new leader from the remaining metastorage replicas. As long as a majority of metastorage nodes are alive, the cluster continues to operate. This is why production deployments should use an odd number of metastorage nodes (3 or 5).

---

## Part 2: Configuration (15 minutes)

### Exercise 3: HOCON Configuration and Dynamic CLI Updates

**Objective:** Explore the HOCON configuration format used by Ignite 3.x and make dynamic configuration changes without restarting the cluster.

**Step 1:** View the full cluster configuration.

```bash
# Full cluster-level configuration
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 cluster config show

# View just the storage section
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 cluster config show \
  --selector=storage

# View just the client connector section
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 node config show \
  --selector=clientConnector
```

**Step 2:** Make a dynamic configuration change.

```bash
# Update garbage collection low watermark (no restart required)
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 cluster config update \
  "gc.lowWatermark=0.3"

# Verify the change
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 cluster config show \
  --selector=gc
```

**Key Takeaway (vs 2.x):** Ignite 2.x configuration was set at startup via Spring XML or Java code. Most changes required a full cluster restart. Ignite 3.x uses HOCON and allows many configuration values to be changed dynamically via the CLI or REST API.

---

### Exercise 4: Explore Node-Level vs Cluster-Level Configuration

**Objective:** Understand the two tiers of configuration in Ignite 3.x.

```bash
echo "=== Cluster-level config ==="
echo "Applies to ALL nodes. Changed via 'cluster config update'."
echo "Examples: storage profiles, distribution zones, GC settings"
echo ""

# Show cluster-level storage config
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 cluster config show \
  --selector=storage

echo ""
echo "=== Node-level config ==="
echo "Applies to ONE node. Changed via 'node config update'."
echo "Examples: network port, REST port, client connector port"
echo ""

# Show node-level network config
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 node config show \
  --selector=network
```

**Key Takeaway (vs 2.x):** Ignite 2.x had a single `IgniteConfiguration` per node. Ignite 3.x separates cluster-wide settings (replicated via RAFT) from node-local settings.

---

### Exercise 5: Create Distribution Zones

**Objective:** Define distribution zones that control replica count, partition count, and storage profile for groups of tables.

**Step 1:** Create zones via the CLI.

```bash
# Create a "hot" zone -- higher replication for frequently accessed data
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 sql \
  "CREATE ZONE IF NOT EXISTS hot_zone WITH replicas=3, partitions=256, storage_profiles='default'"

# Create a "cold" zone -- lower replication for archival data
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 sql \
  "CREATE ZONE IF NOT EXISTS cold_zone WITH replicas=1, partitions=64, storage_profiles='default'"

# Verify zones via system view
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 sql \
  "SELECT * FROM SYSTEM.ZONES"
```

**Expected Output:**
```
╔═══════════╤══════════╤════════════╤═══════════════════╗
║ NAME      │ REPLICAS │ PARTITIONS │ STORAGE_PROFILES  ║
╠═══════════╪══════════╪════════════╪═══════════════════╣
║ hot_zone  │ 3        │ 256        │ default           ║
║ cold_zone │ 1        │ 64         │ default           ║
╚═══════════╧══════════╧════════════╧═══════════════════╝
```

**Step 2:** Query zones from Java.

```java
package com.example.ignite3;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;

public class Ex05_DistributionZones {

    public static void main(String[] args) {
        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            System.out.println("=== Exercise 5: Distribution Zones ===\n");

            // Create zones via SQL from the client
            client.sql().executeScript(
                "CREATE ZONE IF NOT EXISTS app_zone " +
                "WITH replicas=2, partitions=128, storage_profiles='default'");

            // List all zones
            System.out.println("Distribution Zones:");
            try (ResultSet<SqlRow> rs = client.sql().execute(
                    null, "SELECT * FROM SYSTEM.ZONES")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("  Zone: %-15s  Replicas: %d  Partitions: %d%n",
                        row.stringValue("NAME"),
                        row.intValue("REPLICAS"),
                        row.intValue("PARTITIONS"));
                }
            }
        }
    }
}
```

**Key Takeaway (vs 2.x):** In Ignite 2.x, backup count and partition count were set per cache in `CacheConfiguration`. In 3.x, Distribution Zones decouple placement policy from table definitions, allowing multiple tables to share the same zone configuration.

---

### Q&A -- Part 2

**Q1:** Can you change the number of replicas in a zone after tables have been created in it?

**A1:** Yes. You can `ALTER ZONE` to change replicas or partitions. Ignite will rebalance data automatically. This was much harder in 2.x, where changing backup count required cache recreation.

**Q2:** What are storage profiles and how do they relate to zones?

**A2:** A storage profile maps to a storage engine (aimem for volatile in-memory, aipersist for persistent page memory, or rocksdb for LSM-tree disk storage). When you create a zone, you specify which storage profiles are available. Tables in that zone use the specified engine. In 2.x, all caches used the same page memory engine.

**Q3:** Why is HOCON used instead of Spring XML?

**A3:** HOCON is more concise, supports comments, variable substitution, and hierarchical merging. It decouples Ignite configuration from the Spring framework, making Ignite usable without Spring dependencies.

---

## Part 3: Table API (25 minutes)

### Exercise 6: RecordView with Tuples

**Objective:** Use the `RecordView<Tuple>` API for schema-flexible data access.

Create `Ex06_RecordViewTuples.java`:

```java
package com.example.ignite3;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;

import java.util.List;

public class Ex06_RecordViewTuples {

    public static void main(String[] args) {
        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            System.out.println("=== Exercise 6: RecordView with Tuples ===\n");

            // Step 1: Create table via SQL DDL (schema-first approach)
            client.sql().executeScript(
                "CREATE TABLE IF NOT EXISTS Person (" +
                "    personId INT PRIMARY KEY," +
                "    firstName VARCHAR(100)," +
                "    lastName  VARCHAR(100)," +
                "    age       INT" +
                ")");
            System.out.println("Table 'Person' created.\n");

            // Step 2: Obtain a RecordView<Tuple> -- the most flexible view
            Table personTable = client.tables().table("PERSON");
            RecordView<Tuple> view = personTable.recordView();

            // Step 3: INSERT using upsert (insert or update)
            System.out.println("--- Inserting records ---");
            view.upsert(null, Tuple.create()
                .set("personId", 1)
                .set("firstName", "Alice")
                .set("lastName", "Morgan")
                .set("age", 32));

            view.upsert(null, Tuple.create()
                .set("personId", 2)
                .set("firstName", "Bob")
                .set("lastName", "Chen")
                .set("age", 27));

            view.upsert(null, Tuple.create()
                .set("personId", 3)
                .set("firstName", "Carol")
                .set("lastName", "Diaz")
                .set("age", 45));
            System.out.println("Inserted 3 records.\n");

            // Step 4: READ using get -- pass a key-only Tuple
            System.out.println("--- Reading record ---");
            Tuple key = Tuple.create().set("personId", 1);
            Tuple record = view.get(null, key);
            if (record != null) {
                System.out.printf("  personId=%d  name=%s %s  age=%d%n",
                    record.intValue("personId"),
                    record.stringValue("firstName"),
                    record.stringValue("lastName"),
                    record.intValue("age"));
            }

            // Step 5: BATCH READ using getAll
            System.out.println("\n--- Batch read ---");
            List<Tuple> keys = List.of(
                Tuple.create().set("personId", 1),
                Tuple.create().set("personId", 2),
                Tuple.create().set("personId", 3)
            );
            List<Tuple> records = view.getAll(null, keys);
            for (Tuple t : records) {
                if (t != null) {
                    System.out.printf("  personId=%d  %s %s%n",
                        t.intValue("personId"),
                        t.stringValue("firstName"),
                        t.stringValue("lastName"));
                }
            }

            // Step 6: UPDATE -- upsert with new values
            System.out.println("\n--- Updating record ---");
            view.upsert(null, Tuple.create()
                .set("personId", 1)
                .set("firstName", "Alice")
                .set("lastName", "Morgan-Smith")
                .set("age", 33));
            Tuple updated = view.get(null, key);
            System.out.printf("  Updated: %s, age %d%n",
                updated.stringValue("lastName"),
                updated.intValue("age"));

            // Step 7: DELETE
            System.out.println("\n--- Deleting record ---");
            boolean deleted = view.delete(null, Tuple.create().set("personId", 3));
            System.out.println("  Deleted personId=3: " + deleted);

            // Step 8: Verify via SQL
            System.out.println("\n--- Verify via SQL ---");
            try (ResultSet<SqlRow> rs = client.sql().execute(
                    null, "SELECT personId, firstName, lastName, age FROM Person ORDER BY personId")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("  [%d] %s %s, age %d%n",
                        row.intValue("personId"),
                        row.stringValue("firstName"),
                        row.stringValue("lastName"),
                        row.intValue("age"));
                }
            }

            System.out.println("\nExercise 6 complete.");
        }
    }
}
```

**Expected Output:**
```
Table 'Person' created.

--- Inserting records ---
Inserted 3 records.

--- Reading record ---
  personId=1  name=Alice Morgan  age=32

--- Batch read ---
  personId=1  Alice Morgan
  personId=2  Bob Chen
  personId=3  Carol Diaz

--- Updating record ---
  Updated: Morgan-Smith, age 33

--- Deleting record ---
  Deleted personId=3: true

--- Verify via SQL ---
  [1] Alice Morgan-Smith, age 33
  [2] Bob Chen, age 27
```

**Key Takeaway (vs 2.x):** In 2.x, you used `cache.put(key, value)` and `cache.get(key)`. In 3.x, `RecordView<Tuple>` provides `upsert`, `get`, `delete`, and `getAll` on Tuple objects. The schema is defined via DDL, not Java annotations.

---

### Exercise 7: RecordView with POJOs

**Objective:** Map table rows directly to Java objects using `RecordView<MyClass>`.

**Step 1:** Create the POJO class `PersonRecord.java`:

```java
package com.example.ignite3;

public class PersonRecord {
    int personId;
    String firstName;
    String lastName;
    int age;

    // Default constructor required
    public PersonRecord() {}

    public PersonRecord(int personId, String firstName, String lastName, int age) {
        this.personId = personId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
    }

    @Override
    public String toString() {
        return String.format("PersonRecord[id=%d, %s %s, age=%d]",
            personId, firstName, lastName, age);
    }
}
```

**Step 2:** Create `Ex07_RecordViewPojo.java`:

```java
package com.example.ignite3;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;

public class Ex07_RecordViewPojo {

    public static void main(String[] args) {
        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            System.out.println("=== Exercise 7: RecordView with POJOs ===\n");

            // Reuse the Person table from Exercise 6
            client.sql().executeScript(
                "CREATE TABLE IF NOT EXISTS Person (" +
                "    personId INT PRIMARY KEY," +
                "    firstName VARCHAR(100)," +
                "    lastName  VARCHAR(100)," +
                "    age       INT" +
                ")");

            Table personTable = client.tables().table("PERSON");

            // Get a typed RecordView
            RecordView<PersonRecord> view = personTable.recordView(PersonRecord.class);

            // INSERT using POJO
            System.out.println("--- Insert via POJO ---");
            view.upsert(null, new PersonRecord(10, "David", "Lee", 29));
            view.upsert(null, new PersonRecord(11, "Eva", "Park", 38));
            System.out.println("Inserted 2 records.\n");

            // READ using POJO key
            System.out.println("--- Read via POJO ---");
            PersonRecord keyObj = new PersonRecord();
            keyObj.personId = 10;

            PersonRecord result = view.get(null, keyObj);
            System.out.println("  " + result);

            // DELETE using POJO key
            System.out.println("\n--- Delete via POJO ---");
            PersonRecord deleteKey = new PersonRecord();
            deleteKey.personId = 11;
            boolean deleted = view.delete(null, deleteKey);
            System.out.println("  Deleted personId=11: " + deleted);

            System.out.println("\nExercise 7 complete.");
        }
    }
}
```

**Expected Output:**
```
--- Insert via POJO ---
Inserted 2 records.

--- Read via POJO ---
  PersonRecord[id=10, David Lee, age=29]

--- Delete via POJO ---
  Deleted personId=11: true
```

**Key Takeaway (vs 2.x):** In 2.x, POJOs needed `@QuerySqlField` annotations and `Serializable`. In 3.x, the POJO just needs field names matching the SQL columns and a default constructor. No annotations or serialization interface required.

---

### Exercise 8: KeyValueView and SQL Queries

**Objective:** Use `KeyValueView<Tuple,Tuple>` for cache-like access and execute SQL queries with parameters.

Create `Ex08_KeyValueAndSQL.java`:

```java
package com.example.ignite3;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;

public class Ex08_KeyValueAndSQL {

    public static void main(String[] args) {
        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            System.out.println("=== Exercise 8: KeyValueView and SQL ===\n");

            // Create a Products table
            client.sql().executeScript(
                "CREATE TABLE IF NOT EXISTS Product (" +
                "    productId INT PRIMARY KEY," +
                "    name      VARCHAR(200)," +
                "    price     DOUBLE," +
                "    category  VARCHAR(100)" +
                ")");

            Table productTable = client.tables().table("PRODUCT");

            // --- KeyValueView: separate key and value Tuples ---
            System.out.println("--- KeyValueView API ---\n");
            KeyValueView<Tuple, Tuple> kv = productTable.keyValueView();

            // Insert via KV
            kv.put(null,
                Tuple.create().set("productId", 101),
                Tuple.create().set("name", "Laptop").set("price", 999.99).set("category", "Electronics"));
            kv.put(null,
                Tuple.create().set("productId", 102),
                Tuple.create().set("name", "Desk Chair").set("price", 249.50).set("category", "Furniture"));
            kv.put(null,
                Tuple.create().set("productId", 103),
                Tuple.create().set("name", "Keyboard").set("price", 79.99).set("category", "Electronics"));
            kv.put(null,
                Tuple.create().set("productId", 104),
                Tuple.create().set("name", "Monitor").set("price", 549.00).set("category", "Electronics"));

            System.out.println("Inserted 4 products via KeyValueView.\n");

            // Read via KV
            Tuple val = kv.get(null, Tuple.create().set("productId", 101));
            if (val != null) {
                System.out.printf("  Product 101: %s, $%.2f%n",
                    val.stringValue("name"), val.doubleValue("price"));
            }

            // Check existence
            boolean exists = kv.contains(null, Tuple.create().set("productId", 999));
            System.out.println("  Product 999 exists: " + exists);

            // --- SQL Queries ---
            System.out.println("\n--- SQL Queries ---\n");

            // Parameterised query
            System.out.println("Electronics products over $100:");
            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT productId, name, price FROM Product " +
                    "WHERE category = ? AND price > ? ORDER BY price DESC",
                    "Electronics", 100.0)) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("  [%d] %-15s $%.2f%n",
                        row.intValue("productId"),
                        row.stringValue("name"),
                        row.doubleValue("price"));
                }
            }

            // Aggregate query
            System.out.println("\nAverage price by category:");
            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT category, COUNT(*) AS cnt, AVG(price) AS avg_price " +
                    "FROM Product GROUP BY category ORDER BY category")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("  %-15s  count=%d  avg=$%.2f%n",
                        row.stringValue("category"),
                        row.longValue("cnt"),
                        row.doubleValue("avg_price"));
                }
            }

            System.out.println("\nExercise 8 complete.");
        }
    }
}
```

**Expected Output:**
```
--- KeyValueView API ---

Inserted 4 products via KeyValueView.

  Product 101: Laptop, $999.99
  Product 999 exists: false

--- SQL Queries ---

Electronics products over $100:
  [101] Laptop          $999.99
  [104] Monitor         $549.00

Average price by category:
  Electronics      count=3  avg=$542.99
  Furniture        count=1  avg=$249.50
```

**Key Takeaway (vs 2.x):** In 2.x, SQL was an overlay on the cache executed via `cache.query(new SqlFieldsQuery(...))`. In 3.x, `client.sql().execute()` is a standalone first-class API. The same data is accessible through RecordView, KeyValueView, or SQL interchangeably.

---

### Q&A -- Part 3

**Q1:** When should you use RecordView vs KeyValueView vs SQL?

**A1:** Use `RecordView<Tuple>` for generic CRUD when you want to treat rows as single objects. Use `RecordView<POJO>` when you have a Java class matching the schema. Use `KeyValueView` when you think in key-value terms (closest to 2.x `IgniteCache`). Use SQL for queries, joins, aggregations, and DDL. All views operate on the same underlying data.

**Q2:** Does `client.tables().table("PERSON")` return null if the table does not exist?

**A2:** Yes. Always check for null or use `CREATE TABLE IF NOT EXISTS` before calling `client.tables().table()`. In 2.x, `ignite.cache("name")` also returned null, but `ignite.getOrCreateCache("name")` would auto-create. There is no equivalent auto-create for tables in 3.x; you must use DDL.

---

## Part 4: Transactions (15 minutes)

### Exercise 9: Manual Transaction Control

**Objective:** Use `client.transactions().begin()` with explicit `commit()` and `rollback()`.

Create `Ex09_Transactions.java`:

```java
package com.example.ignite3;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.Transaction;

public class Ex09_Transactions {

    public static void main(String[] args) {
        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            System.out.println("=== Exercise 9: Manual Transactions ===\n");

            // Setup
            client.sql().executeScript(
                "CREATE TABLE IF NOT EXISTS Account (" +
                "    accountId INT PRIMARY KEY," +
                "    owner     VARCHAR(100)," +
                "    balance   INT" +
                ")");

            Table accountTable = client.tables().table("ACCOUNT");
            KeyValueView<Tuple, Tuple> kv = accountTable.keyValueView();

            // Initialise accounts outside a transaction
            kv.put(null,
                Tuple.create().set("accountId", 1),
                Tuple.create().set("owner", "Alice").set("balance", 1000));
            kv.put(null,
                Tuple.create().set("accountId", 2),
                Tuple.create().set("owner", "Bob").set("balance", 500));

            System.out.println("Initial balances:");
            printBalances(client);

            // --- Successful transaction ---
            System.out.println("\n--- Transfer $200 from Alice to Bob ---");
            Transaction tx = client.transactions().begin();
            try {
                Tuple aliceVal = kv.get(tx, Tuple.create().set("accountId", 1));
                Tuple bobVal   = kv.get(tx, Tuple.create().set("accountId", 2));

                int aliceBalance = aliceVal.intValue("balance");
                int bobBalance   = bobVal.intValue("balance");

                if (aliceBalance < 200) {
                    tx.rollback();
                    System.out.println("Insufficient funds -- rolled back.");
                    return;
                }

                kv.put(tx,
                    Tuple.create().set("accountId", 1),
                    Tuple.create().set("owner", "Alice").set("balance", aliceBalance - 200));
                kv.put(tx,
                    Tuple.create().set("accountId", 2),
                    Tuple.create().set("owner", "Bob").set("balance", bobBalance + 200));

                tx.commit();
                System.out.println("Transaction committed.\n");
            } catch (Exception e) {
                tx.rollback();
                System.out.println("Transaction rolled back: " + e.getMessage());
            }

            System.out.println("Balances after transfer:");
            printBalances(client);

            // --- Rolled-back transaction ---
            System.out.println("\n--- Attempt invalid transfer (rollback) ---");
            Transaction tx2 = client.transactions().begin();
            try {
                // Deduct from Alice
                Tuple aliceVal = kv.get(tx2, Tuple.create().set("accountId", 1));
                kv.put(tx2,
                    Tuple.create().set("accountId", 1),
                    Tuple.create().set("owner", "Alice")
                        .set("balance", aliceVal.intValue("balance") - 5000));

                // Simulate error
                if (true) throw new RuntimeException("Simulated error");

                tx2.commit();
            } catch (Exception e) {
                tx2.rollback();
                System.out.println("Rolled back: " + e.getMessage());
            }

            System.out.println("\nBalances after rollback (unchanged):");
            printBalances(client);

            System.out.println("\nExercise 9 complete.");
        }
    }

    private static void printBalances(IgniteClient client) {
        try (ResultSet<SqlRow> rs = client.sql().execute(null,
                "SELECT accountId, owner, balance FROM Account ORDER BY accountId")) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                System.out.printf("  Account %d (%s): $%d%n",
                    row.intValue("accountId"),
                    row.stringValue("owner"),
                    row.intValue("balance"));
            }
        }
    }
}
```

**Expected Output:**
```
Initial balances:
  Account 1 (Alice): $1000
  Account 2 (Bob): $500

--- Transfer $200 from Alice to Bob ---
Transaction committed.

Balances after transfer:
  Account 1 (Alice): $800
  Account 2 (Bob): $700

--- Attempt invalid transfer (rollback) ---
Rolled back: Simulated error

Balances after rollback (unchanged):
  Account 1 (Alice): $800
  Account 2 (Bob): $700
```

**Key Takeaway (vs 2.x):** In 2.x, you had to choose `TransactionConcurrency` (PESSIMISTIC/OPTIMISTIC) and `TransactionIsolation` (READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE). In 3.x, all transactions are strictly serializable by default. No concurrency/isolation choices needed.

---

### Exercise 10: runInTransaction and Mixed SQL+KV

**Objective:** Use the `runInTransaction` helper and demonstrate mixing SQL and KV operations in a single transaction -- a capability new to Ignite 3.x.

Create `Ex10_MixedTransaction.java`:

```java
package com.example.ignite3;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.Transaction;

public class Ex10_MixedTransaction {

    public static void main(String[] args) {
        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            System.out.println("=== Exercise 10: Mixed SQL + KV Transaction ===\n");

            // Setup tables
            client.sql().executeScript(
                "CREATE TABLE IF NOT EXISTS Account (" +
                "    accountId INT PRIMARY KEY," +
                "    owner     VARCHAR(100)," +
                "    balance   INT" +
                ");" +
                "CREATE TABLE IF NOT EXISTS AuditLog (" +
                "    logId       INT PRIMARY KEY," +
                "    accountId   INT," +
                "    description VARCHAR(500)," +
                "    amount      INT" +
                ");");

            // Reset data
            client.sql().execute(null, "DELETE FROM Account WHERE accountId IN (1,2)");
            client.sql().execute(null, "DELETE FROM AuditLog WHERE logId >= 0");

            Table accountTable  = client.tables().table("ACCOUNT");
            KeyValueView<Tuple, Tuple> accountKv = accountTable.keyValueView();

            // Seed data via KV
            accountKv.put(null,
                Tuple.create().set("accountId", 1),
                Tuple.create().set("owner", "Alice").set("balance", 1000));
            accountKv.put(null,
                Tuple.create().set("accountId", 2),
                Tuple.create().set("owner", "Bob").set("balance", 500));

            System.out.println("Before transaction:");
            printAccounts(client);

            // --- Mixed SQL + KV in one transaction ---
            System.out.println("\n--- Mixed SQL + KV Transaction ---");
            System.out.println("SQL: UPDATE Account for Alice");
            System.out.println("KV:  put() for Bob");
            System.out.println("SQL: INSERT into AuditLog\n");

            Transaction tx = client.transactions().begin();
            try {
                // SQL operation: debit Alice
                client.sql().execute(tx,
                    "UPDATE Account SET balance = balance - 300 WHERE accountId = 1");

                // KV operation: credit Bob
                Tuple bobVal = accountKv.get(tx, Tuple.create().set("accountId", 2));
                accountKv.put(tx,
                    Tuple.create().set("accountId", 2),
                    Tuple.create().set("owner", "Bob")
                        .set("balance", bobVal.intValue("balance") + 300));

                // SQL operation: audit log
                client.sql().execute(tx,
                    "INSERT INTO AuditLog (logId, accountId, description, amount) " +
                    "VALUES (1, 1, 'Transfer to Bob', -300)");
                client.sql().execute(tx,
                    "INSERT INTO AuditLog (logId, accountId, description, amount) " +
                    "VALUES (2, 2, 'Transfer from Alice', 300)");

                tx.commit();
                System.out.println("Transaction committed.\n");
            } catch (Exception e) {
                tx.rollback();
                System.out.println("Transaction rolled back: " + e.getMessage());
            }

            System.out.println("After transaction:");
            printAccounts(client);

            System.out.println("\nAudit log:");
            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT logId, accountId, description, amount FROM AuditLog ORDER BY logId")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("  [%d] Account %d: %s (%+d)%n",
                        row.intValue("logId"),
                        row.intValue("accountId"),
                        row.stringValue("description"),
                        row.intValue("amount"));
                }
            }

            System.out.println("\nExercise 10 complete.");
        }
    }

    private static void printAccounts(IgniteClient client) {
        try (ResultSet<SqlRow> rs = client.sql().execute(null,
                "SELECT accountId, owner, balance FROM Account ORDER BY accountId")) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                System.out.printf("  Account %d (%s): $%d%n",
                    row.intValue("accountId"),
                    row.stringValue("owner"),
                    row.intValue("balance"));
            }
        }
    }
}
```

**Expected Output:**
```
Before transaction:
  Account 1 (Alice): $1000
  Account 2 (Bob): $500

--- Mixed SQL + KV Transaction ---
SQL: UPDATE Account for Alice
KV:  put() for Bob
SQL: INSERT into AuditLog

Transaction committed.

After transaction:
  Account 1 (Alice): $700
  Account 2 (Bob): $800

Audit log:
  [1] Account 1: Transfer to Bob (-300)
  [2] Account 2: Transfer from Alice (+300)
```

**Key Takeaway (vs 2.x):** Ignite 2.x could not reliably mix SQL DML and KV operations in the same transaction. In 3.x, you can freely combine `client.sql().execute(tx, ...)` with `kv.put(tx, ...)` in a single atomic transaction. This is a major improvement for applications that need both programmatic and SQL access to data.

---

### Q&A -- Part 4

**Q1:** Does Ignite 3.x support read-only transactions?

**A1:** Yes. You can pass `TransactionOptions.builder().readOnly(true).build()` to `client.transactions().begin()`. Read-only transactions allow consistent snapshots without acquiring write locks, which can improve throughput for read-heavy workloads.

**Q2:** What happens if you forget to call `commit()` or `rollback()`?

**A2:** The transaction will eventually time out and be automatically rolled back. However, while it is open it may hold locks, so you should always use try-finally or try-with-resources patterns. In 2.x, the behavior was similar but with configurable concurrency modes that affected lock behavior.

---

## Part 5: Compute Grid (15 minutes)

### Exercise 11: Colocated Compute with JobTarget and JobDescriptor

**Objective:** Execute a compute job on the node that owns specific data using the Ignite 3.x compute API.

Create `Ex11_ComputeGrid.java`:

```java
package com.example.ignite3;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.compute.JobDescriptor;
import org.apache.ignite.compute.JobExecution;
import org.apache.ignite.compute.JobTarget;
import org.apache.ignite.table.Tuple;

import java.util.Set;

public class Ex11_ComputeGrid {

    public static void main(String[] args) throws Exception {
        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            System.out.println("=== Exercise 11: Compute Grid ===\n");

            // Ensure the Person table exists with data
            client.sql().executeScript(
                "CREATE TABLE IF NOT EXISTS Person (" +
                "    personId INT PRIMARY KEY," +
                "    firstName VARCHAR(100)," +
                "    lastName  VARCHAR(100)," +
                "    age       INT" +
                ")");
            client.sql().execute(null,
                "INSERT INTO Person (personId, firstName, lastName, age) " +
                "VALUES (100, 'Test', 'User', 30) " +
                "ON CONFLICT DO NOTHING");

            // --- Execute on any node ---
            System.out.println("--- Execute on any node ---");
            System.out.println("In Ignite 3.x, compute jobs are submitted via:");
            System.out.println("  client.compute().execute(JobTarget, JobDescriptor, args)");
            System.out.println();

            // --- Colocated compute ---
            System.out.println("--- Colocated compute ---");
            System.out.println("JobTarget.colocated() routes the job to the node");
            System.out.println("that owns the specified key in the specified table.");
            System.out.println();
            System.out.println("Example (conceptual):");
            System.out.println("  JobTarget target = JobTarget.colocated(");
            System.out.println("      \"PERSON\",");
            System.out.println("      Tuple.create().set(\"personId\", 100));");
            System.out.println("  JobDescriptor<String, String> descriptor =");
            System.out.println("      JobDescriptor.builder(MyComputeJob.class).build();");
            System.out.println("  String result = client.compute()");
            System.out.println("      .execute(target, descriptor, \"input\");");
            System.out.println();

            // --- Deployment units ---
            System.out.println("--- Deployment Units ---");
            System.out.println("Ignite 3.x replaces peer class loading with deployment units.");
            System.out.println("You package compute job classes into a JAR and deploy via CLI:");
            System.out.println();
            System.out.println("  ignite3 cluster unit deploy --path=my-jobs.jar --id=my-unit:1.0");
            System.out.println();
            System.out.println("Then reference the unit when building the JobDescriptor:");
            System.out.println("  JobDescriptor.builder(\"com.example.MyJob\")");
            System.out.println("      .units(\"my-unit\", \"1.0\")");
            System.out.println("      .build();");

            System.out.println("\n--- Feature Gaps vs 2.x ---");
            System.out.println("Available in 3.x:");
            System.out.println("  + Execute on specific nodes (JobTarget.node())");
            System.out.println("  + Execute on any node (JobTarget.anyNode())");
            System.out.println("  + Colocated compute (JobTarget.colocated())");
            System.out.println("  + Deployment units (replaces peer class loading)");
            System.out.println("  + JobDescriptor with builder pattern");
            System.out.println();
            System.out.println("NOT yet available in 3.x:");
            System.out.println("  - Full MapReduce (ComputeTask / ComputeTaskAdapter)");
            System.out.println("  - Service Grid (cluster singletons, node singletons)");
            System.out.println("  - Broadcast to all nodes");
            System.out.println("  - Failover SPI (automatic job retry)");
            System.out.println("  - Load Balancing SPI");
            System.out.println("  - Java ExecutorService over grid");

            System.out.println("\nExercise 11 complete.");
        }
    }
}
```

**Key Takeaway (vs 2.x):** Ignite 2.x had a rich compute API with `ComputeTask`, `broadcast()`, `affinityRun()`, failover, and load balancing. Ignite 3.x provides a streamlined API centered around `JobTarget` and `JobDescriptor`, with colocated compute and deployment units. Full MapReduce and Service Grid are not yet available.

---

### Exercise 12: Compute Workarounds -- Using SQL Aggregations

**Objective:** Demonstrate how SQL aggregations can replace some compute use cases that previously required MapReduce in 2.x.

Create `Ex12_SQLAsCompute.java`:

```java
package com.example.ignite3;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;

public class Ex12_SQLAsCompute {

    public static void main(String[] args) {
        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            System.out.println("=== Exercise 12: SQL as a Compute Replacement ===\n");

            // Setup: create and populate a Sales table
            client.sql().executeScript(
                "CREATE TABLE IF NOT EXISTS Sales (" +
                "    saleId     INT PRIMARY KEY," +
                "    region     VARCHAR(50)," +
                "    product    VARCHAR(100)," +
                "    amount     DOUBLE," +
                "    quantity   INT" +
                ")");

            // Insert sample data
            String[] inserts = {
                "INSERT INTO Sales VALUES (1,'North','Widget',150.0,10) ON CONFLICT DO NOTHING",
                "INSERT INTO Sales VALUES (2,'South','Widget',200.0,15) ON CONFLICT DO NOTHING",
                "INSERT INTO Sales VALUES (3,'North','Gadget',300.0,5)  ON CONFLICT DO NOTHING",
                "INSERT INTO Sales VALUES (4,'East','Widget',175.0,12)  ON CONFLICT DO NOTHING",
                "INSERT INTO Sales VALUES (5,'South','Gadget',250.0,8)  ON CONFLICT DO NOTHING",
                "INSERT INTO Sales VALUES (6,'East','Gadget',400.0,3)   ON CONFLICT DO NOTHING",
                "INSERT INTO Sales VALUES (7,'North','Widget',125.0,20) ON CONFLICT DO NOTHING",
                "INSERT INTO Sales VALUES (8,'South','Widget',180.0,11) ON CONFLICT DO NOTHING"
            };
            for (String sql : inserts) {
                client.sql().execute(null, sql);
            }

            // --- Aggregation that would have been MapReduce in 2.x ---
            System.out.println("--- Revenue by Region (replaces MapReduce SUM) ---\n");
            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT region, SUM(amount * quantity) AS revenue, " +
                    "COUNT(*) AS transactions " +
                    "FROM Sales GROUP BY region ORDER BY revenue DESC")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("  %-8s  Revenue: $%,.2f  Transactions: %d%n",
                        row.stringValue("region"),
                        row.doubleValue("revenue"),
                        row.longValue("transactions"));
                }
            }

            // --- Top product by quantity ---
            System.out.println("\n--- Top Product by Quantity (replaces custom reduce) ---\n");
            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT product, SUM(quantity) AS total_qty " +
                    "FROM Sales GROUP BY product ORDER BY total_qty DESC")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("  %-10s  Total Qty: %d%n",
                        row.stringValue("product"),
                        row.longValue("total_qty"));
                }
            }

            // --- Cross-region analytics ---
            System.out.println("\n--- Region x Product Breakdown ---\n");
            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT region, product, SUM(amount) AS total_amount " +
                    "FROM Sales GROUP BY region, product " +
                    "ORDER BY region, product")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("  %-8s %-10s $%,.2f%n",
                        row.stringValue("region"),
                        row.stringValue("product"),
                        row.doubleValue("total_amount"));
                }
            }

            System.out.println("\n--- When SQL replaces Compute ---");
            System.out.println("Use SQL aggregations for: SUM, AVG, COUNT, MIN, MAX, GROUP BY");
            System.out.println("Use SQL window functions for: RANK, ROW_NUMBER, running totals");
            System.out.println("Still need compute for: custom algorithms, external I/O, ML inference");

            System.out.println("\nExercise 12 complete.");
        }
    }
}
```

**Key Takeaway (vs 2.x):** Many 2.x MapReduce patterns (sum across nodes, group-by aggregations, top-N) can be replaced with SQL `GROUP BY`, `ORDER BY`, and window functions in 3.x. SQL execution in 3.x is distributed and pushes computation to the data nodes automatically.

---

### Q&A -- Part 5

**Q1:** How do deployment units differ from peer class loading in 2.x?

**A1:** Peer class loading in 2.x automatically transferred bytecode from the originating node to executing nodes. It was convenient but caused class-loader leaks and version conflicts. Deployment units in 3.x require explicit packaging and versioning (JAR upload via CLI), providing better control and reproducibility.

**Q2:** Can you run a compute job on all nodes simultaneously in 3.x?

**A2:** Broadcast-style compute is not directly available in the thin client API as of 3.1.0. You can iterate over `client.clusterNodes()` and submit individual jobs, but there is no built-in `broadcast()` equivalent yet. For most analytics workloads, SQL aggregations running across all partitions achieve the same effect.

---

## Part 6: Distribution Zones and COLOCATE BY (15 minutes)

### Exercise 13: Creating Zones and Colocated Tables

**Objective:** Create distribution zones and tables with `COLOCATE BY` to ensure related data is partitioned together.

Create `Ex13_ColocatedTables.java`:

```java
package com.example.ignite3;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;

public class Ex13_ColocatedTables {

    public static void main(String[] args) {
        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            System.out.println("=== Exercise 13: Distribution Zones & COLOCATE BY ===\n");

            // Step 1: Create a distribution zone
            System.out.println("--- Step 1: Create Distribution Zone ---");
            client.sql().executeScript(
                "CREATE ZONE IF NOT EXISTS ecommerce_zone " +
                "WITH replicas=2, partitions=128, storage_profiles='default'");
            System.out.println("Zone 'ecommerce_zone' created.\n");

            // Step 2: Create a parent table (Customer)
            System.out.println("--- Step 2: Create Customer table ---");
            client.sql().executeScript(
                "CREATE TABLE IF NOT EXISTS Customer (" +
                "    customerId INT PRIMARY KEY," +
                "    name       VARCHAR(200)," +
                "    email      VARCHAR(200)" +
                ") WITH PRIMARY_ZONE='ECOMMERCE_ZONE'");
            System.out.println("Table 'Customer' created in ecommerce_zone.\n");

            // Step 3: Create colocated child table (CustomerOrder)
            System.out.println("--- Step 3: Create colocated CustomerOrder table ---");
            client.sql().executeScript(
                "CREATE TABLE IF NOT EXISTS CustomerOrder (" +
                "    orderId    INT," +
                "    customerId INT," +
                "    product    VARCHAR(200)," +
                "    amount     DOUBLE," +
                "    PRIMARY KEY (orderId, customerId)" +
                ") COLOCATE BY (customerId)" +
                "  WITH PRIMARY_ZONE='ECOMMERCE_ZONE'");
            System.out.println("Table 'CustomerOrder' colocated by customerId.\n");

            // Step 4: Insert data
            System.out.println("--- Step 4: Insert data ---");
            client.sql().execute(null,
                "INSERT INTO Customer VALUES (1, 'Alice Morgan', 'alice@example.com') ON CONFLICT DO NOTHING");
            client.sql().execute(null,
                "INSERT INTO Customer VALUES (2, 'Bob Chen', 'bob@example.com') ON CONFLICT DO NOTHING");

            String[] orders = {
                "INSERT INTO CustomerOrder VALUES (101, 1, 'Laptop', 999.99) ON CONFLICT DO NOTHING",
                "INSERT INTO CustomerOrder VALUES (102, 1, 'Mouse', 29.99)   ON CONFLICT DO NOTHING",
                "INSERT INTO CustomerOrder VALUES (103, 2, 'Keyboard', 79.99) ON CONFLICT DO NOTHING",
                "INSERT INTO CustomerOrder VALUES (104, 2, 'Monitor', 549.00) ON CONFLICT DO NOTHING",
                "INSERT INTO CustomerOrder VALUES (105, 1, 'USB Hub', 45.00)  ON CONFLICT DO NOTHING"
            };
            for (String sql : orders) {
                client.sql().execute(null, sql);
            }
            System.out.println("Inserted 2 customers and 5 orders.\n");

            // Step 5: Colocated JOIN -- efficient because data is on the same node
            System.out.println("--- Step 5: Colocated JOIN ---");
            System.out.println("Because Customer and CustomerOrder are COLOCATE BY customerId,");
            System.out.println("this JOIN executes locally on each partition (no network shuffle).\n");

            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT c.name, o.orderId, o.product, o.amount " +
                    "FROM Customer c " +
                    "JOIN CustomerOrder o ON c.customerId = o.customerId " +
                    "ORDER BY c.name, o.orderId")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("  %-15s Order#%d  %-10s $%.2f%n",
                        row.stringValue("name"),
                        row.intValue("orderId"),
                        row.stringValue("product"),
                        row.doubleValue("amount"));
                }
            }

            // Step 6: Aggregation per customer
            System.out.println("\n--- Step 6: Per-customer totals ---");
            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT c.name, COUNT(o.orderId) AS order_count, " +
                    "SUM(o.amount) AS total_spent " +
                    "FROM Customer c " +
                    "JOIN CustomerOrder o ON c.customerId = o.customerId " +
                    "GROUP BY c.name ORDER BY total_spent DESC")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("  %-15s  Orders: %d  Total: $%,.2f%n",
                        row.stringValue("name"),
                        row.longValue("order_count"),
                        row.doubleValue("total_spent"));
                }
            }

            System.out.println("\nExercise 13 complete.");
        }
    }
}
```

**Expected Output:**
```
--- Step 5: Colocated JOIN ---
  Alice Morgan    Order#101  Laptop     $999.99
  Alice Morgan    Order#102  Mouse      $29.99
  Alice Morgan    Order#105  USB Hub    $45.00
  Bob Chen        Order#103  Keyboard   $79.99
  Bob Chen        Order#104  Monitor    $549.00

--- Step 6: Per-customer totals ---
  Alice Morgan     Orders: 3  Total: $1,074.98
  Bob Chen         Orders: 2  Total: $628.99
```

**Key Takeaway (vs 2.x):** In 2.x, colocation was achieved via `@AffinityKeyMapped` on a Java field and configured per cache. In 3.x, `COLOCATE BY (column)` in the DDL statement is declarative and language-agnostic. Any client in any language benefits from the same colocation.

---

### Exercise 14: Inspecting Zones and Storage Profiles

**Objective:** Query system views to inspect zone configuration and verify colocation.

Create `Ex14_InspectZones.java`:

```java
package com.example.ignite3;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;

public class Ex14_InspectZones {

    public static void main(String[] args) {
        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            System.out.println("=== Exercise 14: Inspect Zones & Storage Profiles ===\n");

            // List all distribution zones
            System.out.println("--- Distribution Zones ---");
            try (ResultSet<SqlRow> rs = client.sql().execute(
                    null, "SELECT * FROM SYSTEM.ZONES")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("  Zone: %-20s  Replicas: %d  Partitions: %d%n",
                        row.stringValue("NAME"),
                        row.intValue("REPLICAS"),
                        row.intValue("PARTITIONS"));
                }
            }

            // List all tables and their zones
            System.out.println("\n--- Tables and Zones ---");
            try (ResultSet<SqlRow> rs = client.sql().execute(
                    null, "SELECT * FROM SYSTEM.TABLES")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("  Table: %-20s  Schema: %s%n",
                        row.stringValue("NAME"),
                        row.stringValue("SCHEMA_NAME"));
                }
            }

            // Discuss storage profiles
            System.out.println("\n--- Storage Profiles ---");
            System.out.println("Ignite 3.x supports multiple storage engines:");
            System.out.println("  aimem     - Volatile in-memory (fastest, no persistence)");
            System.out.println("  aipersist - Page memory + disk persistence (balanced)");
            System.out.println("  rocksdb   - LSM-tree on disk (large datasets, write-optimised)");
            System.out.println();
            System.out.println("In 2.x, all caches used the same B+ tree page memory engine.");
            System.out.println("In 3.x, you choose the engine per zone via storage_profiles.");

            System.out.println("\nExercise 14 complete.");
        }
    }
}
```

**Key Takeaway (vs 2.x):** In 2.x, all data used a single storage engine. In 3.x, distribution zones can reference different storage profiles, allowing hot data to use `aimem` (fast, volatile) while cold data uses `rocksdb` (disk-based, write-optimized) within the same cluster.

---

### Q&A -- Part 6

**Q1:** What happens if two tables in the same zone use `COLOCATE BY` on the same column value?

**A1:** Rows from both tables with the same colocation key value will be stored on the same partition (and therefore the same node). This is exactly the point: JOINs between these tables on that column are local and avoid network shuffles.

**Q2:** Can you change a table's zone after creation?

**A2:** No. The zone is set at table creation time. To move a table to a different zone, you would need to create a new table in the target zone and migrate the data. This is a design-time decision.

**Q3:** How does COLOCATE BY relate to the PRIMARY KEY?

**A3:** The colocation columns must be a prefix of or included in the primary key. This ensures the partitioning algorithm can compute the target partition from the key alone, which is required for efficient single-key lookups.

---

## Part 7: Monitoring (10 minutes)

### Exercise 15: REST API and CLI Monitoring

**Objective:** Monitor the Ignite 3.x cluster using the built-in REST API and CLI.

**Step 1:** CLI monitoring commands.

```bash
# Cluster state
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 cluster state

# Physical topology (all nodes)
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 cluster topology physical

# Logical topology (cluster members)
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 cluster topology logical

# Node version
docker exec -it ignite3-node1 /opt/ignite/bin/ignite3 node version
```

**Step 2:** REST API endpoints.

```bash
# Cluster state
curl -s http://localhost:10300/management/v1/cluster/state | python3 -m json.tool

# Cluster configuration
curl -s http://localhost:10300/management/v1/configuration/cluster | python3 -m json.tool

# Node configuration
curl -s http://localhost:10300/management/v1/configuration/node | python3 -m json.tool
```

**Step 3:** System views from Java.

```java
package com.example.ignite3;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;

public class Ex15_Monitoring {

    public static void main(String[] args) {
        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            System.out.println("=== Exercise 15: Monitoring ===\n");

            // System views
            System.out.println("--- SYSTEM.TABLES ---");
            try (ResultSet<SqlRow> rs = client.sql().execute(
                    null, "SELECT * FROM SYSTEM.TABLES")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.println("  " + row.stringValue("NAME"));
                }
            }

            System.out.println("\n--- SYSTEM.ZONES ---");
            try (ResultSet<SqlRow> rs = client.sql().execute(
                    null, "SELECT * FROM SYSTEM.ZONES")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("  %-20s replicas=%d partitions=%d%n",
                        row.stringValue("NAME"),
                        row.intValue("REPLICAS"),
                        row.intValue("PARTITIONS"));
                }
            }

            System.out.println("\n--- SYSTEM.NODES ---");
            try (ResultSet<SqlRow> rs = client.sql().execute(
                    null, "SELECT * FROM SYSTEM.NODES")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.println("  Node: " + row.stringValue("NAME")
                        + "  ID: " + row.stringValue("ID"));
                }
            }

            System.out.println("\nExercise 15 complete.");
        }
    }
}
```

**Key Takeaway (vs 2.x):** In 2.x, monitoring relied on JMX beans and programmatic `ClusterMetrics`. In 3.x, monitoring is available via REST API (port 10300), CLI commands, and SQL system views (`SYSTEM.TABLES`, `SYSTEM.ZONES`, `SYSTEM.NODES`).

---

### Exercise 16: OpenMetrics and Prometheus Integration

**Objective:** Understand how Ignite 3.x exposes metrics in OpenMetrics format for Prometheus scraping.

```bash
# OpenMetrics endpoint (Prometheus-compatible)
curl -s http://localhost:10300/management/v1/metric/node | head -50

# If the endpoint returns metrics, you can configure Prometheus to scrape it:
echo ""
echo "=== Prometheus Configuration ==="
echo "Add to prometheus.yml:"
echo ""
echo "scrape_configs:"
echo "  - job_name: 'ignite3'"
echo "    metrics_path: '/management/v1/metric/node'"
echo "    static_configs:"
echo "      - targets: ['localhost:10300']"
echo ""
echo "=== Key Metrics to Watch ==="
echo "  ignite_sql_*       - SQL query stats"
echo "  ignite_tx_*        - Transaction stats"
echo "  ignite_compute_*   - Compute job stats"
echo "  ignite_storage_*   - Storage engine stats"
echo ""
echo "=== vs Ignite 2.x ==="
echo "2.x: JMX only (JConsole, VisualVM, JMX-to-Prometheus exporters)"
echo "3.x: Native REST + OpenMetrics (Prometheus-ready out of the box)"
```

**Key Takeaway (vs 2.x):** Ignite 2.x required JMX exporters for Prometheus integration. Ignite 3.x has built-in REST endpoints that serve metrics in OpenMetrics format, making observability setup significantly simpler.

---

### Q&A -- Part 7

**Q1:** Can you create custom metrics in Ignite 3.x?

**A1:** Custom application metrics are not natively supported in the Ignite metrics framework. For application-level metrics, use standard libraries (Micrometer, Dropwizard Metrics) in your application and export alongside Ignite metrics.

**Q2:** How do you monitor query performance in 3.x?

**A2:** Use the SQL system views and REST metrics endpoint. You can also enable SQL query logging via the cluster configuration. In 2.x, you used `SqlFieldsQuery.setLazy()` and cache query metrics.

---

## Part 8: Complete Comparison Summary (10 minutes)

### Exercise 17: Architecture, API, and Feature Matrix

**Objective:** Build a comprehensive comparison chart and migration checklist.

Review the following comparison matrix:

| Aspect | Ignite 2.x | Ignite 3.x |
|--------|-------------|------------|
| **Philosophy** | Cache-first | Schema-first (SQL DDL) |
| **Java Version** | Java 8+ | Java 11+ |
| **Configuration** | Spring XML / Java code | HOCON / CLI / REST |
| **Config Changes** | Requires restart | Dynamic (many settings) |
| **Discovery** | Ring topology (TcpDiscoverySpi) | SWIM gossip protocol |
| **Discovery Port** | 47500 | 3344 |
| **Communication Port** | 47100 | 3344 (shared) |
| **Client Port** | 10800 | 10800 |
| **REST Port** | 8080 (Jetty) | 10300 |
| **Cluster Formation** | Auto-forms on discovery | Explicit `cluster init` |
| **Consensus** | Custom protocol | RAFT |
| **Activation** | ACTIVE / INACTIVE / READ_ONLY | Always active after init |
| **Baseline Topology** | Manual management | Automatic (RAFT) |
| **Primary API** | Cache API (`IgniteCache`) | Table API (`RecordView`, `KeyValueView`) |
| **Schema Definition** | Java annotations (`@QuerySqlField`) | SQL DDL (`CREATE TABLE`) |
| **Schema Evolution** | Limited | Full DDL (`ALTER TABLE ADD/DROP COLUMN`) |
| **SQL Engine** | H2 (legacy) / Calcite | Calcite only |
| **SQL Transactions** | Limited | Full support |
| **SQL+KV in one TX** | No | Yes |
| **Transaction Model** | PESSIMISTIC/OPTIMISTIC + isolation levels | Strictly serializable |
| **Client Model** | Thick vs Thin | Unified thin client |
| **Storage Engines** | Single (page memory) | Pluggable (aimem, aipersist, RocksDB) |
| **Data Colocation** | `@AffinityKeyMapped` | `COLOCATE BY` in DDL |
| **Data Placement** | Affinity function per cache | Distribution Zones |
| **Compute: Basic** | Yes (call, run, broadcast) | Yes (JobTarget, JobDescriptor) |
| **Compute: MapReduce** | Yes (ComputeTask) | Not yet |
| **Compute: Service Grid** | Yes | Not yet |
| **Compute: Peer Class Loading** | Yes | No (deployment units instead) |
| **Serialization** | BinaryMarshaller | Schema-based internal |
| **Async API** | `IgniteFuture` (custom) | `CompletableFuture` (standard Java) |
| **Monitoring** | JMX primary | REST API + CLI + OpenMetrics |
| **System Views** | Limited SQL views | `SYSTEM.TABLES`, `SYSTEM.ZONES`, `SYSTEM.NODES` |
| **Continuous Queries** | Yes | Not yet |
| **Near Cache** | Yes | Not yet |
| **Spring Data** | Yes | Not yet |
| **Hibernate L2 Cache** | Yes | Not yet |

### Migration Checklist

If migrating from Ignite 2.x to 3.x, follow this checklist:

1. **Schema**: Convert `@QuerySqlField` annotations and cache configs to `CREATE TABLE` / `CREATE ZONE` DDL statements.
2. **Configuration**: Convert Spring XML / `IgniteConfiguration` Java code to HOCON files.
3. **Client code**: Replace `Ignition.start()` with `IgniteClient.builder().addresses(...).build()`.
4. **CRUD operations**: Replace `cache.put()/get()` with `RecordView.upsert()/get()` or `KeyValueView.put()/get()`.
5. **SQL queries**: Replace `cache.query(new SqlFieldsQuery(...))` with `client.sql().execute(null, ...)`.
6. **Transactions**: Remove `TransactionConcurrency`/`TransactionIsolation` parameters. Use `client.transactions().begin()`.
7. **Colocation**: Replace `@AffinityKeyMapped` with `COLOCATE BY` in table DDL.
8. **Compute**: Rewrite `ComputeTask` implementations. Use SQL aggregations where possible. Use `JobTarget`/`JobDescriptor` for remaining compute needs.
9. **Monitoring**: Replace JMX tooling with REST API and Prometheus scraping.
10. **Testing**: Verify transaction semantics (strictly serializable may surface concurrency bugs that 2.x hid).

Create `Ex17_ComparisonSummary.java`:

```java
package com.example.ignite3;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;

public class Ex17_ComparisonSummary {

    public static void main(String[] args) {
        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            System.out.println("=== Exercise 17: Ignite 3.x Cluster Summary ===\n");

            // Confirm connection
            System.out.println("Connected to cluster.\n");

            // Show all tables
            System.out.println("--- All Tables ---");
            try (ResultSet<SqlRow> rs = client.sql().execute(
                    null, "SELECT * FROM SYSTEM.TABLES")) {
                int count = 0;
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("  %s.%s%n",
                        row.stringValue("SCHEMA_NAME"),
                        row.stringValue("NAME"));
                    count++;
                }
                System.out.println("  Total: " + count + " tables\n");
            }

            // Show all zones
            System.out.println("--- All Distribution Zones ---");
            try (ResultSet<SqlRow> rs = client.sql().execute(
                    null, "SELECT * FROM SYSTEM.ZONES")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("  %-20s  replicas=%d  partitions=%d%n",
                        row.stringValue("NAME"),
                        row.intValue("REPLICAS"),
                        row.intValue("PARTITIONS"));
                }
            }

            // Show all nodes
            System.out.println("\n--- Cluster Nodes ---");
            try (ResultSet<SqlRow> rs = client.sql().execute(
                    null, "SELECT * FROM SYSTEM.NODES")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.println("  " + row.stringValue("NAME")
                        + "  " + row.stringValue("ID"));
                }
            }

            System.out.println("\n=== Key Takeaways ===");
            System.out.println("1. Ignite 3.x is a ground-up rewrite, not an incremental upgrade");
            System.out.println("2. Schema-first design with SQL DDL makes it RDBMS-friendly");
            System.out.println("3. RAFT consensus provides formally proven consistency");
            System.out.println("4. Unified client model eliminates thick/thin confusion");
            System.out.println("5. Distribution Zones decouple placement from table definitions");
            System.out.println("6. Mixed SQL+KV transactions are a major new capability");
            System.out.println("7. Some 2.x features (MapReduce, Service Grid, near cache) are not yet ported");
            System.out.println("8. Migration requires a full rewrite -- no drop-in upgrade path");

            System.out.println("\nExercise 17 complete.");
        }
    }
}
```

---

### Q&A -- Part 8

**Q1:** Is there a direct migration tool from Ignite 2.x to 3.x?

**A1:** There is no automated migration tool. The APIs, configuration formats, and wire protocols are entirely different. Migration requires recreating schemas via DDL, rewriting application code, and migrating data via export/import or dual-write strategies.

**Q2:** When should a team stay on Ignite 2.x instead of moving to 3.x?

**A2:** Stay on 2.x if your application critically depends on MapReduce compute, Service Grid, continuous queries, near caches, or Spring/Hibernate integrations. Also stay on 2.x if you need a mature, battle-tested platform with a large ecosystem. Choose 3.x for greenfield projects that are SQL-heavy, need strict serializability, or want pluggable storage engines.

**Q3:** Will Ignite 2.x continue to receive updates?

**A3:** Apache Ignite 2.x is in maintenance mode. It receives security fixes and critical bug patches, but new feature development is focused on the 3.x line. Plan for an eventual migration.

---

## Cleanup

```bash
# Drop tables (run from Java or CLI)
# client.sql().execute(null, "DROP TABLE IF EXISTS CustomerOrder");
# client.sql().execute(null, "DROP TABLE IF EXISTS Customer");
# client.sql().execute(null, "DROP TABLE IF EXISTS Product");
# client.sql().execute(null, "DROP TABLE IF EXISTS Sales");
# client.sql().execute(null, "DROP TABLE IF EXISTS Account");
# client.sql().execute(null, "DROP TABLE IF EXISTS AuditLog");
# client.sql().execute(null, "DROP TABLE IF EXISTS Person");

# Stop and remove Docker container
docker stop ignite3-node1
docker rm ignite3-node1
```

---

## Verification Checklist

- [ ] Started Ignite 3.x cluster via Docker and initialised it
- [ ] Connected via `IgniteClient` thin client (Exercise 2)
- [ ] Explored SWIM discovery and RAFT via CLI and REST (Exercise 1)
- [ ] Viewed and updated dynamic configuration via CLI (Exercises 3-4)
- [ ] Created distribution zones (Exercise 5)
- [ ] Used `RecordView<Tuple>` for CRUD (Exercise 6)
- [ ] Used `RecordView<POJO>` for typed access (Exercise 7)
- [ ] Used `KeyValueView` and SQL queries (Exercise 8)
- [ ] Executed manual transactions with commit/rollback (Exercise 9)
- [ ] Mixed SQL and KV in a single transaction (Exercise 10)
- [ ] Explored compute API and deployment units (Exercise 11)
- [ ] Used SQL aggregations as compute replacement (Exercise 12)
- [ ] Created colocated tables with `COLOCATE BY` (Exercise 13)
- [ ] Inspected zones and storage profiles via system views (Exercise 14)
- [ ] Monitored cluster via REST, CLI, and system views (Exercises 15-16)
- [ ] Reviewed complete comparison matrix and migration checklist (Exercise 17)

---

## Additional Resources

- [Apache Ignite 3.x Documentation](https://ignite.apache.org/docs/ignite3/latest/)
- [Ignite 3 Table API Guide](https://ignite.apache.org/docs/ignite3/latest/developers-guide/table-api)
- [Ignite 3 SQL Reference](https://ignite.apache.org/docs/ignite3/latest/sql-reference/ddl)
- [Ignite 3 Distribution Zones](https://ignite.apache.org/docs/ignite3/latest/administrators-guide/distribution-zones)
- [RAFT Consensus Algorithm](https://raft.github.io/)
- [SWIM Protocol Paper](https://www.cs.cornell.edu/projects/Quicksilver/public_pdfs/SWIM.pdf)
- [Apache Ignite GitHub Repository](https://github.com/apache/ignite-3)
