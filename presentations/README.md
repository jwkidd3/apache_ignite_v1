# Apache Ignite Presentations

This directory contains presentation content for all 12 modules of the Apache Ignite training course.

## Presentation Structure

Each presentation module includes:
- **Slide Content**: Detailed talking points for each slide
- **Code Examples**: Copy-paste ready code samples
- **Visuals**: ASCII diagrams and tables
- **Instructor Notes**: Timing, key points, demos, common questions
- **Discussion Prompts**: Engagement opportunities

## Module List

### Day 1: Fundamentals

1. **Module 1: Introduction to In-Memory Computing** (60 min)
   - File: `module01_intro_inmemory_computing.md`
   - Topics: IMC concepts, use cases, Ignite overview, comparisons
   - Key Slides: Performance comparison, use cases, Ignite vs Redis/Hazelcast

2. **Module 2: Ignite Architecture Deep Dive** (60 min)
   - File: `module02_architecture_deep_dive.md`
   - Topics: Cluster topology, data distribution, memory architecture, discovery
   - Key Slides: Node types, partitioning, baseline topology, rebalancing

3. **Module 3: Basic Cache Operations** (60 min)
   - File: `module03_basic_cache_operations.md`
   - Topics: CRUD operations, cache modes, sync/async, batch operations
   - Key Slides: Cache mode comparison, async performance, batch optimization

4. **Module 4: Configuration and Deployment** (60 min)
   - File: `module04_configuration_deployment.md`
   - Topics: XML vs programmatic config, Spring Boot, monitoring, logging
   - Key Slides: Configuration patterns, Spring integration, metrics setup

### Day 2: Advanced Features

5. **Module 5: Data Modeling and Persistence** (60 min)
   - File: `module05_data_modeling_persistence.md`
   - Topics: Affinity keys, colocation, native persistence, cache stores
   - Key Slides: Affinity benefits, persistence architecture, store patterns

6. **Module 6: SQL and Indexing** (60 min)
   - File: `module06_sql_indexing.md`
   - Topics: SQL support, DDL/DML/DQL, indexes, JDBC, distributed joins
   - Key Slides: SQL capabilities, index types, query optimization, joins

7. **Module 7: Transactions and ACID Properties** (60 min)
   - File: `module07_transactions_acid.md`
   - Topics: Transaction models, isolation levels, deadlocks, best practices
   - Key Slides: PESSIMISTIC vs OPTIMISTIC, isolation comparison, deadlock handling

8. **Module 8: Advanced Caching Patterns** (60 min)
   - File: `module08_advanced_caching.md`
   - Topics: Near caches, expiry/eviction, entry processors, continuous queries
   - Key Slides: Near cache benefits, expiry policies, entry processor patterns

### Day 3: Production

9. **Module 9: Compute Grid Fundamentals** (60 min)
   - File: `module09_compute_grid.md`
   - Topics: Distributed computing, closures, affinity compute, MapReduce
   - Key Slides: Compute patterns, affinity benefits, MapReduce workflow

10. **Module 10: Integration and Connectivity** (60 min)
    - File: `module10_integration_connectivity.md`
    - Topics: REST API, Spring integration, Kafka, Hibernate L2, client/server
    - Key Slides: Integration patterns, Kafka streaming, Hibernate setup

11. **Module 11: Performance Tuning and Monitoring** (60 min)
    - File: `module11_performance_tuning.md`
    - Topics: JVM tuning, GC optimization, metrics, benchmarking, anti-patterns
    - Key Slides: JVM settings, GC comparison, performance checklist, anti-patterns

12. **Module 12: Production Deployment and Best Practices** (60 min)
    - File: `module12_production_deployment.md`
    - Topics: Security (SSL/TLS), Docker, Kubernetes, backup/DR, rolling updates
    - Key Slides: Security checklist, K8s deployment, backup strategies, troubleshooting

## Presentation Delivery Tips

### Before Class
- Review all slides and notes
- Test all code examples
- Prepare demo environment
- Check lab materials are ready
- Set up monitoring tools

### During Presentation
- Start with objectives
- Use real-world examples
- Encourage questions
- Show live demos when possible
- Reference upcoming labs
- Watch timing

### After Each Module
- Recap key points
- Answer questions
- Preview next module
- Transition to lab
- Be available during lab

## Timing Guidelines

**60-Minute Module Breakdown:**
- Introduction: 5 minutes
- Core Content: 40-45 minutes
- Advanced Topics: 5-10 minutes
- Q&A and Wrap-up: 5 minutes

**Adjust Based On:**
- Audience experience level
- Questions and discussion
- Demo time needed
- Lab complexity

## Visual Aids

Each presentation includes:
- **Architecture Diagrams**: ASCII art for cluster topology, data flow
- **Code Examples**: Syntax-highlighted, copy-paste ready
- **Comparison Tables**: Feature matrices, performance comparisons
- **Flowcharts**: Process flows, decision trees
- **Checklists**: Best practices, troubleshooting steps

## Instructor Resources

### Additional Materials Needed
- Laptop with Ignite installed
- Projector/screen
- Whiteboard for ad-hoc diagrams
- Lab environment access
- Sample datasets
- Monitoring dashboards

### Demo Environment Setup
```bash
# Start 3-node cluster for demos
bin/ignite.sh -v config/demo-node1.xml &
bin/ignite.sh -v config/demo-node2.xml &
bin/ignite.sh -v config/demo-node3.xml &
```

### Common Demo Scenarios
1. **Module 1**: Show simple put/get, show cluster starting
2. **Module 2**: Add/remove nodes, show rebalancing
3. **Module 3**: Compare sync vs async, batch performance
4. **Module 5**: Demonstrate affinity colocation benefit
5. **Module 6**: Execute SQL queries, show query plans
6. **Module 9**: Run distributed compute, show MapReduce
7. **Module 11**: Show JMX metrics, GC logs
8. **Module 12**: Deploy to Docker, show Kubernetes pods

## Customization Guide

### For Different Audiences

**Developers:**
- More code examples
- Deep technical dives
- Architecture internals
- Optimization techniques

**Architects:**
- Design patterns
- Trade-off decisions
- Capacity planning
- Integration strategies

**Operations:**
- Deployment procedures
- Monitoring setup
- Troubleshooting
- Performance tuning

### For Different Time Constraints

**Half-Day (4 hours):**
- Modules 1, 2, 3, 5 (condensed)
- Focus on essentials
- One lab

**One-Day (8 hours):**
- Modules 1-6 (condensed)
- Essential labs (1, 3, 5, 6)
- Basic deployment

**Two-Day:**
- Modules 1-10
- All labs except 12
- Docker deployment only

**Three-Day (Full):**
- All 12 modules
- All 12 labs
- Full production deployment

## Assessment Questions

Each module includes questions for:
- **Knowledge Check**: Test understanding
- **Discussion**: Stimulate thinking
- **Lab Prep**: Bridge to hands-on

### Example Questions Bank

**Module 1:**
- What are the main benefits of in-memory computing?
- When would you NOT use Ignite?
- How does Ignite compare to Redis?

**Module 2:**
- What's the difference between server and client nodes?
- How does baseline topology work?
- Explain the rebalancing process

**Module 3:**
- Which cache mode for large, scalable data?
- Why use batch operations?
- When to use async operations?

## Additional Resources

### Official Documentation
- Apache Ignite Docs: https://ignite.apache.org/docs/latest/
- API JavaDocs: https://ignite.apache.org/releases/latest/javadoc/

### Community
- Mailing Lists: https://ignite.apache.org/community/resources.html
- Stack Overflow: [apache-ignite] tag
- GitHub: https://github.com/apache/ignite

### Videos
- Apache Ignite YouTube Channel
- Conference talks (ApacheCon, etc.)
- Webinar recordings

### Books
- "High-Performance in-Memory Computing with Apache Ignite"
- Online tutorials and guides

## Feedback and Improvements

We welcome feedback on presentations:
- Slide clarity and organization
- Additional examples needed
- Timing adjustments
- Visual improvements
- Missing topics

## Version History

- v1.0: Initial release (all 12 modules)
- Based on Apache Ignite 2.16.0
- Aligned with 3-day course format

## License

These presentation materials are provided as-is for educational purposes.
Apache Ignite is licensed under Apache License 2.0.

---

**Ready to teach? Start with Module 1!**
