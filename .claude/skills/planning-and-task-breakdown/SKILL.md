---
name: planning-and-task-breakdown
description: Turn an approved spec or clear requirements into a dependency-ordered plan of small, vertically sliced tasks, each with acceptance criteria, a verification step and checkpoints, saved to the project's task list. Use when work is too large to start in one step, needs scoping, or could be parallelized; use spec-driven-development first when requirements are not yet written down.
---

# Planning and Task Breakdown

## When to Use

- Spec needs breaking into implementable units
- Task too large or vague to start
- Work needs parallelizing across multiple agents or sessions
- Scope needs communicating to a human
- Implementation order isn't obvious

**When NOT to use:** Single-file changes with obvious scope, or spec already has well-defined tasks.

## The Planning Process

### Step 1: Enter Plan Mode

Before any code, work read-only:

- Read spec and relevant codebase sections
- Find existing patterns and conventions
- Map dependencies between components
- Note risks and unknowns

**Do NOT write code during planning.** Output: plan document in `tasks/plan.md` plus task list in the task list target (see Output Files; default `tasks/todo.md`). Not implementation.

### Step 2: Identify the Dependency Graph

Map what depends on what:

```
Database schema
    │
    ├── API models/types
    │       │
    │       ├── API endpoints
    │       │       │
    │       │       └── Frontend API client
    │       │               │
    │       │               └── UI components
    │       │
    │       └── Validation logic
    │
    └── Seed data / migrations
```

Build bottom-up along dependency graph: foundations first.

### Step 3: Slice Vertically

Build one complete feature path at a time, not all database, then all API, then all UI:

**Bad (horizontal slicing):**
```
Task 1: Build entire database schema
Task 2: Build all API endpoints
Task 3: Build all UI components
Task 4: Connect everything
```

**Good (vertical slicing):**
```
Task 1: User can create an account (schema + API + UI for registration)
Task 2: User can log in (auth schema + API + UI for login)
Task 3: User can create a task (task schema + API + UI for creation)
Task 4: User can view task list (query + API + UI for list view)
```

Each slice delivers working, testable functionality.

### Step 4: Write Tasks

Task structure, for markdown task list or external tracker item (see Output Files):

```markdown
## Task [N]: [Short descriptive title]

**Description:** One paragraph explaining what this task accomplishes.

**Acceptance criteria:**
- [ ] [Specific, testable condition]
- [ ] [Specific, testable condition]

**Verification:**
- [ ] Tests pass: [the repository's focused-test command]
- [ ] Build succeeds: [the repository's build command]
- [ ] Manual check: [description of what to verify]

**Dependencies:** [Task numbers this depends on, or "None"]

**Files likely touched:**
- `src/path/to/file.ts`
- `tests/path/to/test.ts`

**Estimated scope:** [XS: 1 file | S: 1-2 files | M: 3-5 files] (anything larger is L/XL: split it before writing it down)
```

### Step 5: Order and Checkpoint

Order tasks so:

1. Dependencies satisfied (foundation first)
2. Each task leaves system working
3. Verification checkpoint after every 2-3 tasks
4. High-risk tasks early (fail fast)

Add explicit checkpoints to task list target:

```markdown
## Checkpoint: After Tasks 1-3
- [ ] All tests pass
- [ ] Application builds without errors
- [ ] Core user flow works end-to-end
- [ ] Review with human before proceeding
```

## Task Sizing Guidelines

| Size | Files | Scope | Example |
|------|-------|-------|---------|
| **XS** | 1 | Single function or config change | Add a validation rule |
| **S** | 1-2 | One component or endpoint | Add a new API endpoint |
| **M** | 3-5 | One feature slice | User registration flow |
| **L** | 5-8 | Multi-component feature | Search with filtering and pagination |
| **XL** | 8+ | **Too large — break down further** | — |

Break L or larger into smaller tasks. Agents do best on S and M.

**When to break a task down further:**
- Takes more than one focused session (roughly 2+ hours of agent work)
- Acceptance criteria don't fit in 3 or fewer bullets
- Touches two or more independent subsystems (e.g., auth and billing)
- Title needs "and" (sign of two tasks)

## Output Files

- **Plan document:** `tasks/plan.md`, always markdown. Design decisions, risks and open questions don't map cleanly onto tracker issues.
- **Task list:** each task goes in **task list target** (defined below).

Create `tasks/` if it does not exist.

**Never overwrite an incomplete plan.** Before writing `tasks/plan.md` or `tasks/todo.md`, check if they exist with unchecked tasks:

- Same work replanned (user asked to revise or extend this plan) → update existing files in place.
- Different work → **stop and ask.** Unchecked tasks may be mid-build in another session. Do not delete, overwrite or rename existing files on your own. Present conflict; user decides: finish old plan first, explicitly discard it, or say where new plan goes.

Same for external task list target: never bulk-close or delete another plan's open tracker items to make room.

### Task List Target

Where tasks and checkpoints get recorded.

- **Default: checklist-style markdown file at `tasks/todo.md`.** Use unless project says otherwise.
- **External tracker:** if project agent rules (`CLAUDE.md`, `AGENTS.md`, etc.) or user designate an issue tracker (e.g. GitHub Issues, Jira, Linear, `bd`/beads), create one tracker item per task instead of writing `tasks/todo.md`. Map Step 4 structure onto tracker fields. Acceptance criteria and verification steps go in item body. Dependencies use tracker's linking mechanism (`bd dep add`, "blocked by", etc.). Step 5 checkpoints become tracker items, or a plan-document checklist if tracker has no natural equivalent.

With external tracker, note it in `tasks/plan.md` (e.g. "Tasks tracked in Linear project FOO") so downstream steps and future sessions know where to look. Plan's Task List section then holds ordered index of tracker item IDs or links, not duplicate checklist.

## Plan Document Template

```markdown
# Implementation Plan: [Feature/Project Name]

## Overview
[One paragraph summary of what we're building]

## Architecture Decisions
- [Key decision 1 and rationale]
- [Key decision 2 and rationale]

## Task List

### Phase 1: Foundation
- [ ] Task 1: ...
- [ ] Task 2: ...

### Checkpoint: Foundation
- [ ] Tests pass, builds clean

### Phase 2: Core Features
- [ ] Task 3: ...
- [ ] Task 4: ...

### Checkpoint: Core Features
- [ ] End-to-end flow works

### Phase 3: Polish
- [ ] Task 5: ...
- [ ] Task 6: ...

### Checkpoint: Complete
- [ ] All acceptance criteria met
- [ ] Ready for review

## Risks and Mitigations
| Risk | Impact | Mitigation |
|------|--------|------------|
| [Risk] | [High/Med/Low] | [Strategy] |

## Open Questions
- [Question needing human input]
```

## Parallelization Opportunities

With multiple agents or sessions:

- **Safe to parallelize:** independent feature slices, tests for already-implemented features, documentation
- **Must be sequential:** database migrations, shared state changes, dependency chains
- **Needs coordination:** features sharing an API contract (define contract first, then parallelize)

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "The old `tasks/plan.md` is stale, I'll just replace it" | Unchecked tasks may be mid-build in another session. Overwriting destroys work state stored nowhere else. Stop and ask. |

## Verification

Before implementing, confirm:

- [ ] Every task has acceptance criteria
- [ ] Every task has verification step
- [ ] Task dependencies identified and correctly ordered
- [ ] Tasks recorded in task list target (default `tasks/todo.md`)
- [ ] No pre-existing incomplete plan overwritten without explicit user confirmation
- [ ] No task touches more than ~5 files
- [ ] Checkpoints exist between major phases
- [ ] Human reviewed and approved plan

## Acceptance Criteria vs. Definition of Done

Acceptance criteria are per-task and answer "did we build the right thing?". They sit on top of project-wide Definition of Done. Definition of Done is the standing bar every task clears before counting as done: repository's required checks (tests, lint, type-check, build) from its CI config or agent rules. Task is done only when both are met.
