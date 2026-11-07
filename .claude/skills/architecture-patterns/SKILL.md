---
name: architecture-patterns
description: "Structure a backend codebase's layers and dependencies with Layered, Clean, Hexagonal (ports and adapters) or DDD tactical patterns, including when NOT to use them and how to migrate toward them. Use when deciding or refactoring how code inside a service is organized; use architecture for system-level option trade-offs and saga-orchestration for cross-service consistency."
risk: none
source: community
date_added: "2026-02-27"
---

# Architecture Patterns

Choose and apply code-organization patterns inside a backend service: where business logic lives, which way dependencies point, and how infrastructure plugs in.

## Use this skill when

- Designing the module/layer structure of a new backend service
- Refactoring a tangled codebase toward testable boundaries
- Introducing ports/adapters so infrastructure can be swapped or faked in tests
- Applying DDD tactical patterns (entities, value objects, aggregates, domain events, repositories)
- Splitting a monolith into modules before (or instead of) splitting into services

## Do not use this skill when

- The change is a small local refactor (use `refactoring-guru`)
- You are choosing between system shapes such as monolith vs microservices (use `architecture`)
- The work is frontend-only

## Choose the Pattern

| Pattern | Core idea | Use when | Do NOT use when |
|---------|-----------|----------|-----------------|
| Transaction script / simple layered | Controller -> service -> ORM | CRUD, thin rules, small team, short-lived app | Rules grow complex and duplicated across services |
| Clean Architecture | Concentric layers, dependencies point inward to entities/use cases | Long-lived service, rich rules, many delivery channels (HTTP, CLI, jobs) | Simple CRUD; tiny services; prototypes. Layers then add mapping code without benefit |
| Hexagonal (ports and adapters) | Core exposes/consumes ports; adapters implement them | Several integrations that really vary (DBs, brokers, third-party APIs), strong need for in-memory test doubles | Only one adapter per port will ever exist and tests can hit a real DB cheaply |
| DDD tactical patterns | Aggregates enforce invariants; value objects; domain events | Complex domain with invariants and domain experts available | Data-centric apps where "business logic" is validation and persistence |
| Modular monolith | One deployable, modules with explicit public APIs and no shared tables | Multiple domains, one team or few teams, want service-like boundaries without distributed-systems cost | One tiny domain (overkill) or teams already needing independent deploys at scale |

Decision criteria, in order:

1. **Domain complexity:** count invariants and rules that span multiple entities. Few -> layered; many -> DDD aggregates inside Clean/Hexagonal.
2. **Expected lifetime and change rate:** throwaway or MVP -> simplest; years of evolution -> invest in boundaries.
3. **Integration variance:** do adapters actually vary (tests, multiple providers)? If not, a port is indirection without payoff.
4. **Team size and ownership:** multiple teams in one codebase -> modular monolith with enforced module boundaries.
5. **Testing strategy:** if fast unit tests of business rules matter, keep the core free of framework and I/O.

Clean and Hexagonal are compatible: most real implementations are "a domain core + use cases + ports, with adapters at the edge." Pick one vocabulary and use it consistently.

## Adjacent Patterns (brief)

- **CQRS:** separate write model (commands, invariants) from read models (queries, projections). Use when read and write shapes or loads diverge sharply; not for plain CRUD. Can be done in one database.
- **Event sourcing:** persist domain events as the source of truth and rebuild state from them. Use for strong audit/temporal requirements; costs: event versioning, projections, rebuild time, harder ad hoc queries.
- **Domain events + outbox:** publish integration events reliably from the same transaction as state changes (see `saga-orchestration`).

## Procedure

1. Clarify domain boundaries, invariants, integrations and expected lifetime.
2. Select the pattern with the table above; document why the simpler option was rejected.
3. Define modules and their public interfaces; write the dependency rule (e.g. `domain` imports nothing from `adapters` or frameworks).
4. Enforce the rule mechanically where possible (import-linter, ArchUnit, dependency-cruiser, Go `internal/` packages, module visibility).
5. Migrate incrementally: wrap infrastructure behind a port at the seam you are already changing; move rules into the domain as you touch them; do not rewrite everything at once.
6. Validate: core unit tests run without DB/network; adapters have integration tests; no cyclic module dependencies.

## Done When

- The dependency direction is documented and checked by a tool or test
- Business rules are testable without infrastructure
- Each module or adapter has one clear owner and public interface
- The chosen pattern's cost (mapping code, indirection) is justified by a listed requirement

## Resources

- `resources/implementation-playbook.md`: Python examples of Clean Architecture layers, Hexagonal ports/adapters and DDD value objects, entities, aggregates, domain events and repositories, plus pitfalls (anemic domain, framework coupling, repository leakage).

## Related Skills

`architecture` (system-level trade-offs), `saga-orchestration` (cross-service workflows), `design-patterns` (GoF patterns within modules), `database-design` (persistence model).
