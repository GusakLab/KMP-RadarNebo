---
title: Match Existing Code Style in File and Project
impact: CRITICAL
tags: ctx, consistency, patterns, style
---

## Match Existing Code Style in File and Project

Foreign patterns, even technically better ones, create jarring inconsistency. Before simplifying, study surrounding code for the patterns already in use.

**Incorrect (arrow functions in a file using function declarations):**

```javascript
// Existing file uses function declarations throughout
function validateEmail(email) {
  return email.includes("@");
}

function validatePhone(phone) {
  return phone.length === 10;
}

// New simplified code introduces arrow functions
const validateName = (name) => name.length > 0;

const validateAge = (age) => age >= 0 && age <= 150;
```

**Correct (matching existing function declaration pattern):**

```javascript
// Existing file uses function declarations throughout
function validateEmail(email) {
  return email.includes("@");
}

function validatePhone(phone) {
  return phone.length === 10;
}

// Simplified code follows existing pattern
function validateName(name) {
  return name.length > 0;
}

function validateAge(age) {
  return age >= 0 && age <= 150;
}
```

**Patterns to observe:**

- Function declaration style (arrow vs function keyword)
- Naming conventions (camelCase, snake_case, prefixes)
- Import organization and grouping
- Error handling approach (try/catch, Result types, error callbacks)
- Comment style and documentation format

**When NOT to use:**

- Explicitly refactoring to modernize the entire file
- Team acknowledges existing patterns as anti-patterns
