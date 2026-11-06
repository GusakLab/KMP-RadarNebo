---
title: Fix Feature Envy by Moving Methods
impact: CRITICAL
tags: couple, feature-envy, move-method, cohesion
---

## Fix Feature Envy by Moving Methods

When a method uses more features of another class than its own, move it to that class.

**Incorrect (method envies another class):**

```typescript
class Order {
  customer: Customer
  items: OrderItem[]
}

class Customer {
  name: string
  loyaltyPoints: number
  memberSince: Date
  tier: 'bronze' | 'silver' | 'gold'
}

class OrderPrinter {
  formatOrderSummary(order: Order): string {
    const customer = order.customer
    // Uses 4 Customer fields but no OrderPrinter fields
    const tierLabel = customer.tier.toUpperCase()
    const years = new Date().getFullYear() - customer.memberSince.getFullYear()
    const pointsDisplay = `${customer.loyaltyPoints.toLocaleString()} pts`

    return `${tierLabel} Member: ${customer.name} (${years}yr, ${pointsDisplay})`
  }
}
```

**Correct (method moved to class it uses):**

```typescript
class Customer {
  name: string
  loyaltyPoints: number
  memberSince: Date
  tier: 'bronze' | 'silver' | 'gold'

  formatSummary(): string {
    const tierLabel = this.tier.toUpperCase()
    const years = this.getMemberYears()
    const pointsDisplay = `${this.loyaltyPoints.toLocaleString()} pts`

    return `${tierLabel} Member: ${this.name} (${years}yr, ${pointsDisplay})`
  }

  getMemberYears(): number {
    return new Date().getFullYear() - this.memberSince.getFullYear()
  }
}

class OrderPrinter {
  formatOrderSummary(order: Order): string {
    return order.customer.formatSummary()  // Delegates to where data lives
  }
}
```

**Signs of feature envy:**
- Method takes another object and accesses multiple fields
- Method chains into another object repeatedly
- New field on envied class forces change in envying class

Reference: [Move Function](https://refactoring.com/catalog/moveFunction.html)
