---
title: Use Intention-Revealing Names
impact: HIGH
tags: name, intention, readability, self-documenting
---

## Use Intention-Revealing Names

Names reveal why something exists, what it does, how it's used. Reader should grasp purpose without reading implementation.

**Incorrect (cryptic or generic names):**

```typescript
function proc(lst: number[]): number {
  let t = 0
  for (const x of lst) {
    if (x > 0) {
      t += x
    }
  }
  return t
}

const d = getD()
const flag = checkFlag(d)
if (flag) {
  proc(d.n)
}
```

**Correct (intention-revealing names):**

```typescript
function sumPositiveValues(values: number[]): number {
  let total = 0
  for (const value of values) {
    if (value > 0) {
      total += value
    }
  }
  return total
}

const transaction = getTransaction()
const isApproved = isTransactionApproved(transaction)
if (isApproved) {
  sumPositiveValues(transaction.amounts)
}
```

**Naming guidelines:**
- Variables: describe what they hold (`customerEmail`, not `str` or `data`)
- Functions: describe what they do (`calculateShippingCost`, not `calc`)
- Booleans: `is`, `has`, `can`, `should` prefixes (`isValid`, `hasPermission`)
- Collections: plural nouns (`users`, `orderItems`)

Reference: [Clean Code - Meaningful Names](https://www.oreilly.com/library/view/clean-code-a/9780136083238/)
