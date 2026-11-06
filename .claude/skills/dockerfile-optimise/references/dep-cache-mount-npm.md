---
title: Use Cache Mount for npm, yarn, and pnpm
impact: MEDIUM-HIGH
impactDescription: reuses downloaded packages across builds, 2-5x faster installs
tags: dep, cache-mount, npm, yarn, pnpm, node
---

## Use Cache Mount for npm, yarn, and pnpm

Node package managers cache downloaded tarballs so repeat installs skip network round-trip. Docker's ephemeral filesystem loses this cache whenever install layer rebuilds. `--mount=type=cache` persists cache directory across builds, so registry serves only new or updated packages.

**Incorrect (re-downloads everything on cache miss):**

```dockerfile
FROM node:22-slim
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
```

**Correct (cache mount preserves npm tarball cache):**

```dockerfile
FROM node:22-slim
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci
```

**Correct — yarn Classic v1 (cache mount with explicit cache folder):**

```dockerfile
FROM node:22-slim
WORKDIR /app
COPY package.json yarn.lock ./
RUN --mount=type=cache,target=/root/.yarn \
    YARN_CACHE_FOLDER=/root/.yarn \
    yarn install --frozen-lockfile
```

(`YARN_CACHE_FOLDER` points yarn's cache at mounted directory. `--frozen-lockfile` ensures install does not modify lockfile. **Note:** pattern applies to Yarn Classic (v1) only. In Yarn Berry/v4, `YARN_CACHE_FOLDER` is ignored when `enableGlobalCache: true` (the default), and `--immutable` replaced `--frozen-lockfile`.)

**Correct — yarn Berry v4 (cache mount with project-local cache):**

```dockerfile
FROM node:22-slim
RUN corepack enable
WORKDIR /app
COPY package.json yarn.lock .yarnrc.yml ./
COPY .yarn/ .yarn/
RUN --mount=type=cache,target=/root/.yarn/berry/cache \
    yarn install --immutable
```

(Yarn Berry uses project-local cache by default.)

**Correct — pnpm (cache mount preserves content-addressable store):**

```dockerfile
FROM node:22-slim
RUN corepack enable
WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile
```

(pnpm's content-addressable store deduplicates packages across projects. Cache mount keeps store, so identical package versions never download twice across builds.)

> **See also:** [`cache-mount-package`](cache-mount-package.md) for general overview of cache mounts across all package managers.

Reference: [Docker Build Cache - Optimize](https://docs.docker.com/build/cache/optimize/)
