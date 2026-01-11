# Apache Ignite Training Labs

This directory contains all 12 hands-on labs for the Apache Ignite 3-day training course.

## Course Structure

### Day 1: Fundamentals and Basic Operations (3.5 hours lab time)
- **Lab 1**: Environment Setup and First Cluster (45 min)
- **Lab 2**: Multi-Node Cluster Setup (50 min)
- **Lab 3**: Basic Cache Operations (55 min)
- **Lab 4**: Configuration and Deployment (60 min)

### Day 2: Data Management and Advanced Features (3.75 hours lab time)
- **Lab 5**: Data Modeling and Persistence (45 min)
- **Lab 6**: SQL and Indexing (60 min)
- **Lab 7**: Transactions and ACID Properties (60 min)
- **Lab 8**: Advanced Caching Patterns (60 min)

### Day 3: Distributed Computing and Production (3.5 hours lab time)
- **Lab 9**: Compute Grid Fundamentals (50 min)
- **Lab 10**: Integration and Connectivity (55 min)
- **Lab 11**: Performance Tuning and Monitoring (55 min)
- **Lab 12**: Production Deployment (55 min)

## Lab Descriptions

### Lab 1: Environment Setup and First Cluster
**File**: `lab01_environment_setup.md`

Learn to:
- Install and configure Apache Ignite
- Start your first Ignite node
- Understand basic cluster operations
- Verify installation and connectivity

**Key Concepts**: Node startup, cluster discovery, basic operations

---

### Lab 2: Multi-Node Cluster Setup
**File**: `lab02_multinode_cluster.md`

Learn to:
- Configure static IP discovery
- Set up multi-node clusters
- Manage baseline topology
- Activate clusters

**Key Concepts**: Discovery mechanisms, baseline topology, cluster activation

---

### Lab 3: Basic Cache Operations
**File**: `lab03_basic_cache_operations.md`

Learn to:
- Perform CRUD operations
- Work with different cache modes
- Use synchronous vs asynchronous operations
- Implement batch operations

**Key Concepts**: Cache modes (PARTITIONED, REPLICATED, LOCAL), async operations, batch processing

---

### Lab 4: Configuration and Deployment
**File**: `lab04_configuration_deployment.md`

Learn to:
- Configure Ignite with XML and code
- Integrate with Spring Boot
- Set up monitoring and logging
- Apply configuration best practices

**Key Concepts**: XML vs programmatic config, Spring Boot integration, monitoring

---

### Lab 5: Data Modeling and Persistence
**File**: `lab05_data_modeling_persistence.md`

Learn to:
- Implement data models with affinity keys
- Configure native persistence
- Use cache store patterns
- Integrate with external databases

**Key Concepts**: Affinity keys, persistence, write-through/write-behind patterns

---

### Lab 6: SQL and Indexing
**File**: `lab06_sql_indexing.md`

Learn to:
- Execute SQL queries (DDL, DML, DQL)
- Create and optimize indexes
- Use JDBC driver
- Implement distributed joins

**Key Concepts**: SQL support, indexing strategies, JDBC, distributed joins

---

### Lab 7: Transactions and ACID Properties
**File**: `lab07_transactions_acid.md`

Learn to:
- Implement PESSIMISTIC and OPTIMISTIC transactions
- Work with isolation levels
- Handle deadlocks
- Apply transaction best practices

**Key Concepts**: Transaction models, isolation levels, deadlock detection

---

### Lab 8: Advanced Caching Patterns
**File**: `lab08_advanced_caching.md`

Learn to:
- Configure near caches
- Set expiry policies and eviction
- Use cache entry processors
- Implement continuous queries

**Key Concepts**: Near caches, expiry/eviction, entry processors, event handling

---

### Lab 9: Compute Grid Fundamentals
**File**: `lab09_compute_grid.md`

Learn to:
- Execute distributed compute tasks
- Implement affinity-aware computing
- Create MapReduce operations
- Use load balancing

**Key Concepts**: Distributed computing, affinity computing, MapReduce, closures

---

### Lab 10: Integration and Connectivity
**File**: `lab10_integration_connectivity.md`

Learn to:
- Use REST API
- Integrate with Spring Framework
- Configure Kafka connector
- Set up Hibernate L2 cache

**Key Concepts**: REST API, Spring integration, Kafka streaming, Hibernate L2 cache

---

### Lab 11: Performance Tuning and Monitoring
**File**: `lab11_performance_tuning.md`

Learn to:
- Tune JVM for Ignite
- Monitor performance metrics
- Benchmark configurations
- Identify performance anti-patterns

**Key Concepts**: JVM tuning, GC optimization, metrics, benchmarking

---

### Lab 12: Production Deployment
**File**: `lab12_production_deployment.md`

Learn to:
- Deploy production clusters
- Configure security (SSL/TLS, authentication)
- Set up production configuration
- Optional: Deploy on Docker/Kubernetes

**Key Concepts**: Security, production configuration, SSL/TLS, authentication

---

## Prerequisites

### Software Requirements
- Java 8 or higher
- Maven or Gradle
- IDE (IntelliJ IDEA, Eclipse, or VS Code)
- 8GB+ RAM recommended
- Docker Desktop (optional, for Lab 12 optional exercises only)

### Knowledge Requirements
- Java programming experience
- Basic understanding of databases and SQL
- Familiarity with distributed systems (helpful but not required)
- Maven/Gradle basics

## Getting Started

1. **Clone or download** this repository
2. **Start with Lab 1** and progress sequentially
3. **Complete exercises** in each lab
4. **Verify** your work using the checklist in each lab
5. **Answer lab questions** to test your understanding

## Lab Format

Each lab includes:
- **Objectives**: What you'll learn
- **Prerequisites**: Required knowledge/completed labs
- **Exercises**: Hands-on coding exercises
- **Code Examples**: Complete, runnable Java code
- **Verification Steps**: How to check your work
- **Lab Questions**: Test your understanding
- **Answers**: Detailed explanations
- **Common Issues**: Troubleshooting tips
- **Next Steps**: What comes next

## Tips for Success

1. **Follow the order**: Labs build on each other
2. **Type the code**: Don't just copy-paste
3. **Experiment**: Try variations of the examples
4. **Read the output**: Understand what's happening
5. **Ask questions**: Review the Q&A sections
6. **Take breaks**: Don't rush through labs
7. **Practice**: Repeat exercises to build muscle memory

## Additional Resources

- **Course Flow**: See `../course_flow.md` for daily schedule
- **Outline**: See `../outline.md` for complete course outline
- **Apache Ignite Docs**: https://ignite.apache.org/docs/latest/
- **GitHub Examples**: https://github.com/apache/ignite/tree/master/examples
- **Community**: https://ignite.apache.org/community/

## Support

If you encounter issues:
1. Check the **Common Issues** section in each lab
2. Review the **Troubleshooting Guide** in Lab 12
3. Consult the Apache Ignite documentation
4. Ask questions on Stack Overflow (tag: apache-ignite)
5. Join the Apache Ignite mailing list

## Lab Completion

After completing all labs, you will be able to:
- Deploy and configure Ignite clusters
- Implement caching strategies
- Execute SQL queries and transactions
- Perform distributed computing
- Integrate with external systems
- Tune performance
- Deploy to production
- Troubleshoot common issues

## Next Steps After Course

1. **Practice**: Deploy Ignite in a test environment
2. **Explore**: Try advanced features not covered in labs
3. **Contribute**: Join the Apache Ignite community
4. **Implement**: Use Ignite in real projects
5. **Share**: Teach others what you've learned

## Feedback

We welcome feedback on these labs! Please provide suggestions for:
- Clarity of instructions
- Additional examples needed
- Missing topics
- Difficulty level
- Time estimates

Good luck with your Apache Ignite journey!
