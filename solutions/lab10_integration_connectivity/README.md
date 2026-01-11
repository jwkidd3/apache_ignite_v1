# Lab 10: Integration and Connectivity - Solutions

## Overview

This lab covers integration options for Apache Ignite, including:
- REST API for HTTP-based access
- Thin client connections
- Spring Framework integration patterns
- Hibernate L2 cache integration

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Ignite 2.16.0
- curl (for REST API testing)

## Project Structure

```
lab10_integration_connectivity/
├── pom.xml
├── README.md
└── src/main/java/com/example/ignite/solutions/lab10/
    ├── Lab10RestAPI.java           - Exercise 1: REST API usage
    ├── Lab10ThinClient.java        - Exercise 2: Thin client
    └── Lab10SpringIntegration.java - Exercise 3: Spring patterns
```

## Building the Project

```bash
cd lab10_integration_connectivity
mvn clean compile
```

## Running the Solutions

### Exercise 1: REST API
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab10.Lab10RestAPI"
```

Then test with curl in another terminal:
```bash
# Get version
curl http://localhost:8080/ignite?cmd=version

# Put value
curl "http://localhost:8080/ignite?cmd=put&cacheName=restCache&key=mykey&val=myvalue"

# Get value
curl "http://localhost:8080/ignite?cmd=get&cacheName=restCache&key=key1"

# Get cluster topology
curl http://localhost:8080/ignite?cmd=top
```

### Exercise 2: Thin Client
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab10.Lab10ThinClient"
```

### Exercise 3: Spring Integration
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab10.Lab10SpringIntegration"
```

## Key Concepts

### REST API
- HTTP-based access to Ignite
- Default port: 8080
- Supports GET, PUT, SQL queries
- Useful for non-Java clients and monitoring

### Common REST Commands
| Command | Description |
|---------|-------------|
| version | Get Ignite version |
| top | Get cluster topology |
| get | Get cache entry |
| put | Put cache entry |
| size | Get cache size |
| rmv | Remove entry |
| qryfldexe | Execute SQL query |

### Thin Client
- Lightweight connection without cluster membership
- Available for: Java, .NET, C++, Python, Node.js
- Default port: 10800
- Lower resource usage than thick client

### Thin Client Configuration
```java
ClientConfiguration cfg = new ClientConfiguration()
    .setAddresses("127.0.0.1:10800")
    .setTcpNoDelay(true)
    .setTimeout(5000)
    .setAffinityAwarenessEnabled(true);
```

### Spring Integration Patterns
1. **@Cacheable**: Cache method results
2. **@CacheEvict**: Remove entries on update/delete
3. **@CachePut**: Always update cache
4. **CacheManager**: Manage multiple caches

## Thick vs Thin Client Comparison

| Feature | Thick Client | Thin Client |
|---------|--------------|-------------|
| Cluster membership | Yes | No |
| Resource usage | High | Low |
| Topology updates | Yes | No |
| Affinity awareness | Native | Configurable |
| Compute grid | Full | Limited |
| Transactions | Full | Limited |

## Best Practices

1. Use REST API for monitoring and simple operations
2. Use thin client for lightweight, cross-language access
3. Use thick client for full feature access
4. Configure connection timeouts and retries
5. Enable affinity awareness for thin clients
6. Use batch operations (putAll/getAll) for efficiency

## Common Issues

### REST API Not Responding
- Ensure `ignite-rest-http` dependency is included
- Check port 8080 is available
- Verify node is running

### Thin Client Connection Failed
- Check server port 10800 is open
- Verify ClientConnectorConfiguration on server
- Check firewall settings
