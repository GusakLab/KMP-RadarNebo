---
title: Verify Behavior Preservation Before Finalizing Changes
impact: CRITICAL
tags: behave, verification, testing, review, diff
---

## Verify Behavior Preservation Before Finalizing Changes

Never assume a simplification preserves behavior - verify it. Run existing tests, manually test edge cases, and review diffs for semantic changes. The confidence that code "looks equivalent" is insufficient. Verification must be explicit and documented.

**Incorrect (commits without verification):**

```typescript
// Developer thinks: "This is obviously the same"
// Before
function getLimit(limit: number | undefined): number {
  if (limit) {
    return limit;
  }
  return 50;
}

// After "simplification" - commits directly
function getLimit(limit: number | undefined): number {
  return limit ?? 50;
}
// Forgot to run tests, missed that 0 is handled differently:
// `if (limit)` treats 0 as "unset", `??` keeps 0
```

**Correct (verifies before committing):**

```typescript
// 1. Run existing tests first
// $ npm test -- --grep "getLimit"

// 2. Check edge cases manually
console.log(getLimit(undefined)); // Before: 50, After: 50 ✓
console.log(getLimit(10));        // Before: 10, After: 10 ✓
console.log(getLimit(0));         // Before: 50, After: 0  ✗ behavior change
console.log(getLimit(NaN));       // Before: 50, After: NaN ✗ behavior change

// 3. Review diff for semantic changes
// git diff --word-diff

// 4. Keep the simplification only if the change is intended; otherwise keep
//    the falsy check (`return limit || 50;`) and commit with a verification note
function getLimit(limit: number | undefined): number {
  return limit || 50;
}
```

### Verification Checklist

Before finalizing any simplification:

1. **Run existing tests** - All tests must pass without modification
2. **Check edge cases** - null, undefined, empty, zero, negative, boundary values
3. **Review error paths** - Exceptions, error messages, failure modes
4. **Verify side effects** - Logging, I/O, state changes still occur
5. **Test with real data** - If available, run against production-like inputs
6. **Review the diff** - Look for type changes, reordering, removed code

### Verification Commands by Language

```bash
# TypeScript/JavaScript
npm test
npx tsc --noEmit

# Python
pytest
mypy .

# Go
go test ./...
go vet ./...

# Rust
cargo test
cargo clippy
```

### When Tests Are Insufficient

If existing tests do not cover the modified code:

1. **Add tests before simplifying** - Write tests that capture current behavior
2. **Use snapshot testing** - Capture outputs before and after
3. **Property-based testing** - Generate inputs to find edge cases
4. **Manual verification** - Document what was manually checked

### Benefits

- Catches behavior changes before they reach production
- Builds confidence in simplifications
- Documents verification for reviewers
- Creates a safety net for future changes
