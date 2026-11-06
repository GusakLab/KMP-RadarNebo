---
name: designing-apis
description: Design or review an HTTP, GraphQL or event API contract - protocol choice (REST/GraphQL/tRPC/gRPC/async), resources, status codes, Problem Details errors, versioning, pagination, idempotency, rate limits, OAuth scopes, OpenAPI/AsyncAPI specs and reference docs. Use before or while defining a network API; use implementing-api-patterns for server framework code and api-and-interface-design for in-process module contracts.
---

# Designing APIs

Design well-structured, scalable APIs using REST, GraphQL, or event-driven patterns. Focus on resource design, versioning, error handling, pagination, rate limiting, and security.

## When to Use This Skill

Use when:
- Designing a new REST, GraphQL, or event-driven API
- Establishing API design standards for a team or organization
- Choosing between REST, GraphQL, WebSockets, or message queues
- Planning API versioning and breaking change management
- Defining error response formats and HTTP status code usage
- Implementing pagination, filtering, and rate limiting patterns
- Designing OAuth2 flows or API key authentication
- Creating OpenAPI or AsyncAPI specifications and API reference docs, changelogs and migration guides
- Reviewing an API design before implementation or release

Do NOT use for:
- Server implementation code (use `implementing-api-patterns` for FastAPI, Hono, Axum, Gin, tRPC, gRPC)
- Module/library boundaries inside one process (use `api-and-interface-design`)
- Client libraries for an API (use `designing-sdks`)
- Authentication implementation (use `api-security-best-practices` or `security-and-hardening`)
- Docs-site tooling and renderers (use `generating-documentation`)

## Workflow

1. **Consumers and constraints.** Who calls the API (public third parties, own frontends, internal services)? Latency, caching, real-time and language constraints.
2. **Pick the style** with the matrix below (details in `references/protocol-selection.md`).
3. **Model resources or operations,** then errors, pagination, versioning, idempotency and auth, using the sections below.
4. **Write the contract** (OpenAPI 3.1, AsyncAPI 3.0, GraphQL SDL or `.proto`) with examples for every body.
5. **Review** against `references/api-design-checklist.md` and run `references/api-security-testing.md` before public release.
6. **Check the spec:** `python3 scripts/check_openapi.py <spec-or-dir>` for design gaps, `scripts/validate-openapi.sh <spec>` for Spectral linting.

**Done when:** the contract lints clean, every operation has examples and documented error responses, the checklist has no unexplained gaps, and the versioning/deprecation policy is written down.

## Core Design Principles

### Resource-Oriented Design (REST)

Use nouns for resources, not verbs in URLs:
```
✓ GET    /users              List users
✓ GET    /users/123          Get user 123
✓ POST   /users              Create user
✓ PATCH  /users/123          Update user 123
✓ DELETE /users/123          Delete user 123

✗ GET    /getUsers
✗ POST   /createUser
```

Nest resources for relationships (limit depth to 2-3 levels):
```
✓ GET /users/123/posts
✓ GET /users/123/posts/456/comments
✗ GET /users/123/posts/456/comments/789/replies  (too deep)
```

For complete REST patterns, see references/rest-design.md

### HTTP Method Semantics

| Method | Idempotent | Safe | Use For | Success Status |
|--------|-----------|------|---------|----------------|
| GET | Yes | Yes | Read resource | 200 OK |
| POST | No | No | Create resource | 201 Created |
| PUT | Yes | No | Replace entire resource | 200 OK, 204 No Content |
| PATCH | No | No | Update specific fields | 200 OK, 204 No Content |
| DELETE | Yes | No | Remove resource | 204 No Content, 200 OK |

Idempotent means multiple identical requests have the same effect as one request.

### HTTP Status Codes

**Success (2xx):**
- 200 OK, 201 Created, 204 No Content

**Client Errors (4xx):**
- 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found
- 409 Conflict, 422 Unprocessable Entity, 429 Too Many Requests

**Server Errors (5xx):**
- 500 Internal Server Error, 503 Service Unavailable

For complete status code guide, see references/rest-design.md

## API Style Selection

### Decision Matrix

| Factor | REST | GraphQL | WebSocket | Message Queue |
|--------|------|---------|-----------|---------------|
| Public API | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐ |
| Complex Data | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐ |
| Caching | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐ | ⭐ |
| Real-time | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Simplicity | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |

### Quick Selection

- **Public API, CRUD operations** → REST
- **Complex data, flexible queries** → GraphQL
- **Real-time, bidirectional** → WebSockets
- **Event-driven, microservices** → Message Queue

For detailed protocol selection, see references/protocol-selection.md

## API Versioning

### URL Path Versioning (Recommended)

```
https://api.example.com/v1/users
https://api.example.com/v2/users
```

Pros: Explicit, easy to implement and test
Cons: Maintenance overhead

### Alternative Strategies

- Header-Based: `Accept-Version: v1`
- Media Type: `Accept: application/vnd.example.v1+json`
- Query Parameter: `?version=1` (not recommended)

### Breaking Change Management

Timeline:
1. Month 0: Announce deprecation
2. Months 1-3: Migration period
3. Months 4-6: Deprecation warnings
4. Month 6: Sunset (return 410 Gone)

Include deprecation headers:
```http
Deprecation: @1735689599
Sunset: Sat, 31 Dec 2025 23:59:59 GMT
Link: </api/v2/users>; rel="successor-version"
```

`Deprecation` takes the date the resource was deprecated as `@<unix-seconds>` (RFC 9745); older drafts used `Deprecation: true`.

For complete versioning guide, see references/versioning-strategies.md

## Error Response Standards

### Problem Details (RFC 9457, formerly RFC 7807) (Recommended)

```json
{
  "type": "https://api.example.com/errors/validation",
  "title": "Validation Error",
  "status": 400,
  "detail": "One or more fields failed validation",
  "errors": [
    {
      "field": "email",
      "message": "Must be a valid email address",
      "code": "INVALID_EMAIL"
    }
  ]
}
```

Content-Type: `application/problem+json`

For complete error patterns, see references/error-handling.md

## Pagination Patterns

### Strategy Selection

| Scenario | Strategy | Why |
|----------|----------|-----|
| Small datasets (<1000) | Offset-based | Simple, page numbers |
| Large datasets (>10K) | Cursor-based | Efficient, handles writes |
| Sorted data | Keyset | Consistent results |
| Real-time feeds | Cursor-based | Handles new items |

### Offset-Based (Simple)

```http
GET /users?limit=20&offset=40
```

Response includes: `limit`, `offset`, `total`, `currentPage`

### Cursor-Based (Scalable)

```http
GET /users?limit=20&cursor=eyJpZCI6MTIzfQ==
```

Cursor is base64-encoded JSON with position information.
Response includes: `nextCursor`, `hasNext`

For implementation details, see references/pagination-patterns.md. For Link-header pagination, bulk endpoints, `Idempotency-Key` handling, CORS and health endpoints, see references/rest-operations.md.

## Rate Limiting

### Token Bucket Algorithm

- Each user has bucket with tokens
- Each request consumes 1 token
- Tokens refill at constant rate
- Empty bucket rejects request

### Rate Limit Headers

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 73
X-RateLimit-Reset: 1672531200
```

When exceeded (429):
```http
Retry-After: 3600
```

### Strategies

- Per User: 100 requests/hour
- Per API Key: 1000 requests/hour
- Per IP: 50 requests/hour (unauthenticated)
- Tiered: Free (100/hr), Pro (1000/hr), Enterprise (10000/hr)

For implementation patterns, see references/rate-limiting.md

## API Security Design

### OAuth 2.0 Flows

**Authorization Code Flow (Web Apps):**
1. Redirect user to authorization server
2. User grants permission
3. Exchange code for access token
4. Use token for API requests

**Client Credentials Flow (Service-to-Service):**
1. Authenticate with client ID and secret
2. Receive access token
3. Use token for API requests

### Scope-Based Authorization

Define granular permissions:
```
read:users    - Read user data
write:users   - Create/update users
delete:users  - Delete users
admin:*       - Full admin access
```

### API Key Management

Use header-based keys:
```http
X-API-Key: sk_live_abc123xyz456
```

Best practices:
- Prefix with environment: `sk_live_*`, `sk_test_*`
- Store hashed keys only
- Support key rotation
- Track last-used timestamp

For complete security patterns, see references/authentication.md

## OpenAPI Specification

### Basic Structure

```yaml
openapi: 3.1.0
info:
  title: User Management API
  version: 2.0.0

paths:
  /users:
    get:
      summary: List users
      parameters:
        - name: limit
          in: query
          schema:
            type: integer
      responses:
        '200':
          description: Success
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserList'
```

OpenAPI enables:
- Code generation (server stubs, client SDKs)
- Validation (request/response checking)
- Mock servers (testing against spec)
- Documentation (interactive docs)

For the reference-docs workflow (spec as source of truth, examples, guides, changelog and migration guides), see references/api-reference-docs.md

## AsyncAPI Specification

### Event-Driven APIs

AsyncAPI defines message-based APIs (WebSockets, Kafka, MQTT):

```yaml
asyncapi: 3.0.0
info:
  title: Order Events API
  version: 1.0.0

channels:
  ordersCreated:
    address: orders.created
    messages:
      orderCreated:
        payload:
          type: object
          properties:
            orderId:
              type: string
```

For AsyncAPI examples, see examples/asyncapi/

## GraphQL Design

### Schema Structure

```graphql
type User {
  id: ID!
  username: String!
  posts(limit: Int): [Post!]!
}

type Query {
  user(id: ID!): User
  users(limit: Int): [User!]!
}

type Mutation {
  createUser(input: CreateUserInput!): User!
}
```

### N+1 Problem Solution

Use DataLoader to batch requests:
```javascript
const userLoader = new DataLoader(async (userIds) => {
  // Single query for all users
  const users = await db.users.findByIds(userIds);
  return userIds.map(id => users.find(u => u.id === id));
});
```

For GraphQL patterns, see references/graphql-design.md (type system, DataLoader, Relay pagination, errors) and references/graphql-schema-patterns.md (interfaces/unions, mutation payloads, batch mutations, subscriptions, directives, cost limits, schema evolution).

## Quick Reference Tables

### Pagination Strategy Selection

| Scenario | Strategy |
|----------|----------|
| Small datasets | Offset-based |
| Large datasets | Cursor-based |
| Sorted data | Keyset |
| Real-time feeds | Cursor-based |

### Versioning Strategy Selection

| Factor | URL Path | Header | Media Type |
|--------|----------|--------|------------|
| Visibility | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐ |
| Simplicity | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| Best For | Most APIs | Internal APIs | Content negotiation |

## Integration with Other Skills

- **implementing-api-patterns**: Implement the design in FastAPI, Hono, Axum, Gin, tRPC or gRPC
- **designing-sdks**: Client libraries for the API
- **api-security-best-practices** / **security-and-hardening**: Implement OAuth2, JWT, sessions, input validation
- **api-security**: Authorized offensive assessment of a running API
- **database-design**: Database schemas behind API resources
- **testing-strategies**: API testing (integration, contract, load)
- **generating-documentation**: Render and host the reference docs
- **observability-and-instrumentation**: Monitor API latency, errors and usage

## Additional Resources

**Detailed guidance:**
- references/protocol-selection.md - Choosing REST, GraphQL, tRPC, gRPC, WebSockets or messaging
- references/rest-design.md - RESTful patterns, status codes, query parameters, caching
- references/rest-operations.md - Idempotency keys, bulk endpoints, Link-header pagination, CORS, health endpoints
- references/graphql-design.md - GraphQL type system, DataLoader, pagination, errors
- references/graphql-schema-patterns.md - GraphQL schema shapes, mutation payloads, cost limits, evolution
- references/versioning-strategies.md - Comprehensive versioning guide
- references/error-handling.md - Problem Details implementation details
- references/pagination-patterns.md - Pagination implementation patterns
- references/rate-limiting.md - Rate limiting algorithms and strategies
- references/authentication.md - OAuth2, API keys, scopes
- references/api-reference-docs.md - OpenAPI/AsyncAPI docs workflow, changelog and migration guides
- references/api-design-checklist.md - Pre-implementation and pre-release review checklist
- references/api-security-testing.md - OWASP API Top 10 test plan

**Working examples:**
- examples/asyncapi/ - Event-driven API specification
- examples/graphql/ - GraphQL schema

**Validation and tooling:**
- scripts/check_openapi.py - Read-only design-gap check of OpenAPI/Swagger files (operationIds, error responses, pagination, security)
- scripts/validate-openapi.sh - Lint spec files with Spectral

Attribution for merged material: references/ATTRIBUTION.md
