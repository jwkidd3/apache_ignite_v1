# Apache Ignite Course Flow
## 3-Day Intensive Training Program

---

## **Day 1: Fundamentals and Basic Operations**

### Morning Session (9:00 AM - 12:00 PM)

#### **Module 1: Introduction to In-Memory Computing** (90 minutes)
**Presentation** (60 min)
- What is in-memory computing and why it matters
- Traditional database limitations and performance bottlenecks
- In-memory computing use cases and benefits
- Apache Ignite overview and ecosystem position
- Ignite vs. other solutions (Redis, Hazelcast, etc.)

**Lab 1** (30 min)
- Environment setup and first Ignite cluster startup
- Verify installation and basic cluster operations

---

#### **Module 2: Ignite Architecture Deep Dive** (90 minutes)
**Presentation** (60 min)
- Ignite cluster topology and node types
- Data distribution and partitioning concepts
- Memory architecture (on-heap, off-heap, persistence)
- Discovery mechanisms (multicast, static IP, cloud discovery)
- Baseline topology and cluster activation

**Lab 2** (30 min)
- Multi-node cluster setup with different discovery mechanisms
- Explore cluster topology and node configuration

---

### Afternoon Session (1:00 PM - 5:00 PM)

#### **Module 3: Basic Cache Operations** (90 minutes)
**Presentation** (60 min)
- Cache API fundamentals
- CRUD operations (put, get, remove, replace)
- Cache modes: PARTITIONED, REPLICATED, LOCAL
- Synchronous vs. asynchronous operations
- Batch operations and performance considerations

**Lab 3** (30 min)
- Implementing basic cache operations with different cache modes
- Performance comparison of sync vs async operations

---

#### **Module 4: Configuration and Deployment** (120 minutes)
**Presentation** (60 min)
- XML vs. programmatic configuration
- Spring Boot integration basics
- Cache configuration parameters
- Node and cluster configuration best practices
- Basic monitoring and logging setup

**Lab 4** (60 min)
- Configure caches with different settings
- Spring Boot integration hands-on
- Set up monitoring and logging

---

### **Day 1 Wrap-up** (30 minutes)
- Q&A session
- Review key concepts
- Preview Day 2 topics

---

## **Day 2: Data Management and Advanced Features**

### Morning Session (9:00 AM - 12:00 PM)

#### **Day 1 Recap** (15 minutes)
- Quick review of previous day's key concepts

#### **Module 5: Data Modeling and Persistence** (105 minutes)
**Presentation** (60 min)
- Data modeling best practices for Ignite
- Affinity keys and colocation strategies
- Native persistence layer overview
- Write-through, write-behind, and read-through patterns
- Integration with external databases

**Lab 5** (45 min)
- Implement data models with proper affinity keys
- Configure persistence and cache stores
- Test different caching patterns

---

#### **Module 6: SQL and Indexing** (120 minutes)
**Presentation** (60 min)
- SQL support in Ignite (DDL, DML, DQL)
- Creating and managing indexes
- Query performance optimization
- JDBC driver usage
- Distributed joins and their implications

**Lab 6** (60 min)
- Create tables, indexes, and perform complex queries
- JDBC connection and query execution
- Query performance analysis

---

### Afternoon Session (1:00 PM - 5:00 PM)

#### **Module 7: Transactions and ACID Properties** (90 minutes)
**Presentation** (60 min)
- Transaction concepts in distributed systems
- Ignite transaction models (PESSIMISTIC, OPTIMISTIC)
- Isolation levels and consistency guarantees
- Deadlock detection and resolution
- Transaction best practices

**Lab 7** (30 min)
- Implement transactional operations with different isolation levels
- Handle deadlock scenarios
- Test transaction rollback and commit

---

#### **Module 8: Advanced Caching Patterns** (120 minutes)
**Presentation** (60 min)
- Cache-aside, write-through, write-behind patterns
- Near caches and client-side caching
- Expiry policies and eviction strategies
- Cache entry processors for atomic operations
- Event handling and continuous queries

**Lab 8** (60 min)
- Implement different caching patterns
- Configure near caches and eviction policies
- Set up event listeners and continuous queries

---

### **Day 2 Wrap-up** (30 minutes)
- Q&A session
- Review advanced features
- Preview Day 3 topics

---

## **Day 3: Distributed Computing and Production Readiness**

### Morning Session (9:00 AM - 12:00 PM)

#### **Day 2 Recap** (15 minutes)
- Quick review of previous day's key concepts

#### **Module 9: Compute Grid Fundamentals** (105 minutes)
**Presentation** (60 min)
- Distributed computing concepts
- Compute closures and jobs
- Task execution and load balancing
- Affinity-aware computing for performance
- MapReduce operations

**Lab 9** (45 min)
- Implement distributed computing tasks
- Create MapReduce jobs
- Test affinity-aware computing

---

#### **Module 10: Integration and Connectivity** (120 minutes)
**Presentation** (60 min)
- REST API usage and web console
- Integration with Spring Framework
- Kafka connector and streaming data
- Hibernate L2 cache integration
- Client vs. server node configurations

**Lab 10** (60 min)
- Set up REST API access and web console
- Configure Kafka connector
- Integrate with external systems

---

### Afternoon Session (1:00 PM - 5:00 PM)

#### **Module 11: Performance Tuning and Monitoring** (105 minutes)
**Presentation** (60 min)
- JVM tuning for Ignite workloads
- Memory management and garbage collection
- Performance metrics and monitoring tools
- Benchmarking and load testing strategies
- Common performance anti-patterns

**Lab 11** (45 min)
- Performance analysis and tuning exercise
- JVM parameter optimization
- Load testing with real workloads

---

#### **Module 12: Production Deployment and Best Practices** (120 minutes)
**Presentation** (60 min)
- Cluster sizing and capacity planning
- Security configuration (authentication, SSL/TLS)
- Backup and disaster recovery strategies
- Rolling updates and maintenance procedures
- Troubleshooting common issues
- Container deployment (Docker, Kubernetes basics)

**Lab 12** (60 min)
- Deploy a production-ready cluster
- Configure security and SSL/TLS
- Set up monitoring and alerting
- Deploy on Docker/Kubernetes

---

### **Final Session** (45 minutes)
#### Course Wrap-up and Certification
- Course review and key takeaways (15 min)
- Best practices summary (15 min)
- Final Q&A (15 min)

---

## **Prerequisites**

- Java programming experience (Java 8+)
- Basic understanding of databases and SQL
- Familiarity with distributed systems concepts (helpful but not required)
- Development environment with Java 8+ and Maven/Gradle

## **Materials Needed**

- Laptop with 8GB+ RAM
- Java 8 or higher installed
- Maven or Gradle
- IDE (IntelliJ IDEA, Eclipse, or VS Code)
- Docker Desktop (for Day 3 container exercises)
- Course repository access with lab materials

## **Daily Schedule**

| Time | Activity |
|------|----------|
| 9:00 AM - 10:30 AM | Module presentation + Lab |
| 10:30 AM - 10:45 AM | Break |
| 10:45 AM - 12:00 PM | Module presentation + Lab |
| 12:00 PM - 1:00 PM | Lunch |
| 1:00 PM - 2:30 PM | Module presentation + Lab |
| 2:30 PM - 2:45 PM | Break |
| 2:45 PM - 4:30 PM | Module presentation + Lab |
| 4:30 PM - 5:00 PM | Wrap-up and Q&A |

## **Learning Outcomes**

By the end of this course, participants will be able to:

1. Understand in-memory computing concepts and Apache Ignite architecture
2. Set up and configure multi-node Ignite clusters
3. Implement cache operations and data modeling best practices
4. Execute SQL queries and optimize performance with proper indexing
5. Handle distributed transactions with appropriate isolation levels
6. Implement advanced caching patterns and distributed computing tasks
7. Integrate Ignite with external systems and frameworks
8. Deploy and monitor production-ready Ignite clusters
9. Troubleshoot common issues and optimize performance
10. Apply security best practices and disaster recovery strategies
