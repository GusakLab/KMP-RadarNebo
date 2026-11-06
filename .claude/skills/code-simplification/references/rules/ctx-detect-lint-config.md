---
title: Check for Linting and Formatting Configs
impact: CRITICAL
tags: ctx, linting, formatting, tooling
---

## Check for Linting and Formatting Configs

Simplifications ignoring lint and format configs fail CI or introduce inconsistent formatting. Before changing code, always detect and respect ESLint, Prettier, rustfmt, Biome and similar configs.

**Incorrect (ignoring project's Prettier config):**

```javascript
// Simplified code uses single quotes, but project's .prettierrc specifies double quotes
const message = 'Hello, world';
const items = ['one', 'two', 'three'];

function greet(name) {
  return 'Hello, ' + name;
}
```

**Correct (matching project's Prettier config with double quotes):**

```javascript
// .prettierrc: { "singleQuote": false }
const message = "Hello, world";
const items = ["one", "two", "three"];

function greet(name) {
  return "Hello, " + name;
}
```

**Config files to check:**

- JavaScript/TypeScript: `.eslintrc.*`, `.prettierrc.*`, `biome.json`, `.editorconfig`
- Rust: `rustfmt.toml`, `.rustfmt.toml`
- Python: `pyproject.toml`, `.flake8`, `.black`, `ruff.toml`
- Go: `.golangci.yml`

Run `npm run lint` or equivalent before committing.
