---
name: incremental-implementation
description: Deliver a feature or change in thin, verifiable slices (implement, test, verify, then expand), optionally behind a feature flag. Use when a change is too large to write and verify in one step - roughly more than ~100 lines or several components - or when executing the next task from a plan; small, well-scoped edits do not need it.
---

# Incremental Implementation

## When to Use

- Change spans several components or exceeds ~100 lines before first test
- New feature from task breakdown
- Refactoring existing code

**When NOT to use:** Small, well-scoped edits writable and verifiable in one step, even across a few files (rename, config tweak, one-function fix plus its test).

## The Increment Cycle

```
┌──────────────────────────────────────┐
│                                      │
│   Implement ──→ Test ──→ Verify ──┐  │
│       ▲                           │  │
│       └───── Commit ◄─────────────┘  │
│              │                       │
│              ▼                       │
│          Next slice                  │
│                                      │
└──────────────────────────────────────┘
```

For each slice:

1. **Implement** smallest complete functional piece
2. **Test** — run suite (or write test if none exists)
3. **Verify** — confirm slice works (tests pass, build succeeds, manual check)
4. **Commit** -- if user or repository policy allows commits, save progress with descriptive message (see `git-workflow-and-versioning` for atomic commits); otherwise leave clean checkpoint, report it, and do not commit or push without asking
5. **Move to the next slice** — carry forward, don't restart

## Slicing Strategies

### Vertical Slices (Preferred)

Build one complete path through stack:

```
Slice 1: Create a task (DB + API + basic UI)
    → Tests pass, user can create a task via the UI

Slice 2: List tasks (query + API + UI)
    → Tests pass, user can see their tasks

Slice 3: Edit a task (update + API + UI)
    → Tests pass, user can modify tasks

Slice 4: Delete a task (delete + API + UI + confirmation)
    → Tests pass, full CRUD complete
```

### Contract-First Slicing

Use when backend and frontend develop in parallel:

```
Slice 0: Define the API contract (types, interfaces, OpenAPI spec)
Slice 1a: Implement backend against the contract + API tests
Slice 1b: Implement frontend against mock data matching the contract
Slice 2: Integrate and test end-to-end
```

### Risk-First Slicing

Tackle riskiest or most uncertain piece first:

```
Slice 1: Prove the WebSocket connection works (highest risk)
Slice 2: Build real-time task updates on the proven connection
Slice 3: Add offline support and reconnection
```

## Implementation Rules

### Rule 0: Simplicity First

Before coding, ask: "What is the simplest thing that could work?"

After coding, check:
- Fewer lines possible?
- Abstractions earning their complexity?
- Would a staff engineer say "why didn't you just..."?
- Building for hypothetical future requirements, or current task?

```
SIMPLICITY CHECK:
✗ Generic EventBus with middleware pipeline for one notification
✓ Simple function call

✗ Abstract factory pattern for two similar components
✓ Two straightforward components with shared utilities

✗ Config-driven form builder for three forms
✓ Three form components
```

Three similar lines beat premature abstraction. Write naive, obviously-correct version first. Optimize only after tests prove correctness.

### Rule 0.5: Scope Discipline

Touch only what task requires.

Do NOT:
- "Clean up" code adjacent to your change
- Refactor imports in files you aren't modifying
- Remove comments you don't fully understand
- Add features not in spec because they "seem useful"
- Modernize syntax in files you only read

Spot improvement outside task scope? Note it, don't fix it:

```
NOTICED BUT NOT TOUCHING:
- src/utils/format.ts has an unused import (unrelated to this task)
- The auth middleware could use better error messages (separate task)
→ Want me to create tasks for these?
```

### Rule 1: One Thing at a Time

Each increment changes one logical thing. Don't mix concerns:

**Bad:** One commit adding new component, refactoring existing one, updating build config.

**Good:** Three separate commits, one per change.

### Rule 2: Keep It Compilable

After each increment, project must build and existing tests must pass. Don't leave codebase broken between slices.

### Rule 3: Feature Flags for Incomplete Features

If feature isn't ready for users but increments need merging:

```typescript
// Feature flag for work-in-progress
const ENABLE_TASK_SHARING = process.env.FEATURE_TASK_SHARING === 'true';

if (ENABLE_TASK_SHARING) {
  // New sharing UI
}
```

### Rule 4: Safe Defaults

Default new code to safe, conservative behavior:

```typescript
// Safe: disabled by default, opt-in
export function createTask(data: TaskInput, options?: { notify?: boolean }) {
  const shouldNotify = options?.notify ?? false;
  // ...
}
```

### Rule 5: Rollback-Friendly

Make each increment independently revertable:

- Additive changes (new files, new functions) revert easily
- Keep edits to existing code minimal and focused
- Pair database migrations with rollback migrations
- Don't delete and replace something in same commit; separate them

## Working with Agents

When directing agent to work incrementally:

```
"Let's implement Task 3 from the plan.

Start with just the database schema change and the API endpoint.
Don't touch the UI yet — we'll do that in the next increment.

After implementing, run the repository's test and build commands to
verify nothing is broken."
```

State what is and is NOT in scope for each increment.

## Increment Checklist

After each increment, verify with repository's own commands (see test-driven-development skill, Discover the Stack First section):

- [ ] Change does one thing, completely
- [ ] All existing tests still pass (repository's test command: `npm test`, `./gradlew test`, `pytest`, ...)
- [ ] Build succeeds (repository's build command)
- [ ] Type checking passes, where stack has one (`npx tsc --noEmit`, `mypy`, ...)
- [ ] Linting passes (repository's lint command)
- [ ] New functionality works as expected
- [ ] Change committed with descriptive message (when committing is authorized), or ready as reviewable checkpoint

**Note:** Run each verification command after any change that could affect it. After success, don't rerun same command unless code changed. Rerun on unchanged code adds no information.

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "I'll test it all at the end" | Bugs compound: Slice 1 bug makes Slices 2-5 wrong. Test each slice. |
| "It's faster to do it all at once" | *Feels* faster until something breaks and you can't tell which of 500 changed lines did it. |
| "These changes are too small to commit separately" | Small commits are free. Large ones hide bugs and make rollbacks painful. |
| "I'll add the feature flag later" | Incomplete feature shouldn't be user-visible. Add flag now. |
| "This refactor is small enough to include" | Refactors mixed with features make both harder to review and debug. Separate them. |

## Red Flags

- More than 100 lines written without running tests
- "Let me just quickly add this too" scope expansion
- Skipping test/verify to move faster
- Large uncommitted changes piling up
- Abstractions before third use case demands them
- New utility files for one-time operations

## Verification

After all increments for a task:

- [ ] Each increment tested individually (and committed, when committing is authorized)
- [ ] Full test suite passes
- [ ] Build clean
- [ ] Feature works end-to-end as specified
- [ ] No uncommitted changes remain, or remaining changes reported to user to commit

## See Also

Before declaring task done, apply project-wide Definition of Done as final gate: standing bar every increment clears, regardless of task. See task's acceptance criteria and repository's required checks.
