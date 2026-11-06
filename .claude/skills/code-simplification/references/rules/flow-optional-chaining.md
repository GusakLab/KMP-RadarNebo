---
title: Use Optional Chaining and Nullish Coalescing
impact: HIGH
tags: flow, optional-chaining, nullish-coalescing, null-checks
---

## Use Optional Chaining and Nullish Coalescing

Deep null checks build `if` pyramids that hide real logic. Optional chaining (`?.`) and nullish coalescing (`??`) collapse them into readable expressions with clear intent: "get this value, or use a fallback."

**Incorrect (nested null checks):**

```typescript
function getUserCity(user: User | null): string {
  if (user) {
    if (user.address) {
      if (user.address.city) {
        return user.address.city;
      }
    }
  }
  return 'Unknown';
}

function getDisplayName(profile: Profile | null): string {
  let displayName = 'Anonymous';
  if (profile !== null && profile !== undefined) {
    if (profile.nickname !== null && profile.nickname !== undefined) {
      displayName = profile.nickname;
    } else if (profile.firstName !== null && profile.firstName !== undefined) {
      displayName = profile.firstName;
    }
  }
  return displayName;
}
```

**Correct (optional chaining and nullish coalescing):**

```typescript
function getUserCity(user: User | null): string {
  return user?.address?.city ?? 'Unknown';
}

function getDisplayName(profile: Profile | null): string {
  return profile?.nickname ?? profile?.firstName ?? 'Anonymous';
}
```

**Incorrect (Python - verbose None checks):**

```python
def get_config_value(config, section, key, default=None):
    if config is not None:
        section_data = config.get(section)
        if section_data is not None:
            value = section_data.get(key)
            if value is not None:
                return value
    return default
```

**Correct (Python - dictionary chaining):**

```python
def get_config_value(config, section, key, default=None):
    if config is None:
        return default
    return config.get(section, {}).get(key, default)
```

### When NOT to Apply

- Must distinguish `null`, `undefined`, and `false`/`0`/`''` (use explicit checks)
- Each null check has its own error handling or logging
- `||` falsy-value semantics intentionally differ from `??`
