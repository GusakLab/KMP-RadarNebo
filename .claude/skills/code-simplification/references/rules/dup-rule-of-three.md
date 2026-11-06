---
title: Apply the Rule of Three
impact: MEDIUM
tags: dup, abstraction, dry, timing
---

## Apply the Rule of Three

Wait for third occurrence before extracting helper. First duplication may be coincidence. Second confirms pattern. Third proves extraction worthwhile. Early extraction builds abstractions for patterns that never materialize.

**Incorrect (extracting after first duplication):**

```typescript
// Two similar validation calls - someone extracts immediately
function validateUser(user: User) {
  if (!user.email.includes('@')) throw new Error('Invalid email');
}

function validateContact(contact: Contact) {
  if (!contact.email.includes('@')) throw new Error('Invalid email');
}

// Over-engineered "solution" after seeing just 2 occurrences
function validateEmail(entity: { email: string }, entityName: string) {
  if (!entity.email.includes('@')) {
    throw new Error(`Invalid ${entityName} email`);
  }
}
// Now every caller needs to understand this abstraction
// And if requirements diverge, the abstraction becomes awkward
```

**Correct (wait for third occurrence):**

```typescript
// First occurrence
function validateUser(user: User) {
  if (!user.email.includes('@')) throw new Error('Invalid email');
}

// Second occurrence - note it, don't extract yet
function validateContact(contact: Contact) {
  if (!contact.email.includes('@')) throw new Error('Invalid email');
}

// Third occurrence - NOW extract with confidence
function validateOrder(order: Order) {
  if (!order.customerEmail.includes('@')) throw new Error('Invalid email');
}

// After third occurrence, extract with full understanding of the pattern
function isValidEmail(email: string): boolean {
  return email.includes('@');
}
```

**Incorrect (under-extraction - obvious pattern ignored):**

```python
# Same exact logic copy-pasted 5 times
def process_user_csv(filepath):
    with open(filepath, 'r') as f:
        reader = csv.DictReader(f)
        return [row for row in reader if row.get('active') == 'true']

def process_order_csv(filepath):
    with open(filepath, 'r') as f:
        reader = csv.DictReader(f)
        return [row for row in reader if row.get('active') == 'true']

def process_product_csv(filepath):
    with open(filepath, 'r') as f:
        reader = csv.DictReader(f)
        return [row for row in reader if row.get('active') == 'true']
# Bug fix needed? Change it in 5 places
```

**Correct (extract once pattern established):**

```python
def load_active_records(filepath: str) -> list[dict]:
    """Load records from CSV where active='true'."""
    with open(filepath, 'r') as f:
        reader = csv.DictReader(f)
        return [row for row in reader if row.get('active') == 'true']

# Clear, single source of truth
users = load_active_records('users.csv')
orders = load_active_records('orders.csv')
products = load_active_records('products.csv')
```

### When NOT to Apply

- Security-critical code (authentication, authorization) - extract immediately
- Bug-prone complex algorithms - extract on second occurrence
- Business logic says pattern will recur (known requirements)
