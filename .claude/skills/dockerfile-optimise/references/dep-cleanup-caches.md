---
title: Clean Package Manager Caches in the Same Layer
impact: MEDIUM
impactDescription: reduces image size by removing cached downloads
tags: dep, cleanup, layer-size, apt, pip
---

## Clean Package Manager Caches in the Same Layer

Each `RUN` instruction creates immutable layer. Later layer cannot remove earlier layer's files. It only masks them with whiteout entry; bytes stay in image. Cleaning package manager caches in separate `RUN` has zero effect on image size.

**Incorrect (cleanup in separate layer):**

```dockerfile
FROM ubuntu:24.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    gnupg

RUN rm -rf /var/lib/apt/lists/*
```

(Apt package lists, typically 20-40 MB, get baked into first layer.)

**Correct (cleanup in same layer):**

```dockerfile
FROM ubuntu:24.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    gnupg \
    && rm -rf /var/lib/apt/lists/*
```

(Lists are created and removed in same `RUN`, so never reach final layer.)

**Better (cache mount removes problem entirely):**

```dockerfile
FROM ubuntu:24.04

# Required: disable the Docker-specific hook that wipes apt's cache after install
RUN rm -f /etc/apt/apt.conf.d/docker-clean; \
    echo 'Binary::apt::APT::Keep-Downloaded-Packages "true";' > /etc/apt/apt.conf.d/keep-cache

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    gnupg
```

(Removing `docker-clean` is required. Without it, apt deletes cached `.deb` files before cache mount can persist them. Cache mounts live only in host build cache, never in image layer. No cleanup needed; later builds reuse cache.)

> **See also:** [`cache-mount-package`](cache-mount-package.md) (CRITICAL) for cache mounts across all package managers.

### pip — same principle applies

**Incorrect (cache persists in layer):**

```dockerfile
RUN pip install flask sqlalchemy requests
RUN rm -rf /root/.cache/pip
```

(Pip cache, which can reach hundreds of MB with compiled wheels, sits in first layer.)

**Correct (single layer with cache disabled):**

```dockerfile
RUN pip install --no-cache-dir flask sqlalchemy requests
```

(`--no-cache-dir` stops pip writing cache. Simpler than manual cleanup, but rebuilds must re-download everything.)

**Better (cache mount):**

```dockerfile
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install flask sqlalchemy requests
```

(Cache persists across builds for fast installs, never in image layer.)

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
