---
title: Use Cache Mounts for Package Managers
impact: CRITICAL
impactDescription: eliminates redundant package downloads across builds
tags: cache, cache-mount, package-manager, buildkit
---

## Use Cache Mounts for Package Managers

Without cache mounts, every build missing layer cache re-downloads every package from internet. `RUN --mount=type=cache` persists directory across builds, so package managers reuse downloaded files even when dependency manifest changes. Full download becomes incremental update.

**Incorrect (re-downloads everything on cache miss):**

```dockerfile
FROM python:3.13-slim
WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
```

**Correct (cache mount preserves downloaded packages):**

```dockerfile
FROM python:3.13-slim
WORKDIR /app
COPY requirements.txt ./
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install -r requirements.txt
```

(New dependency downloads only that package. Cached data lives in host mount, not image layer, so images stay small without explicit cleanup commands.)

### Package Manager Cache Directories

| Package Manager | Cache Target | Notes |
|----------------|-------------|-------|
| apt (Debian/Ubuntu) | `/var/cache/apt` + `/var/lib/apt` | Requires disabling `docker-clean` hook; use `sharing=locked` |
| pip (Python) | `/root/.cache/pip` | Remove `--no-cache-dir` flag when using cache mounts |
| npm | `/root/.npm` | Works with `npm ci` and `npm install` |
| yarn Classic v1 | `/root/.yarn` | Set `YARN_CACHE_FOLDER=/root/.yarn` |
| yarn Berry v4 | `/root/.yarn/berry/cache` | Use `--immutable` instead of `--frozen-lockfile` |
| pnpm | `/root/.local/share/pnpm/store` | Content-addressable store deduplicates across projects |
| Go modules | `/go/pkg/mod` | Also cache `/root/.cache/go-build` for compiled artifacts |

### Language-Specific Details

Full incorrect/correct examples and edge cases live in dedicated rules:

- [`dep-cache-mount-apt`](dep-cache-mount-apt.md) — apt/Debian/Ubuntu (requires `docker-clean` removal)
- [`dep-cache-mount-npm`](dep-cache-mount-npm.md) — npm, yarn Classic, yarn Berry, pnpm
- [`dep-cache-mount-pip`](dep-cache-mount-pip.md) — pip/Python

### Go Modules (Quick Reference)

```dockerfile
FROM golang:1.23
WORKDIR /app
COPY go.mod go.sum ./
RUN --mount=type=cache,target=/go/pkg/mod \
    go mod download
```

Reference: [Docker Build Cache - Optimize](https://docs.docker.com/build/cache/optimize/)
