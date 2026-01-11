package com.example.ignite.solutions.lab12;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.*;

/**
 * Lab 12 Challenge Exercise 1: Production Health Checker
 *
 * Demonstrates:
 * - Cluster health monitoring
 * - Node metrics collection
 * - Cache performance metrics
 * - Health check thresholds and alerting
 */
public class Lab12HealthChecker {

    // Health check thresholds
    private static final int MIN_NODES = 3;
    private static final double MAX_HEAP_USAGE_PERCENT = 85.0;
    private static final double MAX_CPU_LOAD_PERCENT = 80.0;
    private static final long MAX_AVG_GET_TIME_NS = 1_000_000; // 1ms

    public static void main(String[] args) {
        System.out.println("=== Production Health Checker ===\n");

        IgniteConfiguration cfg = createConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            // Activate if needed
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                ignite.cluster().state(ClusterState.ACTIVE);
            }

            // Create test cache
            CacheConfiguration<Long, String> cacheCfg = new CacheConfiguration<>("health-check-cache");
            cacheCfg.setStatisticsEnabled(true);
            IgniteCache<Long, String> cache = ignite.getOrCreateCache(cacheCfg);

            // Add test data
            for (long i = 0; i < 100; i++) {
                cache.put(i, "value-" + i);
            }

            // Read data to generate metrics
            for (long i = 0; i < 100; i++) {
                cache.get(i);
            }

            // Run health checks
            HealthCheckResult result = runHealthChecks(ignite);

            // Print results
            printHealthReport(result);

            // Cleanup
            ignite.destroyCache("health-check-cache");

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static HealthCheckResult runHealthChecks(Ignite ignite) {
        HealthCheckResult result = new HealthCheckResult();

        // Check 1: Cluster State
        ClusterState state = ignite.cluster().state();
        result.addCheck("Cluster State",
            state == ClusterState.ACTIVE ? "PASS" : "FAIL",
            "Current: " + state);

        // Check 2: Node Count
        int nodeCount = ignite.cluster().nodes().size();
        result.addCheck("Node Count",
            nodeCount >= MIN_NODES ? "PASS" : "WARN",
            "Nodes: " + nodeCount + " (min: " + MIN_NODES + ")");

        // Check 3: No Client Nodes Only
        long serverNodes = ignite.cluster().nodes().stream()
            .filter(n -> !n.isClient()).count();
        result.addCheck("Server Nodes",
            serverNodes > 0 ? "PASS" : "FAIL",
            "Server nodes: " + serverNodes);

        // Check 4: Node Health (per node)
        for (ClusterNode node : ignite.cluster().nodes()) {
            ClusterMetrics metrics = node.metrics();

            // Heap usage
            double heapUsage = (double) metrics.getHeapMemoryUsed() /
                              metrics.getHeapMemoryMaximum() * 100;
            result.addCheck("Heap Usage (" + node.consistentId() + ")",
                heapUsage < MAX_HEAP_USAGE_PERCENT ? "PASS" : "WARN",
                String.format("%.1f%% (max: %.1f%%)", heapUsage, MAX_HEAP_USAGE_PERCENT));

            // CPU load
            double cpuLoad = metrics.getCurrentCpuLoad() * 100;
            result.addCheck("CPU Load (" + node.consistentId() + ")",
                cpuLoad < MAX_CPU_LOAD_PERCENT ? "PASS" : "WARN",
                String.format("%.1f%% (max: %.1f%%)", cpuLoad, MAX_CPU_LOAD_PERCENT));

            // Uptime
            long uptimeMinutes = metrics.getUpTime() / 60000;
            result.addCheck("Uptime (" + node.consistentId() + ")",
                "INFO",
                uptimeMinutes + " minutes");
        }

        // Check 5: Cache Health
        for (String cacheName : ignite.cacheNames()) {
            IgniteCache<?, ?> cache = ignite.cache(cacheName);
            if (cache != null) {
                CacheMetrics cacheMetrics = cache.localMetrics();

                // Check get time
                float avgGetTime = cacheMetrics.getAverageGetTime();
                result.addCheck("Cache Get Time (" + cacheName + ")",
                    avgGetTime < MAX_AVG_GET_TIME_NS / 1_000_000.0 ? "PASS" : "WARN",
                    String.format("%.2f ms", avgGetTime));

                // Check hit rate (if applicable)
                float hitRate = cacheMetrics.getCacheHitPercentage();
                result.addCheck("Cache Hit Rate (" + cacheName + ")",
                    hitRate > 80 ? "PASS" : (hitRate > 50 ? "WARN" : "INFO"),
                    String.format("%.1f%%", hitRate));

                // Cache size
                long cacheSize = cache.sizeLong();
                result.addCheck("Cache Size (" + cacheName + ")",
                    "INFO",
                    cacheSize + " entries");
            }
        }

        // Check 6: Baseline Consistency
        boolean baselineConsistent = ignite.cluster().currentBaselineTopology() != null;
        result.addCheck("Baseline Topology",
            baselineConsistent ? "PASS" : "WARN",
            baselineConsistent ? "Configured" : "Not configured");

        return result;
    }

    private static void printHealthReport(HealthCheckResult result) {
        System.out.println("========================================");
        System.out.println("       HEALTH CHECK REPORT");
        System.out.println("========================================\n");

        int passed = 0, warned = 0, failed = 0;

        for (HealthCheck check : result.getChecks()) {
            String icon;
            switch (check.status) {
                case "PASS": icon = "[OK]  "; passed++; break;
                case "WARN": icon = "[WARN]"; warned++; break;
                case "FAIL": icon = "[FAIL]"; failed++; break;
                default: icon = "[INFO]"; break;
            }
            System.out.printf("%s %s: %s%n", icon, check.name, check.details);
        }

        System.out.println("\n----------------------------------------");
        System.out.printf("Summary: %d passed, %d warnings, %d failed%n",
            passed, warned, failed);
        System.out.println("----------------------------------------");

        // Overall status
        String overall;
        if (failed > 0) {
            overall = "CRITICAL - Immediate attention required";
        } else if (warned > 0) {
            overall = "WARNING - Review recommended";
        } else {
            overall = "HEALTHY - All checks passed";
        }
        System.out.println("Overall Status: " + overall);

        // Print recommendations
        printRecommendations(result);
    }

    private static void printRecommendations(HealthCheckResult result) {
        System.out.println("\n=== Recommendations ===\n");

        boolean hasRecommendations = false;

        for (HealthCheck check : result.getChecks()) {
            if ("WARN".equals(check.status) || "FAIL".equals(check.status)) {
                hasRecommendations = true;
                if (check.name.contains("Node Count")) {
                    System.out.println("- Add more nodes for high availability (minimum 3 recommended)");
                } else if (check.name.contains("Heap Usage")) {
                    System.out.println("- Consider increasing heap size or adding nodes");
                    System.out.println("  JVM option: -Xmx<size> (e.g., -Xmx4g)");
                } else if (check.name.contains("CPU Load")) {
                    System.out.println("- High CPU usage detected, consider:");
                    System.out.println("  - Adding more nodes to distribute load");
                    System.out.println("  - Optimizing query performance");
                    System.out.println("  - Reviewing compute task efficiency");
                } else if (check.name.contains("Cache Get Time")) {
                    System.out.println("- Slow cache operations detected:");
                    System.out.println("  - Review cache configuration");
                    System.out.println("  - Consider using near cache for hot data");
                    System.out.println("  - Check network latency");
                } else if (check.name.contains("Cache Hit Rate")) {
                    System.out.println("- Low cache hit rate:");
                    System.out.println("  - Review application access patterns");
                    System.out.println("  - Consider cache warm-up on startup");
                } else if (check.name.contains("Baseline")) {
                    System.out.println("- Configure baseline topology for production");
                    System.out.println("  Command: control.sh --baseline set <nodes>");
                } else if (check.name.contains("Server Nodes")) {
                    System.out.println("- CRITICAL: No server nodes available");
                    System.out.println("  - Start at least one server node");
                }
            }
        }

        if (!hasRecommendations) {
            System.out.println("No immediate actions required.");
            System.out.println("Continue monitoring cluster health regularly.");
        }
    }

    private static IgniteConfiguration createConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("health-check-node");
        cfg.setMetricsLogFrequency(0); // Disable metrics logging

        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        spi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(spi);

        return cfg;
    }

    // Health check result classes
    static class HealthCheckResult {
        private List<HealthCheck> checks = new ArrayList<>();

        void addCheck(String name, String status, String details) {
            checks.add(new HealthCheck(name, status, details));
        }

        List<HealthCheck> getChecks() {
            return checks;
        }
    }

    static class HealthCheck {
        String name;
        String status;
        String details;

        HealthCheck(String name, String status, String details) {
            this.name = name;
            this.status = status;
            this.details = details;
        }
    }
}
