---
title: Use Consistent Vocabulary
impact: HIGH
tags: name, consistency, vocabulary, ubiquitous-language
---

## Use Consistent Vocabulary

Pick one word per concept; use it everywhere. Mixed synonyms make readers wonder if different words mean different things.

**Incorrect (synonyms for same concept):**

```typescript
class UserController {
  fetchUser(id: string): User { /* ... */ }
}

class ProductController {
  getProduct(id: string): Product { /* ... */ }
}

class OrderController {
  retrieveOrder(id: string): Order { /* ... */ }
}

// Elsewhere in the codebase
const customer = loadCustomer(id)  // Is 'customer' different from 'user'?
const buyer = getBuyer(id)  // Another synonym?

// Data layer uses yet another term
class UserRepository {
  findById(id: string): User { /* ... */ }
}
```

**Correct (one vocabulary throughout):**

```typescript
// Pick 'get' for all retrieval operations
class UserController {
  getUser(id: string): User { /* ... */ }
}

class ProductController {
  getProduct(id: string): Product { /* ... */ }
}

class OrderController {
  getOrder(id: string): Order { /* ... */ }
}

// Consistent term for the entity
const user = getUser(id)  // Always 'user', never 'customer' or 'buyer'

// Repository layer follows same convention
class UserRepository {
  getById(id: string): User { /* ... */ }
}
```

**Establish conventions for:**
- CRUD operations: `create/get/update/delete` or `add/fetch/modify/remove`
- Collections: `list/find/search/filter` - pick one set
- Status changes: `activate/deactivate` or `enable/disable`
- Entity names: `user` vs `customer` vs `account`

Reference: [Domain-Driven Design - Ubiquitous Language](https://martinfowler.com/bliki/UbiquitousLanguage.html)
