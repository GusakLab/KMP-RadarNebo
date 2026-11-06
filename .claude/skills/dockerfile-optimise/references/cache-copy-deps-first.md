---
title: Copy Dependency Files Before Source Code
impact: CRITICAL
impactDescription: 2-10× build time reduction when only source code changes (the common case)
tags: cache, dependencies, copy, layer-ordering
---

## Copy Dependency Files Before Source Code

Copying whole source tree before installing dependencies makes any source change trigger full reinstall. Copy only dependency manifests (lock files) first: Docker caches expensive install step and re-runs it only when dependencies change.

### Node.js

**Incorrect (full source copy before install):**

```dockerfile
FROM node:22-slim
WORKDIR /app

# Every source file change invalidates the install layer
COPY . .
RUN npm ci --production
```

**Correct (copy manifests, install, then copy source):**

```dockerfile
FROM node:22-slim
WORKDIR /app

# Only changes to package.json or lock file invalidate install
COPY package.json package-lock.json ./
RUN npm ci --production

# Source changes only affect this layer and below
COPY . .
```

### Python

**Incorrect (full source copy before install):**

```dockerfile
FROM python:3.13-slim
WORKDIR /app

# Any source change reinstalls all dependencies
COPY . .
RUN pip install --no-cache-dir -r requirements.txt
```

**Correct (copy requirements first, then source):**

```dockerfile
FROM python:3.13-slim
WORKDIR /app

# Only changes to requirements.txt invalidate the install layer
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Source changes only affect this layer
COPY . .
```

Reference: [Docker Build Cache - Optimize](https://docs.docker.com/build/cache/optimize/)
