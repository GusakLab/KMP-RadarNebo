---
title: Flatten Deep Nesting to Maximum 2-3 Levels
impact: HIGH
tags: flow, nesting, refactoring, complexity, extraction
---

## Flatten Deep Nesting to Maximum 2-3 Levels

Past 2-3 levels, nesting gets exponentially harder to read. At 4+ levels, extract functions, return early, or restructure.

**Incorrect (excessive nesting):**

```typescript
function processTransactions(accounts: Account[]) {
  for (const account of accounts) {
    if (account.isActive) {
      for (const transaction of account.transactions) {
        if (transaction.status === 'pending') {
          if (transaction.amount > 0) {
            for (const rule of account.rules) {
              if (rule.applies(transaction)) {
                if (rule.action === 'flag') {
                  if (transaction.amount > rule.threshold) {
                    transaction.flagged = true;
                    transaction.flagReason = rule.reason;
                    notifyCompliance(account, transaction);
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
```

**Correct (extraction and early continues):**

```typescript
function processTransactions(accounts: Account[]) {
  const activeAccounts = accounts.filter(a => a.isActive);

  for (const account of activeAccounts) {
    processPendingTransactions(account);
  }
}

function processPendingTransactions(account: Account) {
  const pendingTransactions = account.transactions.filter(
    t => t.status === 'pending' && t.amount > 0
  );

  for (const transaction of pendingTransactions) {
    applyRules(account, transaction);
  }
}

function applyRules(account: Account, transaction: Transaction) {
  for (const rule of account.rules) {
    if (!rule.applies(transaction)) {
      continue;
    }
    if (rule.action !== 'flag') {
      continue;
    }
    if (transaction.amount <= rule.threshold) {
      continue;
    }

    transaction.flagged = true;
    transaction.flagReason = rule.reason;
    notifyCompliance(account, transaction);
  }
}
```

**Incorrect (nested validation conditionals):**

```typescript
function validateForm(form: FormData): ValidationResult {
  if (form.email) {
    if (isValidEmail(form.email)) {
      if (form.password) {
        if (form.password.length >= 8) {
          if (form.password === form.confirmPassword) {
            if (form.acceptedTerms) {
              return { valid: true };
            } else {
              return { valid: false, error: 'Must accept terms' };
            }
          } else {
            return { valid: false, error: 'Passwords do not match' };
          }
        } else {
          return { valid: false, error: 'Password too short' };
        }
      } else {
        return { valid: false, error: 'Password required' };
      }
    } else {
      return { valid: false, error: 'Invalid email' };
    }
  } else {
    return { valid: false, error: 'Email required' };
  }
}
```

**Correct (flat validation chain):**

```typescript
function validateForm(form: FormData): ValidationResult {
  if (!form.email) {
    return { valid: false, error: 'Email required' };
  }
  if (!isValidEmail(form.email)) {
    return { valid: false, error: 'Invalid email' };
  }
  if (!form.password) {
    return { valid: false, error: 'Password required' };
  }
  if (form.password.length < 8) {
    return { valid: false, error: 'Password too short' };
  }
  if (form.password !== form.confirmPassword) {
    return { valid: false, error: 'Passwords do not match' };
  }
  if (!form.acceptedTerms) {
    return { valid: false, error: 'Must accept terms' };
  }

  return { valid: true };
}
```

**Incorrect (nested object traversal):**

```typescript
function getDeepValue(config: Config) {
  if (config) {
    if (config.settings) {
      if (config.settings.display) {
        if (config.settings.display.theme) {
          if (config.settings.display.theme.colors) {
            return config.settings.display.theme.colors.primary;
          }
        }
      }
    }
  }
  return DEFAULT_COLOR;
}
```

**Correct (optional chaining or extraction):**

```typescript
function getDeepValue(config: Config) {
  return config?.settings?.display?.theme?.colors?.primary ?? DEFAULT_COLOR;
}

// Or for complex logic:
function getDeepValue(config: Config) {
  const theme = getTheme(config);
  if (!theme?.colors) {
    return DEFAULT_COLOR;
  }
  return theme.colors.primary;
}

function getTheme(config: Config): Theme | undefined {
  return config?.settings?.display?.theme;
}
```

### Flattening Strategies

1. **Early returns/continues** - Exit current scope ASAP
2. **Extract helper functions** - Each handles one level of complexity
3. **Filter before iterate** - Move conditions out of loops
4. **Use optional chaining** - Safe property access
5. **Use lookup tables** - Replace nested conditionals with data

### When NOT to Apply

- State machines with inherent nesting
- Recursive-descent parser/compiler code
- Extraction would need excessive parameter passing
