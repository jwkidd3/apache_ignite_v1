# Lab 04: Configuration and Deployment - Solutions

This directory contains the solution files for Lab 4 of the Apache Ignite training course.

## Overview

Lab 4 focuses on:
- XML vs programmatic configuration
- Spring Boot integration patterns
- Cache parameter configuration
- Monitoring and logging setup
- Configuration best practices

## Solution Files

| File | Description |
|------|-------------|
| `XmlConfig.java` | Exercise 1-2: Loading configuration from XML file |
| `ProgrammaticConfig.java` | Exercise 3: Advanced programmatic configuration |
| `config/IgniteConfig.java` | Exercise 4: Spring-style configuration bean |
| `controller/CacheController.java` | Exercise 4: REST controller simulation |
| `Monitoring.java` | Exercise 5-6: Metrics and monitoring |
| `resources/ignite-config.xml` | XML configuration file |

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Ignite 2.16.0

## Building the Project

```bash
cd lab04_configuration_deployment
mvn clean compile
```

## Running the Solutions

### Exercise 1-2: XML Configuration
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab04.XmlConfig"
```

### Exercise 3: Programmatic Configuration
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab04.ProgrammaticConfig"
```

### Exercise 4: Spring-Style Configuration
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab04.config.IgniteConfig"
```

### Exercise 4: REST Controller Simulation
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab04.controller.CacheController"
```

### Exercise 5-6: Monitoring
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab04.Monitoring"
```

## Key Concepts Demonstrated

1. **XML Configuration**: External configuration file for deployment flexibility
2. **Programmatic Configuration**: Type-safe Java-based configuration
3. **Data Regions**: Memory management with different regions
4. **Cache Configuration**: Cache modes, backups, atomicity
5. **Discovery SPI**: Static IP finder for cluster formation
6. **Metrics**: Cache and cluster metrics access
7. **Spring Integration**: Configuration beans and dependency injection patterns

## Configuration Comparison

### XML Advantages
- Externalized configuration
- No code changes for config updates
- Easier for operations teams
- Environment-specific configurations

### Programmatic Advantages
- Type safety
- IDE support (autocomplete)
- Conditional logic possible
- Easier debugging

## Spring Boot Integration

For actual Spring Boot integration, add these dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>2.7.14</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>2.7.14</version>
</dependency>
```

Then annotate classes with:
- `@Configuration` for IgniteConfig
- `@RestController` for CacheController
- `@Autowired` for Ignite injection

## Monitoring Best Practices

- Enable statistics only on caches that need monitoring
- Monitor hit ratio to measure cache effectiveness
- Track average get/put times for performance
- Watch heap memory usage for capacity planning
- Note: Statistics collection has a small overhead
