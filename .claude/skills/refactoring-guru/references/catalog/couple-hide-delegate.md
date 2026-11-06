---
title: Hide Delegate to Reduce Coupling
impact: CRITICAL
tags: couple, hide-delegate, encapsulation, law-of-demeter
---

## Hide Delegate to Reduce Coupling

When client code navigates object chains (a.b.c.method()), changes to intermediate objects break many callers. Create direct method to hide navigation.

**Incorrect (exposing delegate chain):**

```typescript
class Person {
  department: Department
}

class Department {
  manager: Person
}

// Client code couples to the entire chain
function getManagerName(person: Person): string {
  return person.department.manager.name  // Knows about Department internals
}

function notifyManager(person: Person, message: string): void {
  const managerEmail = person.department.manager.email  // Chain repeated
  sendEmail(managerEmail, message)
}

// If Department structure changes, ALL callers break
```

**Correct (delegating method hides chain):**

```typescript
class Person {
  private department: Department

  getManager(): Person {
    return this.department.manager
  }

  getManagerName(): string {
    return this.department.manager.name
  }

  getManagerEmail(): string {
    return this.department.manager.email
  }
}

// Client code only depends on Person interface
function getManagerName(person: Person): string {
  return person.getManagerName()
}

function notifyManager(person: Person, message: string): void {
  sendEmail(person.getManagerEmail(), message)
}

// Department changes only affect Person class
```

**When NOT to hide:**
- Delegate relationship is part of public API
- Hiding would create excessively large interface
- Chain is stable and unlikely to change

Reference: [Hide Delegate](https://refactoring.com/catalog/hideDelegate.html)
