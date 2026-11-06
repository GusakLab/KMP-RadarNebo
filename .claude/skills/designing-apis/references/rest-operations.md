# REST Operational Patterns

Patterns that sit next to resource design: idempotent retries, bulk endpoints, Link-header pagination, CORS and health endpoints. Read this when an API must survive client retries, accept many items per request, or be called from browsers.

## Idempotency Keys for Non-Idempotent Methods

POST (and sometimes PATCH) is not idempotent, but clients retry on timeouts. Let them send a key:

```http
POST /v1/orders
Idempotency-Key: 6f1c2a8e-4b1d-4a43-9b7e-2f6d0c1e9a10
Content-Type: application/json

{"sku": "A-1", "qty": 2}
```

Server rules:

| Situation | Response |
|-----------|----------|
| First request with key | Process, store `(key, client id, request hash, status, body)` with a TTL (e.g. 24h) |
| Retry, same key + same request hash, finished | Replay the stored status and body |
| Retry while first is still running | `409 Conflict` (client retries later) |
| Same key, different request body | `422 Unprocessable Entity` (key misuse) |
| Key missing on an endpoint that requires it | `400 Bad Request` |

Scope keys per client/account, make the store write atomic with the side effect (same transaction or outbox), and document the TTL. An IETF draft standardizes the `Idempotency-Key` header; the behavior above follows common practice (Stripe-style).

## Bulk / Batch Endpoints

```http
POST /v1/users:batchCreate        # or POST /v1/users/batch
{"items": [{"email": "a@example.com"}, {"email": "b@example.com"}]}
```

```json
{
  "results": [
    {"index": 0, "status": 201, "id": "usr_1"},
    {"index": 1, "status": 409, "error": {"type": "https://api.example.com/errors/duplicate", "title": "Email already exists"}}
  ]
}
```

Decide and document:

- **Atomic vs partial success.** Atomic: all-or-nothing, return a normal 2xx/4xx. Partial: return `200` (or `207 Multi-Status`) with a per-item status; never return 201 when some items failed.
- **Max items per request** (e.g. 100) and max payload size; reject above it with 413 or 422.
- **Ordering**: echo `index` so clients can correlate results.
- **Long-running batches**: return `202 Accepted` + `Location: /v1/jobs/{id}` and let clients poll.

## Link-Header Pagination (RFC 8288)

Useful when the body must stay a bare array (e.g. GitHub-style APIs):

```http
GET /v1/users?page=2&per_page=50

Link: <https://api.example.com/v1/users?page=3&per_page=50>; rel="next",
      <https://api.example.com/v1/users?page=1&per_page=50>; rel="prev",
      <https://api.example.com/v1/users?page=1&per_page=50>; rel="first",
      <https://api.example.com/v1/users?page=8&per_page=50>; rel="last"
```

Clients must follow the URLs rather than build them. For body-embedded cursors see `pagination-patterns.md`.

## CORS

Only needed when browsers on another origin call the API.

- Allow-list exact origins; never reflect the `Origin` header blindly.
- `Access-Control-Allow-Credentials: true` cannot be combined with `Access-Control-Allow-Origin: *`.
- List the methods and headers you actually accept; expose custom response headers (e.g. rate-limit headers) with `Access-Control-Expose-Headers`.
- Cache preflights with `Access-Control-Max-Age`.

```python
# FastAPI / Starlette
app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://app.example.com"],
    allow_credentials=True,
    allow_methods=["GET", "POST", "PATCH", "DELETE"],
    allow_headers=["Authorization", "Content-Type", "Idempotency-Key"],
    expose_headers=["RateLimit-Limit", "RateLimit-Remaining", "Retry-After"],
)
```

## Health Endpoints

| Endpoint | Purpose | Checks | Auth |
|----------|---------|--------|------|
| `/livez` (liveness) | Is the process alive? Restart if not | No dependencies | None, internal only |
| `/readyz` (readiness) | Can it take traffic now? | DB, cache, required downstreams, with short timeouts | None, internal only |
| `/health` (public status) | Simple up/down for load balancers | Same as readiness, minimal body | Optional |

Keep dependency details (versions, hostnames) out of unauthenticated responses. Don't fail liveness because a dependency is down; that causes restart storms.

## Checklist

- [ ] Every retryable POST either is naturally idempotent or accepts `Idempotency-Key`
- [ ] Bulk endpoints document atomicity, max items and per-item error shape
- [ ] Pagination style is consistent across all collection endpoints
- [ ] CORS origins are an explicit allow-list
- [ ] Liveness and readiness are separate and unauthenticated only on internal networks
