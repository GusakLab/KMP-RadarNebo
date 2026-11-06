---
title: Prefer Positive Conditions Over Double Negatives
impact: HIGH
tags: flow, conditions, boolean, readability, naming
---

## Prefer Positive Conditions Over Double Negatives

Double negatives force mental inversion, slowing comprehension and raising error rates. State what IS true, not what ISN'T NOT true.

**Incorrect (double negatives):**

```typescript
if (!user.isNotVerified) {
  sendWelcomeEmail(user);
}

if (!isNotEmpty(list)) {
  return 'No items';
}

if (!config.disableCache !== false) {
  useCache();
}

const cannotProceed = !isValid || !hasPermission;
if (!cannotProceed) {
  proceed();
}
```

**Correct (positive conditions):**

```typescript
if (user.isVerified) {
  sendWelcomeEmail(user);
}

if (isEmpty(list)) {
  return 'No items';
}

if (config.enableCache) {
  useCache();
}

const canProceed = isValid && hasPermission;
if (canProceed) {
  proceed();
}
```

**Incorrect (negative function names):**

```typescript
function isNotAdmin(user: User): boolean {
  return user.role !== 'admin';
}

function hasNoErrors(result: Result): boolean {
  return result.errors.length === 0;
}

function isDisabled(feature: Feature): boolean {
  return !feature.enabled;
}

// Usage becomes confusing
if (!isNotAdmin(user) && !isDisabled(feature)) {
  showAdminPanel();
}
```

**Correct (positive function names):**

```typescript
function isAdmin(user: User): boolean {
  return user.role === 'admin';
}

function isValid(result: Result): boolean {
  return result.errors.length === 0;
}

function isEnabled(feature: Feature): boolean {
  return feature.enabled;
}

// Usage is clear
if (isAdmin(user) && isEnabled(feature)) {
  showAdminPanel();
}
```

**Incorrect (confusing boolean logic):**

```typescript
// What does this even mean?
if (!(user.inactive || !user.emailConfirmed) && !config.skipValidation) {
  allowAccess();
}
```

**Correct (clear positive logic):**

```typescript
const isActiveUser = !user.inactive;
const hasConfirmedEmail = user.emailConfirmed;
const shouldValidate = !config.skipValidation;

if (isActiveUser && hasConfirmedEmail && shouldValidate) {
  allowAccess();
}

// Or extract to a well-named function
function canAccessSystem(user: User, config: Config): boolean {
  const isActiveUser = !user.inactive;
  const hasConfirmedEmail = user.emailConfirmed;
  const requiresValidation = !config.skipValidation;

  return isActiveUser && hasConfirmedEmail && requiresValidation;
}
```

### Refactoring Patterns

| Negative Form | Positive Form |
|--------------|---------------|
| `!isNotValid` | `isValid` |
| `!isEmpty` | `hasItems` |
| `!isDisabled` | `isEnabled` |
| `notFound === false` | `found === true` or `found` |
| `!user.inactive` | `user.isActive` (may need data change) |
| `errors.length === 0` | `isValid` or `hasNoErrors` |

### When NOT to Apply

- Negative is natural domain concept (e.g., `isDeprecated`, `wasDeleted`)
- External API uses negative naming
- Guard clauses where `if (!x) return` is idiomatic
- `!condition` clearer than new named positive
