package com.example.ignite.solutions.lab13;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;

/**
 * Lab 13 Exercises 13-14: Ignite 3.x Data Colocation and Distribution Zones
 *
 * Demonstrates:
 * - Distribution Zones: creating, configuring, assigning tables
 * - COLOCATE BY clause for data colocation
 * - Storage profiles (aimem, aipersist, rocksdb)
 * - Colocated queries via SQL
 * - Contrast with 2.x @AffinityKeyMapped and RendezvousAffinityFunction
 *
 * Prerequisites: Running Ignite 3.x cluster
 */
public class Lab13DataColocation {

    public static void main(String[] args) {
        System.out.println("=== Lab 13: Ignite 3.x Data Colocation & Distribution Zones ===\n");

        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            demonstrateDistributionZones(client);
            demonstrateColocateBy(client);
            demonstrateColocatedQuery(client);
            demonstrateZoneManagement(client);
            printColocationComparison();

            // Cleanup
            client.sql().executeScript(
                "DROP TABLE IF EXISTS colo_orders;" +
                "DROP TABLE IF EXISTS colo_customers;" +
                "DROP ZONE IF EXISTS lab13_zone");

        } catch (Exception e) {
            System.err.println("Could not connect to Ignite 3.x cluster.");
            e.printStackTrace();
        }
    }

    /**
     * Exercise 13: Distribution Zones
     */
    private static void demonstrateDistributionZones(IgniteClient client) {
        System.out.println("=== Exercise 13: Distribution Zones ===\n");

        System.out.println("Distribution Zones control WHERE data is stored:\n");
        System.out.println("  - How many replicas (copies of data)");
        System.out.println("  - How many partitions (data slices)");
        System.out.println("  - Which storage engine to use");
        System.out.println("  - Which nodes participate in the zone");
        System.out.println();

        // Create a distribution zone
        try {
            client.sql().executeScript(
                "CREATE ZONE IF NOT EXISTS lab13_zone WITH " +
                "replicas=1, partitions=25, storage_profiles='default'");
            System.out.println("  Created distribution zone 'lab13_zone':");
            System.out.println("    replicas=1, partitions=25");
            System.out.println();
        } catch (Exception e) {
            System.out.println("  Zone creation: " + e.getMessage());
        }

        System.out.println("Zone DDL Examples:");
        System.out.println("  CREATE ZONE hot_data WITH");
        System.out.println("    replicas=3, partitions=256,");
        System.out.println("    storage_profiles='default';");
        System.out.println();
        System.out.println("  CREATE ZONE cold_data WITH");
        System.out.println("    replicas=1, partitions=64,");
        System.out.println("    storage_profiles='default';");
        System.out.println();
        System.out.println("  ALTER ZONE hot_data SET replicas=5;");
        System.out.println("  DROP ZONE cold_data;");
        System.out.println();

        System.out.println("Contrast with 2.x:");
        System.out.println("  2.x: Each cache has its own backups/partitions/affinity config");
        System.out.println("  3.x: Zones group tables with shared placement rules");
        System.out.println();
    }

    /**
     * Exercise 13b: COLOCATE BY
     */
    private static void demonstrateColocateBy(IgniteClient client) {
        System.out.println("=== COLOCATE BY (3.x) ===\n");

        System.out.println("COLOCATE BY ensures related data is on the same node:\n");

        // Create colocated tables
        try {
            client.sql().executeScript(
                "CREATE TABLE IF NOT EXISTS colo_customers (" +
                "  id INT PRIMARY KEY," +
                "  name VARCHAR," +
                "  city VARCHAR" +
                ") WITH PRIMARY_ZONE='lab13_zone'");

            client.sql().executeScript(
                "CREATE TABLE IF NOT EXISTS colo_orders (" +
                "  orderId INT," +
                "  customerId INT," +
                "  amount DOUBLE," +
                "  PRIMARY KEY (orderId, customerId)" +
                ") COLOCATE BY (customerId)" +
                " WITH PRIMARY_ZONE='lab13_zone'");

            System.out.println("  Created colocated tables:");
            System.out.println("    colo_customers (PK: id)");
            System.out.println("    colo_orders (PK: orderId,customerId) COLOCATE BY customerId");
            System.out.println();

            // Insert data
            Table customers = client.tables().table("COLO_CUSTOMERS");
            RecordView<Tuple> custView = customers.recordView();
            custView.upsert(null, Tuple.create().set("id", 100).set("name", "Alice").set("city", "NYC"));
            custView.upsert(null, Tuple.create().set("id", 200).set("name", "Bob").set("city", "SF"));

            Table orders = client.tables().table("COLO_ORDERS");
            RecordView<Tuple> orderView = orders.recordView();
            orderView.upsert(null, Tuple.create().set("orderId", 1).set("customerId", 100).set("amount", 99.99));
            orderView.upsert(null, Tuple.create().set("orderId", 2).set("customerId", 100).set("amount", 149.50));
            orderView.upsert(null, Tuple.create().set("orderId", 3).set("customerId", 200).set("amount", 250.00));

            System.out.println("  Populated with sample data (2 customers, 3 orders)");
        } catch (Exception e) {
            System.out.println("  Table creation: " + e.getMessage());
        }

        System.out.println();
        System.out.println("How COLOCATE BY works:");
        System.out.println("  - Orders with customerId=100 are stored on same node as customer 100");
        System.out.println("  - Joins between customers and orders avoid network hops");
        System.out.println("  - Partition affinity is based on the colocation column");
        System.out.println();

        System.out.println("Contrast with 2.x:");
        System.out.println("  2.x: @AffinityKeyMapped annotation on Java key class");
        System.out.println("  2.x: Requires custom Java key classes with equals/hashCode");
        System.out.println("  3.x: COLOCATE BY in SQL DDL (no Java code needed)");
        System.out.println("  3.x: Language-agnostic, works from any client");
        System.out.println();
    }

    /**
     * Exercise 14: Colocated query
     */
    private static void demonstrateColocatedQuery(IgniteClient client) {
        System.out.println("=== Exercise 14: Colocated SQL Join ===\n");

        try {
            System.out.println("  Colocated JOIN (data on same node, no network shuffle):");
            System.out.println();
            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT c.name, c.city, o.orderId, o.amount " +
                    "FROM colo_customers c " +
                    "JOIN colo_orders o ON c.id = o.customerId " +
                    "ORDER BY c.name, o.orderId")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("    %s (%s): Order #%d - $%.2f%n",
                        row.stringValue("NAME"), row.stringValue("CITY"),
                        row.intValue("ORDERID"), row.doubleValue("AMOUNT"));
                }
            }

            // Aggregate query
            System.out.println();
            System.out.println("  Aggregate per customer:");
            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT c.name, COUNT(o.orderId) AS order_count, SUM(o.amount) AS total " +
                    "FROM colo_customers c " +
                    "JOIN colo_orders o ON c.id = o.customerId " +
                    "GROUP BY c.name")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("    %s: %d orders, total $%.2f%n",
                        row.stringValue("NAME"), row.longValue("ORDER_COUNT"),
                        row.doubleValue("TOTAL"));
                }
            }
        } catch (Exception e) {
            System.out.println("  Query error: " + e.getMessage());
        }

        System.out.println();
        System.out.println("Why colocation matters for JOINs:");
        System.out.println("  Without colocation: data shuffled across network (slow)");
        System.out.println("  With colocation: join executed locally on each node (fast)");
        System.out.println();
    }

    private static void demonstrateZoneManagement(IgniteClient client) {
        System.out.println("=== Zone Management Demo ===\n");

        try {
            // 1. Create two zones with different settings
            System.out.println("Creating demo zones...");
            client.sql().executeScript(
                "DROP TABLE IF EXISTS hot_test_table;" +
                "DROP TABLE IF EXISTS cold_test_table;" +
                "DROP ZONE IF EXISTS hot_demo_zone;" +
                "DROP ZONE IF EXISTS cold_demo_zone");

            client.sql().executeScript(
                "CREATE ZONE hot_demo_zone WITH replicas=3, partitions=256, storage_profiles='default'");
            System.out.println("  Created hot_demo_zone (replicas=3, partitions=256)");

            client.sql().executeScript(
                "CREATE ZONE cold_demo_zone WITH replicas=1, partitions=32, storage_profiles='default'");
            System.out.println("  Created cold_demo_zone (replicas=1, partitions=32)\n");

            // 2. Query SYSTEM.ZONES to show both zones
            System.out.println("Querying SYSTEM.ZONES (before ALTER):");
            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT * FROM SYSTEM.ZONES WHERE NAME IN ('HOT_DEMO_ZONE', 'COLD_DEMO_ZONE') ORDER BY NAME")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("    Zone: %s, Partitions: %d, Replicas: %d%n",
                        row.stringValue("NAME"),
                        row.intValue("PARTITIONS"),
                        row.intValue("REPLICAS"));
                }
            }
            System.out.println();

            // 3. ALTER ZONE to change replicas
            System.out.println("Altering hot_demo_zone: replicas 3 -> 2");
            client.sql().executeScript(
                "ALTER ZONE hot_demo_zone SET replicas=2");
            System.out.println("  ALTER ZONE completed.\n");

            // 4. Query SYSTEM.ZONES again to verify the change
            System.out.println("Querying SYSTEM.ZONES (after ALTER):");
            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT * FROM SYSTEM.ZONES WHERE NAME IN ('HOT_DEMO_ZONE', 'COLD_DEMO_ZONE') ORDER BY NAME")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("    Zone: %s, Partitions: %d, Replicas: %d%n",
                        row.stringValue("NAME"),
                        row.intValue("PARTITIONS"),
                        row.intValue("REPLICAS"));
                }
            }
            System.out.println();

            // 5. Create a test table in each zone, verify via SYSTEM.TABLES, then drop them
            System.out.println("Creating test tables in each zone...");
            client.sql().executeScript(
                "CREATE TABLE hot_test_table (id INT PRIMARY KEY, val VARCHAR) WITH PRIMARY_ZONE='HOT_DEMO_ZONE'");
            client.sql().executeScript(
                "CREATE TABLE cold_test_table (id INT PRIMARY KEY, val VARCHAR) WITH PRIMARY_ZONE='COLD_DEMO_ZONE'");
            System.out.println("  Created hot_test_table in HOT_DEMO_ZONE");
            System.out.println("  Created cold_test_table in COLD_DEMO_ZONE\n");

            System.out.println("Verifying tables via SYSTEM.TABLES:");
            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT NAME, ZONE_ID FROM SYSTEM.TABLES " +
                    "WHERE NAME IN ('HOT_TEST_TABLE', 'COLD_TEST_TABLE') ORDER BY NAME")) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("    Table: %s, Zone ID: %d%n",
                        row.stringValue("NAME"),
                        row.intValue("ZONE_ID"));
                }
            }
            System.out.println();

            // Drop test tables
            System.out.println("Dropping test tables...");
            client.sql().executeScript(
                "DROP TABLE IF EXISTS hot_test_table;" +
                "DROP TABLE IF EXISTS cold_test_table");
            System.out.println("  Dropped hot_test_table and cold_test_table\n");

            // 6. Drop the demo zones
            System.out.println("Dropping demo zones...");
            client.sql().executeScript(
                "DROP ZONE IF EXISTS hot_demo_zone;" +
                "DROP ZONE IF EXISTS cold_demo_zone");
            System.out.println("  Dropped hot_demo_zone and cold_demo_zone\n");

        } catch (Exception e) {
            System.out.println("  Zone management error: " + e.getMessage());
        }
    }

    private static void printColocationComparison() {
        System.out.println("=== Data Colocation Comparison ===\n");

        System.out.println("  | Aspect           | Ignite 2.x                    | Ignite 3.x                |");
        System.out.println("  |------------------|-------------------------------|---------------------------|");
        System.out.println("  | Colocation       | @AffinityKeyMapped annotation | COLOCATE BY in DDL        |");
        System.out.println("  | Placement control| Affinity function per cache   | Distribution Zones        |");
        System.out.println("  | Storage per table| Same engine for all           | Different per zone profile|");
        System.out.println("  | Replication      | Backups per cache             | Replicas per zone         |");
        System.out.println("  | Partitions       | Per cache config              | Per zone config           |");
        System.out.println("  | Tiered storage   | Not supported                 | Hot/warm/cold zones       |");
        System.out.println("  | Zone management  | N/A                           | CREATE/ALTER/DROP ZONE    |");
        System.out.println("  | Language-agnostic| No (Java annotations)         | Yes (SQL DDL)             |");
        System.out.println();
    }
}
