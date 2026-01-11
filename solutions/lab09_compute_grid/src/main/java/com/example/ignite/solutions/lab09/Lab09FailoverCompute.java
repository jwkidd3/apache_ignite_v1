package com.example.ignite.solutions.lab09;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeJobResultPolicy;
import org.apache.ignite.compute.ComputeTaskAdapter;
import org.apache.ignite.compute.ComputeTaskTimeoutException;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.spi.failover.always.AlwaysFailoverSpi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lab 09 Exercise 6: Fault-Tolerant Compute Operations
 *
 * Demonstrates:
 * - Failover SPI configuration
 * - Custom retry logic with exponential backoff
 * - Fault-tolerant tasks with progress tracking
 * - Timeout with failover
 * - No-failover mode for specific operations
 */
public class Lab09FailoverCompute {

    public static void main(String[] args) {
        // Configure failover SPI
        IgniteConfiguration cfg = new IgniteConfiguration();

        // Option 1: Always failover (default behavior, enhanced)
        AlwaysFailoverSpi failoverSpi = new AlwaysFailoverSpi();
        failoverSpi.setMaximumFailoverAttempts(5);  // Retry up to 5 times
        cfg.setFailoverSpi(failoverSpi);

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("=== Compute with Failover Lab ===\n");
            System.out.println("Failover SPI: AlwaysFailoverSpi (max 5 attempts)");
            System.out.println("Cluster nodes: " + ignite.cluster().nodes().size());

            IgniteCompute compute = ignite.compute();

            // ============================================
            // Example 1: Basic Failover Demonstration
            // ============================================

            System.out.println("\n=== Example 1: Simulated Failover ===");

            try {
                String result = ignite.compute().execute(
                    new FailoverDemoTask(),
                    "test-input"
                );
                System.out.println("Task result: " + result);
            } catch (IgniteException e) {
                System.out.println("Task failed after all retries: " + e.getMessage());
            }

            // ============================================
            // Example 2: Retry Logic with Custom Handler
            // ============================================

            System.out.println("\n=== Example 2: Custom Retry Logic ===");

            RetryableCompute retryableCompute = new RetryableCompute(compute, 3);

            try {
                Integer result = retryableCompute.callWithRetry(() -> {
                    // Simulate occasional failures
                    if (Math.random() < 0.3) {
                        throw new RuntimeException("Simulated transient failure");
                    }
                    System.out.println("Computation successful on node: " +
                        Ignition.ignite().cluster().localNode().consistentId());
                    return 42;
                });
                System.out.println("Result with retry: " + result);
            } catch (Exception e) {
                System.out.println("Failed after retries: " + e.getMessage());
            }

            // ============================================
            // Example 3: Fault-Tolerant Task with Checkpointing
            // ============================================

            System.out.println("\n=== Example 3: Task with Progress Tracking ===");

            try {
                ProcessingResult result = ignite.compute().execute(
                    new FaultTolerantProcessingTask(),
                    100  // Process 100 items
                );
                System.out.println("Processing complete:");
                System.out.println("  Items processed: " + result.processedCount);
                System.out.println("  Failed items: " + result.failedCount);
                System.out.println("  Success rate: " +
                    String.format("%.1f%%", result.successRate * 100));
            } catch (Exception e) {
                System.out.println("Task failed: " + e.getMessage());
            }

            // ============================================
            // Example 4: Compute with Timeout and Failover
            // ============================================

            System.out.println("\n=== Example 4: Timeout with Failover ===");

            try {
                // Set timeout - if exceeded, task will failover to another node
                String result = compute.withTimeout(5000).call(() -> {
                    System.out.println("Starting task with 5s timeout on node: " +
                        Ignition.ignite().cluster().localNode().consistentId());

                    // Simulate work
                    Thread.sleep(1000);

                    return "Completed within timeout";
                });
                System.out.println("Result: " + result);
            } catch (ComputeTaskTimeoutException e) {
                System.out.println("Task timed out - would failover in multi-node cluster");
            }

            // ============================================
            // Example 5: No-Failover Mode
            // ============================================

            System.out.println("\n=== Example 5: No-Failover Mode ===");

            try {
                // Disable failover for this specific computation
                String result = compute.withNoFailover().call(() -> {
                    System.out.println("Running without failover on node: " +
                        Ignition.ignite().cluster().localNode().consistentId());
                    return "No-failover result";
                });
                System.out.println("Result: " + result);
            } catch (Exception e) {
                System.out.println("Failed without retry: " + e.getMessage());
            }

            // ============================================
            // Failover Best Practices
            // ============================================

            System.out.println("\n=== Failover Configuration Options ===");
            System.out.println("1. AlwaysFailoverSpi - Always retry on failure");
            System.out.println("2. JobStealingFailoverSpi - Redistribute to less busy nodes");
            System.out.println("3. NeverFailoverSpi - Never retry (for idempotency concerns)");
            System.out.println("4. Custom FailoverSpi - Implement your own logic");

            System.out.println("\n=== Best Practices ===");
            System.out.println("- Make tasks idempotent when possible");
            System.out.println("- Set reasonable timeout values");
            System.out.println("- Use checkpointing for long-running tasks");
            System.out.println("- Log failover events for debugging");
            System.out.println("- Consider using withNoFailover() for non-retryable ops");
            System.out.println("- Implement proper error handling in reduce phase");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Demonstrates failover behavior in ComputeTask
     */
    static class FailoverDemoTask extends ComputeTaskAdapter<String, String> {
        private static final AtomicInteger attemptCount = new AtomicInteger(0);

        @Override
        public Map<? extends ComputeJob, ClusterNode> map(
                List<ClusterNode> nodes, String arg) {

            Map<ComputeJob, ClusterNode> map = new HashMap<>();

            // Distribute job to first available node
            if (!nodes.isEmpty()) {
                map.put(new FailoverDemoJob(arg), nodes.get(0));
            }

            return map;
        }

        @Override
        public String reduce(List<ComputeJobResult> results) {
            for (ComputeJobResult result : results) {
                if (result.getException() == null) {
                    return result.getData();
                }
            }
            return "All jobs failed";
        }

        @Override
        public ComputeJobResultPolicy result(ComputeJobResult res,
                List<ComputeJobResult> rcvd) {

            if (res.getException() != null) {
                int attempt = attemptCount.incrementAndGet();
                System.out.println("Job failed (attempt " + attempt + "), requesting failover");

                if (attempt < 3) {
                    return ComputeJobResultPolicy.FAILOVER;
                } else {
                    System.out.println("Max attempts reached, accepting failure");
                    attemptCount.set(0);  // Reset for next task
                    return ComputeJobResultPolicy.WAIT;
                }
            }

            attemptCount.set(0);
            return ComputeJobResultPolicy.WAIT;
        }
    }

    static class FailoverDemoJob implements ComputeJob, Serializable {
        private static final AtomicInteger jobAttempts = new AtomicInteger(0);
        private final String input;

        FailoverDemoJob(String input) {
            this.input = input;
        }

        @Override
        public String execute() {
            int attempt = jobAttempts.incrementAndGet();
            System.out.println("Executing job, attempt " + attempt);

            // Simulate failure on first two attempts
            if (attempt <= 2) {
                throw new RuntimeException("Simulated failure on attempt " + attempt);
            }

            jobAttempts.set(0);  // Reset for next task
            return "Success on attempt " + attempt + " with input: " + input;
        }

        @Override
        public void cancel() {
            System.out.println("Job cancelled");
        }
    }

    /**
     * Utility class for retry logic with exponential backoff
     */
    static class RetryableCompute {
        private final IgniteCompute compute;
        private final int maxRetries;

        RetryableCompute(IgniteCompute compute, int maxRetries) {
            this.compute = compute;
            this.maxRetries = maxRetries;
        }

        <T> T callWithRetry(IgniteCallable<T> callable) throws Exception {
            Exception lastException = null;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    System.out.println("Attempt " + attempt + " of " + maxRetries);
                    return compute.call(callable);
                } catch (Exception e) {
                    lastException = e;
                    System.out.println("Attempt " + attempt + " failed: " + e.getMessage());

                    if (attempt < maxRetries) {
                        // Exponential backoff
                        long waitTime = (long) Math.pow(2, attempt) * 100;
                        System.out.println("Waiting " + waitTime + "ms before retry");
                        Thread.sleep(waitTime);
                    }
                }
            }

            throw lastException;
        }
    }

    /**
     * Processing result with statistics
     */
    static class ProcessingResult implements Serializable {
        int processedCount;
        int failedCount;
        double successRate;
    }

    /**
     * Fault-tolerant processing task with progress tracking
     */
    static class FaultTolerantProcessingTask
            extends ComputeTaskAdapter<Integer, ProcessingResult> {

        @IgniteInstanceResource
        private Ignite ignite;

        @Override
        public Map<? extends ComputeJob, ClusterNode> map(
                List<ClusterNode> nodes, Integer itemCount) {

            Map<ComputeJob, ClusterNode> map = new HashMap<>();

            // Distribute items across nodes using round-robin
            int nodeCount = nodes.size();
            for (int i = 0; i < itemCount; i++) {
                ClusterNode node = nodes.get(i % nodeCount);
                map.put(new ProcessingJob(i), node);
            }

            System.out.println("Distributed " + itemCount + " jobs across " + nodeCount + " nodes");
            return map;
        }

        @Override
        public ProcessingResult reduce(List<ComputeJobResult> results) {
            ProcessingResult total = new ProcessingResult();

            for (ComputeJobResult result : results) {
                if (result.getException() == null) {
                    Boolean success = result.getData();
                    total.processedCount++;
                    if (!success) {
                        total.failedCount++;
                    }
                } else {
                    total.failedCount++;
                }
            }

            total.successRate = total.processedCount > 0
                ? (double)(total.processedCount - total.failedCount) / total.processedCount
                : 0;

            return total;
        }

        @Override
        public ComputeJobResultPolicy result(ComputeJobResult res,
                List<ComputeJobResult> rcvd) {
            // Continue collecting results even if some jobs fail
            return ComputeJobResultPolicy.WAIT;
        }
    }

    static class ProcessingJob implements ComputeJob, Serializable {
        private final int itemId;

        ProcessingJob(int itemId) {
            this.itemId = itemId;
        }

        @Override
        public Boolean execute() {
            // Simulate processing with occasional failures
            if (Math.random() < 0.05) {  // 5% failure rate
                return false;
            }
            return true;
        }

        @Override
        public void cancel() {}
    }
}
