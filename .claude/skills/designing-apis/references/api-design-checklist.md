# API Design Review Checklist

Run before implementation and again before public release. Each unchecked item: fix it or document a deliberate exception.

## Resources and Methods (REST)

- [ ] Plural-noun resources; no verbs in paths (custom actions use `POST /orders/{id}:cancel` or sub-resource)
- [ ] Naming consistent across endpoints (case, pluralization, ID format)
- [ ] Nesting max 2 levels
- [ ] GET safe; PUT and DELETE idempotent; retryable POSTs accept `Idempotency-Key` (see `rest-operations.md`)
- [ ] PATCH semantics stated (JSON Merge Patch or JSON Patch)

## Status Codes

- [ ] 201 + `Location` for creates; 204 for empty successes; 202 for async work
- [ ] 400 malformed, 401 unauthenticated, 403 unauthorized, 404 missing, 409 conflict, 422 validation, 429 throttled
- [ ] 5xx only for server faults, never for client mistakes

## Errors

- [ ] One error format everywhere (RFC 9457 Problem Details, supersedes RFC 7807)
- [ ] Field-level validation errors with stable machine-readable codes
- [ ] No stack traces, SQL or internal hostnames in responses
- [ ] Correlation/request ID returned and logged

## Collections

- [ ] Every collection endpoint paginated; default and enforced max page size
- [ ] Pagination style (cursor/offset/Link header) consistent
- [ ] Filtering, sorting and sparse-field parameters follow one convention
- [ ] Bulk endpoints document atomicity and per-item results

## Versioning and Evolution

- [ ] Versioning strategy chosen (URL, header or media type) and applied consistently
- [ ] Breaking-change policy and deprecation timeline published (`Deprecation`/`Sunset` headers)
- [ ] Spec diff / breaking-change check runs in CI

## Security

- [ ] Authentication scheme defined (OAuth 2.0 / OIDC, API keys for server-to-server)
- [ ] Object-level authorization on every ID-taking endpoint (OWASP API1: BOLA)
- [ ] Writable fields allow-listed (no mass assignment)
- [ ] Rate limits per client, `429` + `Retry-After`
- [ ] HTTPS only; no secrets or personal data in URLs
- [ ] CORS is explicit allow-list (browser-facing APIs only)
- [ ] Test pass: `api-security-testing.md`

## Performance

- [ ] Large responses paginated; N+1 access paths identified
- [ ] Cache headers (`Cache-Control`, `ETag`) set where responses cacheable
- [ ] Timeouts and payload size limits documented

## Documentation

- [ ] OpenAPI 3.1 (or AsyncAPI 3.0 / GraphQL SDL) spec is source of truth and lints clean
- [ ] Every operation has summary, examples and documented error responses
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
