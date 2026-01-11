package com.example.ignite.solutions.lab01;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Lab 1 Challenge 3: Node Health Check Utility
 *
 * This exercise demonstrates building a utility that checks
 * and reports node health status.
 */
public class HealthCheck {

    // Health thresholds
    private static final double CPU_WARNING_THRESHOLD = 0.7;    // 70%
    private static final double CPU_CRITICAL_THRESHOLD = 0.9;   // 90%
    private static final double HEAP_WARNING_THRESHOLD = 0.7;   // 70%
    private static final double HEAP_CRITICAL_THRESHOLD = 0.9;  // 90%
    private static final int MAX_REJECTED_JOBS = 10;

    public static void main(String[] args) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("health-check-node");
        cfg.setPeerClassLoadingEnabled(true);

        System.out.println("=== Challenge 3: Node Health Check Utility ===\n");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Starting health check...\n");

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
        report.clusterSize = ignite.cluster().nodes().size();

        // Check each node
        for (ClusterNode node : ignite.cluster().nodes()) {
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
            sb.append("+--------------------------------------------------------------+\n");
            sb.append("|                    CLUSTER HEALTH REPORT                      |\n");
            sb.append("+--------------------------------------------------------------+\n");
            sb.append(String.format("| Timestamp: %-50s |\n", timestamp));
            sb.append(String.format("| Cluster Size: %-47d |\n", clusterSize));
            sb.append(String.format("| Overall Status: %-45s |\n", overallStatus));
            sb.append("+--------------------------------------------------------------+\n");
            sb.append("| NODE DETAILS                                                  |\n");
            sb.append("+--------------------------------------------------------------+\n");

            for (NodeSummary ns : nodeSummaries) {
                sb.append(String.format("| Node: %-55s |\n", ns.nodeId));
                sb.append(String.format("|   CPU: %-6.1f%%  Heap: %-6.1f%%  Active Jobs: %-14d |\n",
                    ns.cpuLoad * 100, ns.heapUsage * 100, ns.activeJobs));
                sb.append(String.format("|   Uptime: %-51s |\n", formatUptime(ns.uptime)));
            }

            if (!issues.isEmpty()) {
                sb.append("+--------------------------------------------------------------+\n");
                sb.append("| ISSUES                                                        |\n");
                sb.append("+--------------------------------------------------------------+\n");
                for (String issue : issues) {
                    sb.append(String.format("| - %-60s |\n", issue));
                }
            }

            sb.append("+--------------------------------------------------------------+");
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
