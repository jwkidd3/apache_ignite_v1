# Apache Ignite - 3-Day Training Course

Professional training course for Apache Ignite, covering fundamentals through production deployment with hands-on labs.

## Course Overview

**Duration:** 3 Days (24 hours)
**Format:** Instructor-led with hands-on labs
**Level:** Intermediate to Advanced
**Apache Ignite Version:** 2.16.0

## What You'll Learn

- In-memory computing fundamentals and Apache Ignite architecture
- Cluster setup, configuration, and management
- Cache operations, data modeling, and persistence
- SQL queries, indexing, and transactions
- Distributed computing and MapReduce operations
- Integration with Spring, Kafka, and Hibernate
- Performance tuning and production deployment
- Docker and Kubernetes deployment

## Prerequisites

- **Java 8+** programming experience
- Basic understanding of **databases and SQL**
- **Maven or Gradle** knowledge
- Development environment with **8GB+ RAM**
- Familiarity with distributed systems (helpful but not required)

## Course Structure

### Day 1: Fundamentals and Basic Operations (3.5 hrs lab / 2.5 hrs lecture)

**Module 1: Introduction to In-Memory Computing (45 min lecture + 45 min lab)**
- What is in-memory computing and why it matters
- Traditional database limitations
- Apache Ignite overview and comparisons
- **Lab 1:** Environment setup and first cluster (45 min)

**Module 2: Ignite Architecture Deep Dive (45 min lecture + 50 min lab)**
- Cluster topology and node types
- Data distribution and partitioning
- Memory architecture (on-heap, off-heap, persistence)
- Discovery mechanisms and baseline topology
- **Lab 2:** Multi-node cluster setup (50 min)

**Module 3: Basic Cache Operations (45 min lecture + 55 min lab)**
- Cache API and CRUD operations
- Cache modes: PARTITIONED, REPLICATED, LOCAL
- Synchronous vs asynchronous operations
- Batch operations
- **Lab 3:** Implementing cache operations (55 min)

**Module 4: Configuration and Deployment (45 min lecture + 60 min lab)**
- XML vs programmatic configuration
- Spring Boot integration
- Monitoring and logging setup
- **Lab 4:** Configuration and Spring Boot integration (60 min)

### Day 2: Data Management and Advanced Features (3.75 hrs lab / 2.5 hrs lecture)

**Module 5: Data Modeling and Persistence (40 min lecture + 45 min lab)**
- Data modeling best practices
- Affinity keys and colocation strategies
- Native persistence layer
- Write-through, write-behind patterns
- **Lab 5:** Data models with persistence (45 min)

**Module 6: SQL and Indexing (40 min lecture + 60 min lab)**
- SQL support (DDL, DML, DQL)
- Creating and managing indexes
- Query optimization
- JDBC driver and distributed joins
- **Lab 6:** SQL queries and indexing (60 min)

**Module 7: Transactions and ACID Properties (35 min lecture + 60 min lab)**
- Transaction models (PESSIMISTIC, OPTIMISTIC)
- Isolation levels
- Deadlock detection and resolution
- **Lab 7:** Transactional operations (60 min)

**Module 8: Advanced Caching Patterns (40 min lecture + 60 min lab)**
- Near caches and client-side caching
- Expiry policies and eviction strategies
- Cache entry processors
- Continuous queries
- **Lab 8:** Advanced caching patterns (60 min)

### Day 3: Distributed Computing and Production (3.5 hrs lab / 2.25 hrs lecture)

**Module 9: Compute Grid Fundamentals (35 min lecture + 50 min lab)**
- Distributed computing concepts
- Compute closures and jobs
- Affinity-aware computing
- MapReduce operations
- **Lab 9:** Distributed computing tasks (50 min)

**Module 10: Integration and Connectivity (35 min lecture + 55 min lab)**
- REST API usage
- Spring Framework integration
- Kafka connector
- Hibernate L2 cache
- **Lab 10:** Integration with external systems (55 min)

**Module 11: Performance Tuning and Monitoring (35 min lecture + 55 min lab)**
- JVM tuning for Ignite workloads
- Memory management and GC
- Performance metrics and monitoring
- Benchmarking strategies
- **Lab 11:** Performance analysis and tuning (55 min)

**Module 12: Production Deployment (30 min lecture + 55 min lab)**
- Security configuration (SSL/TLS, authentication)
- Production configuration best practices
- Optional: Docker and Kubernetes deployment
- **Lab 12:** Production-ready cluster deployment (55 min)

## Repository Structure

```
apache_ignite_v1/
├── README.md                    # This file
├── .gitignore                   # Git ignore rules
├── labs/                        # Hands-on labs
│   ├── lab01_environment_setup.md
│   ├── lab02_multinode_cluster.md
│   ├── lab03_basic_cache_operations.md
│   ├── lab04_configuration_deployment.md
│   ├── lab05_data_modeling_persistence.md
│   ├── lab06_sql_indexing.md
│   ├── lab07_transactions_acid.md
│   ├── lab08_advanced_caching.md
│   ├── lab09_compute_grid.md
│   ├── lab10_integration_connectivity.md
│   ├── lab11_performance_tuning.md
│   └── lab12_version_differences.md
├── presentations/               # reveal.js presentations
│   ├── module-01-intro-inmemory-computing.html
│   ├── module-02-architecture-deep-dive.html
│   ├── module-03-basic-cache-operations.html
│   ├── module-04-configuration-deployment.html
│   ├── module-05-data-modeling-persistence.html
│   ├── module-06-sql-indexing.html
│   ├── module-07-transactions-acid.html
│   ├── module-08-advanced-caching.html
│   ├── module-09-compute-grid.html
│   ├── module-10-integration-connectivity.html
│   ├── module-11-performance-tuning.html
│   └── module-12-version-differences.html
├── examples/                    # Example projects
└── tests/                       # JUnit test suite
    ├── pom.xml
    └── src/test/java/com/example/ignite/tests/
        ├── BaseIgniteTest.java
        ├── ComprehensiveTestSuite.java
        ├── Lab01EnvironmentSetupTest.java
        ├── Lab02MultiNodeClusterTest.java
        ├── Lab03BasicCacheOperationsTest.java
        ├── Lab04ConfigurationDeploymentTest.java
        ├── Lab05DataModelingPersistenceTest.java
        ├── Lab06SqlIndexingTest.java
        ├── Lab07TransactionsAcidTest.java
        ├── Lab08AdvancedCachingTest.java
        ├── Lab09ComputeGridTest.java
        ├── Lab10IntegrationConnectivityTest.java
        ├── Lab11PerformanceTuningTest.java
        └── Lab12VersionDifferencesTest.java
```

## Getting Started

### 1. Clone or Download Repository

```bash
cd /path/to/your/workspace
# If using git:
git clone <repository-url>
cd apache_ignite_v1
```

### 2. Install Prerequisites

```bash
# Verify Java installation
java -version   # Should be 8 or higher

# Verify Maven or Gradle
mvn -version
# or
gradle -version
```

### 3. Download Apache Ignite

Add to your Maven `pom.xml`:
```xml
<dependencies>
    <dependency>
        <groupId>org.apache.ignite</groupId>
        <artifactId>ignite-core</artifactId>
        <version>2.16.0</version>
    </dependency>
    <dependency>
        <groupId>org.apache.ignite</groupId>
        <artifactId>ignite-spring</artifactId>
        <version>2.16.0</version>
    </dependency>
    <dependency>
        <groupId>org.apache.ignite</groupId>
        <artifactId>ignite-indexing</artifactId>
        <version>2.16.0</version>
    </dependency>
</dependencies>
```

Or download binary: https://ignite.apache.org/download.cgi

### 4. Start with Lab 1

```bash
cd labs
# Read and follow lab01_environment_setup.md
```

## Labs Overview

Each lab includes:
- **Clear objectives** - What you'll learn
- **Prerequisites** - Required setup and knowledge
- **Step-by-step exercises** - Detailed instructions with code
- **Verification steps** - How to check your work
- **Lab questions** - Test your understanding (with answers)
- **Common issues** - Troubleshooting guide
- **Next steps** - Preview of upcoming content

**Estimated Time:** 60-105 minutes per lab

## Presentation Materials

The `presentations/` directory contains reveal.js HTML presentations for all 12 modules:

- **Interactive HTML slides** - open directly in any browser
- **Code examples** with syntax highlighting
- **Clean visual design** with responsive layout
- **Navigation** via keyboard arrows, space bar, or touch

Simply open any `.html` file in a browser to view the presentation.

## Daily Schedule

**Typical Day Structure (70% Lab / 30% Lecture):**
```
9:00 AM  - Module 1 Lecture (35-45 min)
9:45 AM  - Lab Exercise (60-90 min)
11:15 AM - Break (15 min)
11:30 AM - Module 2 Lecture (35-45 min)
12:15 PM - Lab Exercise (75-90 min)
1:30 PM  - Lunch (45 min)
2:15 PM  - Module 3 Lecture (35-45 min)
3:00 PM  - Lab Exercise (60-90 min)
4:30 PM  - Break (15 min)
4:45 PM  - Module 4 Lecture (30-45 min)
5:15 PM  - Lab Exercise (remaining time)
6:00 PM  - Wrap-up and Q&A
```

**Time Allocation:**
- **Lab Time:** ~5.5 hours/day (70%)
- **Lecture Time:** ~2.5 hours/day (30%)
- **Total Course Lab Time:** 16.75 hours
- **Total Course Lecture Time:** 7.25 hours

## Version Control Setup

### Initialize Git Repository

```bash
git init
git add .
git commit -m "Initial commit: Apache Ignite training materials"
git branch -M main
```

### What's Ignored (.gitignore)

The `.gitignore` file excludes:
- **Ignite runtime files:** work/, ignite-data/, ignite-wal/, *.log
- **Build artifacts:** target/, build/, *.class
- **IDE files:** .idea/, *.iml, .vscode/
- **macOS files:** .DS_Store, ._*
- **Credentials:** *.jks, *.key, *.pem

### Connect to Remote Repository

```bash
# GitHub
git remote add origin https://github.com/username/apache-ignite-training.git
git push -u origin main

# GitLab
git remote add origin https://gitlab.com/username/apache-ignite-training.git
git push -u origin main
```

## Learning Outcomes

After completing this course, you will be able to:

✅ Understand in-memory computing concepts and benefits
✅ Set up and configure Apache Ignite clusters
✅ Perform efficient cache operations and data modeling
✅ Execute SQL queries and optimize with indexes
✅ Implement distributed transactions with ACID properties
✅ Use advanced caching patterns (near caches, entry processors)
✅ Implement distributed computing and MapReduce operations
✅ Integrate Ignite with Spring, Kafka, and Hibernate
✅ Tune JVM and optimize performance
✅ Deploy production-ready clusters with security
✅ Deploy on Docker and Kubernetes
✅ Troubleshoot common issues

## Key Topics Covered

### Core Concepts
- In-memory computing fundamentals
- Cluster architecture and topology
- Data distribution and partitioning
- Memory management (on-heap, off-heap, persistence)

### Data Operations
- Cache modes (PARTITIONED, REPLICATED, LOCAL)
- CRUD operations and batch processing
- SQL queries (ANSI-99)
- Transactions and isolation levels
- Data modeling with affinity keys

### Advanced Features
- Near caches and expiry policies
- Entry processors for atomic operations
- Continuous queries for real-time updates
- Distributed computing and MapReduce
- Affinity-aware computing

### Integration
- Spring Boot and Spring Framework
- Apache Kafka streaming
- Hibernate L2 cache
- REST API
- JDBC connectivity

### Production
- JVM and garbage collection tuning
- Performance optimization and benchmarking
- Monitoring and metrics collection
- Security (SSL/TLS, authentication)
- Docker and Kubernetes deployment
- Backup and disaster recovery
- Rolling updates and maintenance

## Tips for Success

### For Students
1. **Follow sequentially** - Labs build on each other
2. **Type the code** - Don't just copy-paste
3. **Read the output** - Understand what's happening
4. **Complete questions** - Test your knowledge
5. **Experiment** - Try variations of examples
6. **Ask questions** - Use Q&A sections

### For Instructors
1. **Review all materials** before class
2. **Test lab exercises** in advance
3. **Set up demo environment** with monitoring
4. **Adjust timing** based on audience
5. **Encourage questions** throughout
6. **Share real-world examples** from experience

### For Self-Study
1. **Read presentations** as study guides
2. **Complete all labs** independently
3. **Answer lab questions** without looking
4. **Join Apache Ignite community** for support
5. **Practice with real projects** after course

## Troubleshooting

### Common Setup Issues

**Issue: Java not found**
```bash
# Install Java 8 or higher
brew install openjdk@11  # macOS
# or download from https://adoptium.net/
```

**Issue: Maven not found**
```bash
# Install Maven
brew install maven  # macOS
```

**Issue: Port conflicts**
```bash
# Ignite uses ports 47500-47509, 47100-47199
# Check if ports are available:
lsof -i :47500
```

**Issue: Out of memory**
```bash
# Increase JVM heap size
export JVM_OPTS="-Xms2g -Xmx2g"
```

### Lab-Specific Help

Each lab includes a "Common Issues" section with solutions. If stuck:
1. Check the lab's troubleshooting section
2. Review the lab questions and answers
3. Consult Apache Ignite documentation
4. Check logs for error messages

## Additional Resources

### Official Documentation
- **Apache Ignite:** https://ignite.apache.org/
- **Documentation:** https://ignite.apache.org/docs/latest/
- **API Docs:** https://ignite.apache.org/releases/latest/javadoc/
- **Examples:** https://github.com/apache/ignite/tree/master/examples

### Community
- **GitHub:** https://github.com/apache/ignite
- **Mailing Lists:** https://ignite.apache.org/community/resources.html
- **Stack Overflow:** [apache-ignite] tag
- **Slack:** Join the Apache Ignite community

### Videos and Tutorials
- Apache Ignite YouTube Channel
- Conference presentations (ApacheCon, etc.)
- Webinars and online tutorials

### Books
- "High-Performance in-Memory Computing with Apache Ignite"
- Online guides and tutorials

## Contributing

Found an issue or want to improve the course materials?

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

Or simply open an issue to discuss improvements.

## Customization

### For Different Time Constraints

**Half-Day (4 hours):**
- Modules 1, 2, 3 (condensed)
- Labs 1, 3 only

**One-Day (8 hours):**
- Modules 1-6 (condensed)
- Labs 1, 3, 5, 6

**Two-Day:**
- Modules 1-10
- All labs except 12

### For Different Audiences

**Developers:**
- Focus on code examples
- Deep technical dives
- Optimization techniques

**Architects:**
- Design patterns
- Trade-off decisions
- Integration strategies

**Operations:**
- Deployment procedures
- Monitoring setup
- Troubleshooting

## License

Apache Ignite is licensed under **Apache License 2.0**.

These training materials are provided for educational purposes.

## Support

### For Course Questions
- Review lab Q&A sections
- Check presentation materials
- Consult Apache Ignite documentation

### For Technical Issues
- Apache Ignite mailing lists
- Stack Overflow
- GitHub issues

### For Course Materials Issues
- Open an issue in this repository
- Submit a pull request with improvements

---

## Quick Start Checklist

- [ ] Java 8+ installed and verified
- [ ] Maven or Gradle installed
- [ ] IDE configured (IntelliJ, Eclipse, or VS Code)
- [ ] Apache Ignite dependencies added to project
- [ ] Cloned/downloaded course materials
- [ ] Read Lab 1 instructions
- [ ] Ready to start learning!

---

**Ready to master Apache Ignite?** Start with **Lab 1: Environment Setup** 🚀

For questions, issues, or contributions, please open an issue on GitHub.
