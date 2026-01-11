package com.example.ignite.solutions.lab12;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

/**
 * Lab 12 Exercise 4: Authentication Setup
 *
 * Demonstrates:
 * - Enabling authentication in Ignite
 * - User management via SQL
 * - Permission configuration
 * - Authentication best practices
 */
public class Lab12Authentication {

    public static void main(String[] args) {
        System.out.println("=== Authentication Setup Lab ===\n");

        // Create configuration with authentication enabled
        IgniteConfiguration cfg = createAuthenticatedConfig();

        System.out.println("=== Authentication Configuration ===");
        System.out.println("Authentication: ENABLED");
        System.out.println("Persistence: REQUIRED for auth");
        System.out.println("Default user: ignite / ignite\n");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Node started with authentication enabled\n");

            // Activate cluster
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                ignite.cluster().state(ClusterState.ACTIVE);
            }

            // Print user management SQL commands
            System.out.println("=== User Management Commands (via SQL) ===\n");

            System.out.println("-- Create new user with password");
            System.out.println("CREATE USER admin WITH PASSWORD 'AdminPass123!';");
            System.out.println("");

            System.out.println("-- Create read-only user");
            System.out.println("CREATE USER readonly WITH PASSWORD 'ReadOnly123!';");
            System.out.println("");

            System.out.println("-- Create application service account");
            System.out.println("CREATE USER app_service WITH PASSWORD 'AppService123!';");
            System.out.println("");

            System.out.println("-- Change user password");
            System.out.println("ALTER USER admin WITH PASSWORD 'NewAdminPass456!';");
            System.out.println("");

            System.out.println("-- Drop user");
            System.out.println("DROP USER readonly;");
            System.out.println("");

            System.out.println("=== Permission Configuration ===\n");

            System.out.println("Permissions can be configured for:");
            System.out.println("  - CACHE_READ: Read cache data");
            System.out.println("  - CACHE_PUT: Write cache data");
            System.out.println("  - CACHE_REMOVE: Delete cache data");
            System.out.println("  - CACHE_CREATE: Create new caches");
            System.out.println("  - CACHE_DESTROY: Delete caches");
            System.out.println("  - ADMIN_OPS: Administrative operations");
            System.out.println("  - JOIN_AS_SERVER: Join as server node");

            System.out.println("\n=== Best Practices ===");
            System.out.println("1. Change default password immediately");
            System.out.println("2. Use strong passwords (min 12 chars, mixed case, numbers, symbols)");
            System.out.println("3. Create separate users for applications");
            System.out.println("4. Grant minimum required permissions");
            System.out.println("5. Rotate passwords regularly");
            System.out.println("6. Audit user activities");
            System.out.println("7. Disable or remove unused accounts");

            // Print connecting with authentication
            printClientAuthentication();

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteConfiguration createAuthenticatedConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("auth-node");

        // Authentication requires persistence
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        DataRegionConfiguration regionCfg = new DataRegionConfiguration();
        regionCfg.setName("auth-region");
        regionCfg.setPersistenceEnabled(true);
        regionCfg.setMaxSize(512L * 1024 * 1024); // 512MB
        storageCfg.setDefaultDataRegionConfiguration(regionCfg);
        cfg.setDataStorageConfiguration(storageCfg);

        // Enable authentication
        cfg.setAuthenticationEnabled(true);

        // Discovery
        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        spi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(spi);

        return cfg;
    }

    private static void printClientAuthentication() {
        System.out.println("\n=== Client Authentication ===\n");

        System.out.println("Thin Client Connection (Java):");
        System.out.println("```java");
        System.out.println("ClientConfiguration clientCfg = new ClientConfiguration()");
        System.out.println("    .setAddresses(\"127.0.0.1:10800\")");
        System.out.println("    .setUserName(\"admin\")");
        System.out.println("    .setUserPassword(\"AdminPass123!\");");
        System.out.println("");
        System.out.println("IgniteClient client = Ignition.startClient(clientCfg);");
        System.out.println("```");

        System.out.println("\nJDBC Connection:");
        System.out.println("jdbc:ignite:thin://127.0.0.1:10800?user=admin&password=AdminPass123!");

        System.out.println("\nREST API Authentication:");
        System.out.println("curl 'http://localhost:8080/ignite?cmd=version' \\");
        System.out.println("     --user admin:AdminPass123!");

        System.out.println("\n=== Authentication Troubleshooting ===");
        System.out.println("1. 'Authentication failed' - Check username/password");
        System.out.println("2. 'Authorization failed' - User lacks required permission");
        System.out.println("3. 'Persistence required' - Enable persistence for auth");
        System.out.println("4. 'Cluster not active' - Activate cluster first");
    }
}
