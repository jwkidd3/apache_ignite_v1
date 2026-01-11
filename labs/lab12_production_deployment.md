# Lab 12: Production Deployment and Best Practices

## Duration: 55 minutes

## Objectives
- Deploy production-ready Ignite clusters
- Configure security (authentication, SSL/TLS)
- Set up initial production configuration

## Prerequisites
- Completed Labs 1-11
- Linux/Unix command line experience
- Docker installed (only for optional exercises)
- Kubernetes knowledge (only for optional exercises)

## Part 1: Security Configuration (5 minutes)

### Exercise 1: Security Overview

Create `Lab12SecurityOverview.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

public class Lab12SecurityOverview {

    public static void main(String[] args) {
        System.out.println("=== Production Security Overview ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("secure-node");

        // Configure discovery
        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        spi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(spi);

        System.out.println("=== Security Configuration Layers ===");
        System.out.println("1. Network Security - Firewall rules, port restrictions");
        System.out.println("2. Transport Security - SSL/TLS encryption");
        System.out.println("3. Authentication - User credentials verification");
        System.out.println("4. Authorization - Permission-based access control");
        System.out.println("5. Audit Logging - Track security events\n");

        System.out.println("=== Security Best Practices ===");
        System.out.println("- Use strong passwords (min 12 characters)");
        System.out.println("- Enable SSL/TLS for all communication");
        System.out.println("- Implement role-based access control");
        System.out.println("- Audit security events");
        System.out.println("- Rotate credentials regularly (every 90 days)");
        System.out.println("- Use secure communication between nodes");
        System.out.println("- Keep Ignite version updated");

        System.out.println("\n=== Production Security Checklist ===");
        System.out.println("[ ] Authentication enabled");
        System.out.println("[ ] SSL/TLS configured");
        System.out.println("[ ] Firewall rules in place");
        System.out.println("[ ] Audit logging enabled");
        System.out.println("[ ] Regular security updates");
        System.out.println("[ ] Encrypted communication");
        System.out.println("[ ] Secure credential storage");
    }
}
```

## Part 2: Production Configuration (5 minutes)

### Exercise 2: Production-Ready Configuration

Create `production-config.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean class="org.apache.ignite.configuration.IgniteConfiguration">
        <!-- Cluster name for production -->
        <property name="igniteInstanceName" value="production-node"/>

        <!-- Data storage configuration -->
        <property name="dataStorageConfiguration">
            <bean class="org.apache.ignite.configuration.DataStorageConfiguration">
                <!-- Enable persistence -->
                <property name="defaultDataRegionConfiguration">
                    <bean class="org.apache.ignite.configuration.DataRegionConfiguration">
                        <property name="name" value="default-region"/>
                        <property name="persistenceEnabled" value="true"/>
                        <property name="maxSize" value="#{4L * 1024 * 1024 * 1024}"/>
                        <property name="metricsEnabled" value="true"/>
                    </bean>
                </property>
                <!-- WAL configuration -->
                <property name="walMode" value="FSYNC"/>
                <property name="walSegmentSize" value="#{256 * 1024 * 1024}"/>
                <property name="walArchivePath" value="/data/ignite/wal-archive"/>
                <property name="storagePath" value="/data/ignite/storage"/>
            </bean>
        </property>

        <!-- Discovery configuration -->
        <property name="discoverySpi">
            <bean class="org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi">
                <property name="localPort" value="47500"/>
                <property name="localPortRange" value="10"/>
                <property name="ipFinder">
                    <bean class="org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder">
                        <property name="addresses">
                            <list>
                                <value>node1.example.com:47500..47509</value>
                                <value>node2.example.com:47500..47509</value>
                                <value>node3.example.com:47500..47509</value>
                            </list>
                        </property>
                    </bean>
                </property>
            </bean>
        </property>

        <!-- Communication settings -->
        <property name="communicationSpi">
            <bean class="org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi">
                <property name="localPort" value="47100"/>
                <property name="messageQueueLimit" value="1024"/>
                <property name="socketWriteTimeout" value="5000"/>
            </bean>
        </property>

        <!-- Failure detection -->
        <property name="failureDetectionTimeout" value="10000"/>
        <property name="clientFailureDetectionTimeout" value="30000"/>
    </bean>
</beans>
```

## Part 3: Initial Cluster Setup (5 minutes)

### Exercise 3: Cluster Initialization

Create `Lab12ClusterSetup.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

public class Lab12ClusterSetup {

    public static void main(String[] args) {
        System.out.println("=== Production Cluster Setup ===\n");

        IgniteConfiguration cfg = createProductionConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Node started: " + ignite.name());

            // Activate cluster for persistence
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                System.out.println("Activating cluster...");
                ignite.cluster().state(ClusterState.ACTIVE);
            }

            // Print cluster info
            System.out.println("\n=== Cluster Information ===");
            System.out.println("Cluster state: " + ignite.cluster().state());
            System.out.println("Total nodes: " + ignite.cluster().nodes().size());
            System.out.println("Baseline topology: " +
                ignite.cluster().currentBaselineTopology());

            // Create production cache
            CacheConfiguration<Long, String> cacheCfg = new CacheConfiguration<>("production-cache");
            cacheCfg.setBackups(2);
            cacheCfg.setStatisticsEnabled(true);

            IgniteCache<Long, String> cache = ignite.getOrCreateCache(cacheCfg);

            // Test operations
            cache.put(1L, "Production data");
            System.out.println("\nCache created and tested successfully");
            System.out.println("Cache size: " + cache.size());

            Thread.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteConfiguration createProductionConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("production-node");

        // Data storage with persistence
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        DataRegionConfiguration regionCfg = new DataRegionConfiguration();
        regionCfg.setName("production-region");
        regionCfg.setPersistenceEnabled(true);
        regionCfg.setMaxSize(2L * 1024 * 1024 * 1024); // 2GB
        storageCfg.setDefaultDataRegionConfiguration(regionCfg);
        cfg.setDataStorageConfiguration(storageCfg);

        // Discovery
        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        spi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(spi);

        return cfg;
    }
}
```

## Part 4: SSL/TLS Configuration (15 minutes)

### Exercise 4: Generate Certificates and Configure SSL

Create `generate-certs.sh`:

```bash
#!/bin/bash

# SSL Certificate Generation Script for Apache Ignite
# This script creates keystores and truststores for secure cluster communication

set -e

KEYSTORE_DIR="./ssl"
PASSWORD="ignite-secure-password"
VALIDITY=365
KEY_SIZE=2048

echo "=== Apache Ignite SSL Certificate Generation ==="
echo ""

# Create directory
mkdir -p ${KEYSTORE_DIR}
cd ${KEYSTORE_DIR}

echo "Step 1: Creating Certificate Authority (CA)"
# Generate CA key pair
keytool -genkeypair \
    -alias ignite-ca \
    -keyalg RSA \
    -keysize ${KEY_SIZE} \
    -validity ${VALIDITY} \
    -keystore ca-keystore.jks \
    -storepass ${PASSWORD} \
    -keypass ${PASSWORD} \
    -dname "CN=Ignite CA, OU=Production, O=MyCompany, L=City, ST=State, C=US" \
    -ext bc:c

# Export CA certificate
keytool -exportcert \
    -alias ignite-ca \
    -keystore ca-keystore.jks \
    -storepass ${PASSWORD} \
    -file ca-cert.cer

echo "Step 2: Creating Node Certificates"

# Function to create node certificate
create_node_cert() {
    local NODE_NAME=$1
    local NODE_IP=$2

    echo "Creating certificate for ${NODE_NAME}..."

    # Generate node key pair
    keytool -genkeypair \
        -alias ${NODE_NAME} \
        -keyalg RSA \
        -keysize ${KEY_SIZE} \
        -validity ${VALIDITY} \
        -keystore ${NODE_NAME}-keystore.jks \
        -storepass ${PASSWORD} \
        -keypass ${PASSWORD} \
        -dname "CN=${NODE_NAME}, OU=Production, O=MyCompany, L=City, ST=State, C=US" \
        -ext san=ip:${NODE_IP},dns:${NODE_NAME},dns:localhost

    # Create certificate signing request
    keytool -certreq \
        -alias ${NODE_NAME} \
        -keystore ${NODE_NAME}-keystore.jks \
        -storepass ${PASSWORD} \
        -file ${NODE_NAME}.csr

    # Sign with CA (in production, use proper CA)
    keytool -gencert \
        -alias ignite-ca \
        -keystore ca-keystore.jks \
        -storepass ${PASSWORD} \
        -infile ${NODE_NAME}.csr \
        -outfile ${NODE_NAME}-signed.cer \
        -validity ${VALIDITY} \
        -ext san=ip:${NODE_IP},dns:${NODE_NAME},dns:localhost

    # Import CA certificate into node keystore
    keytool -importcert \
        -alias ignite-ca \
        -keystore ${NODE_NAME}-keystore.jks \
        -storepass ${PASSWORD} \
        -file ca-cert.cer \
        -noprompt

    # Import signed certificate into node keystore
    keytool -importcert \
        -alias ${NODE_NAME} \
        -keystore ${NODE_NAME}-keystore.jks \
        -storepass ${PASSWORD} \
        -file ${NODE_NAME}-signed.cer

    # Export node certificate for truststore
    keytool -exportcert \
        -alias ${NODE_NAME} \
        -keystore ${NODE_NAME}-keystore.jks \
        -storepass ${PASSWORD} \
        -file ${NODE_NAME}.cer
}

# Create certificates for 3 nodes
create_node_cert "node1" "192.168.1.10"
create_node_cert "node2" "192.168.1.11"
create_node_cert "node3" "192.168.1.12"

echo "Step 3: Creating Truststore"
# Create truststore with all certificates
keytool -importcert \
    -alias ignite-ca \
    -file ca-cert.cer \
    -keystore truststore.jks \
    -storepass ${PASSWORD} \
    -noprompt

keytool -importcert \
    -alias node1 \
    -file node1.cer \
    -keystore truststore.jks \
    -storepass ${PASSWORD} \
    -noprompt

keytool -importcert \
    -alias node2 \
    -file node2.cer \
    -keystore truststore.jks \
    -storepass ${PASSWORD} \
    -noprompt

keytool -importcert \
    -alias node3 \
    -file node3.cer \
    -keystore truststore.jks \
    -storepass ${PASSWORD} \
    -noprompt

echo ""
echo "=== Certificate Generation Complete ==="
echo "Files created in ${KEYSTORE_DIR}:"
echo "  - ca-keystore.jks (CA keystore)"
echo "  - node1-keystore.jks, node2-keystore.jks, node3-keystore.jks"
echo "  - truststore.jks (shared truststore)"
echo ""
echo "Password for all stores: ${PASSWORD}"
echo ""

# Cleanup CSR files
rm -f *.csr *-signed.cer

# List generated files
ls -la
```

Create `ssl-config.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean class="org.apache.ignite.configuration.IgniteConfiguration">
        <property name="igniteInstanceName" value="ssl-secured-node"/>

        <!-- SSL Context Factory for all communications -->
        <property name="sslContextFactory">
            <bean class="org.apache.ignite.ssl.SslContextFactory">
                <property name="keyStoreFilePath" value="/opt/ignite/ssl/node1-keystore.jks"/>
                <property name="keyStorePassword" value="ignite-secure-password"/>
                <property name="keyStoreType" value="JKS"/>
                <property name="trustStoreFilePath" value="/opt/ignite/ssl/truststore.jks"/>
                <property name="trustStorePassword" value="ignite-secure-password"/>
                <property name="trustStoreType" value="JKS"/>
                <property name="protocol" value="TLSv1.3"/>
                <property name="cipherSuites">
                    <list>
                        <value>TLS_AES_256_GCM_SHA384</value>
                        <value>TLS_AES_128_GCM_SHA256</value>
                        <value>TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384</value>
                    </list>
                </property>
            </bean>
        </property>

        <!-- Enable SSL for connector (REST, thin client) -->
        <property name="connectorConfiguration">
            <bean class="org.apache.ignite.configuration.ConnectorConfiguration">
                <property name="sslEnabled" value="true"/>
                <property name="sslClientAuth" value="true"/>
            </bean>
        </property>

        <!-- Enable SSL for client connections -->
        <property name="clientConnectorConfiguration">
            <bean class="org.apache.ignite.configuration.ClientConnectorConfiguration">
                <property name="sslEnabled" value="true"/>
                <property name="sslClientAuth" value="true"/>
            </bean>
        </property>
    </bean>
</beans>
```

Create `Lab12SSLConfiguration.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.ConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.ssl.SslContextFactory;

import java.util.Arrays;

public class Lab12SSLConfiguration {

    public static void main(String[] args) {
        System.out.println("=== SSL/TLS Configuration Lab ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("ssl-node");

        // Configure SSL Context Factory
        SslContextFactory sslFactory = new SslContextFactory();
        sslFactory.setKeyStoreFilePath("ssl/node1-keystore.jks");
        sslFactory.setKeyStorePassword("ignite-secure-password".toCharArray());
        sslFactory.setTrustStoreFilePath("ssl/truststore.jks");
        sslFactory.setTrustStorePassword("ignite-secure-password".toCharArray());
        sslFactory.setProtocol("TLSv1.3");

        // Set cipher suites for strong encryption
        sslFactory.setCipherSuites(new String[]{
            "TLS_AES_256_GCM_SHA384",
            "TLS_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"
        });

        cfg.setSslContextFactory(sslFactory);

        // Enable SSL for REST connector
        ConnectorConfiguration connectorCfg = new ConnectorConfiguration();
        connectorCfg.setSslEnabled(true);
        connectorCfg.setSslClientAuth(true);
        cfg.setConnectorConfiguration(connectorCfg);

        // Enable SSL for thin client connections
        ClientConnectorConfiguration clientConnectorCfg = new ClientConnectorConfiguration();
        clientConnectorCfg.setSslEnabled(true);
        clientConnectorCfg.setSslClientAuth(true);
        cfg.setClientConnectorConfiguration(clientConnectorCfg);

        // Configure discovery with SSL
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        // Configure communication SPI (inherits SSL from global config)
        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();
        commSpi.setLocalPort(47100);
        cfg.setCommunicationSpi(commSpi);

        System.out.println("=== SSL Configuration Summary ===");
        System.out.println("Keystore: ssl/node1-keystore.jks");
        System.out.println("Truststore: ssl/truststore.jks");
        System.out.println("Protocol: TLSv1.3");
        System.out.println("Client authentication: Required");
        System.out.println("Connector SSL: Enabled");
        System.out.println("Thin client SSL: Enabled");
        System.out.println("\n=== SSL Components Secured ===");
        System.out.println("1. Discovery (node-to-node)");
        System.out.println("2. Communication (data transfer)");
        System.out.println("3. REST API");
        System.out.println("4. Thin client connections");
        System.out.println("5. JDBC/ODBC connections");

        // Print verification steps
        System.out.println("\n=== Verification Steps ===");
        System.out.println("1. Check SSL handshake in logs");
        System.out.println("2. Verify certificate chain");
        System.out.println("3. Test connection rejection without cert");
        System.out.println("4. Monitor SSL metrics");
    }
}
```

## Part 5: Authentication Setup (10 minutes)

### Exercise 5: Enable Authentication and User Management

Create `Lab12Authentication.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

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

            Thread.sleep(3000);
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
}
```

Create `authentication-config.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean class="org.apache.ignite.configuration.IgniteConfiguration">
        <property name="igniteInstanceName" value="authenticated-node"/>

        <!-- Enable authentication -->
        <property name="authenticationEnabled" value="true"/>

        <!-- Persistence is required for authentication -->
        <property name="dataStorageConfiguration">
            <bean class="org.apache.ignite.configuration.DataStorageConfiguration">
                <property name="defaultDataRegionConfiguration">
                    <bean class="org.apache.ignite.configuration.DataRegionConfiguration">
                        <property name="name" value="auth-region"/>
                        <property name="persistenceEnabled" value="true"/>
                        <property name="maxSize" value="#{1L * 1024 * 1024 * 1024}"/>
                    </bean>
                </property>
            </bean>
        </property>

        <!-- Discovery configuration -->
        <property name="discoverySpi">
            <bean class="org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi">
                <property name="ipFinder">
                    <bean class="org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder">
                        <property name="addresses">
                            <list>
                                <value>127.0.0.1:47500..47509</value>
                            </list>
                        </property>
                    </bean>
                </property>
            </bean>
        </property>
    </bean>
</beans>
```

Create `user-setup.sql`:

```sql
-- User Setup Script for Apache Ignite
-- Run these commands after cluster is started with authentication enabled

-- First, change default ignite user password
ALTER USER ignite WITH PASSWORD 'StrongIgnitePass123!';

-- Create administrator user with full access
CREATE USER admin WITH PASSWORD 'AdminSecure456!';

-- Create application service account
CREATE USER app_user WITH PASSWORD 'AppService789!';

-- Create read-only monitoring user
CREATE USER monitor WITH PASSWORD 'Monitor321!';

-- Create backup operator user
CREATE USER backup_operator WITH PASSWORD 'Backup654!';

-- List all users (view system table)
-- SELECT * FROM SYS.USERS;

-- Best practices for passwords:
-- - Minimum 12 characters
-- - Mix of uppercase and lowercase
-- - Include numbers and special characters
-- - Avoid dictionary words
-- - Unique password for each user
-- - Regular rotation (every 90 days)
```

---

## Verification Steps

### Checklist
- [ ] Security overview completed
- [ ] Production configuration created
- [ ] Initial cluster setup verified
- [ ] SSL/TLS configured and tested
- [ ] Authentication setup completed

### Common Issues

**Issue: SSL handshake failures**
- Verify certificate chain is complete
- Check keystore/truststore passwords

**Issue: Authentication errors**
- Verify user credentials match configuration
- Check authentication plugin is enabled

## Lab Questions

1. Why is SSL/TLS important for production Ignite clusters?
2. What authentication mechanisms does Ignite support?
3. What are the key production configuration considerations?

## Answers

1. **SSL/TLS** encrypts data in transit between nodes and clients, preventing eavesdropping and tampering in production environments.

2. Ignite supports **basic authentication** (username/password) and can integrate with external systems like LDAP through custom plugins.

3. Key considerations include: network configuration (ports, IP addresses), memory settings, persistence configuration, security setup, and proper JVM tuning.

## Completion

You have completed Lab 12 when you can:
- Configure production-ready settings
- Set up SSL/TLS encryption
- Implement authentication

Congratulations! You have completed the Apache Ignite course!

---

## Optional Exercises (If Time Permits)

### Optional: Docker Deployment

Create `Dockerfile`:

```dockerfile
FROM openjdk:11-jre-slim

# Set Ignite version
ENV IGNITE_VERSION 2.16.0
ENV IGNITE_HOME /opt/ignite

# Install required packages
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        wget \
        unzip \
        curl \
    && rm -rf /var/lib/apt/lists/*

# Download and install Apache Ignite
RUN wget -q https://archive.apache.org/dist/ignite/${IGNITE_VERSION}/apache-ignite-${IGNITE_VERSION}-bin.zip \
    && unzip -q apache-ignite-${IGNITE_VERSION}-bin.zip -d /opt \
    && mv /opt/apache-ignite-${IGNITE_VERSION}-bin ${IGNITE_HOME} \
    && rm apache-ignite-${IGNITE_VERSION}-bin.zip

# Create directories for persistence and work
RUN mkdir -p ${IGNITE_HOME}/work \
    && mkdir -p /data/ignite/storage \
    && mkdir -p /data/ignite/wal \
    && mkdir -p /data/ignite/wal-archive \
    && mkdir -p ${IGNITE_HOME}/config

# Copy configuration files
COPY ignite-docker-config.xml ${IGNITE_HOME}/config/
COPY ssl/ ${IGNITE_HOME}/ssl/

# Set working directory
WORKDIR ${IGNITE_HOME}

# Expose ports
# 11211 - REST/Memcache
# 47100 - Communication
# 47500 - Discovery
# 49112 - JMX
# 10800 - Thin client
# 8080 - REST API
EXPOSE 11211 47100 47500 49112 10800 8080

# JVM options for production
ENV JVM_OPTS="-server \
    -Xms2g -Xmx2g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+DisableExplicitGC \
    -XX:+UseStringDeduplication \
    -XX:+ParallelRefProcEnabled \
    -XX:+OptimizeStringConcat \
    -Djava.net.preferIPv4Stack=true"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -f http://localhost:8080/ignite?cmd=version || exit 1

# Start Ignite with configuration
ENTRYPOINT ["sh", "-c"]
CMD ["${IGNITE_HOME}/bin/ignite.sh ${IGNITE_HOME}/config/ignite-docker-config.xml"]
```

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  ignite-node1:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ignite-node1
    hostname: ignite-node1
    environment:
      - IGNITE_INSTANCE_NAME=node1
      - OPTION_LIBS=ignite-rest-http
      - JVM_OPTS=-Xms2g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
    ports:
      - "10800:10800"  # Thin client
      - "8080:8080"    # REST API
      - "47100:47100"  # Communication (first node only)
      - "47500:47500"  # Discovery (first node only)
    volumes:
      - ignite-node1-data:/data/ignite
      - ./config:/opt/ignite/config:ro
    networks:
      ignite-net:
        ipv4_address: 172.28.0.10
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/ignite?cmd=version"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

  ignite-node2:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ignite-node2
    hostname: ignite-node2
    environment:
      - IGNITE_INSTANCE_NAME=node2
      - OPTION_LIBS=ignite-rest-http
      - JVM_OPTS=-Xms2g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
    volumes:
      - ignite-node2-data:/data/ignite
      - ./config:/opt/ignite/config:ro
    networks:
      ignite-net:
        ipv4_address: 172.28.0.11
    depends_on:
      - ignite-node1
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/ignite?cmd=version"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

  ignite-node3:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ignite-node3
    hostname: ignite-node3
    environment:
      - IGNITE_INSTANCE_NAME=node3
      - OPTION_LIBS=ignite-rest-http
      - JVM_OPTS=-Xms2g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
    volumes:
      - ignite-node3-data:/data/ignite
      - ./config:/opt/ignite/config:ro
    networks:
      ignite-net:
        ipv4_address: 172.28.0.12
    depends_on:
      - ignite-node1
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/ignite?cmd=version"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

volumes:
  ignite-node1-data:
    driver: local
  ignite-node2-data:
    driver: local
  ignite-node3-data:
    driver: local

networks:
  ignite-net:
    driver: bridge
    ipam:
      driver: default
      config:
        - subnet: 172.28.0.0/16
          gateway: 172.28.0.1
```

Create `ignite-docker-config.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean class="org.apache.ignite.configuration.IgniteConfiguration">
        <!-- Data storage for persistence -->
        <property name="dataStorageConfiguration">
            <bean class="org.apache.ignite.configuration.DataStorageConfiguration">
                <property name="defaultDataRegionConfiguration">
                    <bean class="org.apache.ignite.configuration.DataRegionConfiguration">
                        <property name="name" value="docker-region"/>
                        <property name="persistenceEnabled" value="true"/>
                        <property name="maxSize" value="#{2L * 1024 * 1024 * 1024}"/>
                        <property name="metricsEnabled" value="true"/>
                    </bean>
                </property>
                <property name="storagePath" value="/data/ignite/storage"/>
                <property name="walPath" value="/data/ignite/wal"/>
                <property name="walArchivePath" value="/data/ignite/wal-archive"/>
            </bean>
        </property>

        <!-- Discovery for Docker network -->
        <property name="discoverySpi">
            <bean class="org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi">
                <property name="ipFinder">
                    <bean class="org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder">
                        <property name="addresses">
                            <list>
                                <value>ignite-node1:47500..47509</value>
                                <value>ignite-node2:47500..47509</value>
                                <value>ignite-node3:47500..47509</value>
                            </list>
                        </property>
                    </bean>
                </property>
            </bean>
        </property>

        <!-- Communication settings -->
        <property name="communicationSpi">
            <bean class="org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi">
                <property name="localPort" value="47100"/>
                <property name="messageQueueLimit" value="1024"/>
            </bean>
        </property>

        <!-- Client connector for thin clients -->
        <property name="clientConnectorConfiguration">
            <bean class="org.apache.ignite.configuration.ClientConnectorConfiguration">
                <property name="port" value="10800"/>
            </bean>
        </property>

        <!-- Failure detection -->
        <property name="failureDetectionTimeout" value="10000"/>
        <property name="clientFailureDetectionTimeout" value="30000"/>

        <!-- Metrics -->
        <property name="metricsLogFrequency" value="60000"/>
    </bean>
</beans>
```

Create `docker-commands.sh`:

```bash
#!/bin/bash

echo "=== Docker Deployment Commands ==="
echo ""

echo "# Build Docker images"
echo "docker-compose build"
echo ""

echo "# Start 3-node cluster"
echo "docker-compose up -d"
echo ""

echo "# View logs for all nodes"
echo "docker-compose logs -f"
echo ""

echo "# View logs for specific node"
echo "docker-compose logs -f ignite-node1"
echo ""

echo "# Check cluster status"
echo "curl 'http://localhost:8080/ignite?cmd=version'"
echo "curl 'http://localhost:8080/ignite?cmd=top'"
echo ""

echo "# Scale cluster (add more nodes)"
echo "docker-compose up -d --scale ignite-node2=3"
echo ""

echo "# Execute command in container"
echo "docker exec -it ignite-node1 /opt/ignite/bin/ignite.sh --baseline"
echo ""

echo "# Stop cluster gracefully"
echo "docker-compose stop"
echo ""

echo "# Remove cluster and volumes"
echo "docker-compose down -v"
echo ""

echo "# Check container health"
echo "docker inspect --format='{{.State.Health.Status}}' ignite-node1"
echo ""

echo "# View container resource usage"
echo "docker stats ignite-node1 ignite-node2 ignite-node3"
```

### Optional: Kubernetes Deployment

### Exercise 7: Deploy on Kubernetes

Create `ignite-namespace.yaml`:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ignite
  labels:
    app: ignite
    environment: production
```

Create `ignite-rbac.yaml`:

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: ignite
  namespace: ignite
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: ignite-role
  namespace: ignite
rules:
- apiGroups: [""]
  resources: ["pods", "endpoints"]
  verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: ignite-role-binding
  namespace: ignite
subjects:
- kind: ServiceAccount
  name: ignite
  namespace: ignite
roleRef:
  kind: Role
  name: ignite-role
  apiGroup: rbac.authorization.k8s.io
```

Create `ignite-configmap.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ignite-config
  namespace: ignite
data:
  ignite-config.xml: |
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans
                               http://www.springframework.org/schema/beans/spring-beans.xsd">

        <bean class="org.apache.ignite.configuration.IgniteConfiguration">
            <!-- Enable peer class loading -->
            <property name="peerClassLoadingEnabled" value="true"/>

            <!-- Data storage configuration -->
            <property name="dataStorageConfiguration">
                <bean class="org.apache.ignite.configuration.DataStorageConfiguration">
                    <property name="defaultDataRegionConfiguration">
                        <bean class="org.apache.ignite.configuration.DataRegionConfiguration">
                            <property name="name" value="k8s-region"/>
                            <property name="persistenceEnabled" value="true"/>
                            <property name="maxSize" value="#{2L * 1024 * 1024 * 1024}"/>
                            <property name="metricsEnabled" value="true"/>
                        </bean>
                    </property>
                    <property name="storagePath" value="/persistence/storage"/>
                    <property name="walPath" value="/persistence/wal"/>
                    <property name="walArchivePath" value="/persistence/wal-archive"/>
                </bean>
            </property>

            <!-- Kubernetes discovery -->
            <property name="discoverySpi">
                <bean class="org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi">
                    <property name="ipFinder">
                        <bean class="org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder">
                            <property name="namespace" value="ignite"/>
                            <property name="serviceName" value="ignite-discovery"/>
                            <property name="masterUrl" value="https://kubernetes.default.svc.cluster.local:443"/>
                            <property name="accountToken" value="/var/run/secrets/kubernetes.io/serviceaccount/token"/>
                        </bean>
                    </property>
                </bean>
            </property>

            <!-- Communication configuration -->
            <property name="communicationSpi">
                <bean class="org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi">
                    <property name="localPort" value="47100"/>
                </bean>
            </property>

            <!-- Failure detection -->
            <property name="failureDetectionTimeout" value="10000"/>
        </bean>
    </beans>
```

Create `ignite-service.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: ignite-service
  namespace: ignite
  labels:
    app: ignite
spec:
  type: LoadBalancer
  ports:
    - name: rest
      port: 8080
      targetPort: 8080
    - name: thin-client
      port: 10800
      targetPort: 10800
    - name: thick-client
      port: 47100
      targetPort: 47100
  selector:
    app: ignite
---
apiVersion: v1
kind: Service
metadata:
  name: ignite-discovery
  namespace: ignite
  labels:
    app: ignite
spec:
  # Headless service for discovery
  clusterIP: None
  ports:
    - name: discovery
      port: 47500
      targetPort: 47500
    - name: communication
      port: 47100
      targetPort: 47100
  selector:
    app: ignite
```

Create `ignite-statefulset.yaml`:

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: ignite-cluster
  namespace: ignite
spec:
  serviceName: ignite-discovery
  replicas: 3
  selector:
    matchLabels:
      app: ignite
  template:
    metadata:
      labels:
        app: ignite
    spec:
      serviceAccountName: ignite
      terminationGracePeriodSeconds: 60
      containers:
      - name: ignite
        image: apacheignite/ignite:2.16.0
        ports:
        - containerPort: 47100
          name: communication
        - containerPort: 47500
          name: discovery
        - containerPort: 10800
          name: thin-client
        - containerPort: 8080
          name: rest
        env:
        - name: IGNITE_INSTANCE_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: OPTION_LIBS
          value: "ignite-kubernetes,ignite-rest-http"
        - name: CONFIG_URI
          value: "/config/ignite-config.xml"
        - name: JVM_OPTS
          value: "-server -Xms2g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+DisableExplicitGC"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /ignite?cmd=version
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
        readinessProbe:
          httpGet:
            path: /ignite?cmd=version
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
        volumeMounts:
        - name: ignite-storage
          mountPath: /persistence
        - name: config-volume
          mountPath: /config
      volumes:
      - name: config-volume
        configMap:
          name: ignite-config
  volumeClaimTemplates:
  - metadata:
      name: ignite-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: standard
      resources:
        requests:
          storage: 10Gi
```

Create `ignite-pdb.yaml` (Pod Disruption Budget):

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: ignite-pdb
  namespace: ignite
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: ignite
```

Create `k8s-commands.sh`:

```bash
#!/bin/bash

echo "=== Kubernetes Deployment Commands ==="
echo ""

echo "# Create namespace"
echo "kubectl apply -f ignite-namespace.yaml"
echo ""

echo "# Deploy RBAC (required for Kubernetes discovery)"
echo "kubectl apply -f ignite-rbac.yaml"
echo ""

echo "# Deploy ConfigMap"
echo "kubectl apply -f ignite-configmap.yaml"
echo ""

echo "# Deploy Services"
echo "kubectl apply -f ignite-service.yaml"
echo ""

echo "# Deploy StatefulSet"
echo "kubectl apply -f ignite-statefulset.yaml"
echo ""

echo "# Deploy Pod Disruption Budget"
echo "kubectl apply -f ignite-pdb.yaml"
echo ""

echo "# Check pods status"
echo "kubectl get pods -n ignite -w"
echo ""

echo "# Check services"
echo "kubectl get svc -n ignite"
echo ""

echo "# View logs"
echo "kubectl logs -f ignite-cluster-0 -n ignite"
echo ""

echo "# Check cluster topology"
echo "kubectl exec -it ignite-cluster-0 -n ignite -- /opt/ignite/bin/control.sh --baseline"
echo ""

echo "# Scale cluster"
echo "kubectl scale statefulset ignite-cluster --replicas=5 -n ignite"
echo ""

echo "# Port forward for local access"
echo "kubectl port-forward svc/ignite-service 10800:10800 -n ignite"
echo ""

echo "# Delete deployment"
echo "kubectl delete namespace ignite"
```

### Optional: Backup and Recovery

### Exercise 8: Snapshot and Recovery Operations

Create `Lab12BackupRecovery.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteSnapshot;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class Lab12BackupRecovery {

    public static void main(String[] args) {
        System.out.println("=== Backup and Recovery Lab ===\n");

        IgniteConfiguration cfg = createPersistentConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            // Activate cluster
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                ignite.cluster().state(ClusterState.ACTIVE);
            }

            System.out.println("Cluster activated\n");

            // Create test cache with data
            CacheConfiguration<Long, String> cacheCfg = new CacheConfiguration<>("backup-test-cache");
            cacheCfg.setBackups(1);
            IgniteCache<Long, String> cache = ignite.getOrCreateCache(cacheCfg);

            // Populate test data
            System.out.println("Populating test data...");
            for (long i = 1; i <= 1000; i++) {
                cache.put(i, "Value-" + i);
            }
            System.out.println("Inserted 1000 records\n");

            // Demonstrate snapshot operations
            IgniteSnapshot snapshotApi = ignite.snapshot();

            String snapshotName = "backup_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            System.out.println("=== Creating Snapshot ===");
            System.out.println("Snapshot name: " + snapshotName);

            try {
                // Create snapshot
                snapshotApi.createSnapshot(snapshotName).get();
                System.out.println("Snapshot created successfully!\n");
            } catch (Exception e) {
                System.out.println("Snapshot creation: " + e.getMessage());
                System.out.println("(This is expected in single-node development mode)\n");
            }

            // Print snapshot commands
            System.out.println("=== Snapshot CLI Commands ===\n");

            System.out.println("# Create a snapshot");
            System.out.println("control.sh --snapshot create my_snapshot");
            System.out.println("");

            System.out.println("# List all snapshots");
            System.out.println("control.sh --snapshot list");
            System.out.println("");

            System.out.println("# Check snapshot status");
            System.out.println("control.sh --snapshot status my_snapshot");
            System.out.println("");

            System.out.println("# Restore from snapshot");
            System.out.println("control.sh --snapshot restore my_snapshot");
            System.out.println("");

            System.out.println("# Restore specific caches");
            System.out.println("control.sh --snapshot restore my_snapshot --caches cache1,cache2");
            System.out.println("");

            System.out.println("# Cancel running snapshot");
            System.out.println("control.sh --snapshot cancel my_snapshot");
            System.out.println("");

            System.out.println("=== WAL Archiving Configuration ===\n");
            System.out.println("WAL archiving enables point-in-time recovery:");
            System.out.println("  - walArchivePath: /data/ignite/wal-archive");
            System.out.println("  - walMode: FSYNC (recommended for production)");
            System.out.println("  - walSegmentSize: 256MB (default)");
            System.out.println("  - walHistorySize: 20 (segments to keep)");
            System.out.println("");

            System.out.println("=== Recovery Procedures ===\n");

            System.out.println("1. Full Recovery from Snapshot:");
            System.out.println("   a. Stop all cluster nodes");
            System.out.println("   b. Clear work directories (optional)");
            System.out.println("   c. Start cluster nodes");
            System.out.println("   d. Activate cluster");
            System.out.println("   e. Run: control.sh --snapshot restore my_snapshot");
            System.out.println("");

            System.out.println("2. Point-in-Time Recovery:");
            System.out.println("   a. Restore from last full snapshot");
            System.out.println("   b. Apply WAL archives up to desired point");
            System.out.println("   c. Verify data consistency");
            System.out.println("");

            System.out.println("3. Single Node Recovery:");
            System.out.println("   a. Stop failed node");
            System.out.println("   b. Clear node's work directory");
            System.out.println("   c. Restart node");
            System.out.println("   d. Node will sync from cluster automatically");

            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteConfiguration createPersistentConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("backup-node");

        // Configure persistence with WAL archiving
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        DataRegionConfiguration regionCfg = new DataRegionConfiguration();
        regionCfg.setName("backup-region");
        regionCfg.setPersistenceEnabled(true);
        regionCfg.setMaxSize(1L * 1024 * 1024 * 1024); // 1GB
        storageCfg.setDefaultDataRegionConfiguration(regionCfg);

        // WAL configuration for recovery
        storageCfg.setWalArchivePath("./ignite-work/wal-archive");
        storageCfg.setWalPath("./ignite-work/wal");
        storageCfg.setStoragePath("./ignite-work/storage");

        cfg.setDataStorageConfiguration(storageCfg);

        // Discovery
        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        spi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(spi);

        return cfg;
    }
}
```

Create `backup-script.sh`:

```bash
#!/bin/bash

# Apache Ignite Backup Script
# Run this script on a schedule for automated backups

set -e

# Configuration
IGNITE_HOME=${IGNITE_HOME:-/opt/ignite}
BACKUP_DIR=${BACKUP_DIR:-/backup/ignite}
CONTROL_SCRIPT="${IGNITE_HOME}/bin/control.sh"
RETENTION_DAYS=${RETENTION_DAYS:-7}
LOG_FILE="${BACKUP_DIR}/backup.log"

# Create backup directory if not exists
mkdir -p ${BACKUP_DIR}

# Generate snapshot name with timestamp
SNAPSHOT_NAME="snapshot_$(date +%Y%m%d_%H%M%S)"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a ${LOG_FILE}
}

log "=== Starting Ignite Backup ==="
log "Snapshot name: ${SNAPSHOT_NAME}"

# Check cluster state
log "Checking cluster state..."
CLUSTER_STATE=$(${CONTROL_SCRIPT} --state 2>/dev/null | grep -o "Cluster state: [A-Z]*" | cut -d' ' -f3)

if [ "${CLUSTER_STATE}" != "ACTIVE" ]; then
    log "ERROR: Cluster is not active (state: ${CLUSTER_STATE})"
    exit 1
fi

log "Cluster state: ${CLUSTER_STATE}"

# Create snapshot
log "Creating snapshot..."
${CONTROL_SCRIPT} --snapshot create ${SNAPSHOT_NAME}

if [ $? -eq 0 ]; then
    log "Snapshot created successfully"
else
    log "ERROR: Snapshot creation failed"
    exit 1
fi

# Wait for snapshot to complete
log "Waiting for snapshot to complete..."
sleep 5

# Verify snapshot
log "Verifying snapshot..."
${CONTROL_SCRIPT} --snapshot check ${SNAPSHOT_NAME}

# Copy snapshot to backup location
SNAPSHOT_PATH="${IGNITE_HOME}/work/snapshots/${SNAPSHOT_NAME}"
if [ -d "${SNAPSHOT_PATH}" ]; then
    log "Copying snapshot to backup location..."
    cp -r ${SNAPSHOT_PATH} ${BACKUP_DIR}/
    log "Snapshot copied to ${BACKUP_DIR}/${SNAPSHOT_NAME}"
fi

# Cleanup old backups
log "Cleaning up backups older than ${RETENTION_DAYS} days..."
find ${BACKUP_DIR} -name "snapshot_*" -type d -mtime +${RETENTION_DAYS} -exec rm -rf {} \;

# List current backups
log "Current backups:"
ls -la ${BACKUP_DIR}/snapshot_* 2>/dev/null || echo "No backups found"

log "=== Backup Complete ==="
```

Create `restore-script.sh`:

```bash
#!/bin/bash

# Apache Ignite Restore Script
# Use this script to restore from a snapshot

set -e

# Configuration
IGNITE_HOME=${IGNITE_HOME:-/opt/ignite}
CONTROL_SCRIPT="${IGNITE_HOME}/bin/control.sh"

if [ -z "$1" ]; then
    echo "Usage: $0 <snapshot_name> [cache_names]"
    echo ""
    echo "Examples:"
    echo "  $0 snapshot_20240115_120000           # Restore all caches"
    echo "  $0 snapshot_20240115_120000 cache1    # Restore specific cache"
    echo ""
    echo "Available snapshots:"
    ${CONTROL_SCRIPT} --snapshot list 2>/dev/null || echo "Cannot list snapshots"
    exit 1
fi

SNAPSHOT_NAME=$1
CACHES=$2

echo "=== Apache Ignite Restore ==="
echo "Snapshot: ${SNAPSHOT_NAME}"
echo ""

# Check cluster state
echo "Checking cluster state..."
CLUSTER_STATE=$(${CONTROL_SCRIPT} --state 2>/dev/null | grep -o "Cluster state: [A-Z]*" | cut -d' ' -f3)
echo "Current state: ${CLUSTER_STATE}"

# Warn about data loss
echo ""
echo "WARNING: This will restore data from snapshot."
echo "Any changes made after the snapshot will be lost."
echo ""
read -p "Continue? (yes/no): " CONFIRM

if [ "${CONFIRM}" != "yes" ]; then
    echo "Restore cancelled."
    exit 0
fi

# Perform restore
echo ""
echo "Starting restore..."

if [ -n "${CACHES}" ]; then
    echo "Restoring caches: ${CACHES}"
    ${CONTROL_SCRIPT} --snapshot restore ${SNAPSHOT_NAME} --caches ${CACHES}
else
    echo "Restoring all caches"
    ${CONTROL_SCRIPT} --snapshot restore ${SNAPSHOT_NAME}
fi

if [ $? -eq 0 ]; then
    echo ""
    echo "=== Restore Complete ==="
    echo "Verify data integrity by checking cache sizes and running test queries."
else
    echo ""
    echo "ERROR: Restore failed"
    exit 1
fi
```

### Optional: Rolling Updates

### Exercise 9: Zero-Downtime Upgrade Procedure

Create `Lab12RollingUpdate.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.BaselineNode;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;
import java.util.Collection;

public class Lab12RollingUpdate {

    public static void main(String[] args) {
        System.out.println("=== Rolling Update Procedure Lab ===\n");

        IgniteConfiguration cfg = createConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            // Activate cluster
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                ignite.cluster().state(ClusterState.ACTIVE);
            }

            System.out.println("=== Current Cluster State ===");
            System.out.println("Cluster state: " + ignite.cluster().state());
            System.out.println("Total nodes: " + ignite.cluster().nodes().size());

            // Show baseline topology
            Collection<BaselineNode> baseline = ignite.cluster().currentBaselineTopology();
            System.out.println("Baseline nodes: " + (baseline != null ? baseline.size() : 0));

            // List nodes
            System.out.println("\nCluster nodes:");
            for (ClusterNode node : ignite.cluster().nodes()) {
                System.out.println("  - " + node.consistentId() +
                    " (server=" + !node.isClient() + ")");
            }

            // Print rolling update procedure
            System.out.println("\n=== Rolling Update Procedure ===\n");

            System.out.println("Phase 1: Pre-Update Preparation");
            System.out.println("  1. Create full backup (snapshot)");
            System.out.println("  2. Verify all nodes healthy");
            System.out.println("  3. Check rebalancing complete");
            System.out.println("  4. Document current baseline");
            System.out.println("  5. Notify stakeholders of maintenance");
            System.out.println("");

            System.out.println("Phase 2: Disable Auto-Baseline");
            System.out.println("  control.sh --baseline auto_adjust disable");
            System.out.println("  (Prevents automatic baseline changes during update)");
            System.out.println("");

            System.out.println("Phase 3: Update Each Node (one at a time)");
            System.out.println("  For each server node:");
            System.out.println("    a. Gracefully stop the node:");
            System.out.println("       kill -SIGTERM <pid>");
            System.out.println("       OR: control.sh --shutdown --node-id <node-id>");
            System.out.println("");
            System.out.println("    b. Wait for rebalancing (data redistribution):");
            System.out.println("       control.sh --cache idle_verify");
            System.out.println("");
            System.out.println("    c. Update Ignite binaries");
            System.out.println("       - Replace JAR files");
            System.out.println("       - Update configuration if needed");
            System.out.println("");
            System.out.println("    d. Start updated node:");
            System.out.println("       ignite.sh config/ignite-config.xml");
            System.out.println("");
            System.out.println("    e. Wait for node to join and sync:");
            System.out.println("       control.sh --baseline");
            System.out.println("");
            System.out.println("    f. Verify node health:");
            System.out.println("       curl 'http://localhost:8080/ignite?cmd=top'");
            System.out.println("");
            System.out.println("    g. Wait for rebalancing to complete");
            System.out.println("");
            System.out.println("    h. Proceed to next node");
            System.out.println("");

            System.out.println("Phase 4: Re-enable Auto-Baseline");
            System.out.println("  control.sh --baseline auto_adjust enable");
            System.out.println("");

            System.out.println("Phase 5: Post-Update Verification");
            System.out.println("  1. Verify all nodes running new version");
            System.out.println("  2. Check cluster state: ACTIVE");
            System.out.println("  3. Verify data integrity:");
            System.out.println("     control.sh --cache idle_verify");
            System.out.println("  4. Run application tests");
            System.out.println("  5. Monitor for 24 hours");
            System.out.println("");

            System.out.println("=== Rollback Procedure ===\n");

            System.out.println("If issues detected during update:");
            System.out.println("  1. Stop the problematic node");
            System.out.println("  2. Restore previous version binaries");
            System.out.println("  3. Restart node");
            System.out.println("  4. Verify cluster health");
            System.out.println("");

            System.out.println("Full rollback (restore from backup):");
            System.out.println("  1. Stop all nodes");
            System.out.println("  2. Restore previous version on all nodes");
            System.out.println("  3. Restore from pre-update snapshot");
            System.out.println("  4. Start cluster");
            System.out.println("  5. Verify data integrity");

            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteConfiguration createConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("update-node");

        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        DataRegionConfiguration regionCfg = new DataRegionConfiguration();
        regionCfg.setName("update-region");
        regionCfg.setPersistenceEnabled(true);
        storageCfg.setDefaultDataRegionConfiguration(regionCfg);
        cfg.setDataStorageConfiguration(storageCfg);

        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        spi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(spi);

        return cfg;
    }
}
```

Create `rolling-update.sh`:

```bash
#!/bin/bash

# Apache Ignite Rolling Update Script
# Performs zero-downtime update of cluster nodes

set -e

IGNITE_HOME=${IGNITE_HOME:-/opt/ignite}
CONTROL_SCRIPT="${IGNITE_HOME}/bin/control.sh"
NEW_VERSION_DIR=${1:-"/opt/ignite-new"}
WAIT_TIMEOUT=300  # 5 minutes

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

check_prerequisites() {
    log "Checking prerequisites..."

    # Check new version exists
    if [ ! -d "${NEW_VERSION_DIR}" ]; then
        log "ERROR: New version directory not found: ${NEW_VERSION_DIR}"
        exit 1
    fi

    # Check cluster is active
    STATE=$(${CONTROL_SCRIPT} --state 2>/dev/null | grep "Cluster state:" | awk '{print $3}')
    if [ "${STATE}" != "ACTIVE" ]; then
        log "ERROR: Cluster must be ACTIVE (current: ${STATE})"
        exit 1
    fi

    log "Prerequisites OK"
}

disable_auto_baseline() {
    log "Disabling auto-baseline adjustment..."
    ${CONTROL_SCRIPT} --baseline auto_adjust disable
}

enable_auto_baseline() {
    log "Re-enabling auto-baseline adjustment..."
    ${CONTROL_SCRIPT} --baseline auto_adjust enable
}

wait_for_rebalance() {
    log "Waiting for rebalancing to complete..."

    for i in $(seq 1 ${WAIT_TIMEOUT}); do
        # Check if rebalancing is in progress
        REBALANCE=$(${CONTROL_SCRIPT} --cache idle_verify 2>&1 | grep -c "MOVING" || true)

        if [ "${REBALANCE}" -eq 0 ]; then
            log "Rebalancing complete"
            return 0
        fi

        sleep 1
    done

    log "WARNING: Rebalancing still in progress after ${WAIT_TIMEOUT} seconds"
}

verify_cluster_health() {
    log "Verifying cluster health..."

    # Check idle_verify
    ${CONTROL_SCRIPT} --cache idle_verify

    # Check cluster state
    ${CONTROL_SCRIPT} --state

    log "Cluster health verified"
}

update_node() {
    local NODE_ID=$1
    local NODE_HOST=$2

    log "=== Updating node: ${NODE_ID} on ${NODE_HOST} ==="

    # 1. Stop node gracefully
    log "Stopping node ${NODE_ID}..."
    ssh ${NODE_HOST} "pkill -SIGTERM -f 'ignite.sh' || true"
    sleep 10

    # 2. Wait for cluster to detect node departure
    log "Waiting for cluster to stabilize..."
    wait_for_rebalance

    # 3. Backup old version
    log "Backing up old version..."
    ssh ${NODE_HOST} "mv ${IGNITE_HOME} ${IGNITE_HOME}.backup.$(date +%Y%m%d)"

    # 4. Install new version
    log "Installing new version..."
    ssh ${NODE_HOST} "cp -r ${NEW_VERSION_DIR} ${IGNITE_HOME}"

    # 5. Copy configuration
    log "Copying configuration..."
    ssh ${NODE_HOST} "cp ${IGNITE_HOME}.backup.*/config/* ${IGNITE_HOME}/config/"

    # 6. Start node
    log "Starting updated node..."
    ssh ${NODE_HOST} "nohup ${IGNITE_HOME}/bin/ignite.sh ${IGNITE_HOME}/config/ignite-config.xml > /dev/null 2>&1 &"

    # 7. Wait for node to join
    log "Waiting for node to join cluster..."
    sleep 30

    # 8. Wait for rebalancing
    wait_for_rebalance

    # 9. Verify
    verify_cluster_health

    log "Node ${NODE_ID} updated successfully"
}

# Main
log "=== Apache Ignite Rolling Update ==="

check_prerequisites

# Create backup before update
log "Creating pre-update backup..."
SNAPSHOT_NAME="pre_update_$(date +%Y%m%d_%H%M%S)"
${CONTROL_SCRIPT} --snapshot create ${SNAPSHOT_NAME}
log "Backup created: ${SNAPSHOT_NAME}"

# Disable auto-baseline
disable_auto_baseline

# Get list of nodes (customize for your environment)
# NODES=("node1:192.168.1.10" "node2:192.168.1.11" "node3:192.168.1.12")
# for NODE in "${NODES[@]}"; do
#     NODE_ID=$(echo $NODE | cut -d: -f1)
#     NODE_HOST=$(echo $NODE | cut -d: -f2)
#     update_node ${NODE_ID} ${NODE_HOST}
# done

log "Note: Customize NODES array for your cluster topology"
log "Example usage: update_node node1 192.168.1.10"

# Re-enable auto-baseline
enable_auto_baseline

# Final verification
verify_cluster_health

log "=== Rolling Update Complete ==="
```

### Optional: Challenge Exercises

### Challenge 1: Production Health Checker

Create `Lab12HealthChecker.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.*;

public class Lab12HealthChecker {

    // Health check thresholds
    private static final int MIN_NODES = 3;
    private static final double MAX_HEAP_USAGE_PERCENT = 85.0;
    private static final double MAX_CPU_LOAD_PERCENT = 80.0;
    private static final long MAX_AVG_GET_TIME_NS = 1_000_000; // 1ms

    public static void main(String[] args) {
        System.out.println("=== Production Health Checker ===\n");

        IgniteConfiguration cfg = createConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            // Activate if needed
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                ignite.cluster().state(ClusterState.ACTIVE);
            }

            // Create test cache
            CacheConfiguration<Long, String> cacheCfg = new CacheConfiguration<>("health-check-cache");
            cacheCfg.setStatisticsEnabled(true);
            IgniteCache<Long, String> cache = ignite.getOrCreateCache(cacheCfg);

            // Add test data
            for (long i = 0; i < 100; i++) {
                cache.put(i, "value-" + i);
            }

            // Run health checks
            HealthCheckResult result = runHealthChecks(ignite);

            // Print results
            printHealthReport(result);

            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static HealthCheckResult runHealthChecks(Ignite ignite) {
        HealthCheckResult result = new HealthCheckResult();

        // Check 1: Cluster State
        ClusterState state = ignite.cluster().state();
        result.addCheck("Cluster State",
            state == ClusterState.ACTIVE ? "PASS" : "FAIL",
            "Current: " + state);

        // Check 2: Node Count
        int nodeCount = ignite.cluster().nodes().size();
        result.addCheck("Node Count",
            nodeCount >= MIN_NODES ? "PASS" : "WARN",
            "Nodes: " + nodeCount + " (min: " + MIN_NODES + ")");

        // Check 3: No Client Nodes Only
        long serverNodes = ignite.cluster().nodes().stream()
            .filter(n -> !n.isClient()).count();
        result.addCheck("Server Nodes",
            serverNodes > 0 ? "PASS" : "FAIL",
            "Server nodes: " + serverNodes);

        // Check 4: Node Health (per node)
        for (ClusterNode node : ignite.cluster().nodes()) {
            ClusterMetrics metrics = node.metrics();

            // Heap usage
            double heapUsage = (double) metrics.getHeapMemoryUsed() /
                              metrics.getHeapMemoryMaximum() * 100;
            result.addCheck("Heap Usage (" + node.consistentId() + ")",
                heapUsage < MAX_HEAP_USAGE_PERCENT ? "PASS" : "WARN",
                String.format("%.1f%% (max: %.1f%%)", heapUsage, MAX_HEAP_USAGE_PERCENT));

            // CPU load
            double cpuLoad = metrics.getCurrentCpuLoad() * 100;
            result.addCheck("CPU Load (" + node.consistentId() + ")",
                cpuLoad < MAX_CPU_LOAD_PERCENT ? "PASS" : "WARN",
                String.format("%.1f%% (max: %.1f%%)", cpuLoad, MAX_CPU_LOAD_PERCENT));
        }

        // Check 5: Cache Health
        for (String cacheName : ignite.cacheNames()) {
            IgniteCache<?, ?> cache = ignite.cache(cacheName);
            if (cache != null) {
                CacheMetrics cacheMetrics = cache.localMetrics();

                // Check get time
                float avgGetTime = cacheMetrics.getAverageGetTime();
                result.addCheck("Cache Get Time (" + cacheName + ")",
                    avgGetTime < MAX_AVG_GET_TIME_NS / 1_000_000.0 ? "PASS" : "WARN",
                    String.format("%.2f ms", avgGetTime));

                // Check hit rate (if applicable)
                float hitRate = cacheMetrics.getCacheHitPercentage();
                result.addCheck("Cache Hit Rate (" + cacheName + ")",
                    hitRate > 80 ? "PASS" : (hitRate > 50 ? "WARN" : "INFO"),
                    String.format("%.1f%%", hitRate));
            }
        }

        // Check 6: Baseline Consistency
        boolean baselineConsistent = ignite.cluster().currentBaselineTopology() != null;
        result.addCheck("Baseline Topology",
            baselineConsistent ? "PASS" : "WARN",
            baselineConsistent ? "Configured" : "Not configured");

        return result;
    }

    private static void printHealthReport(HealthCheckResult result) {
        System.out.println("========================================");
        System.out.println("       HEALTH CHECK REPORT");
        System.out.println("========================================\n");

        int passed = 0, warned = 0, failed = 0;

        for (HealthCheck check : result.getChecks()) {
            String icon;
            switch (check.status) {
                case "PASS": icon = "[OK]  "; passed++; break;
                case "WARN": icon = "[WARN]"; warned++; break;
                case "FAIL": icon = "[FAIL]"; failed++; break;
                default: icon = "[INFO]"; break;
            }
            System.out.printf("%s %s: %s%n", icon, check.name, check.details);
        }

        System.out.println("\n----------------------------------------");
        System.out.printf("Summary: %d passed, %d warnings, %d failed%n",
            passed, warned, failed);
        System.out.println("----------------------------------------");

        // Overall status
        String overall;
        if (failed > 0) {
            overall = "CRITICAL - Immediate attention required";
        } else if (warned > 0) {
            overall = "WARNING - Review recommended";
        } else {
            overall = "HEALTHY - All checks passed";
        }
        System.out.println("Overall Status: " + overall);
    }

    private static IgniteConfiguration createConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("health-check-node");
        cfg.setMetricsLogFrequency(0); // Disable metrics logging

        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        spi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(spi);

        return cfg;
    }

    // Health check result classes
    static class HealthCheckResult {
        private List<HealthCheck> checks = new ArrayList<>();

        void addCheck(String name, String status, String details) {
            checks.add(new HealthCheck(name, status, details));
        }

        List<HealthCheck> getChecks() {
            return checks;
        }
    }

    static class HealthCheck {
        String name;
        String status;
        String details;

        HealthCheck(String name, String status, String details) {
            this.name = name;
            this.status = status;
            this.details = details;
        }
    }
}
```

### Challenge 2: Automated Backup Script

Create `automated-backup.sh`:

```bash
#!/bin/bash

# Automated Backup Script with Monitoring and Alerting
# Schedule with cron: 0 2 * * * /path/to/automated-backup.sh

set -e

# Configuration
IGNITE_HOME=${IGNITE_HOME:-/opt/ignite}
BACKUP_DIR="/backup/ignite"
OFFSITE_BACKUP="/mnt/offsite/ignite"
CONTROL_SCRIPT="${IGNITE_HOME}/bin/control.sh"
LOG_FILE="${BACKUP_DIR}/backup_$(date +%Y%m%d).log"
RETENTION_DAYS=7
ALERT_EMAIL="admin@example.com"
SLACK_WEBHOOK=""  # Optional Slack webhook URL

# Create directories
mkdir -p ${BACKUP_DIR}
mkdir -p ${OFFSITE_BACKUP}

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a ${LOG_FILE}
}

send_alert() {
    local SUBJECT=$1
    local MESSAGE=$2

    # Email alert
    echo "${MESSAGE}" | mail -s "${SUBJECT}" ${ALERT_EMAIL} 2>/dev/null || true

    # Slack alert (if configured)
    if [ -n "${SLACK_WEBHOOK}" ]; then
        curl -s -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"${SUBJECT}: ${MESSAGE}\"}" \
            ${SLACK_WEBHOOK} 2>/dev/null || true
    fi
}

check_cluster_health() {
    log "Checking cluster health..."

    # Check cluster state
    STATE=$(${CONTROL_SCRIPT} --state 2>&1 | grep "Cluster state:" | awk '{print $3}')
    if [ "${STATE}" != "ACTIVE" ]; then
        send_alert "Backup FAILED" "Cluster is not ACTIVE (state: ${STATE})"
        exit 1
    fi

    # Check node count
    NODE_COUNT=$(${CONTROL_SCRIPT} --baseline 2>&1 | grep -c "ConsistentId" || echo "0")
    if [ "${NODE_COUNT}" -lt 2 ]; then
        log "WARNING: Only ${NODE_COUNT} nodes in cluster"
    fi

    log "Cluster health OK (state: ${STATE}, nodes: ${NODE_COUNT})"
}

create_snapshot() {
    SNAPSHOT_NAME=$1

    log "Creating snapshot: ${SNAPSHOT_NAME}"

    # Create snapshot with timeout
    timeout 1800 ${CONTROL_SCRIPT} --snapshot create ${SNAPSHOT_NAME} 2>&1 | tee -a ${LOG_FILE}

    if [ $? -ne 0 ]; then
        send_alert "Backup FAILED" "Snapshot creation failed for ${SNAPSHOT_NAME}"
        exit 1
    fi

    log "Snapshot created successfully"
}

verify_snapshot() {
    SNAPSHOT_NAME=$1

    log "Verifying snapshot: ${SNAPSHOT_NAME}"

    ${CONTROL_SCRIPT} --snapshot check ${SNAPSHOT_NAME} 2>&1 | tee -a ${LOG_FILE}

    if [ $? -ne 0 ]; then
        send_alert "Backup WARNING" "Snapshot verification failed for ${SNAPSHOT_NAME}"
        return 1
    fi

    log "Snapshot verification passed"
}

copy_to_offsite() {
    SNAPSHOT_NAME=$1
    SNAPSHOT_PATH="${IGNITE_HOME}/work/snapshots/${SNAPSHOT_NAME}"

    if [ -d "${SNAPSHOT_PATH}" ]; then
        log "Copying snapshot to offsite location..."

        # Compress and copy
        tar -czf "${OFFSITE_BACKUP}/${SNAPSHOT_NAME}.tar.gz" -C "${IGNITE_HOME}/work/snapshots" ${SNAPSHOT_NAME}

        # Calculate checksum
        sha256sum "${OFFSITE_BACKUP}/${SNAPSHOT_NAME}.tar.gz" > "${OFFSITE_BACKUP}/${SNAPSHOT_NAME}.tar.gz.sha256"

        log "Offsite backup complete: ${OFFSITE_BACKUP}/${SNAPSHOT_NAME}.tar.gz"
    fi
}

cleanup_old_backups() {
    log "Cleaning up backups older than ${RETENTION_DAYS} days..."

    # Local cleanup
    find ${BACKUP_DIR} -name "*.log" -mtime +30 -delete 2>/dev/null || true

    # Offsite cleanup
    find ${OFFSITE_BACKUP} -name "*.tar.gz" -mtime +${RETENTION_DAYS} -delete 2>/dev/null || true
    find ${OFFSITE_BACKUP} -name "*.sha256" -mtime +${RETENTION_DAYS} -delete 2>/dev/null || true

    log "Cleanup complete"
}

# Main execution
log "=========================================="
log "Starting automated backup"
log "=========================================="

# Generate snapshot name
SNAPSHOT_NAME="auto_backup_$(date +%Y%m%d_%H%M%S)"

# Run backup steps
check_cluster_health
create_snapshot ${SNAPSHOT_NAME}
verify_snapshot ${SNAPSHOT_NAME}
copy_to_offsite ${SNAPSHOT_NAME}
cleanup_old_backups

# Calculate backup size
if [ -f "${OFFSITE_BACKUP}/${SNAPSHOT_NAME}.tar.gz" ]; then
    BACKUP_SIZE=$(du -h "${OFFSITE_BACKUP}/${SNAPSHOT_NAME}.tar.gz" | cut -f1)
    log "Backup size: ${BACKUP_SIZE}"
fi

log "=========================================="
log "Backup completed successfully"
log "=========================================="

# Send success notification (optional)
# send_alert "Backup SUCCESS" "Snapshot ${SNAPSHOT_NAME} created successfully (${BACKUP_SIZE})"
```

### Challenge 3: Deployment Validator

Create `Lab12DeploymentValidator.java`:

```java
package com.example.ignite;

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

            Thread.sleep(3000);
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
            } else {
                result.addFail("Server nodes", "Need at least 2 server nodes");
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
                TEST_RECORDS + " records in " + putTime + "ms");

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
                TEST_RECORDS + " reads in " + getTime + "ms");

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
                result.addFail("Transaction rollback", "Rollback failed");
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

            // Concurrent increments
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
                    "Correct final value: " + finalValue);
            } else {
                result.addFail("Concurrent access",
                    "Incorrect value: " + finalValue + " (expected " + (CONCURRENT_OPS * 100) + ")");
            }

            ignite.destroyCache(cacheName);

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
                result.addWarn("Backup configuration", "No backups configured");
            }

            ignite.destroyCache(cacheName);

        } catch (Exception e) {
            result.addFail("Failover", e.getMessage());
        }
    }

    private static void validatePersistence(Ignite ignite, ValidationResult result) {
        System.out.println("Validating persistence...");

        try {
            boolean persistenceEnabled = ignite.configuration()
                .getDataStorageConfiguration()
                .getDefaultDataRegionConfiguration()
                .isPersistenceEnabled();

            if (persistenceEnabled) {
                result.addPass("Persistence", "Native persistence enabled");
            } else {
                result.addWarn("Persistence",
                    "Persistence disabled (data loss on restart)");
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
    }

    private static IgniteConfiguration createConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("validator-node");

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
```

