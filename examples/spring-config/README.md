# Spring Configuration Example

Demonstrates XML and Java-based Spring configuration for Apache Ignite client-server setup.

## Files

### XML Configuration
- **ignite-server-config.xml** - Server configuration with caches
- **ignite-client-config.xml** - Client configuration
- **XmlConfigServer.java** - Server using XML config
- **XmlConfigClient.java** - Client using XML config

### Java Configuration
- **IgniteServerConfig.java** - Server Spring @Configuration class
- **IgniteClientConfig.java** - Client Spring @Configuration class
- **JavaConfigServer.java** - Server using Java config
- **JavaConfigClient.java** - Client using Java config

### Spring-Managed Client (Recommended)
- **SpringClientConfig.java** - Configuration with Ignite as Spring bean
- **CacheService.java** - Service using @Autowired Ignite injection
- **SpringClient.java** - Client using full Spring dependency injection

## Usage

1. Build the project:
   ```bash
   mvn compile
   ```

2. Start a server (choose XML or Java config):
   ```bash
   # XML configuration
   mvn exec:java -Dexec.mainClass="com.example.ignite.XmlConfigServer"

   # Java configuration
   mvn exec:java -Dexec.mainClass="com.example.ignite.JavaConfigServer"
   ```

3. In another terminal, run a client:
   ```bash
   # XML configuration
   mvn exec:java -Dexec.mainClass="com.example.ignite.XmlConfigClient"

   # Java configuration
   mvn exec:java -Dexec.mainClass="com.example.ignite.JavaConfigClient"

   # Spring-managed client (recommended)
   mvn exec:java -Dexec.mainClass="com.example.ignite.SpringClient"
   ```

## Configuration Comparison

| Aspect | XML Config | Java Config |
|--------|-----------|-------------|
| Type safety | Runtime errors | Compile-time checking |
| IDE support | Limited | Full refactoring support |
| Flexibility | Static | Conditional logic possible |
| Readability | Verbose | Concise |

Both approaches produce identical runtime behavior.

## Spring-Managed Ignite (Recommended)

The `SpringClient` example demonstrates the recommended approach for Spring applications:

```java
@Configuration
public class SpringClientConfig {
    @Bean(destroyMethod = "close")
    public Ignite igniteClient(IgniteConfiguration cfg) {
        return Ignition.start(cfg);
    }
}

@Service
public class CacheService {
    @Autowired
    public CacheService(Ignite ignite) {
        this.ignite = ignite;
    }
}
```

Benefits:
- **Lifecycle management**: Spring handles Ignite startup and shutdown
- **Dependency injection**: Services receive Ignite via constructor injection
- **Testability**: Easy to mock Ignite in unit tests
- **Integration**: Works seamlessly with other Spring features (transactions, AOP, etc.)
