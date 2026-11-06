---
title: Inline Trivial Variables
impact: LOW
tags: micro, inline, variable, simplification
---

## Inline Trivial Variables

Inline variable used only once when its expression is equally clear.

**Incorrect (needless intermediate variables):**

```typescript
function isValidOrder(order: Order): boolean {
  const hasItems = order.items.length > 0
  const hasCustomer = order.customerId !== null
  const isNotCancelled = order.status !== 'cancelled'

  return hasItems && hasCustomer && isNotCancelled
}

function formatPrice(amount: number): string {
  const roundedAmount = Math.round(amount * 100) / 100
  const formattedAmount = roundedAmount.toFixed(2)
  const priceWithSymbol = '$' + formattedAmount

  return priceWithSymbol
}

function getDiscount(customer: Customer): number {
  const isPremium = customer.tier === 'premium'
  const discount = isPremium ? 0.2 : 0

  return discount
}
```

**Correct (trivial expressions inlined):**

```typescript
function isValidOrder(order: Order): boolean {
  return order.items.length > 0 &&
         order.customerId !== null &&
         order.status !== 'cancelled'
}

function formatPrice(amount: number): string {
  return '$' + (Math.round(amount * 100) / 100).toFixed(2)
}

function getDiscount(customer: Customer): number {
  return customer.tier === 'premium' ? 0.2 : 0
}
```

**When NOT to inline:**
- Name adds significant clarity to complex expression
- Variable used multiple times
- Expression has side effects (should run only once)
- Inspecting intermediate values helps debugging

**Keep variable when name documents intent:**

```typescript
// Keep - the name adds meaning
function shouldShowWarning(user: User, action: Action): boolean {
  const hasDestructiveIntent = action.type === 'delete' || action.type === 'purge'
  const lacksConfirmation = !action.confirmed
  const isHighValueTarget = user.accountValue > 10000

  return hasDestructiveIntent && lacksConfirmation && isHighValueTarget
}
```

Reference: [Inline Variable](https://refactoring.com/catalog/inlineVariable.html)
