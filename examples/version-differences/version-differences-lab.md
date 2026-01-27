# Apache Ignite Version Differences (2.16 vs 3.x)

## Duration: 60-75 minutes

## Objectives
- Understand the architectural differences between Apache Ignite 2.16 and 3.x
- Compare configuration approaches (Spring XML vs HOCON)
- Explore API differences (Cache API vs Table API)
- Practice with both versions side-by-side
- Understand migration considerations

## Prerequisites
- Completed Labs 1-3 (basic Ignite 2.x familiarity)
- Java 11 or higher (Ignite 3.x requires Java 11+)
- Docker installed (for running Ignite 3.x easily)
- Maven installed
- Internet connection for downloading dependencies

## Example Code and Presentation

Working examples and the presentation for this lab are in `examples/version-differences/`:
- **Presentation:** `examples/version-differences/module-13-version-differences.html`
- **Code examples:** `examples/version-differences/src/`

```bash
cd examples/version-differences
mvn clean compile

# Run examples
mvn exec:java -Dexec.mainClass="com.example.ignite.examples.versiondiff.Ignite2ConfigExample"
mvn exec:java -Dexec.mainClass="com.example.ignite.examples.versiondiff.Ignite2CacheAPIExample"
mvn exec:java -Dexec.mainClass="com.example.ignite.examples.versiondiff.Ignite2TransactionsExample"
mvn exec:java -Dexec.mainClass="com.example.ignite.examples.versiondiff.VersionComparisonSummary"
```

## Part 1: Setting Up Both Environments (15 minutes)

### Step 1: Ignite 2.16 Project Setup

You already have Ignite 2.16 set up from previous labs. Let's create a dedicated comparison project.

Create a new directory `version-comparison/ignite2`:

```bash
mkdir -p version-comparison/ignite2
cd version-comparison/ignite2
```

Create `pom.xml` for Ignite 2.16:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.ignite</groupId>
    <artifactId>ignite2-comparison</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <ignite.version>2.16.0</ignite.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.apache.ignite</groupId>
            <artifactId>ignite-core</artifactId>
            <version>${ignite.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.ignite</groupId>
            <artifactId>ignite-spring</artifactId>
            <version>${ignite.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.ignite</groupId>
            <artifactId>ignite-indexing</artifactId>
            <version>${ignite.version}</version>
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

### Step 2: Ignite 3.x Project Setup

Create a new directory `version-comparison/ignite3`:

```bash
mkdir -p version-comparison/ignite3
cd version-comparison/ignite3
```

Create `pom.xml` for Ignite 3.x client:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.ignite</groupId>
    <artifactId>ignite3-comparison</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <ignite3.version>3.0.0</ignite3.version>
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

### Step 3: Start Ignite 3.x Using Docker

The easiest way to run Ignite 3.x for testing is using Docker:

```bash
# Pull the Ignite 3 image
docker pull apacheignite/ignite:3.0.0

# Start a single-node Ignite 3 cluster
docker run -d \
  --name ignite3-node \
  -p 10300:10300 \
  -p 10800:10800 \
  apacheignite/ignite:3.0.0

# Wait for startup (about 30 seconds)
sleep 30

# Initialize the cluster
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster init \
  --name=myCluster \
  --meta-storage-node=defaultNode
```

**Expected Output:**
```
Cluster was initialized successfully
```

Verify the cluster is running:
```bash
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster status
```

---

## Part 2: Configuration Comparison (15 minutes)

### Exercise 1: Ignite 2.16 Spring XML Configuration

Create `ignite2/src/main/resources/ignite-config.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="ignite.cfg" class="org.apache.ignite.configuration.IgniteConfiguration">
        <!-- Instance name -->
        <property name="igniteInstanceName" value="ignite2-comparison-node"/>

        <!-- Enable peer class loading -->
        <property name="peerClassLoadingEnabled" value="true"/>

        <!-- Discovery configuration -->
        <property name="discoverySpi">
            <bean class="org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi">
                <property name="ipFinder">
                    <bean class="org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder">
                        <property name="addresses">
                            <list>
                                <value>127.0.0.1:47500..47509</value>
                            </list>
                        </property>
                    </bean>
                </property>
            </bean>
        </property>

        <!-- Cache configuration -->
        <property name="cacheConfiguration">
            <list>
                <bean class="org.apache.ignite.configuration.CacheConfiguration">
                    <property name="name" value="persons"/>
                    <property name="cacheMode" value="PARTITIONED"/>
                    <property name="backups" value="1"/>
                    <property name="indexedTypes">
                        <list>
                            <value>java.lang.Integer</value>
                            <value>com.example.ignite.model.Person</value>
                        </list>
                    </property>
                </bean>
            </list>
        </property>
    </bean>
</beans>
```

### Exercise 2: Ignite 2.16 Programmatic Configuration

Create `ignite2/src/main/java/com/example/ignite/Ignite2Config.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

public class Ignite2Config {

    public static void main(String[] args) {
        System.out.println("=== Ignite 2.16 Configuration Demo ===\n");

        // Programmatic configuration
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("ignite2-programmatic-node");
        cfg.setPeerClassLoadingEnabled(true);

        // Discovery configuration
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        // Cache configuration
        CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>("myCache");
        cacheCfg.setBackups(1);
        cfg.setCacheConfiguration(cacheCfg);

        System.out.println("Configuration created:");
        System.out.println("  - Instance Name: " + cfg.getIgniteInstanceName());
        System.out.println("  - Peer Class Loading: " + cfg.isPeerClassLoadingEnabled());
        System.out.println("  - Cache Configs: " + cfg.getCacheConfiguration().length);

        // Start Ignite
        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\nIgnite 2.16 node started!");
            System.out.println("  - Node ID: " + ignite.cluster().localNode().id());
            System.out.println("  - Cluster size: " + ignite.cluster().nodes().size());

            // Configuration is STATIC - cannot be changed at runtime
            System.out.println("\nNote: Configuration changes require node restart in 2.x");

            System.out.println("\nPress Enter to stop...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Exercise 3: Ignite 3.x HOCON Configuration

Create `ignite3/config/ignite-config.conf` (HOCON format):

```hocon
# Ignite 3.x HOCON Configuration
ignite {
    # Network configuration
    network {
        port: 3344
        nodeFinder {
            netClusterNodes: ["localhost:3344"]
        }
    }

    # Cluster configuration
    cluster {
        name: "comparison-cluster"
    }

    # Storage configuration - pluggable engines!
    storage {
        profiles: [
            {
                name: "default"
                engine: "aipersist"  # Options: aimem, aipersist, rocksdb
            }
            {
                name: "in-memory-only"
                engine: "aimem"
            }
        ]
    }

    # REST API configuration
    rest {
        port: 10300
    }

    # Client connector
    clientConnector {
        port: 10800
    }
}
```

**Key Differences:**
1. **Format**: HOCON is more readable than XML
2. **Pluggable Storage**: Choose storage engine per profile
3. **Dynamic Config**: Many settings can be changed at runtime

### Exercise 4: Viewing Ignite 3.x Configuration via CLI

```bash
# View current configuration
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster config show

# View node-specific configuration
docker exec -it ignite3-node /opt/ignite/bin/ignite3 node config show

# Update configuration dynamically (no restart!)
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster config update \
  "gc.lowWatermark=0.3"
```

---

## Part 3: API Comparison (20 minutes)

### Exercise 5: Ignite 2.16 Cache API

Create `ignite2/src/main/java/com/example/ignite/model/Person.java`:

```java
package com.example.ignite.model;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import java.io.Serializable;

public class Person implements Serializable {
    @QuerySqlField(index = true)
    private int id;

    @QuerySqlField
    private String firstName;

    @QuerySqlField(index = true)
    private String lastName;

    @QuerySqlField
    private int age;

    public Person() {}

    public Person(int id, String firstName, String lastName, int age) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    @Override
    public String toString() {
        return String.format("Person[id=%d, name=%s %s, age=%d]",
            id, firstName, lastName, age);
    }
}
```

Create `ignite2/src/main/java/com/example/ignite/Ignite2CacheAPI.java`:

```java
package com.example.ignite;

import com.example.ignite.model.Person;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.util.List;

public class Ignite2CacheAPI {

    public static void main(String[] args) {
        System.out.println("=== Ignite 2.16 Cache API Demo ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("ignite2-cache-demo");
        cfg.setPeerClassLoadingEnabled(true);

        // Cache configuration with indexed types
        CacheConfiguration<Integer, Person> personCacheCfg =
            new CacheConfiguration<>("PersonCache");
        personCacheCfg.setIndexedTypes(Integer.class, Person.class);
        cfg.setCacheConfiguration(personCacheCfg);

        try (Ignite ignite = Ignition.start(cfg)) {
            // Get cache - this is the primary API in 2.x
            IgniteCache<Integer, Person> cache = ignite.cache("PersonCache");

            System.out.println("--- CRUD Operations (Cache API) ---\n");

            // CREATE
            System.out.println("1. Creating persons...");
            cache.put(1, new Person(1, "John", "Doe", 30));
            cache.put(2, new Person(2, "Jane", "Smith", 28));
            cache.put(3, new Person(3, "Bob", "Johnson", 35));
            System.out.println("   Created 3 persons\n");

            // READ
            System.out.println("2. Reading person with ID 1...");
            Person person = cache.get(1);
            System.out.println("   " + person + "\n");

            // UPDATE
            System.out.println("3. Updating person's age...");
            person.setAge(31);
            cache.put(1, person);  // Full object replacement
            System.out.println("   Updated: " + cache.get(1) + "\n");

            // DELETE
            System.out.println("4. Deleting person with ID 3...");
            cache.remove(3);
            System.out.println("   Person 3 deleted: " + (cache.get(3) == null) + "\n");

            // SQL Query (layered on top of cache)
            System.out.println("--- SQL Query (on Cache) ---\n");
            SqlFieldsQuery query = new SqlFieldsQuery(
                "SELECT id, firstName, lastName, age FROM Person WHERE age > ?");
            query.setArgs(25);

            System.out.println("5. Query: Persons older than 25");
            try (QueryCursor<List<?>> cursor = cache.query(query)) {
                for (List<?> row : cursor) {
                    System.out.printf("   ID: %d, Name: %s %s, Age: %d%n",
                        row.get(0), row.get(1), row.get(2), row.get(3));
                }
            }

            System.out.println("\n--- Key Points for Ignite 2.x ---");
            System.out.println("1. Cache API is primary - data accessed via cache.get()/put()");
            System.out.println("2. Schema defined via @QuerySqlField annotations");
            System.out.println("3. SQL queries use cache.query() method");
            System.out.println("4. Update requires full object replacement");

            System.out.println("\nPress Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Exercise 6: Ignite 3.x Table API

Create `ignite3/src/main/java/com/example/ignite/Ignite3TableAPI.java`:

```java
package com.example.ignite;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;

public class Ignite3TableAPI {

    public static void main(String[] args) {
        System.out.println("=== Ignite 3.x Table API Demo ===\n");

        // Connect to Ignite 3 cluster (unified client - no thick/thin distinction)
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {

            System.out.println("Connected to Ignite 3.x cluster!\n");

            // Step 1: Create table using SQL DDL (schema-first approach)
            System.out.println("--- Schema Definition (SQL DDL) ---\n");
            System.out.println("1. Creating table via SQL DDL...");

            client.sql().execute(null,
                "CREATE TABLE IF NOT EXISTS Person (" +
                "    id INT PRIMARY KEY," +
                "    firstName VARCHAR(100)," +
                "    lastName VARCHAR(100)," +
                "    age INT" +
                ")");
            System.out.println("   Table 'Person' created\n");

            // Create index
            System.out.println("2. Creating index...");
            try {
                client.sql().execute(null,
                    "CREATE INDEX IF NOT EXISTS idx_person_lastname ON Person (lastName)");
                System.out.println("   Index created\n");
            } catch (Exception e) {
                System.out.println("   Index already exists or skipped\n");
            }

            // Get table reference
            Table personTable = client.tables().table("Person");

            // Step 2: Using RecordView (Tuple-based)
            System.out.println("--- RecordView API (Tuple-based) ---\n");
            RecordView<Tuple> recordView = personTable.recordView();

            System.out.println("3. Inserting records using RecordView...");
            recordView.upsert(null, Tuple.create()
                .set("id", 1)
                .set("firstName", "John")
                .set("lastName", "Doe")
                .set("age", 30));

            recordView.upsert(null, Tuple.create()
                .set("id", 2)
                .set("firstName", "Jane")
                .set("lastName", "Smith")
                .set("age", 28));
            System.out.println("   Inserted 2 records\n");

            // Read using RecordView
            System.out.println("4. Reading record with ID 1...");
            Tuple key = Tuple.create().set("id", 1);
            Tuple record = recordView.get(null, key);
            if (record != null) {
                System.out.printf("   Person: %s %s, age %d%n",
                    record.stringValue("firstName"),
                    record.stringValue("lastName"),
                    record.intValue("age"));
            }
            System.out.println();

            // Step 3: Using KeyValueView (similar to 2.x cache)
            System.out.println("--- KeyValueView API (Cache-like) ---\n");
            KeyValueView<Tuple, Tuple> kvView = personTable.keyValueView();

            System.out.println("5. Using KeyValueView (familiar to 2.x users)...");
            Tuple keyTuple = Tuple.create().set("id", 3);
            Tuple valueTuple = Tuple.create()
                .set("firstName", "Bob")
                .set("lastName", "Johnson")
                .set("age", 35);
            kvView.put(null, keyTuple, valueTuple);
            System.out.println("   Inserted person with ID 3\n");

            // Step 4: SQL Query (first-class citizen in 3.x)
            System.out.println("--- SQL Query (First-Class API) ---\n");
            System.out.println("6. Query: Persons older than 25");

            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT id, firstName, lastName, age FROM Person WHERE age > ?", 25)) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("   ID: %d, Name: %s %s, Age: %d%n",
                        row.intValue("id"),
                        row.stringValue("firstName"),
                        row.stringValue("lastName"),
                        row.intValue("age"));
                }
            }

            System.out.println("\n--- Key Points for Ignite 3.x ---");
            System.out.println("1. Schema defined via SQL DDL (CREATE TABLE)");
            System.out.println("2. Multiple views of same data: RecordView, KeyValueView, SQL");
            System.out.println("3. SQL is first-class citizen, not layered on cache");
            System.out.println("4. Unified client model - same features everywhere");
            System.out.println("5. Tuple-based API for flexible data access");

            // Cleanup
            System.out.println("\n7. Cleaning up...");
            client.sql().execute(null, "DROP TABLE IF EXISTS Person");
            System.out.println("   Table dropped\n");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("\nMake sure Ignite 3 is running:");
            System.out.println("  docker run -d --name ignite3-node -p 10800:10800 apacheignite/ignite:3.0.0");
        }
    }
}
```

### Exercise 7: Run Both Demos

**Run Ignite 2.16 demo:**
```bash
cd version-comparison/ignite2
mvn compile exec:java -Dexec.mainClass="com.example.ignite.Ignite2CacheAPI"
```

**Run Ignite 3.x demo (ensure Docker container is running):**
```bash
cd version-comparison/ignite3
mvn compile exec:java -Dexec.mainClass="com.example.ignite.Ignite3TableAPI"
```

---

## Part 4: Transaction Comparison (10 minutes)

### Exercise 8: Ignite 2.16 Transactions

Create `ignite2/src/main/java/com/example/ignite/Ignite2Transactions.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

public class Ignite2Transactions {

    public static void main(String[] args) {
        System.out.println("=== Ignite 2.16 Transaction Demo ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("ignite2-tx-demo");

        CacheConfiguration<Integer, Integer> cacheCfg =
            new CacheConfiguration<>("AccountCache");
        cacheCfg.setAtomicityMode(org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL);
        cfg.setCacheConfiguration(cacheCfg);

        try (Ignite ignite = Ignition.start(cfg)) {
            IgniteCache<Integer, Integer> accounts = ignite.cache("AccountCache");

            // Initialize accounts
            accounts.put(1, 1000);  // Account 1: $1000
            accounts.put(2, 500);   // Account 2: $500

            System.out.println("Initial balances:");
            System.out.println("  Account 1: $" + accounts.get(1));
            System.out.println("  Account 2: $" + accounts.get(2));

            // Transaction in 2.x - explicit transaction management
            System.out.println("\nPerforming transfer of $200 from Account 1 to Account 2...\n");

            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ)) {

                int balance1 = accounts.get(1);
                int balance2 = accounts.get(2);

                // Transfer $200
                accounts.put(1, balance1 - 200);
                accounts.put(2, balance2 + 200);

                // Commit transaction
                tx.commit();
                System.out.println("Transaction committed!");
            }

            System.out.println("\nFinal balances:");
            System.out.println("  Account 1: $" + accounts.get(1));
            System.out.println("  Account 2: $" + accounts.get(2));

            System.out.println("\n--- Ignite 2.x Transaction Notes ---");
            System.out.println("1. Uses PESSIMISTIC or OPTIMISTIC concurrency");
            System.out.println("2. Isolation levels: READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE");
            System.out.println("3. SQL transactions limited in 2.x");
            System.out.println("4. Key-Value transactions are robust");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Exercise 9: Ignite 3.x Transactions

Create `ignite3/src/main/java/com/example/ignite/Ignite3Transactions.java`:

```java
package com.example.ignite;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.Transaction;

public class Ignite3Transactions {

    public static void main(String[] args) {
        System.out.println("=== Ignite 3.x Transaction Demo ===\n");

        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {

            // Create accounts table
            client.sql().execute(null,
                "CREATE TABLE IF NOT EXISTS Accounts (" +
                "    id INT PRIMARY KEY," +
                "    balance INT" +
                ")");

            // Initialize accounts via SQL
            client.sql().execute(null,
                "INSERT INTO Accounts (id, balance) VALUES (1, 1000) " +
                "ON CONFLICT DO UPDATE SET balance = 1000");
            client.sql().execute(null,
                "INSERT INTO Accounts (id, balance) VALUES (2, 500) " +
                "ON CONFLICT DO UPDATE SET balance = 500");

            System.out.println("Initial balances:");
            printBalances(client);

            // Transaction in 3.x - supports BOTH SQL and KV in same transaction!
            System.out.println("\nPerforming transfer of $200 from Account 1 to Account 2...\n");

            // Start transaction
            Transaction tx = client.transactions().begin();

            try {
                // Can use SQL within transaction
                client.sql().execute(tx,
                    "UPDATE Accounts SET balance = balance - 200 WHERE id = 1");

                // Can also use Table API in same transaction
                Table accountsTable = client.tables().table("Accounts");
                KeyValueView<Tuple, Tuple> kv = accountsTable.keyValueView();

                Tuple key = Tuple.create().set("id", 2);
                Tuple currentValue = kv.get(tx, key);
                int newBalance = currentValue.intValue("balance") + 200;

                kv.put(tx, key, Tuple.create().set("balance", newBalance));

                // Commit - both SQL and KV operations committed atomically
                tx.commit();
                System.out.println("Transaction committed!");

            } catch (Exception e) {
                tx.rollback();
                System.out.println("Transaction rolled back: " + e.getMessage());
            }

            System.out.println("\nFinal balances:");
            printBalances(client);

            System.out.println("\n--- Ignite 3.x Transaction Notes ---");
            System.out.println("1. Strictly serializable isolation by default");
            System.out.println("2. Full SQL transaction support!");
            System.out.println("3. Can mix SQL and KV operations in same transaction");
            System.out.println("4. RAFT-based consensus ensures consistency");
            System.out.println("5. Great for RDBMS migration");

            // Cleanup
            client.sql().execute(null, "DROP TABLE IF EXISTS Accounts");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printBalances(IgniteClient client) {
        try (ResultSet<SqlRow> rs = client.sql().execute(null,
                "SELECT id, balance FROM Accounts ORDER BY id")) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                System.out.printf("  Account %d: $%d%n",
                    row.intValue("id"), row.intValue("balance"));
            }
        }
    }
}
```

---

## Part 5: Side-by-Side Comparison Summary (5 minutes)

### Exercise 10: Create Comparison Chart

Review and discuss the key differences:

| Aspect | Ignite 2.16 | Ignite 3.x |
|--------|-------------|------------|
| **Philosophy** | Cache-first | Schema-first |
| **Configuration** | Spring XML / Java | HOCON / CLI |
| **Primary API** | Cache API | Table API |
| **Schema Definition** | Java annotations | SQL DDL |
| **SQL Support** | Layered on cache | First-class citizen |
| **Transactions** | KV transactions robust | Full SQL transactions |
| **Client Model** | Thick vs Thin | Unified |
| **Consensus** | Custom protocol | RAFT |
| **Storage** | Fixed engine | Pluggable (aimem/aipersist/RocksDB) |
| **Config Changes** | Requires restart | Dynamic |

---

## Verification Steps

### Checklist
- [ ] Created separate project directories for 2.16 and 3.x
- [ ] Successfully started Ignite 2.16 node
- [ ] Successfully started Ignite 3.x via Docker
- [ ] Ran Cache API demo (2.16)
- [ ] Ran Table API demo (3.x)
- [ ] Observed configuration differences
- [ ] Understood transaction model differences
- [ ] Compared API approaches

### Common Issues and Solutions

**Issue 1: Ignite 3 Docker container not starting**
```bash
# Check container logs
docker logs ignite3-node

# Restart container
docker rm -f ignite3-node
docker run -d --name ignite3-node -p 10300:10300 -p 10800:10800 apacheignite/ignite:3.0.0
```

**Issue 2: Cannot connect to Ignite 3**
```bash
# Verify cluster is initialized
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster status

# Re-initialize if needed
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster init --name=myCluster --meta-storage-node=defaultNode
```

**Issue 3: Port conflicts**
```
# Use different ports
docker run -d --name ignite3-node -p 10301:10300 -p 10801:10800 apacheignite/ignite:3.0.0
# Update client connection: .addresses("localhost:10801")
```

---

## Lab Questions

1. What is the fundamental difference in how data is accessed between Ignite 2.x and 3.x?

2. Why did Apache Ignite 3.x adopt the RAFT consensus algorithm?

3. What are the three storage engine options in Ignite 3.x, and when would you use each?

4. Can you migrate directly from Ignite 2.x to 3.x? Why or why not?

5. What is the advantage of Ignite 3.x's unified client model?

---

## Answers

1. **Ignite 2.x** uses a cache-first approach where data is accessed via the Cache API (key-value operations), with SQL layered on top. **Ignite 3.x** uses a schema-first approach where schema is defined via SQL DDL, and data can be accessed via Table API, KeyValueView, or SQL equally.

2. RAFT provides **industry-standard consistency guarantees**, automatic leader election, and built-in split-brain protection. It replaces Ignite 2.x's custom replication protocol with a well-understood, proven consensus algorithm.

3. The three storage engines are:
   - **aimem**: Pure in-memory storage - fastest, no persistence, good for caches
   - **aipersist**: Memory-first with disk persistence - balance of speed and durability
   - **RocksDB**: Disk-based storage - best for write-heavy workloads with large datasets

4. **No direct migration path exists.** The architectural differences are too significant. Migration requires: setting up a new 3.x cluster, recreating schema via SQL DDL, migrating data, and updating application code. Ignite 3.1+ provides migration tools to help.

5. The unified client model means **all features are available to all clients** regardless of language. In 2.x, thin clients had limited functionality compared to thick clients. In 3.x, there's no distinction - the client protocol supports all operations.

---

## Cleanup

```bash
# Stop Ignite 3 Docker container
docker stop ignite3-node
docker rm ignite3-node

# Clean up project directories (optional)
rm -rf version-comparison/
```

---

## Next Steps

After completing this lab, you should:
- Understand when to choose Ignite 2.x vs 3.x for new projects
- Be able to evaluate migration effort for existing 2.x deployments
- Know the key API differences to plan code changes
- Understand the benefits of Ignite 3.x's architecture

## Additional Resources

- [Apache Ignite 3.0 Documentation](https://ignite.apache.org/docs/ignite3/latest/)
- [What's New in Apache Ignite 3.0](https://ignite.apache.org/blog/whats-new-in-apache-ignite-3-0.html)
- [Ignite 2.16 Release Notes](https://ignite.apache.org/releases/2.16.0/release_notes.html)
- [Getting to Know Apache Ignite 3](https://ignite.apache.org/blog/getting-to-know-apache-ignite-3.html)

## Completion

Once you can successfully:
- Set up and run both Ignite 2.16 and 3.x
- Understand configuration differences
- Use both Cache API (2.x) and Table API (3.x)
- Explain when to use each version

You have completed Version Differences!
