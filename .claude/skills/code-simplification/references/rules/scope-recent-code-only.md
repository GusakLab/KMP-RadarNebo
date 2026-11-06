---
title: Focus on Recently Modified Code Only
impact: HIGH
tags: scope, focus, recent-changes, risk-reduction, git-history
---

## Focus on Recently Modified Code Only

Target recently modified code unless explicitly asked otherwise. Old, stable code survived production testing and carries hidden assumptions. Simplifying it adds risk without matching benefit. Reviewers lack fresh context to validate it.

**Incorrect (simplifying unrelated legacy code):**

```typescript
// Task: Simplify the new getUserProfile function
// PR includes changes to:

// 1. The new function (correct target)
async function getUserProfile(id: string): Promise<Profile> {
  const user = await db.users.findById(id);
  return user ? mapToProfile(user) : null;
}

// 2. Legacy auth module last touched 2 years ago (wrong!)
// "While I was here, I noticed this could be cleaner..."
function validateSession(token: string): boolean {
  // Simplified from 50 lines to 20
  return jwt.verify(token, SECRET) && !isExpired(token);
}

// 3. Old utility last touched 18 months ago (wrong!)
// "This helper was verbose, made it more functional..."
const formatDate = (d: Date) => d.toISOString().split('T')[0];
```

**Correct (focused on recent code only):**

```typescript
// Task: Simplify the new getUserProfile function
// PR only touches getUserProfile (added last week)

// Before
async function getUserProfile(id: string): Promise<Profile> {
  const result = await db.users.findById(id);
  if (result !== null && result !== undefined) {
    const profile = mapToProfile(result);
    return profile;
  } else {
    return null;
  }
}

// After - only this function changes
async function getUserProfile(id: string): Promise<Profile> {
  const user = await db.users.findById(id);
  return user ? mapToProfile(user) : null;
}

// Legacy validateSession: untouched (stable for 2 years)
// Old formatDate utility: untouched (stable for 18 months)
```

### When NOT to Apply

- Explicitly asked to audit or simplify legacy code
- Bug fix requires touching old code
- Old code blocks current feature and must change anyway
- During a planned, scoped refactoring sprint

### How to Identify Recent Code

Use git history to find active areas:

```bash
# Find files with most recent changes
git log --since="30 days ago" --name-only --pretty=format: | sort | uniq -c | sort -rn | head -20

# Check who owns which sections
git blame path/to/file.ts

# Find high-churn files (complexity signals)
git log --since="90 days ago" --name-only --pretty=format: | sort | uniq -c | sort -rn
```

Prioritize files in open PRs or recent issues, or with many recent commits.
