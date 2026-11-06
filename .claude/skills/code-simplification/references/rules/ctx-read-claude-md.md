---
title: Always Read CLAUDE.md Before Simplifying
impact: CRITICAL
tags: ctx, conventions, project-setup
---

## Always Read CLAUDE.md Before Simplifying

CLAUDE.md holds project-specific instructions for writing, testing and structuring code. Read it first so simplifications match team practices.

**Incorrect (simplifying without reading project instructions):**

```typescript
// Developer simplifies by removing "unnecessary" error handling
// Not knowing CLAUDE.md requires explicit error boundaries
function fetchUser(id: string) {
  return api.get(`/users/${id}`);
}
```

**Correct (read CLAUDE.md first, respect error handling requirement):**

```typescript
// CLAUDE.md specifies: "All API calls must have explicit error handling"
function fetchUser(id: string) {
  try {
    return api.get(`/users/${id}`);
  } catch (error) {
    throw new UserFetchError(id, error);
  }
}
```

**When NOT to use:**

- CLAUDE.md does not exist in the project
- Exploratory analysis where no changes will be made

**References:**

- Check `.claude/CLAUDE.md`, `CLAUDE.md`, and parent directory CLAUDE.md files
- Look for `AGENTS.md` or similar instruction files
