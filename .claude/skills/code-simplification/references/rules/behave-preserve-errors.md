---
title: Preserve Error Messages, Types, and Handling
impact: CRITICAL
tags: behave, errors, exceptions, error-handling
---

## Preserve Error Messages, Types, and Handling

Error behavior is part of the API contract. Changing exception types, messages or when errors throw breaks catch blocks, monitoring systems and user expectations. Simplify error handling only when observable error behavior stays identical.

**Incorrect (changes exception type):**

```typescript
// Before: throws specific error type
function parseConfig(json: string): Config {
  try {
    return JSON.parse(json);
  } catch (e) {
    throw new ConfigParseError(`Invalid config: ${e.message}`);
  }
}

// After "simplification": throws generic error
function parseConfig(json: string): Config {
  return JSON.parse(json); // Now throws SyntaxError instead
}
// Breaks: catch (e) { if (e instanceof ConfigParseError) ... }
```

**Correct (preserves error type and message format):**

```typescript
function parseConfig(json: string): Config {
  try {
    return JSON.parse(json);
  } catch (e) {
    throw new ConfigParseError(`Invalid config: ${(e as Error).message}`);
  }
}
```

**Incorrect (changes error message):**

```python
# Before: specific error message
def validate_age(age: int) -> None:
    if age < 0:
        raise ValueError("Age cannot be negative")
    if age > 150:
        raise ValueError("Age cannot exceed 150")

# After "simplification": combined validation
def validate_age(age: int) -> None:
    if not 0 <= age <= 150:
        raise ValueError("Invalid age")  # Different message!
# Breaks: tests checking for specific error messages
```

### When NOT to Apply

- Task is explicitly to improve error messages
- Messages contain sensitive information that should be removed
- Consolidating truly duplicate error paths (same message, same type)
