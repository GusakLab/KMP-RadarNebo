---
title: Use ARG Before FROM to Parameterize Base Images
impact: MEDIUM
impactDescription: enables CI matrix builds and centralized version management
tags: base, arg, from, parameterization, ci
---

## Use ARG Before FROM to Parameterize Base Images

Hardcoded `FROM` version needs Dockerfile edit to change, so CI/CD pipelines testing several language versions need separate Dockerfiles or sed-based substitutions. `ARG` before `FROM` sets version at build time instead.

**Incorrect (hardcoded version):**

```dockerfile
FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
CMD ["python", "app.py"]
```

**Correct (ARG before FROM, version picked at build time):**

```dockerfile
ARG PYTHON_VERSION=3.12
FROM python:${PYTHON_VERSION}-slim

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
CMD ["python", "app.py"]
```

Build commands:

```bash
# Use the default version (3.12)
docker build -t myapp .

# Override for CI matrix testing
docker build --build-arg PYTHON_VERSION=3.13 -t myapp:py313 .
docker build --build-arg PYTHON_VERSION=3.11 -t myapp:py311 .
```

### Important: ARG Scope with FROM

`ARG` declared before `FROM` is available only in `FROM` itself. Re-declare it after `FROM` to use inside build stage:

```dockerfile
ARG NODE_VERSION=22
FROM node:${NODE_VERSION}-slim

# Re-declare to use inside the stage
ARG NODE_VERSION
RUN echo "Building with Node.js ${NODE_VERSION}"
```

(Without re-declaration after `FROM`, `${NODE_VERSION}` inside stage resolves to empty string.)

Reference: [Dockerfile reference - ARG](https://docs.docker.com/reference/dockerfile/#arg)
