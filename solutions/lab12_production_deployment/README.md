# Lab 12: Production Deployment and Best Practices - Solutions

## Overview

This lab covers production deployment essentials for Apache Ignite, including:
- Security configuration (SSL/TLS, authentication)
- Production-ready cluster setup
- Backup and recovery procedures
- Rolling update strategies

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Ignite 2.16.0

## Project Structure

```
lab12_production_deployment/
├── pom.xml
├── README.md
└── src/main/java/com/example/ignite/solutions/lab12/
    ├── Lab12SecurityOverview.java    - Exercise 1: Security overview
    ├── Lab12ClusterSetup.java        - Exercise 2: Production cluster setup
    ├── Lab12SSLConfiguration.java    - Exercise 3: SSL/TLS configuration
    ├── Lab12Authentication.java      - Exercise 4: Authentication setup
    ├── Lab12BackupRecovery.java      - Optional: Backup and recovery
    └── Lab12RollingUpdate.java       - Optional: Rolling updates
```

## Building the Project

```bash
cd lab12_production_deployment
mvn clean compile
```

## Running the Solutions

### Exercise 1: Security Overview
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab12.Lab12SecurityOverview"
```

### Exercise 2: Cluster Setup
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab12.Lab12ClusterSetup"
```

### Exercise 3: SSL Configuration
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab12.Lab12SSLConfiguration"
```

### Exercise 4: Authentication
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab12.Lab12Authentication"
```

### Optional: Backup and Recovery
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab12.Lab12BackupRecovery"
```

### Optional: Rolling Updates
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab12.Lab12RollingUpdate"
```

## Production Deployment Checklist

### Security
- [ ] SSL/TLS enabled for all communication
- [ ] Authentication configured
- [ ] Firewall rules in place
- [ ] Audit logging enabled
- [ ] Default passwords changed

### Configuration
- [ ] JVM properly tuned (-Xms, -Xmx, G1GC)
- [ ] Off-heap memory configured
- [ ] Data regions sized appropriately
- [ ] WAL mode set to FSYNC
- [ ] Backups configured

### Operations
- [ ] Monitoring in place
- [ ] Alerting configured
- [ ] Backup schedule established
- [ ] Recovery procedures documented
- [ ] Rolling update process tested

## SSL Certificate Generation

```bash
# 1. Generate CA keystore
keytool -genkeypair -alias ignite-ca -keyalg RSA -keysize 2048 \
    -validity 365 -keystore ca-keystore.jks -storepass changeit \
    -dname "CN=Ignite CA, OU=Production, O=MyCompany"

# 2. Export CA certificate
keytool -exportcert -alias ignite-ca -keystore ca-keystore.jks \
    -storepass changeit -file ca-cert.cer

# 3. Generate node keystore
keytool -genkeypair -alias node1 -keyalg RSA -keysize 2048 \
    -validity 365 -keystore node1-keystore.jks -storepass changeit \
    -dname "CN=node1, OU=Production, O=MyCompany" \
    -ext san=ip:192.168.1.10,dns:node1

# 4. Create truststore with CA cert
keytool -importcert -alias ignite-ca -file ca-cert.cer \
    -keystore truststore.jks -storepass changeit -noprompt
```

## User Management SQL Commands

```sql
-- Create admin user
CREATE USER admin WITH PASSWORD 'AdminPass123!';

-- Create application user
CREATE USER app_user WITH PASSWORD 'AppUser123!';

-- Change password
ALTER USER admin WITH PASSWORD 'NewAdminPass456!';

-- Remove user
DROP USER old_user;
```

## Backup Commands

```bash
# Create snapshot
control.sh --snapshot create backup_$(date +%Y%m%d)

# List snapshots
control.sh --snapshot list

# Restore snapshot
control.sh --snapshot restore backup_20240115
```

## Rolling Update Commands

```bash
# Disable auto-baseline
control.sh --baseline auto_adjust disable

# Gracefully stop node
kill -SIGTERM <pid>

# Verify data integrity
control.sh --cache idle_verify

# View baseline
control.sh --baseline

# Re-enable auto-baseline
control.sh --baseline auto_adjust enable
```

## Recommended JVM Options

```bash
# Production JVM configuration
-server
-Xms4g -Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+DisableExplicitGC
-XX:+UseStringDeduplication
-XX:+ParallelRefProcEnabled
-XX:MaxDirectMemorySize=8g
-XX:+HeapDumpOnOutOfMemoryError
-Djava.net.preferIPv4Stack=true
```

## Troubleshooting

### SSL Issues
- Verify certificate chain is complete
- Check keystore/truststore passwords
- Enable SSL debug: `-Djavax.net.debug=ssl:handshake`

### Authentication Issues
- Verify persistence is enabled (required for auth)
- Check default credentials: ignite/ignite
- Ensure cluster is activated

### Cluster Issues
- Check network connectivity between nodes
- Verify firewall allows required ports
- Review logs for discovery failures
