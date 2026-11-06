---
name: database-design
description: "Engine-neutral database design decisions: choosing a database and ORM, relational schema and keys, indexing strategy, query/N+1 review, safe zero-downtime migrations, and migration/HA/DR planning (RPO/RTO, backups, failover). Use when deciding what to build before writing data-access code; use using-relational-databases or using-document-databases for driver/ORM implementation and architecting-data for analytics platforms."
risk: safe
source: community
date_added: "2026-02-27"
---

# Database Design

## When to Use

- Choosing database engine or hosting model for new service
- Choosing ORM or query builder
- Designing or reviewing tables, keys, relationships, constraints
- Planning indexes for known queries, or reviewing slow queries for N+1 and missing indexes
- Planning schema change on live system, large data migration, or engine/cloud move
- Setting backup, replication, failover and RPO/RTO targets

Not for: driver/ORM code (use `using-relational-databases`, `using-document-databases`), warehouse/lakehouse modeling (use `architecting-data`), application caching architecture.

## Selective Reading Rule

**Read ONLY files relevant to request.**

| File | Description | When to Read |
|------|-------------|--------------|
| `database-selection.md` | PostgreSQL vs SQLite vs serverless/edge options | Choosing database |
| `orm-selection.md` | Drizzle vs Prisma vs Kysely | Choosing ORM (TypeScript) |
| `schema-design.md` | Normalization, PKs, relationships | Designing schema |
| `indexing.md` | Index types, composite indexes | Performance tuning |
| `optimization.md` | N+1, EXPLAIN ANALYZE | Query optimization |
| `migrations.md` | Expand/contract steps for single schema changes | Schema changes |
| `operations-checklist.md` | Migration approach selection, cutover/rollback, HA/DR, RPO/RTO | Large migrations, availability and recovery design |

## Workflow

1. **Capture access patterns:** top queries and writes, frequency, latency targets, data volume and growth, consistency needs.
2. **Ask about constraints:** existing stack, hosting, team familiarity, compliance, data residency. Ask user instead of defaulting.
3. **Pick engine and ORM** (`database-selection.md`, `orm-selection.md`).
4. **Model schema:** entities, keys, relationships, constraints, nullability (`schema-design.md`).
5. **Design indexes for listed queries**, not speculatively (`indexing.md`). Verify with `EXPLAIN` (`optimization.md`).
6. **Plan change and recovery:** migration steps (`migrations.md`), plus `operations-checklist.md` for anything large, cross-engine or availability-critical.

## Decision Checklist

- [ ] Asked user about database preference and existing infrastructure
- [ ] Engine chosen for this context (not PostgreSQL by reflex, not SQLite for multi-writer services)
- [ ] Deployment environment and connection model considered (serverless needs pooling/HTTP drivers)
- [ ] Every foreign key and every frequent filter/sort has planned index
- [ ] Relationship cardinality and delete behavior defined
- [ ] Migration path backward compatible with running code
- [ ] RPO/RTO and backup/restore plan stated for production data

## Anti-Patterns

- `SELECT *` in production queries
- JSON columns for data with stable structure that is queried relationally
- Ignoring N+1 queries from ORM lazy loading
- One-step breaking migrations (rename/drop) on live tables

## Tooling

`python3 scripts/schema_validator.py <project_path>` runs heuristic checks on **Prisma** `schema.prisma` files only (naming, missing `@id`, missing `createdAt`, unindexed `*Id` fields). Does not validate SQL DDL, Drizzle or other ORMs. For those, review schema manually against `schema-design.md` and `indexing.md`.

## Done When

- Each planned query maps to index or justified table scan
- Constraints enforce invariants the application relies on
- Migration plan reversible, or its point of no return explicit
- Recovery targets written down and backed by tested restore

Attribution for merged material: `ATTRIBUTION.md`.
