# API Security Testing

Test plan for APIs you own or are authorized to test, organized by OWASP API Security Top 10 (2023 edition). Use as security pass of design review or before public release. For full offensive assessment methodology use `api-security` skill; for implementation hardening use `api-security-best-practices`.

## OWASP API Security Top 10 (2023)

| ID | Risk | What to test |
|----|------|--------------|
| API1 | Broken Object Level Authorization (BOLA/IDOR) | Swap resource IDs between two users' sessions |
| API2 | Broken Authentication | Token validation, credential stuffing protection, session/token expiry |
| API3 | Broken Object Property Level Authorization | Mass assignment (extra writable fields), excessive data exposure in responses |
| API4 | Unrestricted Resource Consumption | Rate limits, page-size caps, payload size, expensive queries |
| API5 | Broken Function Level Authorization | Call admin/privileged operations as normal user |
| API6 | Unrestricted Access to Sensitive Business Flows | Automate purchase, signup, voting, coupon flows |
| API7 | Server-Side Request Forgery | Any parameter that makes server fetch a URL |
| API8 | Security Misconfiguration | Verbose errors, debug endpoints, permissive CORS, missing TLS/headers |
| API9 | Improper Inventory Management | Old versions, undocumented/shadow endpoints still reachable |
| API10 | Unsafe Consumption of APIs | Third-party API responses trusted without validation |

## Authentication

| Area | Checks |
|------|--------|
| JWT | `alg: none` and algorithm confusion rejected; signature verified with expected key; `exp`, `iss`, `aud` enforced; weak HMAC secrets ruled out |
| Sessions / tokens | Unpredictable, expire, invalidated on logout and password change |
| API keys | Not accepted in query strings; revocable; scoped |

## Authorization

| Test | Approach |
|------|----------|
| Horizontal (BOLA) | Capture request as user A, replay with user B's credentials, expect 403/404 |
| Vertical | Call privileged operations with low-privilege token |
| Property level | Send fields user may not set (`role`, `ownerId`, `price`); check they are ignored or rejected |
| Context | Access across tenants, organizations or projects |

Automate BOLA matrix: for each endpoint taking an ID, test owner, other user, other tenant, anonymous.

## Input Handling

- Fuzz every parameter (path, query, header, body) with wrong types, boundary values, oversized payloads
- Injection classes relevant to stack: SQL, NoSQL operators (`{"$ne": null}`), command, template, LDAP
- Error responses must not leak stack traces, queries or internal hosts

## Resource Consumption

| Check | Expect |
|-------|--------|
| Rate limit exists | `429` with `Retry-After` |
| Bypass attempts | Rotating `X-Forwarded-For`, case/path variations (`/Users`, `/users/`), other API versions should not reset limits |
| Scope | Per user/key, and per IP for unauthenticated traffic |
| Size limits | Oversized bodies rejected (413); page size capped |

## GraphQL

| Test | Focus |
|------|-------|
| Introspection | Disabled or restricted in production if schema is private |
| Depth / complexity | Deeply nested or wide queries rejected before execution |
| Batching / aliases | Many operations or aliased fields in one request (brute-force amplification) |
| Field authorization | Sensitive fields enforced in resolvers, not only at operation level |

## Done When

- [ ] BOLA matrix passes for every ID-taking endpoint
- [ ] Privileged operations reject non-privileged tokens
- [ ] Mass-assignment probes ignored or rejected
- [ ] Rate and size limits verified, bypass attempts fail
- [ ] Error responses leak no internals
- [ ] Only documented, current API versions reachable
