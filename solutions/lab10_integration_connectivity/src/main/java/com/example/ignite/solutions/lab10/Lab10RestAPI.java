package com.example.ignite.solutions.lab10;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

/**
 * Lab 10 Exercise 1: REST API Usage
 *
 * Demonstrates:
 * - Enabling REST connector
 * - Basic REST endpoints
 * - Cache operations via REST
 */
public class Lab10RestAPI {

    public static void main(String[] args) {
        System.out.println("=== REST API Lab ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("rest-node");

        // Enable REST connector
        ConnectorConfiguration connectorCfg = new ConnectorConfiguration();
        connectorCfg.setPort(8080);
        cfg.setConnectorConfiguration(connectorCfg);

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Ignite node started with REST API enabled");
            System.out.println("REST endpoint: http://localhost:8080/ignite\n");

            // Create sample cache
            CacheConfiguration<String, String> cacheCfg =
                new CacheConfiguration<>("restCache");
            IgniteCache<String, String> cache = ignite.getOrCreateCache(cacheCfg);

            // Add sample data
            cache.put("key1", "value1");
            cache.put("key2", "value2");
            cache.put("user:john", "{\"name\":\"John\",\"age\":30}");

            System.out.println("Sample data added to cache\n");

            System.out.println("=== REST API Endpoints ===\n");

            System.out.println("1. Get Version:");
            System.out.println("   curl http://localhost:8080/ignite?cmd=version\n");

            System.out.println("2. Get Cache Value:");
            System.out.println("   curl \"http://localhost:8080/ignite?cmd=get&cacheName=restCache&key=key1\"\n");

            System.out.println("3. Put Cache Value:");
            System.out.println("   curl \"http://localhost:8080/ignite?cmd=put&cacheName=restCache&key=key3&val=value3\"\n");

            System.out.println("4. Get All Keys:");
            System.out.println("   curl \"http://localhost:8080/ignite?cmd=getall&cacheName=restCache&k1=key1&k2=key2\"\n");

            System.out.println("5. Execute SQL Query:");
            System.out.println("   curl \"http://localhost:8080/ignite?cmd=qryfldexe&pageSize=10&cacheName=restCache&qry=SELECT+*+FROM+String\"\n");

            System.out.println("6. Get Cluster Topology:");
            System.out.println("   curl http://localhost:8080/ignite?cmd=top\n");

            System.out.println("7. Cache Size:");
            System.out.println("   curl \"http://localhost:8080/ignite?cmd=size&cacheName=restCache\"\n");

            System.out.println("=== Testing REST API ===");
            System.out.println("Try the curl commands above in another terminal\n");

            System.out.println("=== REST API Use Cases ===");
            System.out.println("- Multi-language client access");
            System.out.println("- Web applications");
            System.out.println("- Monitoring and management");
            System.out.println("- Quick prototyping");
            System.out.println("- Integration with non-Java systems");

            System.out.println("\nPress Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
