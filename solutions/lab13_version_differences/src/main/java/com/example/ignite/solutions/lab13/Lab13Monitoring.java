package com.example.ignite.solutions.lab13;

import org.apache.ignite.client.IgniteClient;

/**
 * Lab 13 Exercises 15-16: Ignite 3.x Monitoring and Metrics
 *
 * Demonstrates:
 * - REST API monitoring (built-in, no extra modules)
 * - CLI monitoring commands
 * - OpenMetrics / Prometheus integration
 * - System views via SQL
 * - Contrast with 2.x JMX-centric monitoring
 *
 * Prerequisites: Running Ignite 3.x cluster
 */
public class Lab13Monitoring {

    public static void main(String[] args) {
        System.out.println("=== Lab 13: Ignite 3.x Monitoring & Metrics ===\n");

        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            demonstrateRestApiMonitoring();
            demonstrateCliMonitoring();
            demonstrateOpenMetrics();
            demonstrateSystemViews(client);
            demonstrateClientMetrics(client);
            printMonitoringComparison();

        } catch (Exception e) {
            System.err.println("Could not connect to Ignite 3.x cluster.");
            e.printStackTrace();
        }
    }

    /**
     * Exercise 15: REST API monitoring
     */
    private static void demonstrateRestApiMonitoring() {
        System.out.println("=== Exercise 15: REST API Monitoring (3.x) ===\n");

        System.out.println("Ignite 3.x has a built-in REST API (no extra modules):\n");

        System.out.println("  Cluster State:");
        System.out.println("  curl http://localhost:10300/management/v1/cluster/state");
        System.out.println();

        System.out.println("  Cluster Topology:");
        System.out.println("  curl http://localhost:10300/management/v1/cluster/topology/physical");
        System.out.println("  curl http://localhost:10300/management/v1/cluster/topology/logical");
        System.out.println();

        System.out.println("  Node Configuration:");
        System.out.println("  curl http://localhost:10300/management/v1/configuration/node");
        System.out.println();

        System.out.println("  Cluster Configuration:");
        System.out.println("  curl http://localhost:10300/management/v1/configuration/cluster");
        System.out.println();

        System.out.println("  Node Version:");
        System.out.println("  curl http://localhost:10300/management/v1/node/version");
        System.out.println();

        System.out.println("Contrast with 2.x:");
        System.out.println("  2.x: REST API requires ignite-rest-http module (optional)");
        System.out.println("  2.x: curl http://localhost:8080/ignite?cmd=top");
        System.out.println("  3.x: Built-in REST API, no extra dependencies");
        System.out.println();
    }

    /**
     * CLI monitoring
     */
    private static void demonstrateCliMonitoring() {
        System.out.println("=== CLI Monitoring (3.x) ===\n");

        System.out.println("The ignite3 CLI provides comprehensive monitoring:\n");

        System.out.println("  Cluster status:");
        System.out.println("  ignite3 cluster status");
        System.out.println("  ignite3 cluster state");
        System.out.println();

        System.out.println("  Topology:");
        System.out.println("  ignite3 cluster topology physical");
        System.out.println("  ignite3 cluster topology logical");
        System.out.println();

        System.out.println("  Node status:");
        System.out.println("  ignite3 node status");
        System.out.println();

        System.out.println("  Configuration:");
        System.out.println("  ignite3 cluster config show");
        System.out.println("  ignite3 node config show");
        System.out.println("  ignite3 cluster config show --selector=gc");
        System.out.println();

        System.out.println("  Metric sources:");
        System.out.println("  ignite3 node metric source list");
        System.out.println("  ignite3 node metric source enable <sourceName>");
        System.out.println();

        System.out.println("Contrast with 2.x:");
        System.out.println("  2.x: control.sh --state, control.sh --baseline");
        System.out.println("  3.x: ignite3 CLI with richer subcommands");
        System.out.println();
    }

    /**
     * Exercise 16: OpenMetrics / Prometheus
     */
    private static void demonstrateOpenMetrics() {
        System.out.println("=== Exercise 16: OpenMetrics / Prometheus (3.x) ===\n");

        System.out.println("Ignite 3.x supports native OpenMetrics format:\n");

        System.out.println("  Enable metrics export:");
        System.out.println("  ignite3 node metric source enable jvm");
        System.out.println("  ignite3 node metric source enable client.handler");
        System.out.println();

        System.out.println("  Prometheus scrape configuration:");
        System.out.println("  scrape_configs:");
        System.out.println("    - job_name: 'ignite3'");
        System.out.println("      metrics_path: '/management/v1/metric/node'");
        System.out.println("      static_configs:");
        System.out.println("        - targets: ['localhost:10300']");
        System.out.println();

        System.out.println("  Available metric groups:");
        System.out.println("    jvm           - JVM memory, GC, threads");
        System.out.println("    client.handler - Client connections, requests");
        System.out.println("    sql           - SQL query metrics");
        System.out.println("    compute       - Compute job metrics");
        System.out.println("    raft          - RAFT consensus metrics");
        System.out.println();

        System.out.println("Contrast with 2.x:");
        System.out.println("  2.x: JMX MBeans, requires Prometheus JMX exporter as sidecar");
        System.out.println("  3.x: Native OpenMetrics endpoint, direct Prometheus scraping");
        System.out.println("  3.x: No JMX exporter needed, simpler Grafana integration");
        System.out.println();
    }

    /**
     * System views via SQL
     */
    private static void demonstrateSystemViews(IgniteClient client) {
        System.out.println("=== System Views via SQL (3.x) ===\n");

        System.out.println("Query cluster metadata via system views:\n");

        try {
            // List tables
            System.out.println("  SYSTEM.TABLES:");
            try (var rs = client.sql().execute(null, "SELECT * FROM SYSTEM.TABLES")) {
                while (rs.hasNext()) {
                    var row = rs.next();
                    System.out.println("    Table: " + row.stringValue("TABLE_NAME"));
                }
            }
        } catch (Exception e) {
            System.out.println("  (System views require running cluster: " + e.getMessage() + ")");
        }

        System.out.println();
        System.out.println("Available system views:");
        System.out.println("  SYSTEM.TABLES       - All tables");
        System.out.println("  SYSTEM.ZONES        - Distribution zones");
        System.out.println("  SYSTEM.INDEXES      - All indexes");
        System.out.println("  SYSTEM.NODES        - Cluster nodes");
        System.out.println("  SYSTEM.NODE_METRICS - Node-level metrics");
        System.out.println();

        System.out.println("Contrast with 2.x:");
        System.out.println("  2.x: Programmatic APIs (cluster().metrics(), cache.metrics())");
        System.out.println("  2.x: JMX MBeans for detailed metrics");
        System.out.println("  3.x: SQL system views + REST API + CLI");
        System.out.println();
    }

    /**
     * Client-side metrics
     */
    private static void demonstrateClientMetrics(IgniteClient client) {
        System.out.println("=== Client-Side Metrics (3.x) ===\n");

        System.out.println("The IgniteClient tracks connection metrics:\n");
        System.out.println("  Active connections: " + client.connections().size());
        client.connections().forEach(conn -> {
            System.out.println("    Node: " + conn.name());
            System.out.println("    Address: " + conn.address());
        });
        System.out.println();

        System.out.println("Enable client metrics via builder:");
        System.out.println("  IgniteClient client = IgniteClient.builder()");
        System.out.println("      .addresses(\"localhost:10800\")");
        System.out.println("      .metricsEnabled(true)");
        System.out.println("      .build();");
        System.out.println();
        System.out.println("Client metrics include:");
        System.out.println("  - Connection count, handshakes, failures");
        System.out.println("  - Requests sent, received, failed");
        System.out.println("  - Bytes sent/received");
        System.out.println("  - Data streamer metrics");
        System.out.println();
    }

    private static void printMonitoringComparison() {
        System.out.println("=== Monitoring Comparison ===\n");

        System.out.println("  | Aspect              | Ignite 2.x              | Ignite 3.x             |");
        System.out.println("  |---------------------|-------------------------|------------------------|");
        System.out.println("  | Primary interface   | JMX                     | REST API               |");
        System.out.println("  | REST API            | Optional (extra module) | Built-in               |");
        System.out.println("  | CLI tool            | control.sh              | ignite3                |");
        System.out.println("  | Prometheus          | Via JMX exporter        | Native OpenMetrics     |");
        System.out.println("  | Programmatic        | cluster().metrics()     | System views (SQL)     |");
        System.out.println("  | Cache metrics       | CacheMetrics API        | Via system views/REST  |");
        System.out.println("  | JMX                 | Rich MBean tree         | Limited/optional       |");
        System.out.println("  | Grafana             | Via JMX exporter        | Direct REST scraping   |");
        System.out.println("  | System views        | Not available           | SQL SYSTEM schema      |");
        System.out.println();
    }
}
