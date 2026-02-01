package com.example.ignite.solutions.lab13;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;

/**
 * Lab 13 Exercises 3-5: Ignite 3.x Configuration
 *
 * Demonstrates:
 * - HOCON-based configuration (explained)
 * - Dynamic configuration via CLI
 * - Distribution zone configuration via SQL DDL
 * - Storage profiles and engines
 * - Contrast with 2.x Spring XML / Java programmatic config
 *
 * Prerequisites: Running Ignite 3.x cluster
 */
public class Lab13Configuration {

    public static void main(String[] args) {
        System.out.println("=== Lab 13: Ignite 3.x Configuration ===\n");

        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            explainHoconConfig();
            explainDynamicConfig();
            demonstrateDistributionZoneConfig(client);
            explainStorageProfiles();
            printConfigComparison();

        } catch (Exception e) {
            System.err.println("Could not connect to Ignite 3.x cluster.");
            e.printStackTrace();
        }
    }

    /**
     * Exercise 3: HOCON configuration
     */
    private static void explainHoconConfig() {
        System.out.println("=== Exercise 3: HOCON Configuration (3.x) ===\n");

        System.out.println("Ignite 3.x uses HOCON (Human-Optimized Config Object Notation):\n");

        System.out.println("  Example node config (etc/ignite3-config.conf):");
        System.out.println("  {");
        System.out.println("    network {");
        System.out.println("      port: 3344");
        System.out.println("      nodeFinder {");
        System.out.println("        netClusterNodes: [");
        System.out.println("          \"localhost:3344\",");
        System.out.println("          \"localhost:3345\"");
        System.out.println("        ]");
        System.out.println("      }");
        System.out.println("    }");
        System.out.println("    clientConnector {");
        System.out.println("      port: 10800");
        System.out.println("    }");
        System.out.println("    rest {");
        System.out.println("      port: 10300");
        System.out.println("    }");
        System.out.println("  }");
        System.out.println();

        System.out.println("Contrast with Ignite 2.x:");
        System.out.println("  2.x: IgniteConfiguration Java object or Spring XML");
        System.out.println("  2.x: Static at startup, requires restart to change");
        System.out.println("  3.x: HOCON file, plus dynamic updates via CLI/REST");
        System.out.println();

        // --- Live REST API call: fetch cluster configuration ---
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:10300/management/v1/configuration/cluster"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            String preview = body.substring(0, Math.min(body.length(), 300));
            System.out.println("Live cluster configuration (first 300 chars):");
            System.out.println(preview);
            System.out.println("...");
            System.out.println();
        } catch (Exception e) {
            System.out.println("  (Could not fetch cluster config via REST: " + e.getMessage() + ")");
            System.out.println();
        }
    }

    /**
     * Exercise 4: Dynamic configuration
     */
    private static void explainDynamicConfig() {
        System.out.println("=== Exercise 4: Dynamic Configuration via CLI (3.x) ===\n");

        System.out.println("View configuration:");
        System.out.println("  ignite3 cluster config show");
        System.out.println("  ignite3 cluster config show --selector=gc");
        System.out.println("  ignite3 node config show");
        System.out.println();

        System.out.println("Update configuration dynamically (no restart!):");
        System.out.println("  ignite3 cluster config update \\");
        System.out.println("    '{gc.lowWatermark.dataAvailabilityTime: 600000}'");
        System.out.println();

        System.out.println("  ignite3 node config update \\");
        System.out.println("    '{clientConnector.idleTimeout: 60000}'");
        System.out.println();

        System.out.println("Key difference from 2.x:");
        System.out.println("  2.x: Most config is static, set before Ignition.start()");
        System.out.println("  3.x: Cluster-level and node-level config can be changed live");
        System.out.println("  3.x: Changes propagate via RAFT metastorage");
        System.out.println();

        // --- Live REST API call: fetch node configuration ---
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:10300/management/v1/configuration/node"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            String preview = body.substring(0, Math.min(body.length(), 300));
            System.out.println("Live node configuration (first 300 chars):");
            System.out.println(preview);
            System.out.println("...");
            System.out.println();
        } catch (Exception e) {
            System.out.println("  (Could not fetch node config via REST: " + e.getMessage() + ")");
            System.out.println();
        }
    }

    /**
     * Exercise 5: Distribution zone configuration via SQL
     */
    private static void demonstrateDistributionZoneConfig(IgniteClient client) {
        System.out.println("=== Exercise 5: Distribution Zone Config (3.x) ===\n");

        System.out.println("Distribution zones control data placement and replication.\n");

        System.out.println("Creating distribution zones via SQL DDL:");
        System.out.println();

        // Show the SQL (actual execution requires a running cluster)
        System.out.println("  -- Create a zone for frequently accessed data");
        System.out.println("  CREATE ZONE IF NOT EXISTS hot_zone WITH");
        System.out.println("    replicas=3, partitions=256,");
        System.out.println("    storage_profiles='default';");
        System.out.println();
        System.out.println("  -- Create a zone for archival data");
        System.out.println("  CREATE ZONE IF NOT EXISTS cold_zone WITH");
        System.out.println("    replicas=1, partitions=64,");
        System.out.println("    storage_profiles='default';");
        System.out.println();

        // Attempt to create zones on the live cluster
        try {
            client.sql().executeScript(
                "CREATE ZONE IF NOT EXISTS demo_zone WITH replicas=1, partitions=25, storage_profiles='default'");
            System.out.println("  Successfully created 'demo_zone' on the cluster!");

            // List zones via system view
            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT * FROM SYSTEM.ZONES")) {
                System.out.println("\n  Available Zones:");
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.println("    Zone: " + row.stringValue("NAME"));
                }
            }
        } catch (Exception e) {
            System.out.println("  (Zone creation requires a running cluster: " + e.getMessage() + ")");
        }

        System.out.println();
        System.out.println("Contrast with 2.x:");
        System.out.println("  2.x: Per-cache configuration (backups, partitions, affinity function)");
        System.out.println("  3.x: Distribution zones group tables with shared placement rules");
        System.out.println("  3.x: Zones can have different storage engines per profile");
        System.out.println();
    }

    private static void explainStorageProfiles() {
        System.out.println("=== Storage Profiles (3.x) ===\n");

        System.out.println("Ignite 3.x supports multiple storage engines:\n");
        System.out.println("  aimem    - Volatile in-memory (fastest, no persistence)");
        System.out.println("  aipersist - Persistent B+ tree (durable, on-disk)");
        System.out.println("  rocksdb  - RocksDB-based (LSM tree, good for write-heavy)");
        System.out.println();

        System.out.println("  Storage is configured at the zone level:");
        System.out.println("  CREATE ZONE fast_zone WITH storage_profiles='aimem';");
        System.out.println("  CREATE ZONE durable_zone WITH storage_profiles='aipersist';");
        System.out.println();

        System.out.println("Contrast with 2.x:");
        System.out.println("  2.x: Single storage engine, DataRegionConfiguration for memory/persistence");
        System.out.println("  2.x: All caches share the same storage engine");
        System.out.println("  3.x: Different tables can use different engines via zone profiles");
        System.out.println();
    }

    private static void printConfigComparison() {
        System.out.println("=== Configuration Comparison ===\n");

        System.out.println("  | Aspect              | Ignite 2.x                  | Ignite 3.x                |");
        System.out.println("  |---------------------|-----------------------------|---------------------------|");
        System.out.println("  | Format              | Spring XML / Java API       | HOCON / CLI / REST        |");
        System.out.println("  | Scope               | Static at startup           | Dynamic (live updates)    |");
        System.out.println("  | Cache/Table config  | CacheConfiguration object   | CREATE TABLE DDL          |");
        System.out.println("  | Data placement      | Per-cache affinity config   | Distribution Zones        |");
        System.out.println("  | Storage engine      | Single engine for all       | Per-zone storage profiles |");
        System.out.println("  | Memory config       | DataRegionConfiguration     | Storage profile settings  |");
        System.out.println("  | Persistence         | DataStorageConfiguration    | Storage engine choice     |");
        System.out.println("  | Cluster management  | control.sh                  | ignite3 CLI               |");
        System.out.println();
    }
}
