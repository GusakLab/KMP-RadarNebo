---
name: testing-strategies
description: Choose the mix of unit, integration, E2E and contract tests for a project, plus mocking, test-data, coverage/mutation and CI test-stage strategy (TypeScript, Python, Go, Rust). Use when designing, auditing or rebalancing a test suite; for writing one test first use test-driven-development.
---

# Testing Strategies

## When to Use This Skill

Use when:
- New feature needs test coverage
- Designing testing strategy for new project
- Refactoring tests for speed or reliability
- Setting up CI/CD pipelines with test stages
- Choosing unit, integration, or E2E
- Adding contract testing for microservices
- Managing test data (fixtures, factories, property-based testing)

## The Testing Pyramid Framework

### Core Concept

Pyramid sets test distribution for speed and confidence:

```
         /\
        /  \  E2E Tests (10%)
       /----\  - Slow but comprehensive
      /      \  - Full stack validation
     /--------\
    /          \  Integration Tests (20-30%)
   /            \  - Moderate speed
  /--------------\  - Component interactions
 /                \
/------------------\  Unit Tests (60-70%)
                      - Fast feedback
                      - Isolated units
```

**Key Principle**: More unit tests (fast, isolated), fewer E2E tests (slow, comprehensive). Integration tests bridge gap.

### Modern Adaptations

**Microservices Adjustment**:
- Add contract layer between unit and integration
- Raise integration/contract tests to 30% (validate service boundaries)
- E2E tests for critical user journeys only

**Cloud-Native Patterns**:
- Containers for integration tests (ephemeral databases, test services)
- Parallel execution for fast CI/CD feedback
- Risk-based test prioritization (focus on high-impact areas)

Details: `references/testing-pyramid.md`.

## Universal Testing Decision Tree

### Which Test Type Should I Use?

```
START: Need to test [feature]

Q1: Does this involve multiple systems/services?
  ├─ YES → Q2
  └─ NO  → Q3

Q2: Is this a critical user-facing workflow?
  ├─ YES → E2E Test (complete user journey)
  └─ NO  → Integration or Contract Test

Q3: Does this interact with external dependencies (DB, API, filesystem)?
  ├─ YES → Integration Test (real DB, mocked API)
  └─ NO  → Q4

Q4: Is this pure business logic or a pure function?
  ├─ YES → Unit Test (fast, isolated)
  └─ NO  → Component or Integration Test
```

### Test Type Selection Examples

| Feature | Test Type | Rationale |
|---------|-----------|-----------|
| `calculateTotal(items)` | Unit | Pure function, no dependencies |
| `POST /api/users` endpoint | Integration | API + database interaction |
| User registration flow (form → API → redirect) | E2E | Critical user journey, full stack |
| Microservice A → B communication | Contract | Service interface validation |
| `formatCurrency(amount, locale)` | Unit + Property | Pure logic, many edge cases |
| Form validation logic | Unit | Isolated business rules |
| File upload to S3 | Integration | External service interaction |

Full decision frameworks: `references/decision-tree.md`.

## Testing Levels in Detail

### Unit Testing (Foundation - 60-70%)

**Purpose**: Validate small, isolated code units (functions, methods, components)

**Characteristics**:
- Fast (milliseconds per test)
- Isolated (no external dependencies)
- Deterministic (same input = same output)
- Broad coverage (many tests, small scope each)

**When to Use**:
- Pure functions (input → output)
- Business logic and algorithms
- Utility functions
- Component rendering (without integration)
- Validation logic

**Recommended Tools**:
- **TypeScript/JavaScript**: Vitest (primary; fast, native ESM/TypeScript), Jest (common in existing codebases)
- **Python**: pytest
- **Go**: testing package (stdlib) + testify (assertions)
- **Rust**: cargo test (stdlib)

Patterns: `references/unit-testing-patterns.md`.

### Integration Testing (Middle Layer - 20-30%)

**Purpose**: Validate interactions between components, modules, services

**Characteristics**:
- Moderate speed (seconds per test)
- Partial integration (real database, mocked external APIs)
- Focused scope (component boundaries)
- API and database validation

**When to Use**:
- API endpoints (request → response)
- Database operations (CRUD, queries)
- Service-to-service communication
- Event handlers and message processing
- File I/O operations

**Recommended Tools**:
- **TypeScript/JavaScript**: Vitest + MSW (API mocking), Supertest (HTTP testing)
- **Python**: pytest + pytest-httpserver, pytest-postgresql
- **Go**: testing + httptest, testcontainers
- **Rust**: cargo test + mockito, testcontainers

Patterns: `references/integration-testing-patterns.md`.

### End-to-End Testing (Top Layer - 10%)

**Purpose**: Validate complete user workflows across full stack

**Characteristics**:
- Slow (minutes per test suite)
- Full integration (real browser, services, database)
- Wide scope (user journeys start to finish)
- Prone to flakiness (needs careful design)

**When to Use**:
- Critical user journeys (login, checkout, payment)
- Cross-browser compatibility validation
- Real-world scenarios not covered by integration tests
- Regression prevention for core features

**Best Practices**:
- High-value scenarios only (not every edge case)
- Use stable selectors (data-testid, not CSS classes)
- Add retry logic for network flakiness
- Run in parallel for speed

**Recommended Tools**:
- **All Languages**: Playwright (cross-browser, fast)

Patterns: `references/e2e-testing-patterns.md`.

### Contract Testing (Microservices)

**Purpose**: Validate service interfaces and API contracts without full integration

**When to Use**:
- Microservices architecture
- Service-to-service communication
- API contract validation
- Cutting E2E overhead

**Recommended Tool**: Pact (pact.io) - TypeScript, Python, Go, Rust

Patterns: `references/contract-testing.md`.

## Test Data Management Strategies

### When to Use Each Approach

**Fixtures (Static Data)**:
- Pros: Deterministic, easy to debug
- Cons: Can go stale, no variety
- Use When: Known scenarios, regression tests

**Factories (Generated Data)**:
- Pros: Flexible, generates variety
- Cons: Less deterministic, harder to debug
- Use When: Diverse test data, edge cases

**Property-Based Testing (Random Data)**:
- Pros: Finds edge cases not anticipated
- Cons: Can be slow, failures harder to reproduce
- Use When: Complex algorithms, parsers, validators

**Recommended Combination**:
- **Unit Tests**: Fixtures (known inputs) + Property-Based (edge cases)
- **Integration Tests**: Factories (flexible data) + Database seeding
- **E2E Tests**: Fixtures (reproducible scenarios)

**Property-Based Testing Tools**:
- **TypeScript/JavaScript**: fast-check
- **Python**: hypothesis
- **Go**: gopter
- **Rust**: proptest (primary)

Strategies: `references/test-data-strategies.md`.

## Mocking Decision Matrix

### When to Mock vs. Use Real Dependencies

| Dependency | Unit Test | Integration Test | E2E Test |
|------------|-----------|------------------|----------|
| **Database** | Mock (in-memory) | Real (test DB, Docker) | Real (staging DB) |
| **External API** | Mock (MSW, nock) | Mock (MSW, VCR) | Real (or staging) |
| **Filesystem** | Mock (in-memory FS) | Real (temp directory) | Real |
| **Time/Date** | Mock (freezeTime) | Mock (if deterministic) | Real (usually) |
| **Environment Variables** | Mock (setEnv) | Mock (test config) | Real (test env) |
| **Internal Services** | Mock (stub) | Real (or container) | Real |

**Guiding Principles**:
1. **Unit Tests**: Mock everything external
2. **Integration Tests**: Real database (ephemeral), mock external APIs
3. **E2E Tests**: Real everything (or staging equivalents)
4. **Contract Tests**: Mock nothing (test real interfaces)

Mocking patterns: `references/mocking-strategies.md`.

## Language-Specific Quick Starts

### TypeScript/JavaScript

**Unit Testing with Vitest**:
```typescript
import { describe, test, expect } from 'vitest'

test('calculates total with tax', () => {
  const items = [{ price: 10, quantity: 2 }]
  expect(calculateTotal(items, 0.1)).toBe(22)
})
```

**Integration Testing with MSW**:
```typescript
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'

const server = setupServer(
  http.get('/api/user/:id', ({ params }) => {
    return HttpResponse.json({ id: params.id, name: 'Test User' })
  })
)
```

**E2E Testing with Playwright**:
```typescript
import { test, expect } from '@playwright/test'

test('user can checkout', async ({ page }) => {
  await page.goto('https://example.com')
  await page.getByRole('button', { name: 'Add to Cart' }).click()
  await expect(page.getByText('1 item in cart')).toBeVisible()
})
```

### Python

**Unit Testing with pytest**:
```python
def test_calculate_total_with_tax():
    items = [{"price": 10, "quantity": 2}]
    assert calculate_total(items, tax_rate=0.1) == 22
```

**Property-Based Testing with hypothesis**:
```python
from hypothesis import given, strategies as st

@given(st.lists(st.integers()))
def test_reverse_reverse_is_identity(lst):
    assert reverse(reverse(lst)) == lst
```

### Go and Rust

```go
// Go: table-driven test, run with `go test ./...`
func TestAdd(t *testing.T) {
    cases := []struct{ a, b, want int }{{1, 2, 3}, {-1, 1, 0}}
    for _, c := range cases {
        if got := Add(c.a, c.b); got != c.want {
            t.Errorf("Add(%d, %d) = %d, want %d", c.a, c.b, got, c.want)
        }
    }
}
```

```rust
// Rust: unit tests live next to the code, run with `cargo test`
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn adds() { assert_eq!(add(1, 2), 3); }
}
```

Go and Rust fixture, factory, property-based examples (including `proptest`): `references/test-data-strategies.md`.

### Working Examples

Code examples in `examples/`:

- **`examples/typescript/vitest-unit.test.ts`** and **`examples/typescript/vitest-integration.test.ts`** - Vitest unit and integration tests
- **`examples/python/pytest_unit_test.py`** - pytest unit tests and fixtures
- **`examples/python/hypothesis_property_test.py`** - Hypothesis property-based tests

## Coverage and Quality Metrics

### Meaningful Coverage Targets

**Recommended Targets**:
- **Critical Business Logic**: 90%+ coverage
- **API Endpoints**: 80%+ coverage
- **Utility Functions**: 70%+ coverage
- **UI Components**: 60%+ coverage (focus on logic, not markup)
- **Overall Project**: 70-80% coverage

**Anti-Pattern**: Chasing 100% coverage. Leads to testing trivial code and false confidence.

**Coverage Tools**:
- **TypeScript/JavaScript**: Vitest coverage (c8/istanbul)
- **Python**: pytest-cov
- **Go**: go test -cover
- **Rust**: cargo-tarpaulin

### Mutation Testing (Validate Test Quality)

Mutation testing injects bugs and checks tests catch them.

**Tools**:
- **TypeScript/JavaScript**: Stryker Mutator
- **Python**: mutmut
- **Go**: go-mutesting
- **Rust**: cargo-mutants

Coverage strategies: `references/coverage-strategies.md`.

## CI/CD Integration Patterns

### Fast Feedback Loop Strategy

**Stage 1: Pre-Commit (< 30 seconds)**
- Lint and format checks
- Unit tests (critical paths only)

**Stage 2: On Commit (< 2 minutes)**
- All unit tests
- Static analysis

**Stage 3: Pull Request (< 5 minutes)**
- Integration tests
- Coverage reporting

**Stage 4: Pre-Merge (< 10 minutes)**
- E2E tests (critical paths)
- Cross-browser testing

### Parallel Execution

**Speed Up Test Runs**:
- **Vitest**: parallel across files by default; tune with `--pool=threads` or `--pool=forks` and `--maxWorkers`
- **Playwright**: `playwright test --workers=4`
- **pytest**: `pytest -n auto` (requires pytest-xdist)
- **Go**: `go test -parallel 4`

## Common Anti-Patterns to Avoid

### The Ice Cream Cone (Inverted Pyramid)

**Problem**: Too many E2E tests, too few unit tests
**Result**: Slow suites, flaky tests, long feedback loops
**Solution**: Rebalance toward more unit tests, fewer E2E tests

### Testing Implementation Details

**Problem**: Tests coupled to internal structure, not behavior
**Solution**: Test behavior (inputs → outputs), not implementation

### Over-Mocking in Integration Tests

**Problem**: Mocking everything defeats purpose of integration tests
**Solution**: Use real databases (ephemeral), mock only external APIs

### Flaky E2E Tests

**Problem**: Intermittent failures from timing issues
**Solutions**:
- Use auto-wait (built into Playwright)
- Avoid hardcoded waits (`sleep(1000)`)

## Integration with Other Skills

**For form testing**: `building-forms` (form validation and submission test patterns).

**For API testing**: `designing-apis` (REST/GraphQL contract that endpoint and contract tests verify).

**For test-first work on one behavior**: `test-driven-development`. Stakeholder-readable acceptance scenarios: `bdd-principles`.

**For CI/CD pipelines**: `building-ci-pipelines` (test automation, parallel execution, coverage reporting).

**For data visualization testing**: `visualizing-data` (chart configuration snapshot tests, visual regression testing).
