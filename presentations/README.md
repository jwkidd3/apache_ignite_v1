# Apache Ignite Training - Presentations

This directory contains reveal.js-based presentations for the Apache Ignite training course.

## Viewing Presentations

Open any `.html` file directly in a web browser. The presentations use CDN-hosted reveal.js, so an internet connection is required.

### Navigation
- **Arrow keys**: Navigate between slides
- **Space**: Next slide
- **Escape**: Overview mode
- **S**: Speaker notes (if available)
- **F**: Fullscreen

## Presentation Modules

### Day 1: Fundamentals

| Module | Title | Duration | File |
|--------|-------|----------|------|
| 01 | Introduction to In-Memory Computing | 60 min | [module-01-intro-inmemory-computing.html](module-01-intro-inmemory-computing.html) |
| 02 | Architecture Deep Dive | 60 min | [module-02-architecture-deep-dive.html](module-02-architecture-deep-dive.html) |
| 03 | Basic Cache Operations | 60 min | [module-03-basic-cache-operations.html](module-03-basic-cache-operations.html) |
| 04 | Configuration and Deployment | 45 min | [module-04-configuration-deployment.html](module-04-configuration-deployment.html) |

### Day 2: Data Management

| Module | Title | Duration | File |
|--------|-------|----------|------|
| 05 | Data Modeling and Persistence | 60 min | [module-05-data-modeling-persistence.html](module-05-data-modeling-persistence.html) |
| 06 | SQL and Indexing | 60 min | [module-06-sql-indexing.html](module-06-sql-indexing.html) |
| 07 | Transactions and ACID | 45 min | [module-07-transactions-acid.html](module-07-transactions-acid.html) |
| 08 | Advanced Caching Patterns | 60 min | [module-08-advanced-caching.html](module-08-advanced-caching.html) |

### Day 3: Advanced Topics & Production

| Module | Title | Duration | File |
|--------|-------|----------|------|
| 09 | Compute Grid | 45 min | [module-09-compute-grid.html](module-09-compute-grid.html) |
| 10 | Integration and Connectivity | 60 min | [module-10-integration-connectivity.html](module-10-integration-connectivity.html) |
| 11 | Performance Tuning | 45 min | [module-11-performance-tuning.html](module-11-performance-tuning.html) |
| 12 | Production Deployment | 60 min | [module-12-production-deployment.html](module-12-production-deployment.html) |

## Course Flow

```
Day 1 (Fundamentals)
├── Module 01: Introduction to In-Memory Computing
│   └── Lab 01: Environment Setup
├── Module 02: Architecture Deep Dive
│   └── Lab 02: Multi-Node Cluster
├── Module 03: Basic Cache Operations
│   └── Lab 03: Basic Cache Operations
└── Module 04: Configuration and Deployment
    └── Lab 04: Configuration and Deployment

Day 2 (Data Management)
├── Module 05: Data Modeling and Persistence
│   └── Lab 05: Data Modeling and Persistence
├── Module 06: SQL and Indexing
│   └── Lab 06: SQL and Indexing
├── Module 07: Transactions and ACID
│   └── Lab 07: Transactions and ACID
└── Module 08: Advanced Caching Patterns
    └── Lab 08: Advanced Caching

Day 3 (Advanced & Production)
├── Module 09: Compute Grid
│   └── Lab 09: Compute Grid
├── Module 10: Integration and Connectivity
│   └── Lab 10: Integration and Connectivity
├── Module 11: Performance Tuning
│   └── Lab 11: Performance Tuning
└── Module 12: Production Deployment
    └── Lab 12: Production Deployment
```

## Technology

All presentations use:
- [reveal.js 4.5.0](https://revealjs.com/) - HTML presentation framework
- [highlight.js](https://highlightjs.org/) - Syntax highlighting for code blocks
- CDN-hosted resources (no local installation required)

## Customization

The presentations use a consistent theme with:
- White background
- Heading color: `#2c3e50`
- Accent color: `#e67e22` (orange)
- Code highlighting: GitHub theme

## Presentation Delivery Tips

### Before Class
- Review all slides
- Test code examples work
- Prepare demo environment
- Check lab materials are ready

### During Presentation
- Start with objectives
- Use real-world examples
- Encourage questions
- Show live demos when possible
- Reference upcoming labs

### Timing Guidelines

**60-Minute Module Breakdown:**
- Introduction: 5 minutes
- Core Content: 40-45 minutes
- Advanced Topics: 5-10 minutes
- Q&A and Wrap-up: 5 minutes

## Additional Resources

- Apache Ignite Docs: https://ignite.apache.org/docs/latest/
- API JavaDocs: https://ignite.apache.org/releases/latest/javadoc/
- GitHub: https://github.com/apache/ignite
- Stack Overflow: [apache-ignite] tag
