---
name: spec-driven-development
description: "Write and gate a specification (objective, constraints, boundaries, testable success criteria, capability map for multi-module requests) before building a new project or substantial change. Use when requirements are ambiguous or span several modules; use interview-me to clarify intent first and planning-and-task-breakdown once the spec is approved. Ordinary well-specified edits do not need a spec."
---

# Spec-Driven Development

## Overview

Write structured spec before any code. Spec is shared source of truth with human engineer: what we build, why, and how we know it's done.

## When to Use

- New project or substantial feature
- Ambiguous or incomplete requirements
- Change spans several modules or services with boundaries not yet decided
- Architectural decision ahead

**When NOT to use:** Single-line fixes, typos, or well-specified edits (even multi-file) with unambiguous, self-contained requirements.

## The Gated Workflow

Four phases, preceded by Phase 0 scope check that activates only when one request bundles several independently testable capabilities. Do not advance until current phase is validated.

```
SPECIFY ──→ PLAN ──→ TASKS ──→ IMPLEMENT
   │          │        │          │
   ▼          ▼        ▼          ▼
 Human      Human    Human      Human
 reviews    reviews  reviews    reviews
```

### Phase 0: Scope Check

Most requests describe one capability; if so, skip straight to Specify. Phase 0 is the exception and puts no hierarchy on single-capability features.

**Detection.** Decompose before specifying when one requirement bundles several independently testable capabilities:

- Distinct capabilities with own consumers or data (e.g. identity, billing, notifications, reporting)
- Acceptance criteria cluster into groups that could ship and be verified separately
- One capability could be cut or replaced without rewriting others' requirements

**Propose a capability map before any spec.** Small, reviewable: module table plus build order, not project plan:

```markdown
# Capability Map: [Initiative Name]

| Module id | Responsibility | Depends on |
|---|---|---|
| identity | Accounts, sessions, SSO | — |
| billing | Plans, invoices, payments | identity |
| notifications | Email and webhook fan-out | identity |
| reporting | Usage dashboards | billing, notifications |

Build order: identity → billing, notifications → reporting
```

- **Stable module ids.** Kebab-case, chosen once, never renamed mid-initiative. Specs, plans and downstream commands select work by id, not by guessing which spec is active.
- **Dependency direction, no cycles.** Dependencies point one way. Two modules needing each other are one module.
- **Interfaces live at the boundary.** Map records that `billing` depends on `identity`. Their contract goes in provider module's spec (design it with `api-and-interface-design`).

**The map is gated like every phase.** Human reviews module boundaries, dependency direction and build order before any module spec.

**Then recurse per module.** Run Specify → Plan → Tasks → Implement per module, in dependency order. Each gets own spec, scoped to its objective, boundaries and success criteria. Save approved map at project root, module specs beside it, named by module id (`SPEC-identity.md`, `SPEC-billing.md`). Map, not filename guessing, indexes what exists.

### Phase 1: Specify

Start from high-level vision. Ask human clarifying questions until requirements are concrete.

**Surface assumptions immediately.** List them before any spec content:

```
ASSUMPTIONS I'M MAKING:
1. This is a web application (not native mobile)
2. Authentication uses session-based cookies (not JWT)
3. The database is PostgreSQL (based on existing Prisma schema)
4. We're targeting modern browsers only (no IE11)
→ Correct me now or I'll proceed with these.
```

Don't silently fill in ambiguous requirements.

**Write spec covering six core areas:**

1. **Objective** — What and why? Who is user? What is success?

2. **Commands** — Full executable commands with flags, not only tool names.
   ```
   Build: npm run build
   Test: npm test -- --coverage
   Lint: npm run lint -- --fix
   Dev: npm run dev
   ```

3. **Project Structure** — Where source, tests and docs live.
   ```
   src/           → Application source code
   src/components → React components
   src/lib        → Shared utilities
   tests/         → Unit and integration tests
   e2e/           → End-to-end tests
   docs/          → Documentation
   ```

4. **Code Style** — One real code snippet beats three paragraphs describing style. Include naming conventions, formatting rules, good-output examples.

5. **Testing Strategy** — Framework, test locations, coverage expectations, test level per concern.

6. **Boundaries** — Three tiers:
   - **Always do:** Run tests before commits, follow naming conventions, validate inputs
   - **Ask first:** Database schema changes, adding dependencies, changing CI config
   - **Never do:** Commit secrets, edit vendor directories, remove failing tests without approval

**Spec template:**

```markdown
# Spec: [Project/Feature Name]

## Objective
[What we're building and why. User stories or acceptance criteria.]

## Tech Stack
[Framework, language, key dependencies with versions]

## Commands
[Build, test, lint, dev — full commands]

## Project Structure
[Directory layout with descriptions]

## Code Style
[Example snippet + key conventions]

## Testing Strategy
[Framework, test locations, coverage requirements, test levels]

## Boundaries
- Always: [...]
- Ask first: [...]
- Never: [...]

## Success Criteria
[How we'll know this is done — specific, testable conditions]

## Open Questions
[Anything unresolved that needs human input]
```

**External spec tools:** If project already uses OpenSpec or another specification system, keep its artifact format and storage conventions. Do not create duplicate `SPEC.md`. This skill owns clarification, content and approval gates; external tool owns how approved spec is represented.

**Reframe instructions as success criteria.** Turn vague requirements into concrete conditions:

```
REQUIREMENT: "Make the dashboard faster"

REFRAMED SUCCESS CRITERIA:
- Dashboard LCP < 2.5s on 4G connection
- Initial data load completes in < 500ms
- No layout shift during load (CLS < 0.1)
→ Are these the right targets?
```

**Stop after writing the spec (CRITICAL).** Once spec is saved:

1. Summarize it, list any Open Questions.
2. Ask human to approve or request changes.
3. **STOP YOUR TURN IMMEDIATELY.** Do NOT start Phase 2, invoke `planning-and-task-breakdown`, or write code this turn. Planning starts only after human approves spec in later turn.

### Phases 2-4: Plan, Tasks, Implement

After human approves spec (in later turn):

1. **Plan and tasks:** follow `planning-and-task-breakdown`. It owns dependency graph, vertical slicing, task sizing and template, and plan and task list storage (default `tasks/plan.md` and `tasks/todo.md`, unless project uses external tracker). Plan and tasks each get own human review gate.
2. **Implement:** run tasks one at a time with `incremental-implementation` and `test-driven-development`. Use `context-engineering` to load only spec sections and source files current task needs.

## Keeping the Spec Alive

Living document, not one-time artifact:

- **Update when decisions change** — Data model must change? Update spec first, then implement.
- **Update when scope changes** — Reflect added or cut features in spec.
- **Commit the spec** — Keep it in version control with code.
- **Reference the spec in PRs** — Link each PR to spec section it implements.

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "This is simple, I don't need a spec" | Simple tasks need no *long* spec, but still need acceptance criteria. Two lines is fine. |
| "I'll write the spec after I code it" | That's documentation, not specification. Spec's value is forcing clarity *before* code. |
| "The spec will slow us down" | 15-minute spec prevents hours of rework. Waterfall in 15 minutes beats debugging in 15 hours. |
| "Requirements will change anyway" | Hence living document. Outdated spec still beats no spec. |
| "The user knows what they want" | Clear requests still carry implicit assumptions. Spec surfaces them. |
| "It's one big feature; splitting it is overhead" | If acceptance criteria cluster into independently testable groups, monolithic spec makes every downstream task reason over whole contract. Ten-line capability map is cheaper. |
| "I'll decompose during planning" | Planning slices tasks within a spec, after oversized artifact exists. Decide module boundaries and dependency direction before writing spec, not after. |

## Red Flags

- Asking "should I just start building?" before clarifying what "done" means
- Implementing features not in any spec or task list
- Undocumented architectural decisions

## Verification

Before implementation, confirm:

- [ ] Spec covers all six core areas
- [ ] Human reviewed and approved spec
- [ ] Turn ended after saving spec; approval came in later turn
- [ ] Success criteria specific and testable
- [ ] Boundaries (Always/Ask First/Never) defined
- [ ] Spec saved to file in repository
- [ ] If request bundles several independently testable capabilities, capability map (module ids, dependency direction, build order) approved before any module spec
- [ ] Every module spec traces to module id in approved map
