---
title: Consolidate Related RUN Instructions
impact: MEDIUM
impactDescription: reduces image layer count and total size
tags: cache, layers, run, consolidation
---

## Consolidate Related RUN Instructions

Each `RUN` creates new layer. Layers are additive: files created in one layer and deleted in later one still take image space. Put related operations in single `RUN`, so temporary files, caches and build artifacts get cleaned up in layer that created them.

**Incorrect (separate RUN instructions, cleanup does not reduce size):**

```dockerfile
FROM ubuntu:24.04

# Layer 1: downloads ~35MB of .deb files into /var/cache/apt
RUN apt-get update

# Layer 2: installs packages, adds ~120MB
RUN apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    curl

# Layer 3: removes lists, but Layer 1 still contains the 35MB
RUN rm -rf /var/lib/apt/lists/*
```

(`rm -rf` in Layer 3 adds whiteout entry that hides files but does not reclaim 35 MB from Layer 1. Image carries all three layers at full size.)

**Correct (single RUN with chained commands):**

```dockerfile
FROM ubuntu:24.04

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        build-essential \
        libpq-dev \
        curl \
    && rm -rf /var/lib/apt/lists/*
```

**Alternative (heredoc syntax for readability):**

```dockerfile
FROM ubuntu:24.04

RUN <<EOF
set -e
apt-get update
apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    curl
rm -rf /var/lib/apt/lists/*
EOF
```

(Heredoc syntax, available since Docker BuildKit, keeps multi-command operations readable in single layer. **Always include `set -e`** at top of heredocs. Unlike `&&` chaining, heredoc runs commands sequentially but does not stop on failure by default. Without `set -e`, failed `apt-get update` silently continues to `apt-get install` with stale index.)

**When NOT to consolidate:** Keep layers separate when they change at different frequencies and you want to preserve cache granularity, such as system packages and application dependencies.

Reference: [Docker Build - Best Practices](https://docs.docker.com/build/building/best-practices/)
