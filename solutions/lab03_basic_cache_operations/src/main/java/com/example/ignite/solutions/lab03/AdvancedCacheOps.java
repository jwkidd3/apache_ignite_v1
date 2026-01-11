package com.example.ignite.solutions.lab03;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

/**
 * Lab 3 Optional: Advanced Cache Operations
 *
 * This exercise demonstrates advanced operations including
 * EntryProcessors and distributed locking.
 */
public class AdvancedCacheOps {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Advanced Cache Operations Lab ===\n");

            CacheConfiguration<String, Integer> cfg =
                new CacheConfiguration<>("advancedCache");
            IgniteCache<String, Integer> cache = ignite.getOrCreateCache(cfg);

            // 1. getAndPutIfAbsent - atomically get and put if not present
            System.out.println("1. getAndPutIfAbsent Operations:");
            cache.put("counter", 100);

            Integer oldVal = cache.getAndPutIfAbsent("counter", 200);
            System.out.println("   Existing key - old value: " + oldVal);
            System.out.println("   Current value: " + cache.get("counter"));

            oldVal = cache.getAndPutIfAbsent("newCounter", 50);
            System.out.println("   New key - old value: " + oldVal + " (null expected)");
            System.out.println("   New key current value: " + cache.get("newCounter"));

            // 2. getAndReplace - atomically get and replace
            System.out.println("\n2. getAndReplace Operations:");
            oldVal = cache.getAndReplace("counter", 150);
            System.out.println("   Old value: " + oldVal);
            System.out.println("   New value: " + cache.get("counter"));

            oldVal = cache.getAndReplace("nonExistent", 999);
            System.out.println("   Non-existent key result: " + oldVal + " (null expected)");

            // 3. EntryProcessor - invoke operations directly on cache entries
            System.out.println("\n3. EntryProcessor (invoke) Operations:");

            // Increment counter using EntryProcessor
            Integer result = cache.invoke("counter", new IncrementProcessor(), 10);
            System.out.println("   Invoked increment by 10, new value: " + result);

            // Another increment
            result = cache.invoke("counter", new IncrementProcessor(), 5);
            System.out.println("   Invoked increment by 5, new value: " + result);

            // Multiply using EntryProcessor
            result = cache.invoke("counter", new MultiplyProcessor(), 2);
            System.out.println("   Invoked multiply by 2, new value: " + result);

            // 4. Lock operations for explicit locking
            System.out.println("\n4. Lock Operations:");

            Lock lock = cache.lock("counter");
            System.out.println("   Acquiring lock on 'counter'...");

            lock.lock();
            try {
                System.out.println("   Lock acquired!");
                Integer val = cache.get("counter");
                System.out.println("   Current value: " + val);

                // Simulate some work
                Thread.sleep(100);

                cache.put("counter", val + 100);
                System.out.println("   Updated value: " + cache.get("counter"));
            } finally {
                lock.unlock();
                System.out.println("   Lock released!");
            }

            // 5. tryLock with timeout
            System.out.println("\n5. TryLock with Timeout:");

            Lock tryLock = cache.lock("counter");
            boolean acquired = tryLock.tryLock();
            if (acquired) {
                try {
                    System.out.println("   TryLock succeeded!");
                    cache.put("counter", cache.get("counter") + 1);
                } finally {
                    tryLock.unlock();
                }
            } else {
                System.out.println("   TryLock failed - lock held by another thread");
            }

            // 6. invokeAll for batch processing
            System.out.println("\n6. InvokeAll for Batch Processing:");
            cache.put("val1", 10);
            cache.put("val2", 20);
            cache.put("val3", 30);

            Set<String> keys = new HashSet<>();
            keys.add("val1");
            keys.add("val2");
            keys.add("val3");

            Map<String, javax.cache.processor.EntryProcessorResult<Integer>> results =
                cache.invokeAll(keys, new IncrementProcessor(), 5);

            System.out.println("   After invokeAll (increment by 5):");
            for (Map.Entry<String, javax.cache.processor.EntryProcessorResult<Integer>> entry : results.entrySet()) {
                System.out.println("   " + entry.getKey() + " = " + entry.getValue().get());
            }

            System.out.println("\n=== Advanced Operations Summary ===");
            System.out.println("- getAndPutIfAbsent: Atomic read-and-insert");
            System.out.println("- getAndReplace: Atomic read-and-update");
            System.out.println("- invoke/EntryProcessor: Server-side processing");
            System.out.println("- lock: Explicit distributed locking");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // EntryProcessor to increment a value
    static class IncrementProcessor implements EntryProcessor<String, Integer, Integer> {
        @Override
        public Integer process(MutableEntry<String, Integer> entry, Object... args)
                throws EntryProcessorException {
            int increment = (Integer) args[0];
            int currentValue = entry.exists() ? entry.getValue() : 0;
            int newValue = currentValue + increment;
            entry.setValue(newValue);
            return newValue;
        }
    }

    // EntryProcessor to multiply a value
    static class MultiplyProcessor implements EntryProcessor<String, Integer, Integer> {
        @Override
        public Integer process(MutableEntry<String, Integer> entry, Object... args)
                throws EntryProcessorException {
            int multiplier = (Integer) args[0];
            int currentValue = entry.exists() ? entry.getValue() : 0;
            int newValue = currentValue * multiplier;
            entry.setValue(newValue);
            return newValue;
        }
    }
}
