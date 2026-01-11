package com.example.ignite.solutions.lab09;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteInClosure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Lab 09 Exercise 7: Asynchronous and Parallel Execution
 *
 * Demonstrates:
 * - Basic async execution with IgniteFuture
 * - Futures with timeout
 * - Async callbacks with listeners
 * - Parallel job execution
 * - Async broadcast
 * - Chaining async operations
 * - Bulk async callables
 */
public class Lab09AsyncCompute {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Async Compute Operations Lab ===\n");

            IgniteCompute compute = ignite.compute();

            // ============================================
            // Example 1: Basic Async Execution
            // ============================================

            System.out.println("=== Example 1: Basic Async Execution ===");

            // Submit async task
            IgniteFuture<String> future = compute.callAsync(() -> {
                System.out.println("Async task starting...");
                Thread.sleep(1000);
                return "Async result from node: " +
                    Ignition.ignite().cluster().localNode().consistentId();
            });

            System.out.println("Task submitted, doing other work...");

            // Do other work while task executes
            for (int i = 1; i <= 3; i++) {
                System.out.println("Main thread working... " + i);
                Thread.sleep(300);
            }

            // Get result (blocks if not complete)
            String result = future.get();
            System.out.println("Async result: " + result);

            // ============================================
            // Example 2: Future with Timeout
            // ============================================

            System.out.println("\n=== Example 2: Future with Timeout ===");

            IgniteFuture<Integer> timedFuture = compute.callAsync(() -> {
                Thread.sleep(500);
                return 42;
            });

            try {
                Integer timedResult = timedFuture.get(2, TimeUnit.SECONDS);
                System.out.println("Got result within timeout: " + timedResult);
            } catch (Exception e) {
                System.out.println("Timeout waiting for result");
            }

            // ============================================
            // Example 3: Async with Callback (Listen)
            // ============================================

            System.out.println("\n=== Example 3: Async with Callback ===");

            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger callbackResult = new AtomicInteger();

            IgniteFuture<Integer> callbackFuture = compute.callAsync(() -> {
                Thread.sleep(500);
                return 100;
            });

            // Add listener for async notification
            callbackFuture.listen(new IgniteInClosure<IgniteFuture<Integer>>() {
                @Override
                public void apply(IgniteFuture<Integer> f) {
                    try {
                        int value = f.get();
                        System.out.println("Callback received result: " + value);
                        callbackResult.set(value);
                    } catch (Exception e) {
                        System.out.println("Callback error: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
            });

            System.out.println("Waiting for callback...");
            latch.await(5, TimeUnit.SECONDS);
            System.out.println("Callback completed with value: " + callbackResult.get());

            // ============================================
            // Example 4: Parallel Job Execution
            // ============================================

            System.out.println("\n=== Example 4: Parallel Job Execution ===");

            // Create multiple compute tasks
            int numTasks = 10;
            List<IgniteFuture<Integer>> futures = new ArrayList<>();

            long startTime = System.currentTimeMillis();

            // Submit all tasks asynchronously
            for (int i = 0; i < numTasks; i++) {
                final int taskId = i;
                IgniteFuture<Integer> f = compute.callAsync(() -> {
                    // Simulate work
                    Thread.sleep(500);
                    System.out.println("Task " + taskId + " completed on node: " +
                        Ignition.ignite().cluster().localNode().consistentId());
                    return taskId * taskId;
                });
                futures.add(f);
            }

            System.out.println("Submitted " + numTasks + " parallel tasks");

            // Collect all results
            List<Integer> results = new ArrayList<>();
            for (IgniteFuture<Integer> f : futures) {
                results.add(f.get());
            }

            long parallelTime = System.currentTimeMillis() - startTime;

            System.out.println("All tasks completed in " + parallelTime + " ms");
            System.out.println("Results: " + results);
            System.out.println("Sequential would take: ~" + (numTasks * 500) + " ms");

            // ============================================
            // Example 5: Async Broadcast
            // ============================================

            System.out.println("\n=== Example 5: Async Broadcast ===");

            IgniteFuture<Void> broadcastFuture = compute.broadcastAsync(() -> {
                System.out.println("Broadcast received on node: " +
                    Ignition.ignite().cluster().localNode().consistentId());
            });

            System.out.println("Broadcast sent asynchronously");
            broadcastFuture.get();  // Wait for all nodes to receive
            System.out.println("All nodes processed broadcast");

            Thread.sleep(300);

            // ============================================
            // Example 6: Chaining Async Operations
            // ============================================

            System.out.println("\n=== Example 6: Chaining Async Operations ===");

            // First async operation
            IgniteFuture<Integer> step1 = compute.callAsync(() -> {
                System.out.println("Step 1: Fetching data...");
                Thread.sleep(300);
                return 10;
            });

            // Chain with callback
            CountDownLatch chainLatch = new CountDownLatch(1);
            final AtomicInteger finalResult = new AtomicInteger();

            step1.listen(f1 -> {
                try {
                    int value1 = f1.get();
                    System.out.println("Step 1 result: " + value1);

                    // Start step 2 based on step 1 result
                    IgniteFuture<Integer> step2 = compute.callAsync(() -> {
                        System.out.println("Step 2: Processing...");
                        Thread.sleep(300);
                        return value1 * 2;
                    });

                    step2.listen(f2 -> {
                        try {
                            int value2 = f2.get();
                            System.out.println("Step 2 result: " + value2);

                            // Final step
                            IgniteFuture<Integer> step3 = compute.callAsync(() -> {
                                System.out.println("Step 3: Finalizing...");
                                return value2 + 5;
                            });

                            finalResult.set(step3.get());
                            System.out.println("Final result: " + finalResult.get());
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            chainLatch.countDown();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    chainLatch.countDown();
                }
            });

            chainLatch.await(10, TimeUnit.SECONDS);

            // ============================================
            // Example 7: Bulk Async with Collection
            // ============================================

            System.out.println("\n=== Example 7: Bulk Async Callables ===");

            Collection<IgniteCallable<Long>> callables = IntStream.range(0, 5)
                .mapToObj(i -> (IgniteCallable<Long>) () -> {
                    // Compute factorial
                    long factorial = 1;
                    for (int n = 1; n <= (i + 5); n++) {
                        factorial *= n;
                    }
                    return factorial;
                })
                .collect(Collectors.toList());

            // Execute all callables and get future for collection of results
            IgniteFuture<Collection<Long>> bulkFuture = compute.callAsync(callables);

            System.out.println("Bulk async submitted");
            Collection<Long> bulkResults = bulkFuture.get();
            System.out.println("Factorial results: " + bulkResults);

            // ============================================
            // Example 8: Async Run (No Return Value)
            // ============================================

            System.out.println("\n=== Example 8: Async Run ===");

            CountDownLatch runLatch = new CountDownLatch(1);

            IgniteFuture<Void> runFuture = compute.runAsync(() -> {
                System.out.println("Async runnable executing on node: " +
                    Ignition.ignite().cluster().localNode().consistentId());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Async runnable completed");
            });

            runFuture.listen(f -> {
                System.out.println("Run completed (via listener)");
                runLatch.countDown();
            });

            runLatch.await(5, TimeUnit.SECONDS);

            // ============================================
            // Async Best Practices
            // ============================================

            System.out.println("\n=== Async Compute Best Practices ===");
            System.out.println("1. Use async for I/O-bound or long-running operations");
            System.out.println("2. Add listeners instead of blocking on get()");
            System.out.println("3. Set appropriate timeouts on future.get()");
            System.out.println("4. Handle exceptions in callbacks");
            System.out.println("5. Use parallel execution for independent tasks");
            System.out.println("6. Consider backpressure for large task volumes");
            System.out.println("7. Monitor pending futures count");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
