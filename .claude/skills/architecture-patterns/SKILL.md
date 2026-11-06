---
name: architecture-patterns
description: "Structure a backend codebase's layers and dependencies with Layered, Clean, Hexagonal (ports and adapters) or DDD tactical patterns, including when NOT to use them and how to migrate toward them. Use when deciding or refactoring how code inside a service is organized; use architecture for system-level option trade-offs and saga-orchestration for cross-service consistency."
risk: none
source: community
date_added: "2026-02-27"
---

# Architecture Patterns

## Use this skill when

- Designing module/layer structure of new backend service
- Refactoring tangled codebase toward testable boundaries
- Introducing ports/adapters to swap or fake infrastructure in tests
- Applying DDD tactical patterns (entities, value objects, aggregates, domain events, repositories)
- Splitting monolith into modules before (or instead of) services

## Do not use this skill when

- Change is small local refactor (use `refactoring-guru`)
- Choosing system shape, e.g. monolith vs microservices (use `architecture`)
- Work is frontend-only

## Choose the Pattern

| Pattern | Core idea | Use when | Do NOT use when |
|---------|-----------|----------|-----------------|
| Transaction script / simple layered | Controller -> service -> ORM | CRUD, thin rules, small team, short-lived app | Rules grow complex and duplicated across services |
| Clean Architecture | Concentric layers, dependencies point inward to entities/use cases | Long-lived service, rich rules, many delivery channels (HTTP, CLI, jobs) | Simple CRUD, tiny services, prototypes: layers add mapping code without benefit |
| Hexagonal (ports and adapters) | Core exposes/consumes ports; adapters implement them | Several integrations that vary in practice (DBs, brokers, third-party APIs), strong need for in-memory test doubles | Only one adapter per port will ever exist and tests can hit real DB cheaply |
| DDD tactical patterns | Aggregates enforce invariants; value objects; domain events | Complex domain with invariants and domain experts available | Data-centric apps where "business logic" is validation and persistence |
| Modular monolith | One deployable, modules with explicit public APIs and no shared tables | Multiple domains, one or few teams, want service-like boundaries without distributed-systems cost | One tiny domain (overkill) or teams already needing independent deploys at scale |

Decision criteria, in order:

1. **Domain complexity:** count invariants and rules spanning multiple entities. Few -> layered; many -> DDD aggregates inside Clean/Hexagonal.
2. **Expected lifetime and change rate:** throwaway or MVP -> simplest; years of evolution -> invest in boundaries.
3. **Integration variance:** do adapters vary in practice (tests, multiple providers)? If not, port is indirection without payoff.
4. **Team size and ownership:** multiple teams in one codebase -> modular monolith with enforced module boundaries.
5. **Testing strategy:** if fast unit tests of business rules matter, keep core free of framework and I/O.

Clean and Hexagonal are compatible: most real implementations are "a domain core + use cases + ports, with adapters at the edge." Pick one vocabulary, use it consistently.

## Adjacent Patterns (brief)

- **CQRS:** separate write model (commands, invariants) from read models (queries, projections). Use when read and write shapes or loads diverge sharply. Not for plain CRUD. Works in one database.
- **Event sourcing:** persist domain events as source of truth; rebuild state from them. Use for strong audit/temporal requirements. Costs: event versioning, projections, rebuild time, harder ad hoc queries.
- **Domain events + outbox:** publish integration events reliably from same transaction as state changes (see `saga-orchestration`).

## Procedure

1. Clarify domain boundaries, invariants, integrations, expected lifetime.
2. Select pattern with table above. Document why simpler option was rejected.
3. Define modules and public interfaces. Write dependency rule (e.g. `domain` imports nothing from `adapters` or frameworks).
4. Enforce rule mechanically where possible (import-linter, ArchUnit, dependency-cruiser, Go `internal/` packages, module visibility).
5. Migrate incrementally: wrap infrastructure behind port at seam you are already changing. Move rules into domain as you touch them. Do not rewrite everything at once.
6. Validate: core unit tests run without DB/network; adapters have integration tests; no cyclic module dependencies.

## Done When

- Dependency direction documented and checked by tool or test
- Business rules testable without infrastructure
- Each module or adapter has one clear owner and public interface
- Chosen pattern's cost (mapping code, indirection) justified by listed requirement

## Resources

- `resources/implementation-playbook.md`: Python examples of Clean Architecture layers, Hexagonal ports/adapters and DDD value objects, entities, aggregates, domain events and repositories, plus pitfalls (anemic domain, framework coupling, repository leakage).

## Related Skills

`architecture` (system-level trade-offs), `saga-orchestration` (cross-service workflows), `design-patterns` (GoF patterns within modules), `database-design` (persistence model).
