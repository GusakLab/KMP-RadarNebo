---
title: Prefer String Interpolation Over Concatenation
impact: MEDIUM-HIGH
tags: name, string-interpolation, readability, template-literals, f-strings
---

## Prefer String Interpolation Over Concatenation

Concatenation scatters output format across operators and quotes, hiding final string shape. Template literals (JavaScript/TypeScript), f-strings (Python), and format strings (Go, Rust) keep structure intact, values inline.

**Incorrect (string concatenation):**

```typescript
function formatUserGreeting(user: User): string {
  return 'Hello, ' + user.firstName + ' ' + user.lastName +
    '! You have ' + user.unreadCount + ' unread messages.';
}

function buildApiUrl(baseUrl: string, version: number, resource: string): string {
  return baseUrl + '/api/v' + version + '/' + resource;
}

function logOrderEvent(orderId: string, status: string, timestamp: Date): void {
  console.log('[' + timestamp.toISOString() + '] Order ' + orderId +
    ' changed to ' + status);
}
```

**Correct (template literals):**

```typescript
function formatUserGreeting(user: User): string {
  return `Hello, ${user.firstName} ${user.lastName}! You have ${user.unreadCount} unread messages.`;
}

function buildApiUrl(baseUrl: string, version: number, resource: string): string {
  return `${baseUrl}/api/v${version}/${resource}`;
}

function logOrderEvent(orderId: string, status: string, timestamp: Date): void {
  console.log(`[${timestamp.toISOString()}] Order ${orderId} changed to ${status}`);
}
```

**Incorrect (Python - concatenation and % formatting):**

```python
def format_error_message(code, endpoint, method):
    # Concatenation: hard to see the shape
    return "Error " + str(code) + " on " + method + " " + endpoint

def format_progress(task, done, total):
    # % formatting: positional args drift from placeholders
    return "%s: %d/%d done (%s)" % (task, done, total, ", ".join(task.tags))
```

**Correct (Python - f-strings):**

```python
def format_error_message(code, endpoint, method):
    return f"Error {code} on {method} {endpoint}"

def format_progress(task, done, total):
    return f"{task}: {done}/{total} done ({', '.join(task.tags)})"
```

**Incorrect (Go - fmt.Sprintf with positional verbs):**

```go
func formatLogEntry(level string, component string, message string) string {
    return level + " [" + component + "] " + message
}
```

**Correct (Go - fmt.Sprintf):**

```go
func formatLogEntry(level string, component string, message string) string {
    return fmt.Sprintf("%s [%s] %s", level, component, message)
}
```

### When NOT to Apply

- Building SQL queries (use parameterized queries, never interpolation)
- Pieces come from a loop or dynamic collection (use `join`)
- Performance-critical paths with many concatenations (use `strings.Builder` in Go, `StringBuilder` in Java)
- Simple two-part concatenation with equal readability
