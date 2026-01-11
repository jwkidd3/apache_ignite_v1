package com.example.ignite.solutions.lab01;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;

/**
 * Lab 1 Exercise 4: Cluster Metrics Exploration
 *
 * This exercise demonstrates how to access and display
 * cluster-wide and per-node metrics.
 */
public class ClusterMetrics {

    public static void main(String[] args) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("metrics-explorer-node");
        cfg.setPeerClassLoadingEnabled(true);

        System.out.println("Starting Ignite node for metrics exploration...\n");

        try (Ignite ignite = Ignition.start(cfg)) {
            // Get the local node
            ClusterNode localNode = ignite.cluster().localNode();

            // Get cluster-wide metrics
            org.apache.ignite.cluster.ClusterMetrics clusterMetrics = ignite.cluster().metrics();

            System.out.println("=== Cluster-Wide Metrics ===");
            System.out.println("Total nodes in cluster: " + ignite.cluster().nodes().size());
            System.out.println("Total CPUs across cluster: " + clusterMetrics.getTotalCpus());
            System.out.println("Average CPU load: " + String.format("%.2f%%", clusterMetrics.getAverageCpuLoad() * 100));
            System.out.println("Current CPU load: " + String.format("%.2f%%", clusterMetrics.getCurrentCpuLoad() * 100));

            System.out.println("\n=== Memory Metrics ===");
            System.out.println("Heap memory used: " + formatBytes(clusterMetrics.getHeapMemoryUsed()));
            System.out.println("Heap memory maximum: " + formatBytes(clusterMetrics.getHeapMemoryMaximum()));
            System.out.println("Heap memory committed: " + formatBytes(clusterMetrics.getHeapMemoryCommitted()));
            System.out.println("Non-heap memory used: " + formatBytes(clusterMetrics.getNonHeapMemoryUsed()));

            System.out.println("\n=== Job Metrics ===");
            System.out.println("Active jobs: " + clusterMetrics.getCurrentActiveJobs());
            System.out.println("Waiting jobs: " + clusterMetrics.getCurrentWaitingJobs());
            System.out.println("Executed jobs: " + clusterMetrics.getTotalExecutedJobs());
            System.out.println("Rejected jobs: " + clusterMetrics.getTotalRejectedJobs());
            System.out.println("Cancelled jobs: " + clusterMetrics.getTotalCancelledJobs());

            System.out.println("\n=== Thread Metrics ===");
            System.out.println("Current thread count: " + clusterMetrics.getCurrentThreadCount());
            System.out.println("Maximum thread count: " + clusterMetrics.getMaximumThreadCount());
            System.out.println("Current daemon thread count: " + clusterMetrics.getCurrentDaemonThreadCount());

            System.out.println("\n=== Per-Node Metrics ===");
            for (ClusterNode node : ignite.cluster().nodes()) {
                org.apache.ignite.cluster.ClusterMetrics nodeMetrics = node.metrics();
                System.out.println("\nNode: " + node.id().toString().substring(0, 8) + "...");
                System.out.println("  CPUs: " + nodeMetrics.getTotalCpus());
                System.out.println("  Heap used: " + formatBytes(nodeMetrics.getHeapMemoryUsed()));
                System.out.println("  Active jobs: " + nodeMetrics.getCurrentActiveJobs());
                System.out.println("  Uptime: " + formatUptime(nodeMetrics.getUpTime()));
            }

            System.out.println("\n\nMetrics exploration complete. Press Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private static String formatUptime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("%d hours, %d minutes, %d seconds",
            hours, minutes % 60, seconds % 60);
    }
}
