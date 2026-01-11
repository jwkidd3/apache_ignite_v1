package com.example.ignite.solutions.lab09;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteReducer;

import java.util.Arrays;
import java.util.Collection;

/**
 * Lab 09 Exercise 2: Closure-Based Computing
 *
 * Demonstrates:
 * - Apply closure to collections
 * - Using reducers for aggregation
 * - Processing pipelines
 * - Functional programming patterns
 */
public class Lab09ClosureCompute {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Closure-Based Computing Lab ===\n");

            IgniteCompute compute = ignite.compute();

            // Example 1: Apply closure to collection
            System.out.println("=== Example 1: Apply Closure to Collection ===");

            Collection<String> words = Arrays.asList("Hello", "Apache", "Ignite", "Compute");

            // Apply closure to each element - distributed across cluster
            Collection<Integer> lengths = compute.apply(
                (String word) -> {
                    System.out.println("Processing '" + word + "' on node: " +
                        Ignition.ignite().cluster().localNode().consistentId());
                    return word.length();
                },
                words
            );

            System.out.println("Word lengths: " + lengths);

            // Example 2: Apply with reducer
            System.out.println("\n=== Example 2: Apply with Reducer ===");

            Collection<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

            Integer sum = compute.apply(
                (Integer n) -> n * n,  // Square each number
                numbers,
                new IgniteReducer<Integer, Integer>() {
                    private int sum = 0;

                    @Override
                    public boolean collect(Integer result) {
                        sum += result;
                        return true;  // Continue collecting
                    }

                    @Override
                    public Integer reduce() {
                        return sum;
                    }
                }
            );

            System.out.println("Sum of squares (1-10): " + sum);
            System.out.println("Expected: 385");

            // Example 3: Chained closures
            System.out.println("\n=== Example 3: Processing Pipeline ===");

            CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>("pipelineCache");
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            // Load sample data
            cache.put(1, "  Hello World  ");
            cache.put(2, "  Apache Ignite  ");
            cache.put(3, "  Distributed Computing  ");

            // Process with closure pipeline
            for (int key = 1; key <= 3; key++) {
                final int k = key;
                String processed = compute.call(() -> {
                    String value = Ignition.ignite().<Integer, String>cache("pipelineCache").get(k);
                    // Trim, uppercase, add prefix
                    return "PROCESSED: " + value.trim().toUpperCase();
                });
                System.out.println("Key " + key + ": " + processed);
            }

            Thread.sleep(500);

            // Example 4: Maximum finder with reducer
            System.out.println("\n=== Example 4: Find Maximum Value ===");

            Collection<Integer> values = Arrays.asList(45, 12, 89, 34, 67, 23, 98, 56);

            Integer max = compute.apply(
                (Integer v) -> v,  // Identity function
                values,
                new IgniteReducer<Integer, Integer>() {
                    private Integer max = null;

                    @Override
                    public boolean collect(Integer result) {
                        if (max == null || result > max) {
                            max = result;
                        }
                        return true;
                    }

                    @Override
                    public Integer reduce() {
                        return max;
                    }
                }
            );

            System.out.println("Maximum value: " + max);

            System.out.println("\n=== Closure Computing Benefits ===");
            System.out.println("- Functional programming style");
            System.out.println("- Automatic distribution");
            System.out.println("- Built-in reduction");
            System.out.println("- Composable operations");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
