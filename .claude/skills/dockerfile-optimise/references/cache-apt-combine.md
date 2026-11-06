---
title: Combine apt-get update with install
impact: HIGH
impactDescription: prevents stale package index causing install failures
tags: cache, apt-get, debian, ubuntu
---

## Combine apt-get update with install

Separate `RUN` instructions for `apt-get update` and `apt-get install` cache update layer on its own. Adding package to install instruction later reuses stale cached index, which can point to deleted or outdated versions and fail build.

**Incorrect (separate RUN instructions):**

```dockerfile
FROM ubuntu:24.04

# This layer gets cached with today's package index
RUN apt-get update

# Weeks later, adding "jq" uses the stale index from the cached layer above.
# The referenced package versions may no longer exist on the mirror.
RUN apt-get install -y \
    curl \
    ca-certificates \
    jq
```

(Adding or changing packages triggers `E: Unable to locate package` or version mismatch errors.)

**Correct (single RUN with cleanup):**

```dockerfile
FROM ubuntu:24.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    jq \
    && rm -rf /var/lib/apt/lists/*
```

(Update and install always run together, so index is fresh. `--no-install-recommends` skips unneeded suggested packages. Removing `/var/lib/apt/lists/*` cuts layer by 20-40 MB.)

**Alternative (cache mount approach for faster rebuilds):**

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
    jq
```

(`docker-clean` removal is required. Without it, apt deletes cached `.deb` files before cache mount can persist them. Cache mounts keep downloaded `.deb` files and package lists outside image layer, so no `rm -rf` cleanup needed. `sharing=locked` prevents parallel builds from corrupting shared package database.)

Reference: [Docker Build - Best Practices](https://docs.docker.com/build/building/best-practices/)
