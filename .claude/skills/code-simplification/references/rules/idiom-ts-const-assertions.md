---
title: Use const Assertions and readonly for Immutability
impact: LOW-MEDIUM
tags: idiom, typescript, immutability, const-assertions
---

## Use const Assertions and readonly for Immutability

Const assertions (`as const`) and `readonly` signal intent and block accidental mutation. TypeScript then infers literal types, not widened ones.

**Incorrect (mutable by default, widened types):**

```typescript
// Type is string[], not readonly ["admin", "user", "guest"]
const ROLES = ["admin", "user", "guest"];

// Type is { status: string; code: number }, loses literal info
const ERROR_CODES = {
  NOT_FOUND: { status: "not_found", code: 404 },
  FORBIDDEN: { status: "forbidden", code: 403 },
};

// Can be accidentally mutated
function processItems(items: string[]) {
  items.push("extra"); // Mutates original array
  return items.sort();
}
```

**Correct (immutable, precise types):**

```typescript
// Type is readonly ["admin", "user", "guest"]
const ROLES = ["admin", "user", "guest"] as const;
type Role = typeof ROLES[number]; // "admin" | "user" | "guest"

// Type preserves literal values
const ERROR_CODES = {
  NOT_FOUND: { status: "not_found", code: 404 },
  FORBIDDEN: { status: "forbidden", code: 403 },
} as const;
type ErrorStatus = typeof ERROR_CODES[keyof typeof ERROR_CODES]["status"];

// Signals no mutation, creates new array
function processItems(items: readonly string[]): string[] {
  return [...items, "extra"].sort();
}
```

### When NOT to Use

- Arrays or objects genuinely needing mutation for performance
- Loop counters and accumulators
- Builder patterns relying on mutation

### Benefits

- Literal type inference enables exhaustive switch checks
- Catches accidental `.push()`, `.pop()`, assignment at compile time
- Derives union types from const arrays/objects
