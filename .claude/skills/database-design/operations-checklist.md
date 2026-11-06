# Migration Planning and HA/DR Checklist

> Read when planning large or risky data migration, engine/cloud move, or database availability and recovery design. For per-change zero-downtime steps, see `migrations.md`.

## 1. Choose the Migration Approach

| Approach | How it works | Use when | Main risk |
|----------|--------------|----------|-----------|
| Expand/contract (online) | Add new schema, dual-write or backfill, switch reads, drop old | Schema changes on live system | Long-lived dual paths |
| Big bang | Stop writes, migrate, switch | Small data, acceptable downtime window | Window overrun |
| Trickle / chunked | Copy in batches while live, catch up, cut over | Large tables, engine or cloud moves | Drift between source and target |
| Parallel run | Old and new both serve; compare results | High-risk logic or engine changes | Cost, comparison noise |
| Strangler | Move one bounded context/table group at a time | Splitting monolith database | Cross-boundary joins |

## 2. Migration Plan Checklist

**Before**
- [ ] Inventory affected tables, row counts, write rates, every reader/writer (services, jobs, BI, replicas, CDC consumers)
- [ ] Migration tool and versioning decided (Flyway, Liquibase, Alembic, Prisma Migrate, Atlas, etc.); migrations in version control
- [ ] Each step backward compatible with currently deployed code
- [ ] Large-table operations chunked (batch size, throttle, pause/resume) and avoid long locks. Check engine's online DDL behavior (`CREATE INDEX CONCURRENTLY`, `ALGORITHM=INPLACE`, gh-ost/pt-osc)
- [ ] Rehearsed on production-sized copy; duration and lock impact measured
- [ ] Fresh backup or snapshot verified restorable before starting

**Validation**
- [ ] Row counts and checksums (per chunk or per key range) match between source and target
- [ ] Constraints, indexes, sequences/identity values, triggers, grants and collation carried over
- [ ] Application smoke tests and sample of critical queries pass against target; query plans checked

**Cutover**
- [ ] Owner, time window, communication plan
- [ ] Explicit go/no-go criteria and rollback triggers (error rate, latency, replication lag, validation mismatch)
- [ ] Rollback procedure written and rehearsed. Know point after which rollback means data loss (e.g. after new-only writes)
- [ ] Post-cutover monitoring for at least one business cycle; old schema/system kept until sign-off

## 3. High Availability and Disaster Recovery

**Set targets first**
- [ ] RPO (max data loss) and RTO (max downtime) agreed per database, from business impact, not defaults
- [ ] Failure scenarios listed: instance loss, AZ loss, region loss, corruption/bad migration, accidental delete, ransomware/credential compromise

**Backups**
- [ ] Automated full + incremental/WAL/binlog backups enabling point-in-time recovery inside RPO
- [ ] Backups encrypted, stored in separate account/region, retention matching legal requirements
- [ ] At least one copy protected from deletion by same credentials (immutable/locked storage)
- [ ] Restore tested on schedule and timed; time compared against RTO

**Availability**
- [ ] Topology chosen: single primary + synchronous standby (RPO about 0), async replicas (RPO = lag), multi-primary/distributed SQL (conflict or latency trade-offs)
- [ ] Automatic failover tested; clients reconnect via stable endpoint; connection pools handle failover
- [ ] Replication lag monitored and alerted; reads needing read-your-writes go to primary
- [ ] Multi-region needed? If yes: failover automated or runbooked, DNS/endpoint switch tested, data residency respected

**Operations**
- [ ] Monitoring: latency, throughput, connections, lock waits, replication lag, storage growth, slow queries
- [ ] Capacity forecast for storage and connections over next 12 months
- [ ] DR runbook exists; game day has exercised it

## Done When

- RPO/RTO written down; timed restore test meets them
- Migration has rehearsed rollback and validated data parity
- Failover exercised, not only configured
