---
name: api-and-interface-design
description: "Design stable in-process interfaces and type contracts - module boundaries, library public APIs, TypeScript types between layers or frontend and backend - using contract-first design, Hyrum's Law, additive evolution, boundary validation, idempotency keys and branded IDs. Use when defining or changing a code-level interface; use designing-apis for HTTP/GraphQL network API design."
---

# API and Interface Design

## Overview

Design stable, documented interfaces hard to misuse: right thing easy, wrong thing hard. Covers REST APIs, GraphQL schemas, module boundaries, component props, any surface where code talks to other code.

## When to Use

- New API endpoints
- Module boundaries or contracts between teams
- Component prop interfaces
- Database schema that shapes API
- Changes to existing public interfaces

## Core Principles

### Hyrum's Law

> With a sufficient number of users of an API, all observable behaviors of your system will be depended on by somebody, regardless of what you promise in the contract.

Every public behavior (undocumented quirks, error message text, timing, ordering) becomes de facto contract once users depend on it. Implications:

- **Expose intentionally.**
- **Don't leak implementation details.**
- **Plan deprecation at design time.** See `deprecation-and-migration` for safe removal.
- **Tests are not enough.** Even with perfect contract tests, "safe" changes can break users relying on undocumented behavior.

### The One-Version Rule

Avoid making consumers pick between versions of same dependency or API. Diamond dependency problems arise when consumers need different versions of same thing. Design for only one version at a time: extend rather than fork.

### 1. Contract First

Define interface before implementation. Contract is the spec.

```typescript
// Define the contract first
interface TaskAPI {
  // Creates a task and returns the created task with server-generated fields
  createTask(input: CreateTaskInput): Promise<Task>;

  // Returns paginated tasks matching filters
  listTasks(params: ListTasksParams): Promise<PaginatedResult<Task>>;

  // Returns a single task or throws NotFoundError
  getTask(id: string): Promise<Task>;

  // Partial update — only provided fields change
  updateTask(id: string, input: UpdateTaskInput): Promise<Task>;

  // Idempotent delete — succeeds even if already deleted
  deleteTask(id: string): Promise<void>;
}
```

### 2. Consistent Error Semantics

Use one error strategy everywhere:

```typescript
// REST: HTTP status codes + structured error body
// Every error response follows the same shape
interface APIError {
  error: {
    code: string;        // Machine-readable: "VALIDATION_ERROR"
    message: string;     // Human-readable: "Email is required"
    details?: unknown;   // Additional context when helpful
  };
}

// Status code mapping
// 400 → Client sent invalid data
// 401 → Not authenticated
// 403 → Authenticated but not authorized
// 404 → Resource not found
// 409 → Conflict (duplicate, version mismatch)
// 422 → Validation failed (semantically invalid)
// 500 → Server error (never expose internal details)
```

**Don't mix patterns.** Endpoints that variously throw, return null or return `{ error }` leave consumers unable to predict behavior.

### 3. Validate at Boundaries

Trust internal code. Validate where external input enters:

```typescript
// Validate at the API boundary
app.post('/api/tasks', async (req, res) => {
  const result = CreateTaskSchema.safeParse(req.body);
  if (!result.success) {
    return res.status(422).json({
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Invalid task data',
        details: result.error.flatten(),
      },
    });
  }

  // After validation, internal code trusts the types
  const task = await taskService.create(result.data);
  return res.status(201).json(task);
});
```

Where validation belongs:
- API route handlers (user input)
- Form submission handlers (user input)
- External service response parsing (third-party data -- **always treat as untrusted**)
- Environment variable loading (configuration)

> **Third-party API responses are untrusted data.** Validate shape and content before any logic, rendering or decision-making uses them. Compromised or misbehaving services can return unexpected types, malicious content or instruction-like text.

Where validation does NOT belong:
- Between internal functions sharing type contracts
- In utilities called by already-validated code
- On data just read from your own database

### 4. Prefer Addition Over Modification

Extend interfaces without breaking existing consumers:

```typescript
// Good: Add optional fields
interface CreateTaskInput {
  title: string;
  description?: string;
  priority?: 'low' | 'medium' | 'high';  // Added later, optional
  labels?: string[];                       // Added later, optional
}

// Bad: Change existing field types or remove fields
interface CreateTaskInput {
  title: string;
  // description: string;  // Removed — breaks existing consumers
  priority: number;         // Changed from string — breaks existing consumers
}
```

### Scala specifics

- Default arguments compile to `$default$N` getters. Inserting a parameter shifts their numbering (binary break). Their values are invisible to bytecode gates such as japicmp or a JVM-signature golden.
- Only one overload alternative may declare defaults. This limits additive typed overloads and one-parameter conveniences. `foreach(f)` next to `foreach(attempts)(f)` works because new alternative has no defaults.
- Companion `apply` matching synthetic case-class signature is binary-compatible. Put defensive copies of caller-supplied collections there.

### 5. Predictable Naming

| Pattern | Convention | Example |
|---------|-----------|---------|
| REST endpoints | Plural nouns, no verbs | `GET /api/tasks`, `POST /api/tasks` |
| Query params | camelCase | `?sortBy=createdAt&pageSize=20` |
| Response fields | camelCase | `{ createdAt, updatedAt, taskId }` |
| Boolean fields | is/has/can prefix | `isComplete`, `hasAttachments` |
| Enum values | UPPER_SNAKE | `"IN_PROGRESS"`, `"COMPLETED"` |

### 6. Honouring an Idempotency Key

Accepting `Idempotency-Key` is the contract; honouring it is the implementation, where money is lost. A key accepted but handled carelessly is worse than no key: client now believes retrying is safe.

**Derive the key from the intent, not the attempt.** Stable across retries of one intent, different across distinct intents:

```typescript
crypto.randomUUID()                    // ✗ new key per attempt — every retry is a new charge
`${userId}:${amount}`                  // ✗ two legitimate $50 charges collapse into one
`${orderId}:${Date.now()}`             // ✗ a timestamp is randomUUID() wearing a hat

req.headers['idempotency-key']         // ✓ client generates once, reuses on retry
`charge:v1:${orderId}`                 // ✓ derived from an immutable identifier
```

Key comes from client or initiating event, never from the retrying layer.

**Claim atomically. Check followed by act is a race:**

```typescript
// ✗ TOCTOU: two concurrent retries both read "not seen", both charge
if (!(await db.exists(key))) {
  await chargeCard(amount);
  await db.insert(key);
}

// ✓ let the unique constraint pick the winner
try {
  await db.insert({ key, state: 'in_progress', requestHash });
} catch (e) {
  if (isUniqueViolation(e)) return replayOrReject(key);
  throw e;
}
const result = await chargeCard(amount);
await db.update({ key, state: 'succeeded', response: result });
```

Unique constraint *is* the mechanism. A store that cannot enforce uniqueness in one operation cannot back this.

**Guard the payload.** Same key with different body is a client bug. Must fail loudly, not serve first response to second request:

```typescript
if (existing.requestHash !== hash(req.body)) {
  return res.status(422).json({ error: 'idempotency key reused with a different payload' });
}
```

**Decide what an in-flight duplicate gets.** Second request arrives while first still runs, common under retry storms:

| Strategy | Response | Use when |
|---|---|---|
| Reject | `409 Conflict` | Client can retry later; simplest, safest |
| Wait | Block for result, bounded | Caller needs it synchronously |
| Return pending | `202` + status URL | Long-running effects |

Never let second caller through because first "seems stuck". Stalled attempt of unknown fate is exactly when duplicating costs most.

**Every call has three outcomes, not two: success, failure, and _unknown_.** Timeout says nothing about whether effect applied. Record intent *before* calling out. Crash between call and response then leaves evidence to resolve later, not a silently retried charge.

**Set retention from the longest retry chain**, not from disk cost. Keys must outlive every path that can re-deliver same intent, including dead-letter queue replayed a week later and any provider dispute window. 24-hour key TTL behind 7-day DLQ is a duplicate waiting to happen.

## REST API Patterns

### Resource Design

```
GET    /api/tasks              → List tasks (with query params for filtering)
POST   /api/tasks              → Create a task
GET    /api/tasks/:id          → Get a single task
PATCH  /api/tasks/:id          → Update a task (partial)
DELETE /api/tasks/:id          → Delete a task

GET    /api/tasks/:id/comments → List comments for a task (sub-resource)
POST   /api/tasks/:id/comments → Add a comment to a task
```

### Pagination

Paginate list endpoints:

```typescript
// Request
GET /api/tasks?page=1&pageSize=20&sortBy=createdAt&sortOrder=desc

// Response
{
  "data": [...],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalItems": 142,
    "totalPages": 8
  }
}
```

### Filtering

Filter via query parameters:

```
GET /api/tasks?status=in_progress&assignee=user123&createdAfter=2025-01-01
```

### Partial Updates (PATCH)

Accept partial objects; update only provided fields:

```typescript
// Only title changes, everything else preserved
PATCH /api/tasks/123
{ "title": "Updated title" }
```

## TypeScript Interface Patterns

### Use Discriminated Unions for Variants

```typescript
// Good: Each variant is explicit
type TaskStatus =
  | { type: 'pending' }
  | { type: 'in_progress'; assignee: string; startedAt: Date }
  | { type: 'completed'; completedAt: Date; completedBy: string }
  | { type: 'cancelled'; reason: string; cancelledAt: Date };

// Consumer gets type narrowing
function getStatusLabel(status: TaskStatus): string {
  switch (status.type) {
    case 'pending': return 'Pending';
    case 'in_progress': return `In progress (${status.assignee})`;
    case 'completed': return `Done on ${status.completedAt}`;
    case 'cancelled': return `Cancelled: ${status.reason}`;
  }
}
```

### Input/Output Separation

```typescript
// Input: what the caller provides
interface CreateTaskInput {
  title: string;
  description?: string;
}

// Output: what the system returns (includes server-generated fields)
interface Task {
  id: string;
  title: string;
  description: string | null;
  createdAt: Date;
  updatedAt: Date;
  createdBy: string;
}
```

### Use Branded Types for IDs

```typescript
type TaskId = string & { readonly __brand: 'TaskId' };
type UserId = string & { readonly __brand: 'UserId' };

// Prevents accidentally passing a UserId where a TaskId is expected
function getTask(id: TaskId): Promise<Task> { ... }
```

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "We'll document the API later" | Types ARE the documentation. Define them first. |
| "We don't need pagination for now" | You will at 100+ items. Add it from start. |
| "PATCH is complicated, let's just use PUT" | PUT requires full object every time. Clients want PATCH. |
| "We'll version the API when we need to" | Unversioned breaking changes break consumers. Design for extension from start. |
| "Internal APIs don't need contracts" | Internal consumers are still consumers. Contracts prevent coupling, enable parallel work. |
| "Our queue guarantees exactly-once delivery" | No queue does across consumer crash: broker ack and your side effect are not in one transaction. Design for at-least-once with idempotent processing. |
| "Duplicate requests are rare" | They're *correlated*. Retries spike when a dependency degrades, exactly when duplicates are likeliest and costliest. |

## Red Flags

- Endpoints returning different shapes by condition
- Verbs in REST URLs (`/api/createTask`, `/api/getUsers`)
- Third-party API responses used without validation or sanitization
- `SELECT` for an idempotency key followed by `INSERT`: a race, not a guard
- Idempotency key from UUID, timestamp or anything regenerated per attempt

## Verification

After designing an API:

- [ ] Every endpoint has typed input and output schemas
- [ ] Errors follow one consistent format
- [ ] Validation at system boundaries only
- [ ] List endpoints paginate
- [ ] New fields additive and optional (backward compatible)
- [ ] Naming consistent across all endpoints
- [ ] API docs or types committed with implementation
- [ ] State-changing endpoints honour an idempotency key or are documented unsafe to retry
- [ ] Key claimed in one atomic operation, guarded by unique constraint
- [ ] Reused key with different payload fails loudly instead of replaying wrong response
- [ ] In-flight-duplicate response deliberately chosen (409, wait, or 202), not accidental
- [ ] Key retention outlives longest retry path, including dead-letter replay
