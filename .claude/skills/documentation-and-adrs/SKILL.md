---
name: documentation-and-adrs
description: Write ADRs, READMEs, inline comments, API reference updates and changelog entries that record why a change was made, and check documented examples against the code. Use when making a hard-to-reverse decision, changing a public API, or shipping user-visible behavior; use generating-documentation for docs-site and doc-tooling setup.
---

# Documentation and ADRs

Document decisions, not just code. Capture the *why*: context, constraints, trade-offs and alternatives considered. Code shows *what*.

## When to Use

- Making significant architectural decision
- Choosing between competing approaches
- Adding or changing public API
- Shipping user-facing behavior change
- Onboarding new team members (or agents)
- Explaining same thing repeatedly

**When NOT to use:** Don't document obvious code. Don't write comments restating code. Don't document throwaway prototypes.

## Architecture Decision Records (ADRs)

### When to Write an ADR

- Choosing framework, library or major dependency
- Designing data model or database schema
- Selecting authentication strategy
- Choosing API architecture (REST vs. GraphQL vs. tRPC)
- Choosing build tools, hosting platforms or infrastructure
- Any decision expensive to reverse

### Match the existing convention first

Before creating an ADR, check available repository context for established convention: existing ADRs, project instructions, ADR configuration or tooling (e.g. `.adr-dir` file). Established convention overrides defaults below. Match:

- **Location and format** — e.g. `docs/adr/*.md`, `Documentation/Decisions/*.rst`, MADR layout or `adr-tools` setup. Match directory, file extension and markup (Markdown vs reStructuredText).
- **Numbering and naming** — continue existing sequence and filename pattern (`ADR-004-Title.rst`, `0004-title.md`, …). Don't restart at 001 or add second scheme.
- **Section headings** — reuse project's headings, not this template's.

If evidence conflicts, surface the conflict. Do not silently add another scheme. Use default below only when no convention can be established.

### ADR Template

Store ADRs in `docs/decisions/`, numbered sequentially (unless project uses another location; see above):

```markdown
# ADR-001: Use PostgreSQL for primary database

## Status
Proposed | Accepted | Superseded by ADR-XXX | Deprecated

## Date
2025-01-15

## Context
We need a primary database for the task management application. Key requirements:
- Relational data model (users, tasks, teams with relationships)
- ACID transactions for task state changes
- Support for full-text search on task content
- Managed hosting available (for small team, limited ops capacity)

## Decision
Use PostgreSQL with Prisma ORM.

## Alternatives Considered

### MongoDB
- Pros: Flexible schema, easy to start with
- Cons: Our data is inherently relational; would need to manage relationships manually
- Rejected: Relational data in a document store leads to complex joins or data duplication

### SQLite
- Pros: Zero configuration, embedded, fast for reads
- Cons: Limited concurrent write support, no managed hosting for production
- Rejected: Not suitable for multi-user web application in production

### MySQL
- Pros: Mature, widely supported
- Cons: PostgreSQL has better JSON support, full-text search, and ecosystem tooling
- Rejected: PostgreSQL is the better fit for our feature requirements

## Consequences
- Prisma provides type-safe database access and migration management
- We can use PostgreSQL's full-text search instead of adding Elasticsearch
- Team needs PostgreSQL knowledge (standard skill, low risk)
- Hosting on managed service (Supabase, Neon, or RDS)
```

### ADR Lifecycle

```
PROPOSED → ACCEPTED → (SUPERSEDED or DEPRECATED)
```

- **Don't delete old ADRs.** They hold historical context.
- When decision changes, write new ADR that references and supersedes old one.

## Inline Documentation

### When to Comment

Comment the *why*, not the *what*:

```typescript
// BAD: Restates the code
// Increment counter by 1
counter += 1;

// GOOD: Explains non-obvious intent
// Rate limit uses a sliding window — reset counter at window boundary,
// not on a fixed schedule, to prevent burst attacks at window edges
if (now - windowStart > WINDOW_SIZE_MS) {
  counter = 0;
  windowStart = now;
}
```

### When NOT to Comment

```typescript
// Don't comment self-explanatory code
function calculateTotal(items: CartItem[]): number {
  return items.reduce((sum, item) => sum + item.price * item.quantity, 0);
}

// Don't leave TODO comments for things you should just do now
// TODO: add error handling  ← Just add it

// Don't leave commented-out code
// const oldImplementation = () => { ... }  ← Delete it, git has history
```

### Document Known Gotchas

```typescript
/**
 * IMPORTANT: This function must be called before the first render.
 * If called after hydration, it causes a flash of unstyled content
 * because the theme context isn't available during SSR.
 *
 * See ADR-003 for the full design rationale.
 */
export function initializeTheme(theme: Theme): void {
  // ...
}
```

## API Documentation

For public APIs (REST, GraphQL, library interfaces): document while designing; don't wait for API to stabilize. Doc is first test of design.

### Inline with Types (Preferred for TypeScript)

```typescript
/**
 * Creates a new task.
 *
 * @param input - Task creation data (title required, description optional)
 * @returns The created task with server-generated ID and timestamps
 * @throws {ValidationError} If title is empty or exceeds 200 characters
 * @throws {AuthenticationError} If the user is not authenticated
 *
 * @example
 * const task = await createTask({ title: 'Buy groceries' });
 * console.log(task.id); // "task_abc123"
 */
export async function createTask(input: CreateTaskInput): Promise<Task> {
  // ...
}
```

### OpenAPI / Swagger for REST APIs

```yaml
paths:
  /api/tasks:
    post:
      summary: Create a task
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateTaskInput'
      responses:
        '201':
          description: Task created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Task'
        '422':
          description: Validation error
```

### Keep API docs true to the code

Examples, generated or hand-written, are claims about code. Check them:

1. Read implementation and its test fixtures, not just route name or type signature.
2. Update request/response examples and any affected prose (pagination, errors, auth).
3. Run project's existing doc or schema build (OpenAPI validation, TypeDoc/Sphinx build, doc tests). Where safe, run documented read-only call against local or test fixture.
4. Record commands that ran and checks you could not run. Never present untested example as verified.
5. Keep secrets, internal hostnames and real customer data out of examples.

*Worked example:* endpoint changes from offset to cursor pagination. Read handler and its tests. Update response example (`next_cursor`) and pagination section. Regenerate or validate OpenAPI spec with repository's command. Exercise one documented request against test server. Report: "spec validated with `<cmd>`; example request checked against fixture X."

For doc tooling setup (docs sites, generators, CI publishing), use `generating-documentation`.

## README Structure

Every project needs README covering:

```markdown
# Project Name

One-paragraph description of what this project does.

## Quick Start
1. Clone the repo
2. Install dependencies: `npm install`
3. Set up environment: `cp .env.example .env`
4. Run the dev server: `npm run dev`

## Commands
| Command | Description |
|---------|-------------|
| `npm run dev` | Start development server |
| `npm test` | Run tests |
| `npm run build` | Production build |
| `npm run lint` | Run linter |

## Architecture
Brief overview of the project structure and key design decisions.
Link to ADRs for details.

## Contributing
How to contribute, coding standards, PR process.
```

## Changelog Maintenance

For shipped features:

```markdown
# Changelog

## [1.2.0] - 2025-01-20
### Added
- Task sharing: users can share tasks with team members (#123)
- Email notifications for task assignments (#124)

### Fixed
- Duplicate tasks appearing when rapidly clicking create button (#125)

### Changed
- Task list now loads 50 items per page (was 20) for better UX (#126)
```

## Documentation for Agents

For AI agent context:

- **CLAUDE.md / rules files** — Document project conventions so agents follow them
- **Spec files** — Keep specs current so agents build right thing
- **ADRs** — Show agents why past decisions were made (prevents re-deciding)
- **Inline gotchas** — Keep agents out of known traps

## Verification

After documenting:

- [ ] ADRs exist for all significant architectural decisions
- [ ] README covers quick start, commands and architecture overview
- [ ] API functions have parameter and return type documentation
- [ ] Known gotchas documented inline where they matter
- [ ] No commented-out code remains
- [ ] Rules files (CLAUDE.md etc.) current and accurate
- [ ] Changed API examples checked against code or build, with commands run reported
