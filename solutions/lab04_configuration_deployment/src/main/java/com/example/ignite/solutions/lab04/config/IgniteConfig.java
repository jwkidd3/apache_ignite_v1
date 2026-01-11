package com.example.ignite.solutions.lab04.config;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

/**
 * Lab 4 Exercise 4: Spring Boot Configuration Bean
 *
 * This class provides Spring-style configuration for Ignite.
 * Can be used with @Configuration annotation in Spring Boot.
 */
// @Configuration  // Uncomment for Spring Boot usage
public class IgniteConfig {

    // @Bean  // Uncomment for Spring Boot usage
    public Ignite igniteInstance() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("springboot-node");
        cfg.setPeerClassLoadingEnabled(true);

        // Cache configuration
        CacheConfiguration<Integer, String> cacheCfg =
            new CacheConfiguration<>("springCache");
        cacheCfg.setBackups(1);
        cacheCfg.setStatisticsEnabled(true);

        cfg.setCacheConfiguration(cacheCfg);

        return Ignition.start(cfg);
    }

    /**
     * Alternative factory method for creating multiple caches
     */
    public static CacheConfiguration<String, String> createSpringCache() {
        CacheConfiguration<String, String> cacheCfg =
            new CacheConfiguration<>("springCache");
        cacheCfg.setBackups(1);
        cacheCfg.setStatisticsEnabled(true);
        return cacheCfg;
    }

    /**
     * Standalone test method
     */
    public static void main(String[] args) {
        IgniteConfig config = new IgniteConfig();

        System.out.println("=== Spring-Style Configuration Test ===\n");

        try (Ignite ignite = config.igniteInstance()) {
            System.out.println("Ignite started via configuration bean");
            System.out.println("Node name: " + ignite.name());
            System.out.println("Available caches: " + ignite.cacheNames());

            // Test the cache
            var cache = ignite.cache("springCache");
            if (cache != null) {
                cache.put(1, "Test Value");
                System.out.println("Cache test: " + cache.get(1));
            }

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
