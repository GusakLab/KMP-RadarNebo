# API Reference Documentation

Reference docs for HTTP, GraphQL or event APIs. Read when writing or auditing OpenAPI/AsyncAPI spec, publishing docs portal, or shipping breaking change (needs changelog and migration guide).

## Principle: the Spec Is the Source of Truth

Keep one machine-readable contract. Generate everything else from it:

| API kind | Contract | Generated from it |
|----------|----------|-------------------|
| REST/HTTP | OpenAPI 3.1 (`openapi.yaml`) | Reference pages, mock server, client SDKs, request validation, contract tests |
| Events / WebSocket / Kafka | AsyncAPI 3.0 | Channel/message reference, payload schemas, codegen |
| GraphQL | SDL (`schema.graphql`) with descriptions | Schema explorer, type reference |
| gRPC | `.proto` with comments | Reference pages, stubs |

Decide early: spec written first (design-first) or emitted from code annotations (code-first). Either works if CI fails when spec and implementation drift.

## Workflow

1. **Inventory.** List every operation (method + path, channel, or GraphQL field) with owner. Flag undocumented and deprecated ones.
2. **Write or extract the spec.** Use `components/schemas` for shared models; `$ref` instead of copy-paste. Give each operation `operationId`, `summary`, `description`, `tags`.
3. **Describe auth once.** `components/securitySchemes` (OAuth 2.0 flows with scopes, bearer, API key) plus `security` per operation.
4. **Document every response,** including errors: reuse one Problem Details (RFC 9457) schema. List error `type` values caller can act on.
5. **Add examples** for every request and response body (`examples:` with realistic but fake data, never real tokens or personal data).
6. **Lint and validate** in CI (e.g. Spectral or Redocly CLI; `scripts/validate-openapi.sh` wraps Spectral). Add breaking-change diff against last released spec.
7. **Render** with reference renderer (Redoc, Swagger UI, Scalar, or your docs-site plugin). Host next to guides.
8. **Write the guides the spec cannot express:** quickstart (first successful call in under five minutes), authentication walkthrough, pagination, errors and retries, rate limits, webhooks, idempotency.
9. **Publish a changelog**; update it each release.

## Minimal OpenAPI 3.1 Operation

```yaml
paths:
  /v1/orders/{orderId}:
    get:
      operationId: getOrder
      summary: Get an order
      tags: [Orders]
      security: [{ oauth2: [orders:read] }]
      parameters:
        - name: orderId
          in: path
          required: true
          schema: { type: string, pattern: '^ord_[A-Za-z0-9]+$' }
      responses:
        '200':
          description: The order
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Order' }
              examples:
                paid: { $ref: '#/components/examples/PaidOrder' }
        '404':
          $ref: '#/components/responses/NotFound'
```

## Changelog and Migration Guides

One changelog entry per release, grouped as **Added / Changed / Deprecated / Removed / Fixed / Security**. Each line names affected operation.

For every breaking change, publish migration guide with:

- What changed and why, with effective and sunset dates
- Before/after request and response examples
- Field/endpoint mapping table (old -> new)
- How to detect affected calls (e.g. `Deprecation` response header, usage dashboard)
- Whether old and new can run side by side during migration window

See `versioning-strategies.md` for deprecation headers and timelines.

## Quality Gates

- [ ] Spec lints clean; breaking-change diff reviewed
- [ ] Every operation has summary, examples, all response codes
- [ ] Auth, pagination, errors and rate limits each documented once and linked
- [ ] Quickstart verified against real (sandbox) environment
- [ ] Code samples compile/run (generate or test them)
- [ ] Changelog updated; migration guide exists for each breaking change
- [ ] Deprecated operations marked `deprecated: true` in spec
