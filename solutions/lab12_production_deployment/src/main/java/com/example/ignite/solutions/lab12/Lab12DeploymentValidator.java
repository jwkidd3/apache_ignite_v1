package com.example.ignite.solutions.lab12;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.transactions.Transaction;

import java.util.*;
import java.util.concurrent.*;

/**
 * Lab 12 Challenge Exercise 3: Deployment Validator
 *
 * Demonstrates:
 * - Production deployment validation
 * - Cluster topology verification
 * - Cache operation testing
 * - Transaction validation
 * - Concurrent access testing
 * - Failover configuration verification
 */
public class Lab12DeploymentValidator {

    private static final int TEST_RECORDS = 1000;
    private static final int CONCURRENT_OPS = 10;
    private static final long OPERATION_TIMEOUT_MS = 5000;

    public static void main(String[] args) {
        System.out.println("=== Deployment Validator ===\n");
        System.out.println("This tool validates a production Ignite deployment.\n");

        IgniteConfiguration cfg = createConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            // Activate if needed
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                ignite.cluster().state(ClusterState.ACTIVE);
            }

            ValidationResult result = new ValidationResult();

            // Run all validations
            validateClusterTopology(ignite, result);
            validateCacheOperations(ignite, result);
            validateTransactions(ignite, result);
            validateConcurrentAccess(ignite, result);
            validateFailover(ignite, result);
            validatePersistence(ignite, result);

            // Print final report
            printValidationReport(result);

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void validateClusterTopology(Ignite ignite, ValidationResult result) {
        System.out.println("Validating cluster topology...");

        try {
            Collection<ClusterNode> nodes = ignite.cluster().nodes();

            // Check minimum nodes
            if (nodes.size() >= 3) {
                result.addPass("Minimum node count",
                    "Found " + nodes.size() + " nodes");
            } else {
                result.addWarn("Minimum node count",
                    "Only " + nodes.size() + " nodes (recommend 3+)");
            }

            // Check for server nodes
            long serverCount = nodes.stream().filter(n -> !n.isClient()).count();
            if (serverCount >= 2) {
                result.addPass("Server nodes", serverCount + " server nodes available");
            } else if (serverCount >= 1) {
                result.addWarn("Server nodes", "Only " + serverCount + " server node (recommend 2+)");
            } else {
                result.addFail("Server nodes", "Need at least 1 server node");
            }

            // Check consistent IDs
            Set<Object> consistentIds = new HashSet<>();
            for (ClusterNode node : nodes) {
                if (!consistentIds.add(node.consistentId())) {
                    result.addFail("Unique node IDs", "Duplicate consistent ID found");
                    return;
                }
            }
            result.addPass("Unique node IDs", "All nodes have unique IDs");

            // Check cluster state
            ClusterState state = ignite.cluster().state();
            if (state == ClusterState.ACTIVE) {
                result.addPass("Cluster state", "Cluster is ACTIVE");
            } else {
                result.addFail("Cluster state", "Cluster state is " + state);
            }

        } catch (Exception e) {
            result.addFail("Cluster topology", e.getMessage());
        }
    }

    private static void validateCacheOperations(Ignite ignite, ValidationResult result) {
        System.out.println("Validating cache operations...");

        String cacheName = "validation-cache";

        try {
            CacheConfiguration<Long, String> cfg = new CacheConfiguration<>(cacheName);
            cfg.setCacheMode(CacheMode.PARTITIONED);
            cfg.setBackups(1);
            cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);

            IgniteCache<Long, String> cache = ignite.getOrCreateCache(cfg);

            // Test put operations
            long startTime = System.currentTimeMillis();
            for (long i = 0; i < TEST_RECORDS; i++) {
                cache.put(i, "value-" + i);
            }
            long putTime = System.currentTimeMillis() - startTime;
            result.addPass("Cache put operations",
                TEST_RECORDS + " records in " + putTime + "ms (" +
                String.format("%.1f", (double) TEST_RECORDS * 1000 / putTime) + " ops/sec)");

            // Test get operations
            startTime = System.currentTimeMillis();
            for (long i = 0; i < TEST_RECORDS; i++) {
                String value = cache.get(i);
                if (value == null || !value.equals("value-" + i)) {
                    result.addFail("Cache get operations", "Data mismatch at key " + i);
                    return;
                }
            }
            long getTime = System.currentTimeMillis() - startTime;
            result.addPass("Cache get operations",
                TEST_RECORDS + " reads in " + getTime + "ms (" +
                String.format("%.1f", (double) TEST_RECORDS * 1000 / getTime) + " ops/sec)");

            // Test remove operations
            cache.removeAll();
            if (cache.size() == 0) {
                result.addPass("Cache remove operations", "All records removed");
            } else {
                result.addFail("Cache remove operations", "Records remain after removeAll");
            }

            ignite.destroyCache(cacheName);

        } catch (Exception e) {
            result.addFail("Cache operations", e.getMessage());
        }
    }

    private static void validateTransactions(Ignite ignite, ValidationResult result) {
        System.out.println("Validating transactions...");

        String cacheName = "tx-validation-cache";

        try {
            CacheConfiguration<Long, Long> cfg = new CacheConfiguration<>(cacheName);
            cfg.setCacheMode(CacheMode.PARTITIONED);
            cfg.setBackups(1);
            cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

            IgniteCache<Long, Long> cache = ignite.getOrCreateCache(cfg);

            // Test successful transaction
            try (Transaction tx = ignite.transactions().txStart()) {
                cache.put(1L, 100L);
                cache.put(2L, 200L);
                tx.commit();
            }

            if (cache.get(1L) == 100L && cache.get(2L) == 200L) {
                result.addPass("Transaction commit", "Data persisted correctly");
            } else {
                result.addFail("Transaction commit", "Data not persisted");
            }

            // Test rollback
            try (Transaction tx = ignite.transactions().txStart()) {
                cache.put(1L, 999L);
                tx.rollback();
            }

            if (cache.get(1L) == 100L) {
                result.addPass("Transaction rollback", "Rollback successful");
            } else {
                result.addFail("Transaction rollback", "Rollback failed - value changed");
            }

            // Test multi-key transaction
            try (Transaction tx = ignite.transactions().txStart()) {
                cache.put(10L, 1000L);
                cache.put(11L, 1100L);
                cache.put(12L, 1200L);
                tx.commit();
            }

            if (cache.get(10L) == 1000L && cache.get(11L) == 1100L && cache.get(12L) == 1200L) {
                result.addPass("Multi-key transaction", "Multiple keys committed atomically");
            } else {
                result.addFail("Multi-key transaction", "Data inconsistency detected");
            }

            ignite.destroyCache(cacheName);

        } catch (Exception e) {
            result.addFail("Transactions", e.getMessage());
        }
    }

    private static void validateConcurrentAccess(Ignite ignite, ValidationResult result) {
        System.out.println("Validating concurrent access...");

        String cacheName = "concurrent-validation-cache";

        try {
            CacheConfiguration<Long, Long> cfg = new CacheConfiguration<>(cacheName);
            cfg.setCacheMode(CacheMode.PARTITIONED);
            cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);

            IgniteCache<Long, Long> cache = ignite.getOrCreateCache(cfg);
            cache.put(1L, 0L);

            ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_OPS);
            List<Future<Boolean>> futures = new ArrayList<>();

            // Concurrent increments using EntryProcessor
            for (int i = 0; i < CONCURRENT_OPS; i++) {
                futures.add(executor.submit(() -> {
                    for (int j = 0; j < 100; j++) {
                        cache.invoke(1L, (entry, args) -> {
                            Long val = entry.getValue();
                            entry.setValue(val + 1);
                            return null;
                        });
                    }
                    return true;
                }));
            }

            // Wait for completion
            for (Future<Boolean> future : futures) {
                future.get(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }
            executor.shutdown();

            Long finalValue = cache.get(1L);
            if (finalValue == CONCURRENT_OPS * 100L) {
                result.addPass("Concurrent access",
                    "Correct final value: " + finalValue + " (" + CONCURRENT_OPS + " threads x 100 ops)");
            } else {
                result.addFail("Concurrent access",
                    "Incorrect value: " + finalValue + " (expected " + (CONCURRENT_OPS * 100) + ")");
            }

            ignite.destroyCache(cacheName);

        } catch (TimeoutException e) {
            result.addFail("Concurrent access", "Operation timed out");
        } catch (Exception e) {
            result.addFail("Concurrent access", e.getMessage());
        }
    }

    private static void validateFailover(Ignite ignite, ValidationResult result) {
        System.out.println("Validating failover configuration...");

        try {
            // Check backup configuration
            String cacheName = "failover-validation-cache";
            CacheConfiguration<Long, String> cfg = new CacheConfiguration<>(cacheName);
            cfg.setCacheMode(CacheMode.PARTITIONED);
            cfg.setBackups(1);

            IgniteCache<Long, String> cache = ignite.getOrCreateCache(cfg);
            cache.put(1L, "test");

            // Verify backup exists
            int backups = cache.getConfiguration(CacheConfiguration.class).getBackups();
            if (backups >= 1) {
                result.addPass("Backup configuration", backups + " backup(s) configured");
            } else {
                result.addWarn("Backup configuration", "No backups configured - data loss risk");
            }

            // Verify partitioning
            CacheMode cacheMode = cache.getConfiguration(CacheConfiguration.class).getCacheMode();
            if (cacheMode == CacheMode.PARTITIONED) {
                result.addPass("Cache mode", "PARTITIONED mode for scalability");
            } else if (cacheMode == CacheMode.REPLICATED) {
                result.addPass("Cache mode", "REPLICATED mode for high availability");
            } else {
                result.addWarn("Cache mode", cacheMode + " - consider PARTITIONED for scalability");
            }

            ignite.destroyCache(cacheName);

        } catch (Exception e) {
            result.addFail("Failover", e.getMessage());
        }
    }

    private static void validatePersistence(Ignite ignite, ValidationResult result) {
        System.out.println("Validating persistence...");

        try {
            boolean persistenceEnabled = false;

            if (ignite.configuration().getDataStorageConfiguration() != null &&
                ignite.configuration().getDataStorageConfiguration().getDefaultDataRegionConfiguration() != null) {
                persistenceEnabled = ignite.configuration()
                    .getDataStorageConfiguration()
                    .getDefaultDataRegionConfiguration()
                    .isPersistenceEnabled();
            }

            if (persistenceEnabled) {
                result.addPass("Persistence", "Native persistence enabled");
            } else {
                result.addWarn("Persistence",
                    "Persistence disabled - data will be lost on restart");
            }

            // Check WAL configuration
            if (persistenceEnabled) {
                String walPath = ignite.configuration()
                    .getDataStorageConfiguration()
                    .getWalPath();
                if (walPath != null) {
                    result.addPass("WAL configuration", "WAL path configured: " + walPath);
                } else {
                    result.addInfo("WAL configuration", "Using default WAL path");
                }
            }

        } catch (Exception e) {
            result.addInfo("Persistence", "Could not verify: " + e.getMessage());
        }
    }

    private static void printValidationReport(ValidationResult result) {
        System.out.println("\n==========================================");
        System.out.println("       DEPLOYMENT VALIDATION REPORT");
        System.out.println("==========================================\n");

        for (ValidationCheck check : result.getChecks()) {
            String icon;
            switch (check.status) {
                case "PASS": icon = "[PASS]"; break;
                case "WARN": icon = "[WARN]"; break;
                case "FAIL": icon = "[FAIL]"; break;
                default: icon = "[INFO]"; break;
            }
            System.out.printf("%s %s: %s%n", icon, check.name, check.details);
        }

        System.out.println("\n------------------------------------------");
        System.out.printf("Results: %d passed, %d warnings, %d failed%n",
            result.passCount, result.warnCount, result.failCount);
        System.out.println("------------------------------------------");

        if (result.failCount > 0) {
            System.out.println("\nDEPLOYMENT VALIDATION: FAILED");
            System.out.println("Address failed checks before production use.");
        } else if (result.warnCount > 0) {
            System.out.println("\nDEPLOYMENT VALIDATION: PASSED WITH WARNINGS");
            System.out.println("Review warnings for production readiness.");
        } else {
            System.out.println("\nDEPLOYMENT VALIDATION: PASSED");
            System.out.println("Deployment is ready for production use.");
        }

        // Print production checklist
        printProductionChecklist();
    }

    private static void printProductionChecklist() {
        System.out.println("\n=== Production Checklist ===\n");
        System.out.println("Before going to production, verify:");
        System.out.println("[ ] Minimum 3 server nodes deployed");
        System.out.println("[ ] Backups configured for all caches");
        System.out.println("[ ] Native persistence enabled");
        System.out.println("[ ] SSL/TLS configured for all connections");
        System.out.println("[ ] Authentication enabled");
        System.out.println("[ ] JVM tuning applied (heap size, GC settings)");
        System.out.println("[ ] Baseline topology configured");
        System.out.println("[ ] Monitoring and alerting set up");
        System.out.println("[ ] Backup and recovery procedures tested");
        System.out.println("[ ] Rolling update procedure documented");
    }

    private static IgniteConfiguration createConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("validator-node");
        cfg.setMetricsLogFrequency(0); // Disable metrics logging for cleaner output

        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        spi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(spi);

        return cfg;
    }

    // Result classes
    static class ValidationResult {
        List<ValidationCheck> checks = new ArrayList<>();
        int passCount = 0, warnCount = 0, failCount = 0;

        void addPass(String name, String details) {
            checks.add(new ValidationCheck(name, "PASS", details));
            passCount++;
        }

        void addWarn(String name, String details) {
            checks.add(new ValidationCheck(name, "WARN", details));
            warnCount++;
        }

        void addFail(String name, String details) {
            checks.add(new ValidationCheck(name, "FAIL", details));
            failCount++;
        }

        void addInfo(String name, String details) {
            checks.add(new ValidationCheck(name, "INFO", details));
        }

        List<ValidationCheck> getChecks() {
            return checks;
        }
    }

    static class ValidationCheck {
        String name;
        String status;
        String details;

        ValidationCheck(String name, String status, String details) {
            this.name = name;
            this.status = status;
            this.details = details;
        }
    }
}
