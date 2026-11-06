# Coverage and Quality Metrics

## Table of Contents

1. [Meaningful Coverage Targets](#meaningful-coverage-targets)
2. [Coverage Tools](#coverage-tools)
3. [Mutation Testing](#mutation-testing)
4. [Beyond Line Coverage](#beyond-line-coverage)

## Meaningful Coverage Targets

### Anti-Pattern: 100% Coverage Goal

**Better Approach**: Risk-based targets

### Recommended Targets

| Code Type | Target | Rationale |
|-----------|--------|-----------|
| **Critical Business Logic** | 90%+ | Payment, auth, data integrity |
| **API Endpoints** | 80%+ | Public interfaces |
| **Utility Functions** | 70%+ | Widely reused code |
| **UI Components** | 60%+ | Focus on logic, not markup |
| **Overall Project** | 70-80% | Balanced coverage |

### What NOT to Test

- Simple getters/setters
- Framework-generated code
- Third-party libraries
- Configuration files
- Trivial pass-through functions

## Coverage Tools

### TypeScript/JavaScript

**Vitest Coverage** (recommended):
```bash
npm install -D @vitest/coverage-v8
vitest --coverage
```

**Configuration**:
```typescript
// vitest.config.ts
export default {
  test: {
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'json'],
      include: ['src/**/*.ts'],
      exclude: ['**/*.test.ts', '**/*.spec.ts'],
      lines: 70,
      functions: 70,
      branches: 70,
      statements: 70
    }
  }
}
```

### Python

**pytest-cov**:
```bash
pip install pytest-cov
pytest --cov=src --cov-report=html --cov-report=term
```

**Configuration (.coveragerc)**:
```ini
[run]
omit = */tests/*, */migrations/*

[report]
exclude_lines =
    pragma: no cover
    def __repr__
    raise AssertionError
    raise NotImplementedError
```

### Go

**Built-in coverage**:
```bash
go test -cover ./...
go test -coverprofile=coverage.out ./...
go tool cover -html=coverage.out
```

### Rust

**cargo-tarpaulin**:
```bash
cargo install cargo-tarpaulin
cargo tarpaulin --out Html
```

## Mutation Testing

### What Is Mutation Testing?

Mutation testing checks test quality: inject bugs (mutations), see if tests catch them.

**Example**:
```typescript
// Original code
function add(a, b) {
  return a + b
}

// Mutation 1: Change + to -
function add(a, b) {
  return a - b  // If tests still pass, they're weak!
}

// Mutation 2: Change return value
function add(a, b) {
  return 0  // If tests still pass, they're weak!
}
```

### Mutation Testing Tools

**TypeScript/JavaScript: Stryker Mutator**:
```bash
npm install -D @stryker-mutator/core @stryker-mutator/vitest-runner
npx stryker run
```

**Python: mutmut**:
```bash
pip install mutmut
mutmut run
```

**Rust: cargo-mutants**:
```bash
cargo install cargo-mutants
cargo mutants
```

### When to Use Mutation Testing

✅ **Use mutation testing for**:
- High-criticality code (payment, auth)
- Core business logic
- Checking test suite quality

❌ **Skip mutation testing for**:
- Simple CRUD operations
- UI components (slow, low value)
- Integration tests (too slow)

## Beyond Line Coverage

### Branch Coverage

Test every conditional branch:

```typescript
function getUserStatus(age: number) {
  if (age < 18) {
    return 'minor'
  } else {
    return 'adult'
  }
}

// Need tests for BOTH branches:
test('returns minor for age < 18', () => {
  expect(getUserStatus(15)).toBe('minor')
})

test('returns adult for age >= 18', () => {
  expect(getUserStatus(25)).toBe('adult')
})
```

### Function Coverage

Every function called at least once.

### Statement Coverage

Every statement executed.

## CI/CD Integration

### Enforce Coverage Thresholds

**GitHub Actions**:
```yaml
- name: Run tests with coverage
  run: npm run test:coverage

- name: Check coverage threshold
  run: |
    COVERAGE=$(cat coverage/coverage-summary.json | jq '.total.lines.pct')
    if (( $(echo "$COVERAGE < 70" | bc -l) )); then
      echo "Coverage $COVERAGE% is below 70%"
      exit 1
    fi
```

### Track Coverage Trends

Track coverage over time with Codecov or Coveralls:
- Prevent coverage regressions
- Comment coverage changes on PRs
