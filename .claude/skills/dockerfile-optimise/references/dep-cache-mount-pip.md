---
title: Use Cache Mount for pip
impact: MEDIUM-HIGH
impactDescription: reuses wheel cache across builds, 2-5x faster installs
tags: dep, cache-mount, pip, python
---

## Use Cache Mount for pip

pip caches downloaded packages and compiled wheels in `~/.cache/pip`, so later installs skip download and compilation. Docker discards this cache whenever layer rebuilds. Worse, many Dockerfiles pass `--no-cache-dir` to shrink image, disabling caching entirely. `--mount=type=cache` keeps cache outside image layer: fast rebuilds and small image.

**Incorrect (caching explicitly disabled):**

```dockerfile
FROM python:3.13-slim
WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
```

(`--no-cache-dir` forces pip to download and compile every package from scratch each rebuild. Packages with C extensions like `numpy` or `pandas` take minutes to build each time.)

**Correct (cache mount preserves downloaded wheels):**

```dockerfile
FROM python:3.13-slim
WORKDIR /app
COPY requirements.txt ./
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install -r requirements.txt
```

(pip reuses wheels from persistent cache. New dependency downloads and compiles only that one package. Cache lives in mount, not image layer, so image stays small without `--no-cache-dir`.)

> **See also:** [`cache-mount-package`](cache-mount-package.md) for cache mounts across all package managers including Go modules.

Reference: [Docker Build Cache - Optimize](https://docs.docker.com/build/cache/optimize/)
