# API Reference Documentation

How to produce and maintain reference docs for an HTTP, GraphQL or event API. Read this when writing or auditing an OpenAPI/AsyncAPI spec, publishing a docs portal, or shipping a breaking change that needs a changelog and migration guide.

## Principle: the Spec Is the Source of Truth

Keep one machine-readable contract and generate everything else from it:

| API kind | Contract | Generated from it |
|----------|----------|-------------------|
| REST/HTTP | OpenAPI 3.1 (`openapi.yaml`) | Reference pages, mock server, client SDKs, request validation, contract tests |
| Events / WebSocket / Kafka | AsyncAPI 3.0 | Channel/message reference, payload schemas, codegen |
| GraphQL | SDL (`schema.graphql`) with descriptions | Schema explorer, type reference |
| gRPC | `.proto` with comments | Reference pages, stubs |

Decide early whether the spec is written first (design-first) or emitted from code annotations (code-first). Either works if CI fails when spec and implementation drift.

## Workflow

1. **Inventory.** List every operation (method + path, channel, or GraphQL field) and its owner. Flag undocumented and deprecated ones.
2. **Write or extract the spec.** Use `components/schemas` for shared models; `$ref` instead of copy-paste. Give each operation an `operationId`, `summary`, `description`, `tags`.
3. **Describe auth once.** `components/securitySchemes` (OAuth 2.0 flows with scopes, bearer, API key) plus `security` per operation.
4. **Document every response,** including errors: reuse one Problem Details (RFC 9457) schema and list the error `type` values a caller can act on.
5. **Add examples** for every request and response body (`examples:` with realistic but fake data, never real tokens or personal data).
6. **Lint and validate** in CI (for example Spectral or Redocly CLI; `scripts/validate-openapi.sh` wraps Spectral). Add a breaking-change diff against the last released spec.
7. **Render** with a reference renderer (Redoc, Swagger UI, Scalar, or your docs-site plugin). Host it next to the guides.
8. **Write the guides the spec cannot express:** quickstart (first successful call in under five minutes), authentication walkthrough, pagination, errors and retries, rate limits, webhooks, idempotency.
9. **Publish a changelog** and keep it current with each release.

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

Changelog entry per release, grouped as **Added / Changed / Deprecated / Removed / Fixed / Security**, each line naming the affected operation.

For every breaking change publish a migration guide containing:

- What changed and why, with the effective and sunset dates
- Before/after request and response examples
- A field/endpoint mapping table (old -> new)
- How to detect affected calls (e.g. `Deprecation` response header, usage dashboard)
- Whether old and new can run side by side during the migration window

See `versioning-strategies.md` for deprecation headers and timelines.

## Quality Gates

- [ ] Spec lints clean; breaking-change diff reviewed
- [ ] Every operation has summary, examples, all response codes
- [ ] Auth, pagination, errors and rate limits each documented once and linked
- [ ] Quickstart verified against a real (sandbox) environment
- [ ] Code samples compile/run (generate them or test them)
- [ ] Changelog updated; migration guide exists for each breaking change
- [ ] Deprecated operations marked `deprecated: true` in the spec
