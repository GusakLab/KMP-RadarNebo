# API Design Review Checklist

Run this before implementation starts and again before a public release. Each unchecked item is either a fix or a documented, deliberate exception.

## Resources and Methods (REST)

- [ ] Resources are plural nouns; no verbs in paths (custom actions use `POST /orders/{id}:cancel` or a sub-resource)
- [ ] Naming is consistent across endpoints (case, pluralization, ID format)
- [ ] Nesting is at most 2 levels deep
- [ ] GET is safe; PUT and DELETE are idempotent; retryable POSTs accept `Idempotency-Key` (see `rest-operations.md`)
- [ ] PATCH semantics are stated (JSON Merge Patch or JSON Patch)

## Status Codes

- [ ] 201 + `Location` for creates; 204 for empty successes; 202 for async work
- [ ] 400 malformed, 401 unauthenticated, 403 unauthorized, 404 missing, 409 conflict, 422 validation, 429 throttled
- [ ] 5xx only for server faults, never for client mistakes

## Errors

- [ ] One error format everywhere (RFC 9457 Problem Details, which supersedes RFC 7807)
- [ ] Field-level validation errors with stable machine-readable codes
- [ ] No stack traces, SQL or internal hostnames in responses
- [ ] A correlation/request ID is returned and logged

## Collections

- [ ] Every collection endpoint is paginated, with a default and an enforced maximum page size
- [ ] Pagination style (cursor/offset/Link header) is consistent
- [ ] Filtering, sorting and sparse-field parameters follow one convention
- [ ] Bulk endpoints document atomicity and per-item results

## Versioning and Evolution

- [ ] Versioning strategy chosen (URL, header or media type) and applied consistently
- [ ] Breaking-change policy and deprecation timeline published (`Deprecation`/`Sunset` headers)
- [ ] Spec diff / breaking-change check runs in CI

## Security

- [ ] Authentication scheme defined (OAuth 2.0 / OIDC, API keys for server-to-server)
- [ ] Object-level authorization on every endpoint that takes an ID (OWASP API1: BOLA)
- [ ] Writable fields are allow-listed (no mass assignment)
- [ ] Rate limits per client, `429` + `Retry-After`
- [ ] HTTPS only; no secrets or personal data in URLs
- [ ] CORS is an explicit allow-list (browser-facing APIs only)
- [ ] See `api-security-testing.md` for the test pass

## Performance

- [ ] Large responses paginated; N+1 access paths identified
- [ ] Cache headers (`Cache-Control`, `ETag`) set where responses are cacheable
- [ ] Timeouts and payload size limits documented

## Documentation

- [ ] OpenAPI 3.1 (or AsyncAPI 3.0 / GraphQL SDL) spec is the source of truth and lints clean
- [ ] Every operation has a summary, examples and documented error responses
- [ ] Auth flow, rate limits and pagination explained once, linked everywhere
- [ ] Changelog and migration guide for each breaking change (see `api-reference-docs.md`)

## Operations

- [ ] Separate liveness and readiness endpoints
- [ ] Per-endpoint latency, error-rate and throughput metrics
- [ ] Contract tests or schema validation in CI

## GraphQL-Specific

- [ ] Nullability decided per field; mutations use input + payload types
- [ ] Expected errors modeled in payloads or result unions
- [ ] DataLoader (or equivalent batching) on every relationship resolver
- [ ] Depth and complexity limits; max list size enforced
- [ ] Introspection and persisted-query policy decided for production
- [ ] Deprecated fields marked with `@deprecated(reason:)`; schema diff in CI
