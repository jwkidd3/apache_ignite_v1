package com.example.ignite.solutions.lab12;

import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.ConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.ssl.SslContextFactory;

import java.util.Arrays;

/**
 * Lab 12 Exercise 3: SSL/TLS Configuration
 *
 * Demonstrates:
 * - SSL context factory configuration
 * - Keystore and truststore setup
 * - Enabling SSL for all communication channels
 * - TLS protocol and cipher suite configuration
 */
public class Lab12SSLConfiguration {

    public static void main(String[] args) {
        System.out.println("=== SSL/TLS Configuration Lab ===\n");

        // Print certificate generation instructions
        printCertificateGenerationSteps();

        // Show SSL configuration
        System.out.println("\n=== SSL Configuration Example ===\n");

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

        // Print troubleshooting tips
        printTroubleshootingTips();
    }

    private static void printCertificateGenerationSteps() {
        System.out.println("=== Certificate Generation Steps ===\n");

        System.out.println("Step 1: Create Certificate Authority (CA)");
        System.out.println("keytool -genkeypair \\");
        System.out.println("    -alias ignite-ca \\");
        System.out.println("    -keyalg RSA \\");
        System.out.println("    -keysize 2048 \\");
        System.out.println("    -validity 365 \\");
        System.out.println("    -keystore ca-keystore.jks \\");
        System.out.println("    -storepass ignite-secure-password \\");
        System.out.println("    -dname \"CN=Ignite CA, OU=Production, O=MyCompany\"");
        System.out.println("");

        System.out.println("Step 2: Export CA Certificate");
        System.out.println("keytool -exportcert \\");
        System.out.println("    -alias ignite-ca \\");
        System.out.println("    -keystore ca-keystore.jks \\");
        System.out.println("    -storepass ignite-secure-password \\");
        System.out.println("    -file ca-cert.cer");
        System.out.println("");

        System.out.println("Step 3: Generate Node Keystore");
        System.out.println("keytool -genkeypair \\");
        System.out.println("    -alias node1 \\");
        System.out.println("    -keyalg RSA \\");
        System.out.println("    -keysize 2048 \\");
        System.out.println("    -validity 365 \\");
        System.out.println("    -keystore node1-keystore.jks \\");
        System.out.println("    -storepass ignite-secure-password \\");
        System.out.println("    -dname \"CN=node1, OU=Production, O=MyCompany\" \\");
        System.out.println("    -ext san=ip:192.168.1.10,dns:node1");
        System.out.println("");

        System.out.println("Step 4: Create Certificate Signing Request");
        System.out.println("keytool -certreq \\");
        System.out.println("    -alias node1 \\");
        System.out.println("    -keystore node1-keystore.jks \\");
        System.out.println("    -storepass ignite-secure-password \\");
        System.out.println("    -file node1.csr");
        System.out.println("");

        System.out.println("Step 5: Sign Certificate with CA");
        System.out.println("keytool -gencert \\");
        System.out.println("    -alias ignite-ca \\");
        System.out.println("    -keystore ca-keystore.jks \\");
        System.out.println("    -storepass ignite-secure-password \\");
        System.out.println("    -infile node1.csr \\");
        System.out.println("    -outfile node1-signed.cer");
        System.out.println("");

        System.out.println("Step 6: Import CA into Node Keystore");
        System.out.println("keytool -importcert \\");
        System.out.println("    -alias ignite-ca \\");
        System.out.println("    -keystore node1-keystore.jks \\");
        System.out.println("    -storepass ignite-secure-password \\");
        System.out.println("    -file ca-cert.cer -noprompt");
        System.out.println("");

        System.out.println("Step 7: Import Signed Cert into Node Keystore");
        System.out.println("keytool -importcert \\");
        System.out.println("    -alias node1 \\");
        System.out.println("    -keystore node1-keystore.jks \\");
        System.out.println("    -storepass ignite-secure-password \\");
        System.out.println("    -file node1-signed.cer");
        System.out.println("");

        System.out.println("Step 8: Create Truststore");
        System.out.println("keytool -importcert \\");
        System.out.println("    -alias ignite-ca \\");
        System.out.println("    -file ca-cert.cer \\");
        System.out.println("    -keystore truststore.jks \\");
        System.out.println("    -storepass ignite-secure-password -noprompt");
    }

    private static void printTroubleshootingTips() {
        System.out.println("\n=== SSL Troubleshooting Tips ===");

        System.out.println("\nCommon Issues:");
        System.out.println("1. Certificate chain incomplete");
        System.out.println("   - Ensure CA certificate is in truststore");
        System.out.println("   - Verify signed certificate includes CA chain");

        System.out.println("\n2. Hostname verification failed");
        System.out.println("   - Check SAN (Subject Alternative Name) includes all IPs/hostnames");
        System.out.println("   - Use -ext san=ip:...,dns:... when generating certs");

        System.out.println("\n3. SSL handshake timeout");
        System.out.println("   - Verify firewall allows SSL ports");
        System.out.println("   - Check cipher suite compatibility");
        System.out.println("   - Increase handshake timeout if needed");

        System.out.println("\n4. Password issues");
        System.out.println("   - Keystore and key passwords must match");
        System.out.println("   - Use char[] for passwords in code");

        System.out.println("\nDebugging:");
        System.out.println("Add JVM option: -Djavax.net.debug=ssl:handshake");
        System.out.println("This enables detailed SSL handshake logging");
    }
}
