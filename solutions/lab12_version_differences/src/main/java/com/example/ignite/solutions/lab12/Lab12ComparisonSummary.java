package com.example.ignite.solutions.lab12;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;

/**
 * Lab 12 Exercise 17: Complete Comparison Summary
 *
 * Runs a comprehensive demonstration of Ignite 3.x features and prints
 * a side-by-side comparison with Ignite 2.x covering all major areas:
 * architecture, API, configuration, transactions, compute, colocation,
 * monitoring, and migration guidance.
 *
 * Prerequisites: Running Ignite 3.x cluster
 */
public class Lab12ComparisonSummary {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║   Lab 12: Apache Ignite 2.x vs 3.x - Complete Comparison    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            System.out.println("Connected to Ignite 3.x cluster!\n");

            runLiveDemo(client);
            printArchitectureComparison();
            printApiComparison();
            printFeatureMatrix();
            printMigrationChecklist();
            printDecisionMatrix();

            // Cleanup demo tables
            client.sql().executeScript(
                "DROP TABLE IF EXISTS summary_demo;" +
                "DROP ZONE IF EXISTS summary_zone");

        } catch (Exception e) {
            System.err.println("Could not connect to Ignite 3.x cluster.");
            System.err.println("Running in offline mode (comparison tables only).\n");

            printArchitectureComparison();
            printApiComparison();
            printFeatureMatrix();
            printMigrationChecklist();
            printDecisionMatrix();
        }
    }

    /**
     * Live demo showcasing key 3.x features
     */
    private static void runLiveDemo(IgniteClient client) {
        System.out.println("=== Live Demo: Key Ignite 3.x Features ===\n");

        // 1. Create zone
        System.out.println("1. Distribution Zone:");
        try {
            client.sql().executeScript(
                "CREATE ZONE IF NOT EXISTS summary_zone WITH " +
                "replicas=1, partitions=10, storage_profiles='default'");
            System.out.println("   Created zone 'summary_zone' (replicas=1, partitions=10)");
        } catch (Exception e) {
            System.out.println("   Zone: " + e.getMessage());
        }

        // 2. Create table
        System.out.println("\n2. Schema-First Table Creation:");
        try {
            client.sql().executeScript(
                "CREATE TABLE IF NOT EXISTS summary_demo (" +
                "  id INT PRIMARY KEY," +
                "  name VARCHAR," +
                "  value DOUBLE" +
                ") WITH PRIMARY_ZONE='summary_zone'");
            System.out.println("   Created table in 'summary_zone' via SQL DDL");
        } catch (Exception e) {
            System.out.println("   Table: " + e.getMessage());
        }

        // 3. Table API operations
        System.out.println("\n3. Table API (RecordView):");
        try {
            Table table = client.tables().table("SUMMARY_DEMO");
            RecordView<Tuple> view = table.recordView();

            view.upsert(null, Tuple.create().set("id", 1).set("name", "Alpha").set("value", 10.5));
            view.upsert(null, Tuple.create().set("id", 2).set("name", "Beta").set("value", 20.3));
            view.upsert(null, Tuple.create().set("id", 3).set("name", "Gamma").set("value", 30.7));

            Tuple result = view.get(null, Tuple.create().set("id", 1));
            System.out.println("   Inserted 3 records, retrieved: " + result.stringValue("name"));
        } catch (Exception e) {
            System.out.println("   Table API: " + e.getMessage());
        }

        // 4. SQL query
        System.out.println("\n4. SQL Query:");
        try (ResultSet<SqlRow> rs = client.sql().execute(null,
                "SELECT name, value FROM summary_demo ORDER BY value DESC")) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                System.out.printf("   %s = %.1f%n", row.stringValue("NAME"), row.doubleValue("VALUE"));
            }
        } catch (Exception e) {
            System.out.println("   SQL: " + e.getMessage());
        }

        // 5. Transaction
        System.out.println("\n5. Transaction (runInTransaction):");
        try {
            Table table = client.tables().table("SUMMARY_DEMO");
            RecordView<Tuple> view = table.recordView();

            client.transactions().runInTransaction(tx -> {
                Tuple rec = view.get(tx, Tuple.create().set("id", 1));
                double newVal = rec.doubleValue("value") + 100.0;
                view.upsert(tx, Tuple.create().set("id", 1).set("name", "Alpha").set("value", newVal));
            });

            Tuple updated = view.get(null, Tuple.create().set("id", 1));
            System.out.println("   Updated id=1 value to " + updated.doubleValue("value") + " in transaction");
        } catch (Exception e) {
            System.out.println("   Transaction: " + e.getMessage());
        }

        // 6. Connection info
        System.out.println("\n6. Cluster Connection:");
        client.connections().forEach(conn -> {
            System.out.println("   Node: " + conn.name() + " @ " + conn.address());
        });

        System.out.println();
    }

    private static void printArchitectureComparison() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║          Architecture Comparison                     ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        String fmt = "  %-22s %-28s %-28s%n";
        System.out.printf(fmt, "Component", "Ignite 2.x", "Ignite 3.x");
        System.out.printf(fmt, "──────────────────────", "────────────────────────────", "────────────────────────────");
        System.out.printf(fmt, "Discovery", "Ring (TcpDiscoverySpi)", "SWIM gossip protocol");
        System.out.printf(fmt, "Consensus", "Custom protocol", "RAFT");
        System.out.printf(fmt, "Storage", "Single B+ tree engine", "Pluggable (aimem/aipersist/");
        System.out.printf(fmt, "", "", "rocksdb)");
        System.out.printf(fmt, "Configuration", "Spring XML / Java API", "HOCON / CLI / REST");
        System.out.printf(fmt, "SQL Engine", "H2 + Calcite", "Calcite only");
        System.out.printf(fmt, "Client Model", "Thick + Thin", "Thin only (unified)");
        System.out.printf(fmt, "Schema Approach", "Cache-first (Java)", "Schema-first (SQL DDL)");
        System.out.printf(fmt, "Data Model", "Cache API (key-value)", "Table API (views)");
        System.out.printf(fmt, "Cluster Init", "Auto-discovery", "Explicit init required");
        System.out.printf(fmt, "Metadata", "Distributed cache", "RAFT metastorage");
        System.out.println();
    }

    private static void printApiComparison() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║               API Comparison                         ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        String fmt = "  %-22s %-28s %-28s%n";
        System.out.printf(fmt, "Operation", "Ignite 2.x", "Ignite 3.x");
        System.out.printf(fmt, "──────────────────────", "────────────────────────────", "────────────────────────────");
        System.out.printf(fmt, "Connect", "Ignition.start(cfg)", "IgniteClient.builder().build()");
        System.out.printf(fmt, "Define Schema", "@QuerySqlField", "CREATE TABLE (SQL DDL)");
        System.out.printf(fmt, "Get Table/Cache", "ignite.cache(name)", "client.tables().table(name)");
        System.out.printf(fmt, "CRUD", "cache.put/get/remove", "view.upsert/get/delete");
        System.out.printf(fmt, "SQL Query", "SqlFieldsQuery", "client.sql().execute()");
        System.out.printf(fmt, "SQL Result", "List<List<?>>", "ResultSet<SqlRow>");
        System.out.printf(fmt, "Binary Access", "BinaryObject", "Tuple");
        System.out.printf(fmt, "POJO Mapping", "@QuerySqlField + cache", "recordView(MyClass.class)");
        System.out.printf(fmt, "Async", "IgniteFuture", "CompletableFuture");
        System.out.printf(fmt, "Transaction", "txStart(concurrency,isol)", "transactions().begin()");
        System.out.printf(fmt, "Auto Transaction", "N/A", "runInTransaction(consumer)");
        System.out.printf(fmt, "Compute", "compute().call(callable)", "compute().execute(target,desc)");
        System.out.printf(fmt, "Colocation", "@AffinityKeyMapped", "COLOCATE BY (SQL DDL)");
        System.out.printf(fmt, "Data Placement", "Affinity function", "Distribution Zones");
        System.out.println();
    }

    private static void printFeatureMatrix() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║             Feature Availability Matrix               ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        String fmt = "  %-30s %-12s %-12s%n";
        System.out.printf(fmt, "Feature", "2.x", "3.x");
        System.out.printf(fmt, "──────────────────────────────", "────────────", "────────────");
        System.out.printf(fmt, "Key-Value API", "Yes", "Yes (Table)");
        System.out.printf(fmt, "SQL Queries", "Yes", "Yes");
        System.out.printf(fmt, "Transactions", "Yes", "Yes");
        System.out.printf(fmt, "Compute Jobs", "Yes", "Yes");
        System.out.printf(fmt, "MapReduce", "Yes", "No");
        System.out.printf(fmt, "Service Grid", "Yes", "No");
        System.out.printf(fmt, "Continuous Queries", "Yes", "No");
        System.out.printf(fmt, "Near Cache", "Yes", "No");
        System.out.printf(fmt, "Thick Client", "Yes", "No");
        System.out.printf(fmt, "Peer Class Loading", "Yes", "No");
        System.out.printf(fmt, "Distribution Zones", "No", "Yes");
        System.out.printf(fmt, "COLOCATE BY", "No", "Yes");
        System.out.printf(fmt, "Mixed SQL+KV Transactions", "No", "Yes");
        System.out.printf(fmt, "Dynamic Configuration", "No", "Yes");
        System.out.printf(fmt, "Pluggable Storage Engines", "No", "Yes");
        System.out.printf(fmt, "Deployment Units", "No", "Yes");
        System.out.printf(fmt, "Native OpenMetrics", "No", "Yes");
        System.out.printf(fmt, "Built-in REST API", "No", "Yes");
        System.out.printf(fmt, "RAFT Consensus", "No", "Yes");
        System.out.printf(fmt, "Strong Consistency", "Optional", "Default");
        System.out.println();
    }

    private static void printMigrationChecklist() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║          Migration Checklist: 2.x → 3.x              ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        System.out.println("  [ ] Replace Ignition.start() with IgniteClient.builder()");
        System.out.println("  [ ] Convert CacheConfiguration to CREATE TABLE DDL");
        System.out.println("  [ ] Replace cache.put/get with view.upsert/get");
        System.out.println("  [ ] Convert @QuerySqlField POJOs to SQL DDL schemas");
        System.out.println("  [ ] Replace SqlFieldsQuery with client.sql().execute()");
        System.out.println("  [ ] Convert BinaryObject usage to Tuple API");
        System.out.println("  [ ] Replace IgniteFuture with CompletableFuture");
        System.out.println("  [ ] Simplify transactions (remove concurrency/isolation params)");
        System.out.println("  [ ] Replace @AffinityKeyMapped with COLOCATE BY");
        System.out.println("  [ ] Convert cache backups/partitions to Distribution Zones");
        System.out.println("  [ ] Replace Spring XML config with HOCON");
        System.out.println("  [ ] Update monitoring from JMX to REST/OpenMetrics");
        System.out.println("  [ ] Replace control.sh with ignite3 CLI");
        System.out.println("  [ ] Handle MapReduce gaps (manual split + client-side reduce)");
        System.out.println("  [ ] Handle Service Grid gaps (external service deployment)");
        System.out.println("  [ ] Handle Continuous Query gaps (polling or change data capture)");
        System.out.println("  [ ] Set up deployment units for compute jobs");
        System.out.println("  [ ] Update Maven dependencies (ignite-client:3.x)");
        System.out.println("  [ ] Verify Java version (11+ for client, 17+ for embedded)");
        System.out.println();
    }

    private static void printDecisionMatrix() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║           Decision Matrix: When to Use Which          ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        System.out.println("  Use Ignite 2.x when you need:");
        System.out.println("    - MapReduce (ComputeTaskAdapter)");
        System.out.println("    - Service Grid (deployed services)");
        System.out.println("    - Continuous Queries (real-time notifications)");
        System.out.println("    - Near Cache (client-side caching)");
        System.out.println("    - Thick/embedded client mode");
        System.out.println("    - Mature, battle-tested production deployment");
        System.out.println();

        System.out.println("  Use Ignite 3.x when you need:");
        System.out.println("    - Strong consistency by default (RAFT)");
        System.out.println("    - Mixed SQL + KV transactions");
        System.out.println("    - Pluggable storage engines");
        System.out.println("    - Cloud-native monitoring (OpenMetrics)");
        System.out.println("    - Dynamic configuration updates");
        System.out.println("    - Schema-first development approach");
        System.out.println("    - Simplified transaction model");
        System.out.println("    - Tiered storage with distribution zones");
        System.out.println("    - Future-proof architecture");
        System.out.println();

        System.out.println("  Key Decision Factors:");
        System.out.println("    1. Feature requirements - check the matrix above");
        System.out.println("    2. Consistency requirements - 3.x is stronger by default");
        System.out.println("    3. Operational model - 3.x is more cloud-native");
        System.out.println("    4. Team expertise - 2.x has more community resources");
        System.out.println("    5. New vs existing - new projects favor 3.x");
        System.out.println();
    }
}
