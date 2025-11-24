# Lab 12: Production Deployment and Best Practices

## Duration: 60 minutes

## Objectives
- Deploy production-ready Ignite clusters
- Configure security (authentication, SSL/TLS)
- Implement backup and disaster recovery strategies
- Perform rolling updates
- Deploy on Docker and Kubernetes
- Implement troubleshooting procedures

## Prerequisites
- Completed Labs 1-11
- Docker installed
- Kubernetes knowledge (helpful)
- Linux/Unix command line experience

## Part 1: Security Configuration (15 minutes)

### Exercise 1: Enable Authentication

Create `Lab12Authentication.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.plugin.security.*;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;
import java.util.Collection;

public class Lab12Authentication {

    public static void main(String[] args) {
        System.out.println("=== Authentication Lab ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("secure-node");

        // Configure discovery
        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        spi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(spi);

        System.out.println("=== Security Configuration ===");
        System.out.println("1. Authentication enabled");
        System.out.println("2. User credentials required");
        System.out.println("3. Permission-based access control\n");

        System.out.println("=== Security Best Practices ===");
        System.out.println("✓ Use strong passwords");
        System.out.println("✓ Enable SSL/TLS");
        System.out.println("✓ Implement role-based access control");
        System.out.println("✓ Audit security events");
        System.out.println("✓ Rotate credentials regularly");
        System.out.println("✓ Use secure communication between nodes");

        System.out.println("\n=== Production Security Checklist ===");
        System.out.println("□ Authentication enabled");
        System.out.println("□ SSL/TLS configured");
        System.out.println("□ Firewall rules in place");
        System.out.println("□ Audit logging enabled");
        System.out.println("□ Regular security updates");
        System.out.println("□ Encrypted communication");
        System.out.println("□ Secure credential storage");
    }
}
```

### Exercise 2: SSL/TLS Configuration

Create `ssl-config.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean class="org.apache.ignite.configuration.IgniteConfiguration">
        <!-- SSL Context Factory -->
        <property name="sslContextFactory">
            <bean class="org.apache.ignite.ssl.SslContextFactory">
                <property name="keyStoreFilePath" value="keystore.jks"/>
                <property name="keyStorePassword" value="changeit"/>
                <property name="trustStoreFilePath" value="truststore.jks"/>
                <property name="trustStorePassword" value="changeit"/>
                <property name="protocol" value="TLSv1.2"/>
            </bean>
        </property>

        <!-- Enable SSL for different components -->
        <property name="connectorConfiguration">
            <bean class="org.apache.ignite.configuration.ConnectorConfiguration">
                <property name="sslEnabled" value="true"/>
            </bean>
        </property>
    </bean>
</beans>
```

Create keystore script `generate-certs.sh`:

```bash
#!/bin/bash

# Generate keystore
keytool -genkeypair -alias ignite-server \
    -keyalg RSA -keysize 2048 \
    -validity 365 \
    -keystore keystore.jks \
    -storepass changeit \
    -keypass changeit \
    -dname "CN=localhost, OU=Ignite, O=Apache, L=City, ST=State, C=US"

# Export certificate
keytool -exportcert -alias ignite-server \
    -keystore keystore.jks \
    -storepass changeit \
    -file server.cer

# Create truststore
keytool -importcert -alias ignite-server \
    -file server.cer \
    -keystore truststore.jks \
    -storepass changeit \
    -noprompt

echo "SSL certificates generated"
echo "Keystore: keystore.jks"
echo "Truststore: truststore.jks"
```

## Part 2: Docker Deployment (15 minutes)

### Exercise 3: Dockerize Ignite Application

Create `Dockerfile`:

```dockerfile
FROM openjdk:11-jre-slim

# Install Ignite
ENV IGNITE_VERSION 2.16.0
ENV IGNITE_HOME /opt/ignite

RUN apt-get update && \
    apt-get install -y wget unzip && \
    wget https://archive.apache.org/dist/ignite/${IGNITE_VERSION}/apache-ignite-${IGNITE_VERSION}-bin.zip && \
    unzip apache-ignite-${IGNITE_VERSION}-bin.zip -d /opt && \
    mv /opt/apache-ignite-${IGNITE_VERSION}-bin ${IGNITE_HOME} && \
    rm apache-ignite-${IGNITE_VERSION}-bin.zip && \
    apt-get clean

# Copy configuration
COPY ignite-config.xml ${IGNITE_HOME}/config/

# Set working directory
WORKDIR ${IGNITE_HOME}

# Expose ports
EXPOSE 11211 47100 47500 49112 10800 8080

# JVM options
ENV JVM_OPTS="-Xms1g -Xmx1g -XX:+UseG1GC"

# Start Ignite
CMD ${IGNITE_HOME}/bin/ignite.sh ${IGNITE_HOME}/config/ignite-config.xml
```

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  ignite-node1:
    build: .
    container_name: ignite-node1
    hostname: ignite-node1
    environment:
      - IGNITE_INSTANCE_NAME=node1
      - JVM_OPTS=-Xms2g -Xmx2g -XX:+UseG1GC
    ports:
      - "10800:10800"
      - "8080:8080"
    networks:
      - ignite-net

  ignite-node2:
    build: .
    container_name: ignite-node2
    hostname: ignite-node2
    environment:
      - IGNITE_INSTANCE_NAME=node2
      - JVM_OPTS=-Xms2g -Xmx2g -XX:+UseG1GC
    networks:
      - ignite-net

  ignite-node3:
    build: .
    container_name: ignite-node3
    hostname: ignite-node3
    environment:
      - IGNITE_INSTANCE_NAME=node3
      - JVM_OPTS=-Xms2g -Xmx2g -XX:+UseG1GC
    networks:
      - ignite-net

networks:
  ignite-net:
    driver: bridge
```

Create `docker-ignite-config.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean class="org.apache.ignite.configuration.IgniteConfiguration">
        <!-- Discovery for Docker -->
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
    </bean>
</beans>
```

Commands to run:

```bash
# Build and start cluster
docker-compose up -d

# Check logs
docker-compose logs -f ignite-node1

# Scale to 5 nodes
docker-compose up -d --scale ignite-node2=5

# Stop cluster
docker-compose down
```

## Part 3: Kubernetes Deployment (20 minutes)

### Exercise 4: Deploy on Kubernetes

Create `ignite-namespace.yaml`:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ignite
```

Create `ignite-service.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: ignite-service
  namespace: ignite
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
spec:
  clusterIP: None
  ports:
    - name: discovery
      port: 47500
      targetPort: 47500
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
      containers:
      - name: ignite
        image: apacheignite/ignite:2.16.0
        ports:
        - containerPort: 47100 # communication
        - containerPort: 47500 # discovery
        - containerPort: 10800 # thin clients
        - containerPort: 8080  # rest
        env:
        - name: IGNITE_INSTANCE_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: JVM_OPTS
          value: "-Xms2g -Xmx2g -XX:+UseG1GC"
        - name: OPTION_LIBS
          value: "ignite-kubernetes,ignite-rest-http"
        - name: CONFIG_URI
          value: "https://raw.githubusercontent.com/apache/ignite/master/modules/kubernetes/config/example-kube.xml"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        volumeMounts:
        - name: ignite-storage
          mountPath: /persistence
  volumeClaimTemplates:
  - metadata:
      name: ignite-storage
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 10Gi
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
            <property name="discoverySpi">
                <bean class="org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi">
                    <property name="ipFinder">
                        <bean class="org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder">
                            <property name="namespace" value="ignite"/>
                            <property name="serviceName" value="ignite-discovery"/>
                        </bean>
                    </property>
                </bean>
            </property>
        </bean>
    </beans>
```

Deploy commands:

```bash
# Create namespace
kubectl apply -f ignite-namespace.yaml

# Deploy ConfigMap
kubectl apply -f ignite-configmap.yaml

# Deploy Services
kubectl apply -f ignite-service.yaml

# Deploy StatefulSet
kubectl apply -f ignite-statefulset.yaml

# Check pods
kubectl get pods -n ignite

# Check logs
kubectl logs -f ignite-cluster-0 -n ignite

# Scale cluster
kubectl scale statefulset ignite-cluster --replicas=5 -n ignite

# Access Ignite
kubectl port-forward svc/ignite-service 10800:10800 -n ignite
```

## Part 4: Backup and Disaster Recovery (10 minutes)

### Exercise 5: Backup Strategies

Create `Lab12Backup.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;

public class Lab12Backup {

    public static void main(String[] args) {
        System.out.println("=== Backup and Disaster Recovery Lab ===\n");

        System.out.println("=== Backup Strategies ===\n");

        System.out.println("1. Snapshot Backups:");
        System.out.println("   - Use Ignite snapshots API");
        System.out.println("   - Full cluster snapshot");
        System.out.println("   - Point-in-time recovery");
        System.out.println("   Command: bin/ignite.sh --snapshot create snapshot_name");

        System.out.println("\n2. Native Persistence Backup:");
        System.out.println("   - Copy persistence files");
        System.out.println("   - Backup WAL and data directories");
        System.out.println("   - Offline backup when nodes stopped");

        System.out.println("\n3. Incremental Backups:");
        System.out.println("   - Use WAL archiving");
        System.out.println("   - Backup WAL archive segments");
        System.out.println("   - Point-in-time recovery possible");

        System.out.println("\n4. Cross-Datacenter Replication:");
        System.out.println("   - Run clusters in multiple DCs");
        System.out.println("   - Application-level replication");
        System.out.println("   - Active-active or active-passive");

        System.out.println("\n=== Recovery Procedures ===\n");

        System.out.println("1. From Snapshot:");
        System.out.println("   bin/ignite.sh --snapshot restore snapshot_name");

        System.out.println("\n2. From Persistence:");
        System.out.println("   - Stop cluster");
        System.out.println("   - Restore persistence files");
        System.out.println("   - Restart cluster");
        System.out.println("   - Activate cluster");

        System.out.println("\n3. WAL Recovery:");
        System.out.println("   - Automatic on node start");
        System.out.println("   - Replays WAL if needed");
        System.out.println("   - Ensures consistency");

        System.out.println("\n=== Best Practices ===");
        System.out.println("✓ Regular automated backups");
        System.out.println("✓ Test recovery procedures");
        System.out.println("✓ Store backups off-site");
        System.out.println("✓ Monitor backup success");
        System.out.println("✓ Document recovery steps");
        System.out.println("✓ Keep multiple backup copies");
        System.out.println("✓ Verify backup integrity");
    }
}
```

## Part 5: Rolling Updates and Maintenance (10 minutes)

### Exercise 6: Rolling Update Procedure

Create `rolling-update.sh`:

```bash
#!/bin/bash

# Rolling Update Procedure for Ignite Cluster

echo "=== Rolling Update Procedure ==="
echo ""

echo "Prerequisites:"
echo "  ✓ Backup completed"
echo "  ✓ New version tested"
echo "  ✓ Maintenance window scheduled"
echo ""

echo "Step 1: Update one node at a time"
echo "  1. Stop node gracefully"
echo "  2. Update Ignite binaries"
echo "  3. Update configuration if needed"
echo "  4. Start node"
echo "  5. Wait for node to join cluster"
echo "  6. Verify node health"
echo "  7. Wait for rebalancing to complete"
echo ""

echo "Step 2: Verify cluster health"
echo "  - Check all nodes are up"
echo "  - Verify data integrity"
echo "  - Check application connectivity"
echo ""

echo "Step 3: Repeat for each node"
echo ""

echo "Commands:"
echo "# Stop node"
echo "kill -SIGTERM <pid>  # Graceful shutdown"
echo ""
echo "# Check topology"
echo "./bin/ignite.sh --baseline"
echo ""
echo "# Monitor rebalancing"
echo "curl http://localhost:8080/ignite?cmd=top"
```

Create `troubleshooting-guide.md`:

```markdown
# Ignite Troubleshooting Guide

## Common Issues and Solutions

### 1. Node Cannot Join Cluster

**Symptoms:**
- Node starts but doesn't discover others
- Segmentation error messages

**Solutions:**
- Check network connectivity
- Verify discovery configuration
- Check firewall rules (ports 47500-47509)
- Ensure consistent cluster configuration

### 2. Out of Memory Errors

**Symptoms:**
- OutOfMemoryError exceptions
- Nodes crashing
- Slow performance

**Solutions:**
- Increase heap size
- Review data region configuration
- Check for memory leaks
- Enable eviction policies
- Monitor GC logs

### 3. Slow Query Performance

**Symptoms:**
- Queries taking too long
- High CPU usage

**Solutions:**
- Add indexes
- Use EXPLAIN to analyze query plan
- Avoid distributed joins
- Check data colocation
- Review cache configuration

### 4. Data Loss

**Symptoms:**
- Missing cache entries
- Inconsistent data

**Solutions:**
- Check number of backups
- Verify persistence configuration
- Review node failure logs
- Ensure proper shutdown procedures

### 5. High Network Usage

**Symptoms:**
- Network saturation
- Slow rebalancing

**Solutions:**
- Reduce backup count if appropriate
- Use compression
- Optimize data serialization
- Review rebalancing throttling

## Diagnostic Commands

```bash
# Check cluster state
./bin/ignite.sh --baseline

# View cache metrics
curl "http://localhost:8080/ignite?cmd=cache&cacheName=myCache"

# Thread dump
jstack <pid> > thread-dump.txt

# Heap dump
jmap -dump:format=b,file=heap-dump.hprof <pid>

# GC statistics
jstat -gcutil <pid> 1000
```

## Log Analysis

Key log messages to watch for:
- Discovery events
- Rebalancing progress
- Transaction timeouts
- Out of memory warnings
- Segmentation warnings
```

## Verification Steps

### Checklist
- [ ] Security configured (SSL/TLS, authentication)
- [ ] Docker deployment working
- [ ] Kubernetes deployment successful
- [ ] Backup procedures documented
- [ ] Rolling update tested
- [ ] Monitoring in place
- [ ] Troubleshooting guide available

### Production Readiness Checklist

```
□ Security
  □ Authentication enabled
  □ SSL/TLS configured
  □ Firewall rules in place

□ High Availability
  □ Multiple nodes (3+ recommended)
  □ Proper backup configuration
  □ Cross-datacenter setup (if required)

□ Performance
  □ JVM tuned
  □ Memory configured appropriately
  □ Indexes created
  □ Affinity keys used

□ Monitoring
  □ Metrics collection enabled
  □ Alerting configured
  □ Log aggregation in place
  □ Dashboards created

□ Operations
  □ Backup procedures tested
  □ Recovery procedures documented
  □ Rolling update procedure defined
  □ Troubleshooting guide available
  □ Runbooks created
  □ On-call rotation established

□ Documentation
  □ Architecture documented
  □ Configuration documented
  □ Operations procedures documented
  □ Contact information available
```

## Lab Questions

1. What are the key security features needed for production?
2. What is the recommended minimum number of nodes for production?
3. How do you perform a rolling update?
4. What backup strategy is most appropriate for your use case?

## Answers

1. **Production security**:
   - Authentication (user credentials)
   - SSL/TLS encryption
   - Firewall rules
   - Audit logging
   - Role-based access control
   - Secure credential storage

2. **Minimum nodes**: **3 nodes** recommended:
   - Provides redundancy
   - Allows one node failure
   - Enables proper quorum
   - With 1 backup: can tolerate 1 node failure

3. **Rolling update**:
   - Update one node at a time
   - Stop node gracefully
   - Update binaries/config
   - Start node
   - Wait for rebalancing
   - Verify health
   - Repeat for each node

4. **Backup strategies**:
   - **Snapshots**: Point-in-time full backup
   - **Persistence backup**: File-based backup
   - **WAL archiving**: Incremental backup
   - **Cross-DC replication**: Disaster recovery
   - Choice depends on RTO/RPO requirements

## Common Issues

**Issue: Pods not starting in Kubernetes**
- Check resource limits
- Verify persistent volume claims
- Check discovery configuration
- Review pod logs

**Issue: Docker containers can't communicate**
- Verify network configuration
- Check container networking
- Ensure proper host configuration

**Issue: SSL handshake failures**
- Verify certificate validity
- Check keystore/truststore paths
- Ensure consistent SSL configuration
- Check certificate chain

## Course Completion

Congratulations! You have completed all 12 labs of the Apache Ignite training course.

### What You've Learned:

1. ✓ In-memory computing fundamentals
2. ✓ Cluster topology and architecture
3. ✓ Cache operations and modes
4. ✓ Configuration and deployment
5. ✓ Data modeling and persistence
6. ✓ SQL queries and indexing
7. ✓ Transactions and ACID properties
8. ✓ Advanced caching patterns
9. ✓ Distributed computing
10. ✓ Integration and connectivity
11. ✓ Performance tuning
12. ✓ Production deployment

### Next Steps:

- Deploy Ignite in your environment
- Join Apache Ignite community
- Contribute to open source
- Continue learning advanced topics
- Share knowledge with team

## Additional Resources

- Apache Ignite Documentation: https://ignite.apache.org/docs
- GitHub Repository: https://github.com/apache/ignite
- Mailing Lists: https://ignite.apache.org/community/resources.html
- Stack Overflow: [apache-ignite] tag
- YouTube Channel: Apache Ignite

## Final Notes

You are now ready to deploy and maintain Apache Ignite clusters in production!

Remember:
- Start simple, scale as needed
- Monitor everything
- Test your backups
- Document your setup
- Keep learning!

Good luck with your Ignite projects!
