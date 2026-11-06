---
title: Preserve All Return Values and Outputs
impact: CRITICAL
tags: behave, outputs, return-values, testing
---

## Preserve All Return Values and Outputs

Simplification must never alter what a function returns. Even "equivalent" values (empty array vs null, reordered object keys) can break downstream consumers. Every output (return values, yielded items, emitted events) must stay byte-for-byte identical.

**Incorrect (changes return type from null to undefined):**

```typescript
// Before: returns null for missing users
function findUser(id: string): User | null {
  const user = users.find(u => u.id === id);
  if (!user) {
    return null;
  }
  return user;
}

// After "simplification": now returns undefined
function findUser(id: string): User | undefined {
  return users.find(u => u.id === id);
}
// Breaks: if (findUser(id) === null) { ... }
```

**Correct (preserves null return type):**

```typescript
function findUser(id: string): User | null {
  return users.find(u => u.id === id) ?? null;
}
```

**Incorrect (changes output ordering):**

```python
# Before: returns sorted by insertion order
def get_config():
    return {"host": "localhost", "port": 8080, "debug": True}

# After "simplification": alphabetical order
def get_config():
    return dict(sorted({"debug": True, "host": "localhost", "port": 8080}.items()))
# Breaks: code that relies on iteration order or serialization
```
