# Hardening Patterns

Threat modeling, headers/CORS rules and supply-chain gate are stated in `../SKILL.md` (sections "Process: Threat Model First", "Headers, CORS, and responses", "Dependencies and supply chain"). Pre-deployment checklist is in `predeploy-and-supabase.md`.

## OWASP Top 10 Prevention Patterns

Prevention patterns, not a ranking. OWASP Top 10 (2021) order, for reference: A01 Broken Access Control, A02 Cryptographic Failures, A03 Injection, A04 Insecure Design, A05 Security Misconfiguration, A06 Vulnerable and Outdated Components, A07 Identification and Authentication Failures, A08 Software and Data Integrity Failures, A09 Security Logging and Monitoring Failures, A10 Server-Side Request Forgery.

### Injection

```typescript
// BAD: SQL injection via string concatenation
const query = `SELECT * FROM users WHERE id = '${userId}'`;

// GOOD: Parameterized query
const user = await db.query('SELECT * FROM users WHERE id = $1', [userId]);

// GOOD: ORM with parameterized input
const user = await prisma.user.findUnique({ where: { id: userId } });
```

### Broken Authentication

```typescript
// Password hashing
import { hash, compare } from 'bcrypt';

const SALT_ROUNDS = 12;
const hashedPassword = await hash(plaintext, SALT_ROUNDS);
const isValid = await compare(plaintext, hashedPassword);

// Session management
app.use(session({
  secret: process.env.SESSION_SECRET,  // From environment, not code
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,     // Not accessible via JavaScript
    secure: true,       // HTTPS only
    sameSite: 'lax',    // CSRF protection
    maxAge: 24 * 60 * 60 * 1000,  // 24 hours
  },
}));
```

### Cross-Site Scripting (XSS)

```typescript
// BAD: Rendering user input as HTML
element.innerHTML = userInput;

// GOOD: Use framework auto-escaping (React does this by default)
return <div>{userInput}</div>;

// If you MUST render HTML, sanitize first
import DOMPurify from 'dompurify';
const clean = DOMPurify.sanitize(userInput);
```

### Broken Access Control

```typescript
// Always check authorization, not just authentication
app.patch('/api/tasks/:id', authenticate, async (req, res) => {
  const task = await taskService.findById(req.params.id);

  // Check that the authenticated user owns this resource
  if (task.ownerId !== req.user.id) {
    return res.status(403).json({
      error: { code: 'FORBIDDEN', message: 'Not authorized to modify this task' }
    });
  }

  // Proceed with update
  const updated = await taskService.update(req.params.id, req.body);
  return res.json(updated);
});
```

### Security Misconfiguration

```typescript
// Security headers (use helmet for Express)
import helmet from 'helmet';
app.use(helmet());

// Content Security Policy
app.use(helmet.contentSecurityPolicy({
  directives: {
    defaultSrc: ["'self'"],
    scriptSrc: ["'self'"],
    styleSrc: ["'self'", "'unsafe-inline'"],  // Tighten if possible
    imgSrc: ["'self'", 'data:', 'https:'],
    connectSrc: ["'self'"],
  },
}));

// CORS — restrict to known origins
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || 'http://localhost:3000',
  credentials: true,
}));
```

### Sensitive Data Exposure

```typescript
// Never return sensitive fields in API responses
function sanitizeUser(user: UserRecord): PublicUser {
  const { passwordHash, resetToken, ...publicFields } = user;
  return publicFields;
}

// Use environment variables for secrets
const API_KEY = process.env.STRIPE_API_KEY;
if (!API_KEY) throw new Error('STRIPE_API_KEY not configured');
```

### Server-Side Request Forgery (SSRF)

Whenever server fetches a user-influenced URL (webhooks, "import from URL", image proxies, link previews), an attacker can aim it at internal services (cloud metadata, `localhost`, private IPs).

```typescript
// BAD: fetch whatever the user gives you
await fetch(req.body.webhookUrl);

// GOOD: allowlist scheme + host, reject if ANY resolved IP is private, forbid redirects
import { lookup } from 'node:dns/promises';
import ipaddr from 'ipaddr.js';

const ALLOWED_HOSTS = new Set(['hooks.example.com']);

async function assertSafeUrl(raw: string): Promise<URL> {
  const url = new URL(raw);
  if (url.protocol !== 'https:') throw new Error('https only');
  if (!ALLOWED_HOSTS.has(url.hostname)) throw new Error('host not allowed');
  // Resolve ALL records; a single private/reserved address fails the check.
  const addrs = await lookup(url.hostname, { all: true });
  if (addrs.some((a) => ipaddr.parse(a.address).range() !== 'unicast')) {
    throw new Error('private/reserved IP');
  }
  return url;
}

await fetch(await assertSafeUrl(req.body.webhookUrl), { redirect: 'error' });
```

`range() !== 'unicast'` check covers loopback, link-local `169.254.169.254` (cloud metadata, #1 SSRF target), private and unique-local ranges across IPv4 and IPv6.

**Caveat: still has a TOCTOU gap.** `fetch` resolves DNS again after the check. Attacker with a short-TTL record can rebind to an internal IP between validation and connection. For high-risk surfaces, resolve once and connect to pinned IP, or put a filtering agent in front (`request-filtering-agent` / `ssrf-req-filter`).

## Input Validation Patterns

### Schema Validation at Boundaries

```typescript
import { z } from 'zod';

const CreateTaskSchema = z.object({
  title: z.string().min(1).max(200).trim(),
  description: z.string().max(2000).optional(),
  priority: z.enum(['low', 'medium', 'high']).default('medium'),
  dueDate: z.string().datetime().optional(),
});

// Validate at the route handler
app.post('/api/tasks', async (req, res) => {
  const result = CreateTaskSchema.safeParse(req.body);
  if (!result.success) {
    return res.status(422).json({
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Invalid input',
        details: result.error.flatten(),
      },
    });
  }
  // result.data is now typed and validated
  const task = await taskService.create(result.data);
  return res.status(201).json(task);
});
```

### File Upload Safety

```typescript
// Restrict file types and sizes
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_SIZE = 5 * 1024 * 1024; // 5MB

function validateUpload(file: UploadedFile) {
  if (!ALLOWED_TYPES.includes(file.mimetype)) {
    throw new ValidationError('File type not allowed');
  }
  if (file.size > MAX_SIZE) {
    throw new ValidationError('File too large (max 5MB)');
  }
  // Don't trust the file extension — check magic bytes if critical
}
```

### Destructive Operations on Derived Paths

Delete, move or overwrite is only as safe as the value naming its target. Reading that value from the kernel, a job payload or a sibling service proves where it *arrived from*, not who *wrote* it. Another process's command line is as attacker-controlled as a form field. Shape check ("absolute path, at least one directory deep") proves well-formedness and gets mistaken for authorization. That is how a cleanup routine deletes the root instead of the leaf.

Before a destructive call, require all three: resolved target sits under an **allowlisted root** (compare after resolving symlinks, never on the raw string); it is at least one level **below** that root, so a root is never itself the target; it carries **evidence that it is yours**, read *before* the operation and before any teardown removing it. Otherwise "absent" and "not mine" are indistinguishable. On refusal, log rejected target and stop. Cleanup falling back to a broader default path is the failure this guards against. Worked example:

```python
import shutil
from pathlib import Path

ALLOWED_ROOTS = [Path("/var/lib/myapp/workspaces").resolve()]

def safe_remove_tree(raw: str, expected_owner_id: str) -> None:
    target = Path(raw).resolve(strict=True)          # resolve symlinks first
    root = next((r for r in ALLOWED_ROOTS if target.is_relative_to(r)), None)
    if root is None or target == root:                # outside allowlist, or the root itself
        raise PermissionError(f"refusing to delete {target}")
    marker = target / ".owner"                        # read evidence BEFORE any teardown
    if not marker.is_file() or marker.read_text().strip() != expected_owner_id:
        raise PermissionError(f"{target} is not ours")
    # expected_owner_id must come from authenticated state (DB row, signed job), not the request
    shutil.rmtree(target)                             # on refusal above: log and stop, no fallback path
```

Two limits, because the check reads stronger than it is. Marker inside the tree is self-attestation: anything that can write there can write the marker. So expected owner must come from authenticated state. Marker needs integrity protection (restrictive ownership, or a MAC) before it counts as authorization. Resolving a path then operating on the *name* is a check/use race wherever an untrusted process can swap an ancestor. On a shared volume, hold target by descriptor and use no-follow, beneath-the-root operations, or ensure the hierarchy cannot change for the duration.

## Dependency Audit Triage

Package-manager audits report known advisories. They do not prove a package is trustworthy or that vulnerable code is reachable. Use this decision tree:

```
The native package-manager audit reports a vulnerability
├── Severity: critical or high
│   ├── Is the vulnerable code reachable in runtime, build, test, or deployment paths?
│   │   ├── YES --> Fix immediately (update, patch, or replace the dependency)
│   │   └── NO (confirmed unused across those paths) --> Fix soon, but not a blocker
│   └── Is a fix available?
│       ├── YES --> Update to the patched version
│       └── NO --> Check for workarounds, consider replacing the dependency, or add to allowlist with a review date
├── Severity: moderate
│   ├── Reachable in production? --> Fix in the next release cycle
│   └── Dev-only? --> Fix when convenient, track in backlog
└── Severity: low
    └── Track and fix during regular dependency updates
```

**Key questions:**
- Is vulnerable function called in your code path?
- Is dependency runtime or dev-only?
- Is vulnerability exploitable in your deployment context (e.g., a server-side vulnerability in a client-only app)?

## Rate Limiting

```typescript
import rateLimit from 'express-rate-limit';

// General API rate limit
app.use('/api/', rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100,                   // 100 requests per window
  standardHeaders: true,
  legacyHeaders: false,
}));

// Stricter limit for auth endpoints
app.use('/api/auth/', rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,  // 10 attempts per 15 minutes
}));
```

**Count in a shared store once there is more than one process.** `express-rate-limit` keeps counters in process memory by default. Behind a load balancer each instance holds its own count, so effective limit is `max × instances`. On serverless or edge runtimes a fresh invocation starts from zero, so auth limit above may never fire. Pass a shared `store` (Redis via `rate-limit-redis`), or use an HTTP-based limiter that works where a long-lived TCP connection does not (for example `@upstash/ratelimit`):

```typescript
import { Ratelimit } from '@upstash/ratelimit';
import { Redis } from '@upstash/redis';

const authLimiter = new Ratelimit({
  redis: Redis.fromEnv(),                       // UPSTASH_REDIS_REST_URL + _TOKEN
  limiter: Ratelimit.slidingWindow(10, '15 m'), // 10 attempts per 15 minutes, across all instances
});
const { success } = await authLimiter.limit(`login:${req.ip}`);
if (!success) return res.status(429).end();
```

## Secrets Management

```
.env files:
  ├── .env.example  → Committed (template with placeholder values)
  ├── .env          → NOT committed (contains real secrets)
  └── .env.local    → NOT committed (local overrides)

.gitignore must include:
  .env
  .env.local
  .env.*.local
  *.pem
  *.key
```

**Always check before committing:**
```bash
# Check for accidentally staged secrets; lists file names only, so matched values never reach terminal or logs
git diff --cached --name-only -i -G 'password|secret|api_key|token'
```

**If a secret is ever committed, rotate it.** Deleting the line or rewriting history is not enough. Assume it is compromised the moment it reaches a remote. Revoke and reissue the key first, then purge it from history.

## Data Classification

Classify personal data when adding fields, so export and deletion requests can find it later:

| Class | Examples | Handling |
|---|---|---|
| **Non-personal** | Aggregates, anonymized counts | Normal handling |
| **Personal (PII)** | Name, email, IP, device/user IDs | Minimize, access-control, include in export/delete |
| **Sensitive** | Health, finance, location, biometrics, gov IDs, anything about minors | Extra basis to collect, stricter access, often encryption + audit logging |

## LLM Output Handling

```typescript
// BAD: trusting model output as a command or as markup
const sql = await llm.generate(`Write SQL for: ${userQuestion}`);
await db.query(sql);                                   // arbitrary query execution
container.innerHTML = await llm.reply(userMessage);   // stored XSS, via the model

// GOOD: model output is data — parse defensively, then validate, then encode
let intent;
try {
  intent = CommandSchema.parse(JSON.parse(await llm.replyJson(userMessage)));
} catch {
  throw new ValidationError('unexpected model output'); // JSON.parse or schema failed
}
await runAllowlistedAction(intent.action, intent.params);
container.textContent = await llm.reply(userMessage);
```
