---
title: Use Cache Mount for apt Package Manager
impact: MEDIUM-HIGH
impactDescription: eliminates repeated package downloads on layer rebuilds
tags: dep, cache-mount, apt, debian, ubuntu
---

## Use Cache Mount for apt Package Manager

Standard `apt-get install` downloads every package from archive each time layer rebuilds, even when only one dependency changed. `--mount=type=cache` persists package index and downloaded `.deb` files across builds: full re-download becomes incremental update. Especially valuable when installing many system-level libraries for C extension compilation.

**Incorrect (full re-download on any change):**

```dockerfile
FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    libxml2-dev \
    libxslt1-dev \
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*
```

**Correct (cache mount preserves apt data across builds):**

```dockerfile
FROM debian:bookworm-slim

# Required: official Debian/Ubuntu Docker images delete cached .deb files
# after every install via a DPkg post-invoke hook. Remove it first.
RUN rm -f /etc/apt/apt.conf.d/docker-clean; \
    echo 'Binary::apt::APT::Keep-Downloaded-Packages "true";' > /etc/apt/apt.conf.d/keep-cache

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    libxml2-dev \
    libxslt1-dev \
    curl \
    ca-certificates
```

(`docker-clean` removal is essential. Without it, apt deletes cached `.deb` files before cache mount can persist them. `sharing=locked` serializes concurrent cache access, preventing database corruption during parallel builds. `.deb` files and package index live in mount, not image layer, so no `rm -rf /var/lib/apt/lists/*` cleanup is needed.)

> **See also:** [`cache-mount-package`](cache-mount-package.md) for general overview of cache mounts across all package managers.

Reference: [Docker Build Cache - Optimize](https://docs.docker.com/build/cache/optimize/)
