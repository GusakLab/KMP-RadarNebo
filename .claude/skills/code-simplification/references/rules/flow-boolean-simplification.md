---
title: Simplify Boolean Expressions
impact: HIGH
tags: flow, boolean, de-morgans, double-negation, conditions
---

## Simplify Boolean Expressions

Double negations, redundant comparisons and tangled logic slow readers and hide bugs. Apply De Morgan's laws, drop double negations, compare directly to booleans.

**Incorrect (double negations, redundant comparisons):**

```typescript
function shouldShowBanner(user: User, settings: Settings): boolean {
  if (!(!user.isSubscribed)) {
    return false;
  }
  if (user.dismissed !== true && !settings.bannersDisabled) {
    return true;
  }
  return false;
}

function canAccess(role: string, isActive: boolean): boolean {
  if (!(role !== 'admin' && role !== 'moderator') === false) {
    return false;
  }
  return isActive !== false;
}
```

**Correct (simplified booleans):**

```typescript
function shouldShowBanner(user: User, settings: Settings): boolean {
  if (user.isSubscribed) {
    return false;
  }
  return !user.dismissed && !settings.bannersDisabled;
}

function canAccess(role: string, isActive: boolean): boolean {
  if (role !== 'admin' && role !== 'moderator') {
    return false;
  }
  return isActive;
}
```

**Incorrect (Python - verbose boolean logic):**

```python
def is_valid_order(order):
    if not (order.total <= 0) and not (len(order.items) == 0):
        if order.customer is not None and order.customer.verified == True:
            return True
    return False
```

**Correct (Python - direct boolean expressions):**

```python
def is_valid_order(order):
    has_items = len(order.items) > 0
    has_positive_total = order.total > 0
    has_verified_customer = order.customer is not None and order.customer.verified

    return has_items and has_positive_total and has_verified_customer
```

**Incorrect (Go - nested negations in error checks):**

```go
func validateConfig(config *Config) error {
    if !(config.Host == "" || config.Port == 0) {
        if !(config.Timeout < 0) {
            return nil
        }
    }
    return errors.New("invalid configuration")
}
```

**Correct (Go - positive conditions):**

```go
func validateConfig(config *Config) error {
    if config.Host == "" || config.Port == 0 {
        return errors.New("invalid configuration")
    }
    if config.Timeout < 0 {
        return errors.New("invalid configuration")
    }
    return nil
}
```

### Key Simplification Rules

| Pattern | Simplifies To |
|---------|---------------|
| `!!x` | `x` |
| `!(a && b)` | `!a \|\| !b` (De Morgan's) |
| `!(a \|\| b)` | `!a && !b` (De Morgan's) |
| `x === true` | `x` |
| `x === false` | `!x` |
| `x !== true` | `!x` |
| `!(x !== y)` | `x === y` |

### When NOT to Apply

- Nullable booleans (`x === true` explicitly excludes `null`/`undefined`)
- Intermediate names add domain context (extracting `isEligible` from complex expression is good)
- Simplified form reads worse than expanded form
