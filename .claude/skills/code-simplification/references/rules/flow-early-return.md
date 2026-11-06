---
title: Use Early Returns to Reduce Nesting
impact: HIGH
tags: flow, nesting, readability, cognitive-load
---

## Use Early Returns to Reduce Nesting

Early returns drop wrapper conditions: handle each edge case immediately and exit. Code reads linearly at one indentation level.

**Incorrect (deeply nested logic):**

```typescript
function processOrder(order: Order): Result {
  if (order) {
    if (order.items.length > 0) {
      if (order.customer) {
        if (order.customer.isActive) {
          if (order.paymentMethod) {
            const total = calculateTotal(order.items);
            if (total > 0) {
              const discount = getDiscount(order.customer);
              const finalTotal = total - discount;
              return chargeCustomer(order.customer, finalTotal);
            } else {
              return { error: 'Invalid total' };
            }
          } else {
            return { error: 'No payment method' };
          }
        } else {
          return { error: 'Customer inactive' };
        }
      } else {
        return { error: 'No customer' };
      }
    } else {
      return { error: 'No items' };
    }
  } else {
    return { error: 'No order' };
  }
}
```

**Correct (early returns, flat structure):**

```typescript
function processOrder(order: Order): Result {
  if (!order) {
    return { error: 'No order' };
  }
  if (order.items.length === 0) {
    return { error: 'No items' };
  }
  if (!order.customer) {
    return { error: 'No customer' };
  }
  if (!order.customer.isActive) {
    return { error: 'Customer inactive' };
  }
  if (!order.paymentMethod) {
    return { error: 'No payment method' };
  }

  const total = calculateTotal(order.items);
  if (total <= 0) {
    return { error: 'Invalid total' };
  }

  const discount = getDiscount(order.customer);
  const finalTotal = total - discount;
  return chargeCustomer(order.customer, finalTotal);
}
```

### Related

- See also [`flow-guard-clauses`](flow-guard-clauses.md) for validating preconditions at function entry

### When NOT to Apply

- Nesting mirrors meaningful domain hierarchy
- Functional code where early return breaks composition
- Exhaustive pattern matching that requires all branches
