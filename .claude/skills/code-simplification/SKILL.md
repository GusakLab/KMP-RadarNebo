---
name: code-simplification
description: "Simplify working code without changing behavior. Defaults to recently changed code; flattens control flow, removes dead code and needless abstraction, clarifies names, and matches project conventions. Use when code works but is harder to read or maintain than it should be; for smell-driven structural refactoring use refactoring-guru."
---

# Code Simplification

> Inspired by [Claude Code Simplifier plugin](https://github.com/anthropics/claude-plugins-official/blob/main/plugins/code-simplifier/agents/code-simplifier.md). Adapted as model-agnostic, process-driven skill for any AI coding agent.

Each simplification must pass one test: "Would a new team member understand this faster than the original?"

## When to Use

- Feature works, tests pass, but implementation feels heavy
- Code review flags readability or complexity issues
- Deep nesting, long functions or unclear names
- Code written under time pressure
- Related logic scattered across files needs consolidating
- Merged changes introduced duplication or inconsistency

**When NOT to use:**

- Code already clean and readable. Don't simplify for its own sake
- You don't yet understand code. Comprehend first
- Performance-critical code where "simpler" version is measurably slower
- Module about to be rewritten. Simplifying throwaway code wastes effort

## The Five Principles

### 1. Preserve Behavior Exactly

Change only how code expresses behavior, never what it does. Inputs, outputs, side effects, error behavior and edge cases must stay identical. Unsure a simplification preserves behavior? Don't make it.

```
ASK BEFORE EVERY CHANGE:
→ Does this produce the same output for every input?
→ Does this maintain the same error behavior?
→ Does this preserve the same side effects and ordering?
→ Do all existing tests still pass without modification?
```

### 2. Follow Project Conventions

Simplify toward codebase consistency, not external preferences. Before simplifying:

```
1. Read CLAUDE.md / project conventions
2. Study how neighboring code handles similar patterns
3. Match the project's style for:
   - Import ordering and module system
   - Function declaration style
   - Naming conventions
   - Error handling patterns
   - Type annotation depth
```

### 3. Prefer Clarity Over Cleverness

Explicit beats compact when compact needs a mental pause to parse.

```typescript
// UNCLEAR: Dense ternary chain
const label = isNew ? 'New' : isUpdated ? 'Updated' : isArchived ? 'Archived' : 'Active';

// CLEAR: Readable mapping
function getStatusLabel(item: Item): string {
  if (item.isNew) return 'New';
  if (item.isUpdated) return 'Updated';
  if (item.isArchived) return 'Archived';
  return 'Active';
}
```

```typescript
// UNCLEAR: Chained reduces with inline logic
const result = items.reduce((acc, item) => ({
  ...acc,
  [item.id]: { ...acc[item.id], count: (acc[item.id]?.count ?? 0) + 1 }
}), {});

// CLEAR: Named intermediate step
const countById = new Map<string, number>();
for (const item of items) {
  countById.set(item.id, (countById.get(item.id) ?? 0) + 1);
}
```

### 4. Maintain Balance

Failure mode: over-simplification. Watch for these traps:

- **Inlining too aggressively** — removing helper that named a concept hurts call-site readability
- **Combining unrelated logic** — two simple functions merged into one complex one is not simpler
- **Removing "unnecessary" abstraction** — some abstractions serve extensibility or testability, not complexity
- **Optimizing for line count** — goal is easier comprehension, not fewer lines

### 5. Scope to What Changed

Default to recently modified code. No drive-by refactors of unrelated code unless explicitly asked to broaden scope. Unscoped simplification adds diff noise and risks unintended regressions.

## The Simplification Process

### Step 1: Understand Before Touching (Chesterton's Fence)

Before changing or removing anything, learn why it exists. Chesterton's Fence: don't tear down a fence across a road until you know why it's there. Then decide if the reason still applies.

```
BEFORE SIMPLIFYING, ANSWER:
- What is this code's responsibility?
- What calls it? What does it call?
- What are the edge cases and error paths?
- Are there tests that define the expected behavior?
- Why might it have been written this way? (Performance? Platform constraint? Historical reason?)
- Check git blame: what was the original context for this code?
```

Can't answer these? Not ready to simplify. Read more context first.

### Step 2: Identify Simplification Opportunities

Scan for these patterns. Each is a concrete signal, not a vague smell:

**Structural complexity:**

| Pattern | Signal | Simplification |
|---------|--------|----------------|
| Deep nesting (3+ levels) | Hard-to-follow control flow | Extract conditions into guard clauses or helpers |
| Long functions (50+ lines) | Multiple responsibilities | Split into focused, well-named functions |
| Nested ternaries | Needs mental stack to parse | Replace with if/else chains, switch or lookup objects |
| Boolean parameter flags | `doThing(true, false, true)` | Use options objects or separate functions |
| Repeated conditionals | Same `if` check in multiple places | Extract to well-named predicate function |

**Naming and readability:**

| Pattern | Signal | Simplification |
|---------|--------|----------------|
| Generic names | `data`, `result`, `temp`, `val`, `item` | Rename to describe content: `userProfile`, `validationErrors` |
| Abbreviated names | `usr`, `cfg`, `btn`, `evt` | Full words unless abbreviation is universal (`id`, `url`, `api`) |
| Misleading names | `get` function that also mutates state | Rename to match actual behavior |
| Comments explaining "what" | `// increment counter` above `count++` | Delete. Code is clear enough |
| Comments explaining "why" | `// Retry because the API is flaky under load` | Keep. They carry intent code can't express |

**Redundancy:**

| Pattern | Signal | Simplification |
|---------|--------|----------------|
| Duplicated logic | Same 5+ lines in multiple places | Extract to shared function |
| Dead code | Unreachable branches, unused variables, commented-out blocks | Remove, only after confirming it's truly dead |
| Unnecessary abstractions | Wrapper adding no value | Inline it, call underlying function directly |
| Over-engineered patterns | Factory-for-a-factory, strategy-with-one-strategy | Replace with simple direct approach |
| Redundant type assertions | Cast to already-inferred type | Remove assertion |

### Step 3: Apply Changes Incrementally

Make one simplification at a time. Run tests after each. **Submit refactoring separately from feature or bug fix changes.** PR that refactors and adds a feature is two PRs. Split it.

```
FOR EACH SIMPLIFICATION:
1. Make the change
2. Run the test suite
3. If tests pass → commit (or continue to next simplification)
4. If tests fail → revert and reconsider
```

**The Rule of 500:** If a refactoring would touch more than 500 lines, automate (codemods, sed scripts, AST transforms) instead of editing by hand. Manual edits at that scale are error-prone and exhausting to review.

### Step 4: Verify the Result

After all simplifications, step back and judge the whole:

```
COMPARE BEFORE AND AFTER:
- Is the simplified version genuinely easier to understand?
- Did you introduce any new patterns inconsistent with the codebase?
- Is the diff clean and reviewable?
- Would a teammate approve this change?
```

Revert if "simplified" version is harder to understand or review.

## Rule Reference

[references/rules-index.md](references/rules-index.md) indexes 47 focused rules, each with incorrect/correct examples and a "When NOT to use" section. Coverage: context discovery, behavior preservation, scope, control flow, naming, duplication, dead code, language idioms (TypeScript, Python, Go, Rust). In Step 2, when you need exact treatment for a pattern, open index, then read only matching rule file.

## Language-Specific Guidance

### TypeScript / JavaScript

```typescript
// SIMPLIFY: Unnecessary async wrapper (only when findById always returns a
// promise and never throws synchronously, and there is no try/catch/finally
// around the return; otherwise `return await` changes error behavior and
// async stack traces, so keep it)
// Before
async function getUser(id: string): Promise<User> {
  return await userService.findById(id);
}
// After
function getUser(id: string): Promise<User> {
  return userService.findById(id);
}

// SIMPLIFY: Verbose conditional assignment
// Before
let displayName: string;
if (user.nickname) {
  displayName = user.nickname;
} else {
  displayName = user.fullName;
}
// After
const displayName = user.nickname || user.fullName;

// SIMPLIFY: Manual array building
// Before
const activeUsers: User[] = [];
for (const user of users) {
  if (user.isActive) {
    activeUsers.push(user);
  }
}
// After
const activeUsers = users.filter((user) => user.isActive);

// SIMPLIFY: Redundant boolean return
// Before
function isValid(input: string): boolean {
  if (input.length > 0 && input.length < 100) {
    return true;
  }
  return false;
}
// After
function isValid(input: string): boolean {
  return input.length > 0 && input.length < 100;
}
```

### Python

```python
# SIMPLIFY: Verbose dictionary building
# Before
result = {}
for item in items:
    result[item.id] = item.name
# After
result = {item.id: item.name for item in items}

# SIMPLIFY: Nested conditionals with early return
# Before
def process(data):
    if data is not None:
        if data.is_valid():
            if data.has_permission():
                return do_work(data)
            else:
                raise PermissionError("No permission")
        else:
            raise ValueError("Invalid data")
    else:
        raise TypeError("Data is None")
# After
def process(data):
    if data is None:
        raise TypeError("Data is None")
    if not data.is_valid():
        raise ValueError("Invalid data")
    if not data.has_permission():
        raise PermissionError("No permission")
    return do_work(data)
```

### React / JSX

```tsx
// SIMPLIFY: Verbose conditional rendering
// Before
function UserBadge({ user }: Props) {
  if (user.isAdmin) {
    return <Badge variant="admin">Admin</Badge>;
  } else {
    return <Badge variant="default">User</Badge>;
  }
}
// After
function UserBadge({ user }: Props) {
  const variant = user.isAdmin ? 'admin' : 'default';
  const label = user.isAdmin ? 'Admin' : 'User';
  return <Badge variant={variant}>{label}</Badge>;
}

// SIMPLIFY: Prop drilling through intermediate components
// Before — consider whether context or composition solves this better.
// This is a judgment call — flag it, don't auto-refactor.
```

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "It's working, no need to touch it" | Hard-to-read code is hard to fix when it breaks. Simplifying now speeds every future change. |
| "The types make it self-documenting" | Types document structure, not intent. Well-named function explains *why* better than type signature explains *what*. |
| "This abstraction might be useful later" | Don't keep speculative abstractions. Unused now means complexity without value. Remove it, re-add when needed. |
| "The original author must have had a reason" | Maybe. Check git blame, apply Chesterton's Fence. Accumulated complexity often has no reason, only residue of iteration under pressure. |

## Verification

After simplification pass:

- [ ] All existing tests pass unmodified
- [ ] Build succeeds, no new warnings
- [ ] Linter/formatter passes (no style regressions)
- [ ] Each simplification is reviewable, incremental change
- [ ] Diff clean, no unrelated changes mixed in
- [ ] Simplified code follows project conventions (checked against CLAUDE.md or equivalent)
- [ ] No error handling removed or weakened
- [ ] No dead code left behind (unused imports, unreachable branches)
- [ ] Teammate or review agent would approve change as net improvement
