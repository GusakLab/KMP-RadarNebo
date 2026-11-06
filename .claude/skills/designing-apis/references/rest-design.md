# RESTful API Design Patterns

## Table of Contents

1. [Resource Naming Conventions](#resource-naming-conventions)
2. [HTTP Method Semantics](#http-method-semantics)
3. [Status Code Selection](#status-code-selection)
4. [Query Parameters](#query-parameters)
5. [HATEOAS](#hateoas)
6. [Content Negotiation](#content-negotiation)
7. [Caching](#caching)
8. [Best Practices Checklist](#best-practices-checklist)

## Resource Naming Conventions

### Core Rules

1. **Nouns, not verbs**
   ```
   ✓ /users
   ✗ /getUsers
   ```

2. **Plural nouns for collections**
   ```
   ✓ /users
   ✗ /user
   ```

3. **Lowercase with hyphens (kebab-case)**
   ```
   ✓ /user-profiles
   ✗ /userProfiles
   ✗ /user_profiles
   ```

4. **Nest resources for relationships**
   ```
   ✓ /users/123/posts
   ✓ /users/123/posts/456
   ✗ /posts?userId=123  (for nested resources)
   ```

5. **Limit nesting depth to 2-3 levels**
   ```
   ✓ /users/123/posts/456/comments
   ✗ /users/123/posts/456/comments/789/replies
   ```

### Common Patterns

**User Resources:**
```
GET    /users                    List all users
GET    /users/123                Get user 123
POST   /users                    Create new user
PUT    /users/123                Replace user 123
PATCH  /users/123                Update user 123
DELETE /users/123                Delete user 123
```

**Nested Resources:**
```
GET    /users/123/posts          List posts by user 123
POST   /users/123/posts          Create post for user 123
GET    /users/123/posts/456      Get specific post
DELETE /users/123/posts/456      Delete post
```

**Collection Actions:**
```
GET    /users/123/followers      List followers
POST   /users/123/followers      Follow user 123
DELETE /users/123/followers/456  Unfollow user 456
```

### Special Cases

**Search and Filtering:**
```
GET /users?status=active&role=admin
GET /search?q=api+design&type=documentation
```

**Custom Actions (when REST doesn't fit):**
```
POST /users/123/activate
POST /orders/456/refund
POST /posts/789/publish
```

Use custom actions sparingly.

## HTTP Method Semantics

### GET - Read Resource

**Characteristics:**
- Safe: does not modify server state
- Idempotent: repeated identical requests give same result
- Cacheable: responses may be cached

**Usage:**
```http
GET /users/123
Accept: application/json

200 OK
Content-Type: application/json
Cache-Control: max-age=3600

{
  "id": "123",
  "username": "alice",
  "email": "alice@example.com"
}
```

**No Request Body:** GET requests should not carry a body.

### POST - Create Resource

**Characteristics:**
- Not safe: modifies server state
- Not idempotent: each request creates new resource
- Not cacheable (unless explicitly set)

**Usage:**
```http
POST /users
Content-Type: application/json

{
  "username": "bob",
  "email": "bob@example.com"
}

201 Created
Location: /users/124
Content-Type: application/json

{
  "id": "124",
  "username": "bob",
  "email": "bob@example.com"
}
```

**Key Points:**
- Return 201 Created on success
- Include Location header with URI of created resource
- Return created resource in response body

### PUT - Replace Entire Resource

**Characteristics:**
- Not safe: modifies server state
- Idempotent: repeated identical requests give same result
- Replaces entire resource

**Usage:**
```http
PUT /users/123
Content-Type: application/json

{
  "username": "alice",
  "email": "alice@newdomain.com",
  "bio": "Updated bio",
  "status": "active"
}

200 OK
Content-Type: application/json

{
  "id": "123",
  "username": "alice",
  "email": "alice@newdomain.com",
  "bio": "Updated bio",
  "status": "active"
}
```

**Key Points:**
- Send all fields, not only changed ones
- Missing fields may be set to null or default
- Use PATCH for partial updates

### PATCH - Partial Update

**Characteristics:**
- Not safe: modifies server state
- Not idempotent (typically)
- Updates specific fields only

**Usage:**
```http
PATCH /users/123
Content-Type: application/json

{
  "email": "alice@newdomain.com"
}

200 OK
Content-Type: application/json

{
  "id": "123",
  "username": "alice",
  "email": "alice@newdomain.com",
  "bio": "Original bio",
  "status": "active"
}
```

**Key Points:**
- Send only changed fields
- Other fields stay unchanged
- Use JSON Patch (RFC 6902) for complex updates

**JSON Patch Example:**
```http
PATCH /users/123
Content-Type: application/json-patch+json

[
  { "op": "replace", "path": "/email", "value": "alice@newdomain.com" },
  { "op": "add", "path": "/tags/-", "value": "premium" }
]
```

### DELETE - Remove Resource

**Characteristics:**
- Not safe: modifies server state
- Idempotent: repeated DELETE requests give same result
- Resource no longer exists afterward

**Usage:**
```http
DELETE /users/123

204 No Content
```

**Alternative with confirmation:**
```http
DELETE /users/123

200 OK
Content-Type: application/json

{
  "message": "User 123 deleted successfully",
  "deletedAt": "2025-12-03T10:30:00Z"
}
```

**Key Points:**
- Use 204 No Content (no response body)
- Or 200 OK with confirmation message
- Second DELETE of same resource should return 404 Not Found

## Status Code Selection

### 2xx Success

**200 OK** - Standard success with body

**201 Created** - Resource created

**202 Accepted** - Accepted for async processing
```http
POST /reports/generate
202 Accepted
{ "jobId": "job-789", "status": "processing" }
```

**204 No Content** - Success, no response body

### 3xx Redirection

**301 Moved Permanently** - Resource moved permanently
```http
GET /old-endpoint
301 Moved Permanently
Location: /new-endpoint
```

**302 Found** - Temporary redirect
```http
GET /users/me
302 Found
Location: /users/123
```

**304 Not Modified** - Cached version still valid

### 4xx Client Errors

**400 Bad Request** - Invalid syntax or validation error
```json
{
  "type": "https://api.example.com/errors/validation",
  "title": "Validation Error",
  "status": 400,
  "errors": [
    { "field": "email", "message": "Invalid email format" }
  ]
}
```

**401 Unauthorized** - Authentication required or failed
```json
{
  "type": "https://api.example.com/errors/unauthorized",
  "title": "Authentication Required",
  "status": 401,
  "detail": "Valid credentials required"
}
```

**403 Forbidden** - Authenticated but not authorized
```json
{
  "type": "https://api.example.com/errors/forbidden",
  "title": "Insufficient Permissions",
  "status": 403,
  "requiredScope": "admin:write"
}
```

**404 Not Found** - Resource does not exist
```json
{
  "type": "https://api.example.com/errors/not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "User with id '123' not found"
}
```

**405 Method Not Allowed** - HTTP method not supported
```http
POST /users/123
405 Method Not Allowed
Allow: GET, PUT, PATCH, DELETE
```

**409 Conflict** - Conflicts with current state
```json
{
  "type": "https://api.example.com/errors/conflict",
  "title": "Resource Conflict",
  "status": 409,
  "detail": "Username already exists"
}
```

**422 Unprocessable Entity** - Valid syntax, semantic errors
```json
{
  "type": "https://api.example.com/errors/validation",
  "title": "Validation Error",
  "status": 422,
  "detail": "Age must be positive number"
}
```

**429 Too Many Requests** - Rate limit exceeded
```http
429 Too Many Requests
Retry-After: 3600
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
```

### 5xx Server Errors

**500 Internal Server Error** - Generic server error
```json
{
  "type": "https://api.example.com/errors/internal",
  "title": "Internal Server Error",
  "status": 500,
  "traceId": "abc-123"
}
```

**503 Service Unavailable** - Temporarily unavailable
```http
503 Service Unavailable
Retry-After: 120
```

## Query Parameters

### Filtering

Filter collection by field values:
```
GET /users?status=active
GET /posts?authorId=123&published=true
GET /products?category=electronics&inStock=true
```

### Sorting

Sort by field:
```
GET /users?sort=createdAt:desc
GET /products?sort=price:asc
GET /posts?sort=title:asc,createdAt:desc  (multiple sorts)
```

### Searching

Full-text search:
```
GET /users?q=alice
GET /posts?search=api+design
```

### Field Selection (Sparse Fieldsets)

Request specific fields only:
```
GET /users?fields=id,username,email
GET /posts?fields=id,title,authorId
```

Response holds only requested fields, shrinking payload.

### Pagination

Full guide: pagination-patterns.md.

### Combined Example

```
GET /products?
  category=electronics&
  inStock=true&
  minPrice=100&
  maxPrice=500&
  sort=price:asc&
  fields=id,name,price&
  limit=20&
  offset=40
```

## HATEOAS

Hypermedia As The Engine Of Application State: include links to related resources.

### When to Use HATEOAS

**Use when:**
- API must evolve without breaking clients
- Clients navigate API dynamically
- Workflow-driven applications

**Skip when:**
- Mobile apps (bandwidth concerns)
- Simple CRUD APIs
- Clients hardcode URLs anyway

### Example with Links

```json
{
  "id": "123",
  "username": "alice",
  "email": "alice@example.com",
  "_links": {
    "self": {
      "href": "/users/123"
    },
    "posts": {
      "href": "/users/123/posts"
    },
    "followers": {
      "href": "/users/123/followers"
    },
    "follow": {
      "href": "/users/123/followers",
      "method": "POST"
    },
    "unfollow": {
      "href": "/users/123/followers/456",
      "method": "DELETE"
    }
  }
}
```

### HAL Format

Hypertext Application Language (HAL) standard:
```json
{
  "_links": {
    "self": { "href": "/orders/123" },
    "customer": { "href": "/customers/456" },
    "items": { "href": "/orders/123/items" }
  },
  "orderId": "123",
  "total": 99.99,
  "_embedded": {
    "customer": {
      "_links": { "self": { "href": "/customers/456" } },
      "name": "Alice Smith"
    }
  }
}
```

## Content Negotiation

### Accept Header

Client states preferred response format(s):
```http
Accept: application/json, application/xml;q=0.9
```

### Common Media Types

- `application/json` - JSON (most common)
- `application/xml` - XML
- `application/hal+json` - HAL format
- `application/problem+json` - RFC 7807 errors
- `text/csv` - CSV export
- `application/pdf` - PDF reports

### Example Supporting Multiple Formats

```http
# Request JSON
GET /users/123
Accept: application/json
→ 200 OK, Content-Type: application/json

# Request XML
GET /users/123
Accept: application/xml
→ 200 OK, Content-Type: application/xml

# Request unsupported format
GET /users/123
Accept: application/yaml
→ 406 Not Acceptable
```

## Caching

### Cache-Control Header

Controls caching:

**No caching:**
```http
Cache-Control: no-store
```

**Cache for 1 hour:**
```http
Cache-Control: max-age=3600
```

**Cache but revalidate:**
```http
Cache-Control: max-age=3600, must-revalidate
```

**Private cache (browser only):**
```http
Cache-Control: private, max-age=3600
```

**Public cache (CDN allowed):**
```http
Cache-Control: public, max-age=86400
```

### ETags for Conditional Requests

**Initial request:**
```http
GET /users/123

200 OK
ETag: "abc123"
Content-Type: application/json
{ "id": "123", "username": "alice" }
```

**Conditional request:**
```http
GET /users/123
If-None-Match: "abc123"

304 Not Modified  (if unchanged)
```

**Update with optimistic locking:**
```http
PUT /users/123
If-Match: "abc123"
{ "username": "alice", "email": "new@example.com" }

200 OK  (if ETag matches)
412 Precondition Failed  (if ETag changed)
```

### Last-Modified

Timestamp-based alternative to ETags:

**Initial request:**
```http
GET /users/123

200 OK
Last-Modified: Wed, 01 Dec 2025 12:00:00 GMT
```

**Conditional request:**
```http
GET /users/123
If-Modified-Since: Wed, 01 Dec 2025 12:00:00 GMT

304 Not Modified  (if unchanged)
```

## Best Practices Checklist

### Headers
- [ ] Content-Type in all responses
- [ ] Location header for 201 Created
- [ ] Cache-Control for caching strategy
- [ ] Rate limit headers (X-RateLimit-*)

### Error Handling
- [ ] RFC 7807 Problem Details format
- [ ] Consistent error structure
- [ ] Meaningful error messages
- [ ] Error type URIs with documentation

### Documentation
- [ ] OpenAPI 3.1 specification
- [ ] Request/response examples
- [ ] Error response examples
- [ ] Authentication requirements
