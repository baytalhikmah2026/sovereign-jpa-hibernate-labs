# Sovereign JPA & Hibernate 7 Labs

The finished code for every lab of the Udemy course **Master Sovereign JPA & Hibernate 7 Architecture** by Bayt Al Hikmah.

One Spring Boot project, `sovereign-persistence` (an online shop), grows from Lab 1 to Lab 100. Each lab ends with a Git tag (`lab-01` … `lab-100`) that holds the project exactly as it is at the end of that lab.

## Stack

Java 25 · Spring Boot 4.0.9 · Hibernate ORM 7.2 (Jakarta Persistence 3.2) · Maven Wrapper · PostgreSQL 18 with pgvector · Testcontainers 2 · HashiCorp Vault · Docker Compose

## How to use a lab snapshot

- Browse a lab: click its link below (it opens the code at that lab's tag).
- Start a lab from the previous lab's code: clone the repository and check out the tag, for example:

```bash
git clone https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs.git
cd sovereign-jpa-hibernate-labs
git checkout lab-12
```

- Build and run the tests (Docker must be running): `./mvnw test` (macOS/Linux) or `.\mvnw test` (Windows PowerShell).

Local passwords, certificates, snapshots and logs are created by the labs and are never stored in this repository.

## Module 1: The Sovereign Sandbox & Persistence Foundations

- [Lab 1: Architecting the Zero-Trust Local Environment with Docker Testcontainers](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-01)
- [Lab 2: Establishing the Persistence Context: EntityManager State Transitions](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-02)
- [Lab 3: Bootstrapping HikariCP: Connection Pool Mathematical Optimization](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-03)
- [Lab 4: Cryptographic Credential Injection via Local Vault](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-04)
- [Lab 5: The Naming Governance Protocol—Physical vs. Logical Naming Strategies](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-05)
- [Lab 6: The Primary Key Dilemma: UUIDv7 vs. Clustered Sequences](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-06)
- [Lab 7: The Data Gatekeeper – Enforcing JSR-380 Bean Validation](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-07)
- [Lab 8: Advanced Logging Orchestration: Intercepting Pre-Compiled SQL](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-08)
- [Lab 9: Constructing the "Safety Net" — Automated Local Rollback Scripts](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-09)
- [Lab 10: Initial Success Milestone: The 10,000-Row Deterministic Insert](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-10)

## Module 2: Advanced Structural Mapping & Relational Integrity

- [Lab 11: Eradicating Primitive Types: The Power of @Embeddable and Value Objects](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-11)
- [Lab 12: Complex Composite Keys: @IdClass vs. @EmbeddedId Performance](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-12)
- [Lab 13: Neutralizing the Bidirectional @OneToMany Trap](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-13)
- [Lab 14: Decoupling the @ManyToMany: Why Hidden Join Tables Destroy Scale](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-14)
- [Lab 15: Mapping Polymorphic Structures: Single Table vs. Joined Inheritance](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-15)
- [Lab 16: The Table-Per-Class Antipattern: When NOT to Use It](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-16)
- [Lab 17: Mastering AttributeConverter: JSONB and Vector Embeddings in DB](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-17)
- [Lab 18: Mapping Database-Specific Enums](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-18)
- [Lab 19: @ElementCollection—Safe Usage for High-Read, Low-Write Scenarios](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-19)
- [Lab 20: Architectural Bridge – Utilizing @SecondaryTable for Legacy Integration](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-20)
- [Lab 21: Database-Generated Default Values and @Generated Sourcing](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-21)
- [Lab 22: Temporal Precision in 2026: Instant, ZonedDateTime, and DB Timezones](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-22)
- [Lab 23: The Ghost in the Machine — Implementing Soft Deletes with @SQLDelete](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-23)
- [Lab 24: Mapping Immutable Entities for High-Concurrency Read Caches](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-24)
- [Lab 25: The Silent Killer—Relational Integrity Validation](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-25)

## Module 3: High-Performance Query Orchestration

- [Lab 26: JPQL Beyond the Basics: Deterministic Projections](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-26)
- [Lab 27: The Criteria API: Building Type-Safe Dynamic Query Engines](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-27)
- [Lab 28: Generating the JPA Metamodel for Compile-Time Query Safety](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-28)
- [Lab 29: Cross-Join vs. Inner Join vs. Left Join: Execution Plan Analysis](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-29)
- [Lab 30: High-Velocity Analytics: Mastering Complex Aggregations](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-30)
- [Lab 31: The Subquery Mechanics — Correlated vs. Uncorrelated Performance](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-31)
- [Lab 32: Bypassing the ORM—Safe Execution of Native SQL](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-32)
- [Lab 33: The Server-Side Powerhouse — Integrating Stored Procedures](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-33)
- [Lab 34: Managing Keyset Pagination](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-34)
- [Lab 35: Handling Massive Result Sets](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-35)
- [Lab 36: Resilience Engineering — Implementing Query Timeouts](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-36)
- [Lab 37: Hibernate Query Hints – Forcing Deterministic Execution Paths](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-37)
- [Lab 38: DTO Projections—Tuple vs. Constructor Expressions](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-38)
- [Lab 39: Integrating Blaze-Persistence for CTEs (Common Table Expressions)](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-39)
- [Lab 40: Compiling Queries: The Shift to Hibernate Statement Caching](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-40)

## Module 4: Fetching Topologies & Latency Eradication

- [Lab 41: Visualizing the N+1 Catastrophe: Network Packet Sniffing](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-41)
- [Lab 42: Forcing Eager Initialization Safely — The JOIN FETCH Directive](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-42)
- [Lab 43: Designing Static @NamedEntityGraph for Reusable Fetch Profiles](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-43)
- [Lab 44: Programmatic Optimization with Dynamic EntityGraphs](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-44)
- [Lab 45: The Cartesian Product Problem—Multiple Bag Fetches Exception](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-45)
- [Lab 46: Batch Fetching Salvation](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-46)
- [Lab 47: The @Fetch(FetchMode.SUBSELECT) Strategy for Deep Hierarchies](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-47)
- [Lab 48: Activating Hibernate Bytecode Enhancement for Lazy Property Loading](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-48)
- [Lab 49: LOB Management — Streaming Gigabyte BLOBs/CLOBs without OOM Errors](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-49)
- [Lab 50: Advanced Proxy Mechanics & Safe Un-proxying](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-50)
- [Lab 51: DTOs over Entities – When to Abandon the Persistence Context](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-51)
- [Lab 52: StatelessSession: High-Speed Batch Processing Without the Cache](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-52)
- [Lab 53: Velocity at Scale — Mastering JDBC Batch Inserts and Updates](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-53)
- [Lab 54: High-Velocity ID Orchestration: Mastering Sequence Optimizers](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-54)
- [Lab 55: The Zero-Latency Benchmark — Profiling Fetch Strategies with JMH](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-55)

## Module 5: Concurrency, Locking & State Management

- [Lab 56: First-Level Cache Deep Dive: Memory Profiling flush() and clear()](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-56)
- [Lab 57: The merge() Antipattern: Why It Destroys Performance](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-57)
- [Lab 58: Optimistic Concurrency Control: Mastering @Version](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-58)
- [Lab 59: Pessimistic Read vs. Pessimistic Write: Database Row-Level Locking](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-59)
- [Lab 60: Implementing Lock Timeouts (jakarta.persistence.lock.timeout)](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-60)
- [Lab 61: The Ghost in the Machine: Mastering Transaction Isolation](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-61)
- [Lab 62: Second-Level Cache (L2) Architecture: Redis Integration](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-62)
- [Lab 63: Configuring Entity and Collection Caching Regions](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-63)
- [Lab 64: The Query Cache Trap: When Caching Destroys Throughput](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-64)
- [Lab 65: Distributed Cache Invalidation Strategies via Message Brokers](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-65)
- [Lab 66: The Invisible Hand—Mastering Hibernate Interceptors](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-66)
- [Lab 67: JPA EntityListeners — Synchronizing State with External Systems](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-67)
- [Lab 68: Transaction Propagation: Mastering the Atomic Boundary](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-68)
- [Lab 69: The Deadlock Autopsy — Deciphering RDBMS Contention Graphs](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-69)
- [Lab 70: Chaos Engineering: Inducing and Surviving Transaction Collisions](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-70)

## Module 6: Cloud-Native Scaling & Multi-Tenancy

- [Lab 71: Multi-Tenancy Architectures: Schema-per-Tenant Implementation](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-71)
- [Lab 72: Multi-Tenancy Architectures: Database-per-Tenant with Dynamic Routing](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-72)
- [Lab 73: Discriminator-Based Multi-Tenancy with Row-Level Security](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-73)
- [Lab 74: Implementing CurrentTenantIdentifierResolver in Spring Boot](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-74)
- [Lab 75: Read/Write Replica Routing—Splitting the Persistence Unit](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-75)
- [Lab 76: Sharding Large Datasets: Hibernate Search Integration](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-76)
- [Lab 77: Replacing 2PC/XA Transactions with the Outbox Pattern](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-77)
- [Lab 78: Capturing Data Changes (CDC) with Debezium and JPA](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-78)
- [Lab 79: The Custom Repository Factory – Engineering the Persistence Backbone](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-79)
- [Lab 80: Connection Leak Detection and Automatic Thread Recovery](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-80)
- [Lab 81: Exposing Micrometer Metrics: Monitoring the Hibernate Statistics API](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-81)
- [Lab 82: Distributed Tracing—OpenTelemetry Interceptors for JPA Queries](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-82)
- [Lab 83: Resilience4j Integration: Circuit Breaking the Database Layer](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-83)
- [Lab 84: Graceful Degradation — Serving Stale Cache During Database Outages](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-84)
- [Lab 85: Deploying the Persistence Tier to Kubernetes via Helm](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-85)

## Module 7: Sovereign Compliance, Auditing, & Security

- [Lab 86: Hardware-Rooted Security: Connecting JPA via mTLS and KMS](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-86)
- [Lab 87: Hibernate Envers: Setting Up Cryptographic Auditing](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-87)
- [Lab 88: Customizing the Envers RevisionEntity for Identity Tracking](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-88)
- [Lab 89: Querying Temporal Data with Envers AuditReader](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-89)
- [Lab 90: GDPR Right to be Forgotten—Cascading Data Erasure Algorithms](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-90)
- [Lab 91: Database-Level Field Encryption with Custom JPA Converters](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-91)
- [Lab 92: DORA Compliance—Tamper-Proof Audit Log Shipping](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-92)
- [Lab 93: Zero-Trust Database Provisioning with HashiCorp Vault](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-93)
- [Lab 94: Managing Liquibase/Flyway Migrations in a Multi-Node Cluster](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-94)
- [Lab 95: Implementing Zero-Downtime Expand/Contract Schema Evolutions](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-95)

## Module 8: The Capstone & Autonomous Orchestration

- [Lab 96: Architecting for AI Agents: Vector Similarity Search in JPA](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-96)
- [Lab 97: Bridging JPA with Distributed SQL (CockroachDB/Spanner)](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-97)
- [Lab 98: Tuning Global Read Latency across Multi-Region Clusters](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-98)
- [Lab 99: Capstone Pre-Flight – Final Architecture Penetration Test](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-99)
- [Lab 100: The Sovereign Global E-Commerce Data Grid](https://github.com/baytalhikmah2026/sovereign-jpa-hibernate-labs/tree/lab-100)
