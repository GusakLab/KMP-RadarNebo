---
title: Delete Code, Never Comment It Out
impact: MEDIUM
tags: dead, comments, version-control, git, cleanup
---

## Delete Code, Never Comment It Out

Commented-out code is visual noise raising unanswerable questions. Version control exists to recover old code. Use it.

**Incorrect (commented code preserved):**

```javascript
export async function fetchUserData(userId) {
  // const cachedUser = cache.get(userId);
  // if (cachedUser) {
  //   return cachedUser;
  // }

  const response = await api.get(`/users/${userId}`);

  // Old implementation - keeping just in case
  // const response = await fetch(`${API_URL}/users/${userId}`, {
  //   headers: { 'Authorization': `Bearer ${token}` },
  //   cache: 'no-store'
  // });
  // if (!response.ok) {
  //   throw new Error(`HTTP ${response.status}`);
  // }
  // const data = await response.json();

  // TODO: add caching back when Redis is configured
  // cache.set(userId, response.data, TTL);

  return response.data;
}
```

**Correct (clean code, history in git):**

```javascript
export async function fetchUserData(userId) {
  const response = await api.get(`/users/${userId}`);
  return response.data;
}
```

**Incorrect (multiple commented alternatives):**

```python
class OrderProcessor:
    def calculate_tax(self, order):
        # V1: Simple percentage
        # return order.subtotal * 0.08

        # V2: State-based tax
        # tax_rate = TAX_RATES.get(order.state, 0.0)
        # return order.subtotal * tax_rate

        # V3: External tax service (current)
        return self.tax_service.calculate(order)

        # V4: Planned - tax by item category
        # total_tax = 0
        # for item in order.items:
        #     rate = CATEGORY_RATES.get(item.category, DEFAULT_RATE)
        #     total_tax += item.price * rate
        # return total_tax
```

**Correct (single implementation, alternatives in git history or issues):**

```python
class OrderProcessor:
    def calculate_tax(self, order):
        return self.tax_service.calculate(order)
```

**Incorrect (debug code commented):**

```go
func ProcessBatch(items []Item) error {
    // fmt.Printf("DEBUG: Processing %d items\n", len(items))

    for i, item := range items {
        // fmt.Printf("DEBUG: Item %d: %+v\n", i, item)
        if err := process(item); err != nil {
            // fmt.Printf("DEBUG: Error at %d: %v\n", i, err)
            return fmt.Errorf("item %d: %w", i, err)
        }
    }

    // fmt.Println("DEBUG: Batch complete")
    return nil
}
```

**Correct (use proper logging or delete debug code):**

```go
func ProcessBatch(items []Item) error {
    for i, item := range items {
        if err := process(item); err != nil {
            return fmt.Errorf("item %d: %w", i, err)
        }
    }
    return nil
}
```

### Recovery Workflow

To get old code back:
1. `git log --oneline -20` - find the commit
2. `git show <commit>:path/to/file` - view old version
3. `git checkout <commit> -- path/to/file` - restore if needed

### When NOT to Apply

- Active debugging session (delete before committing)
- Code review showing before/after for context (use diff view instead)
- Documentation explicitly showing what NOT to do
