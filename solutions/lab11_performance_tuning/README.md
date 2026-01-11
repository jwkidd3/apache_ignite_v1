# Lab 11: Performance Tuning and Monitoring - Solutions

## Overview

This lab covers performance optimization for Apache Ignite, including:
- JVM tuning for Ignite workloads
- Performance metrics collection and monitoring
- Benchmarking strategies
- Common anti-patterns to avoid

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Ignite 2.16.0

## Project Structure

```
lab11_performance_tuning/
├── pom.xml
├── README.md
└── src/main/java/com/example/ignite/solutions/lab11/
    ├── Lab11JVMTuning.java     - Exercise 1: JVM configuration
    ├── Lab11Monitoring.java    - Exercise 2: Performance monitoring
    ├── Lab11Benchmark.java     - Exercise 3: Benchmarking
    └── Lab11AntiPatterns.java  - Exercise 4: Anti-patterns
```

## Building the Project

```bash
cd lab11_performance_tuning
mvn clean compile
```

## Running the Solutions

### Exercise 1: JVM Tuning
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab11.Lab11JVMTuning"
```

### Exercise 2: Monitoring
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab11.Lab11Monitoring"
```

### Exercise 3: Benchmarking
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab11.Lab11Benchmark"
```

### Exercise 4: Anti-Patterns
```bash
mvn exec:java -Dexec.mainClass="com.example.ignite.solutions.lab11.Lab11AntiPatterns"
```

## Recommended JVM Options

```bash
# Heap configuration
-Xms4g -Xmx4g

# G1GC (recommended)
-XX:+UseG1GC
-XX:G1HeapRegionSize=32m
-XX:MaxGCPauseMillis=200
-XX:InitiatingHeapOccupancyPercent=45

# Performance
-XX:+ParallelRefProcEnabled
-XX:+AlwaysPreTouch
-XX:+UseStringDeduplication

# Off-heap memory
-XX:MaxDirectMemorySize=8g

# Diagnostics
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=./heapdump.hprof

# GC Logging (Java 11+)
-Xlog:gc*:file=gc.log:time,uptime,level,tags:filecount=10,filesize=100M
```

## Key Metrics to Monitor

### Cache Metrics
| Metric | Target | Description |
|--------|--------|-------------|
| Hit Rate | > 90% | Cache effectiveness |
| Avg Get Time | < 1ms | Read latency |
| Avg Put Time | < 5ms | Write latency |
| Off-Heap Size | < Max | Memory usage |

### Cluster Metrics
| Metric | Description |
|--------|-------------|
| CPU Load | Should be balanced across nodes |
| Heap Usage | Keep below 80% |
| Network Traffic | Monitor for bottlenecks |
| Job Wait Time | Indicator of thread pool sizing |

## Performance Tips

### JVM Tuning
1. Set -Xms equal to -Xmx
2. Use G1GC for most workloads
3. Configure MaxDirectMemorySize for off-heap
4. Enable GC logging for diagnostics

### Data Region Tuning
1. Size regions based on data volume
2. Enable page eviction for large datasets
3. Configure WAL mode based on durability needs
4. Monitor allocation rates

### Cache Tuning
1. Use ATOMIC mode when transactions not needed
2. Configure appropriate backup count
3. Use batch operations (putAll/getAll)
4. Enable statistics for monitoring

### Query Tuning
1. Create indexes for filtered columns
2. Use collocated joins when possible
3. Avoid full scans with proper WHERE clauses
4. Paginate large result sets

## Common Anti-Patterns

1. **Small heap, large off-heap** - Causes GC issues
2. **Too many backups** - Wastes memory
3. **Large transactions** - Causes lock contention
4. **Individual puts in loops** - Use batch operations
5. **Missing indexes** - Causes full scans
6. **Ignoring metrics** - Can't optimize blindly

## Monitoring Tools

1. **JConsole/VisualVM** - JMX monitoring
2. **Ignite Web Console** - Cluster management
3. **Prometheus + Grafana** - Metrics collection
4. **Java Flight Recorder** - Profiling
5. **Async Profiler** - Low-overhead profiling
