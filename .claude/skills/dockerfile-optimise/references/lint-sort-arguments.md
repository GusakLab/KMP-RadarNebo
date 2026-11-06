---
title: Sort Multi-Line Arguments Alphabetically
impact: LOW
impactDescription: prevents package duplication and simplifies code review
tags: lint, readability, maintainability, apt-get
---

## Sort Multi-Line Arguments Alphabetically

Unordered package lists in `RUN` hide duplicates, bloat diffs and cause frequent merge conflicts. Packages added ad hoc over months can appear two or three times unnoticed. Alphabetical order makes duplicates obvious and diffs show exactly what changed.

**Incorrect (unsorted packages with a hidden duplicate):**

```dockerfile
FROM python:3.12-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    libpq-dev \
    curl \
    build-essential \
    curl \
    wget \
    libffi-dev \
    && rm -rf /var/lib/apt/lists/*
```

(`curl` appears twice, hard to spot unsorted.)

**Correct (alphabetically sorted, one package per line):**

```dockerfile
FROM python:3.12-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    curl \
    git \
    libffi-dev \
    libpq-dev \
    wget \
    && rm -rf /var/lib/apt/lists/*
```

(Trailing `&& rm -rf /var/lib/apt/lists/*` cleanup stays visually separate.)

### Why This Matters for Code Review

Unsorted lists produce noisy diffs on add or remove:

```diff
 RUN apt-get update && apt-get install -y --no-install-recommends \
-    git \
-    libpq-dev \
     curl \
     build-essential \
+    jq \
+    libpq-dev \
+    git \
     wget \
```

Same change on sorted list is clean single-line diff:

```diff
 RUN apt-get update && apt-get install -y --no-install-recommends \
     build-essential \
     curl \
     git \
+    jq \
     libpq-dev \
     wget \
```

### Applies Beyond apt-get

Same rule fits any multi-line argument list:

```dockerfile
# pip packages
RUN pip install --no-cache-dir \
    celery==5.4.0 \
    flask==3.1.0 \
    gunicorn==23.0.0 \
    redis==5.2.1 \
    sqlalchemy==2.0.36

# apk packages (Alpine)
RUN apk add --no-cache \
    curl \
    git \
    openssh-client \
    python3
```

### COPY and Multi-Source Instructions

Sort sources in multi-source COPY too, for same readability gain:

```dockerfile
COPY \
    docker-entrypoint.sh \
    healthcheck.sh \
    migrate.sh \
    /usr/local/bin/
```

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
