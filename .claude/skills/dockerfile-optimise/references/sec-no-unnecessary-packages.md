---
title: Avoid Installing Unnecessary Packages
impact: MEDIUM
impactDescription: reduces attack surface and image size
tags: sec, packages, minimal, attack-surface
---

## Avoid Installing Unnecessary Packages

Every installed package is potential vulnerability vector. Debug utilities, editors, network diagnostics and documentation packages help in development but serve no purpose in production. They grow image size and build time and, critically, widen attack surface with binaries attacker can leverage after initial access.

**Incorrect (debug and convenience tools in a production image):**

```dockerfile
FROM python:3.12-slim

WORKDIR /app

RUN apt-get update && apt-get install -y \
    vim \
    curl \
    wget \
    net-tools \
    iputils-ping \
    htop \
    strace \
    tcpdump \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

CMD ["gunicorn", "app.wsgi:application"]
```

(None needed to serve application. `tcpdump` and `strace` give attacker network sniffing and process tracing. `curl` and `wget` fetch more payloads after initial compromise. Adds ~80MB and dozens of CVE-trackable packages.)

**Correct (only runtime dependencies, no recommends):**

```dockerfile
FROM python:3.12-slim

WORKDIR /app

# Install only the runtime libraries the application actually needs
# --no-install-recommends prevents apt from pulling suggested packages
RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq5 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

USER nobody
CMD ["gunicorn", "app.wsgi:application"]
```

(Only `libpq5`: runtime library psycopg2 needs for PostgreSQL.)

**Correct (debug tools isolated in a development stage):**

```dockerfile
# syntax=docker/dockerfile:1

# -- Base stage: shared runtime configuration --
FROM python:3.12-slim AS base

WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq5 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

# -- Development stage: includes debug tools --
FROM base AS development

RUN apt-get update && apt-get install -y --no-install-recommends \
    vim \
    curl \
    net-tools \
    strace \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir debugpy ipdb

# debugpy has no auth (attach = code execution): publish on host loopback only
CMD ["python", "-m", "debugpy", "--listen", "0.0.0.0:5678", "manage.py", "runserver"]

# -- Production stage: minimal runtime only --
FROM base AS production

USER nobody
CMD ["gunicorn", "app.wsgi:application"]
```

Build commands:

```bash
# Development (includes debug tools)
docker build --target development -t myapp:dev .
docker run -p 127.0.0.1:5678:5678 myapp:dev   # never -p 5678:5678 (all interfaces)

# Production (no debug tools, minimal attack surface)
docker build --target production -t myapp:prod .
```

### Audit Checklist

Before shipping to production, verify no unnecessary packages:

```bash
# List all installed packages in the image
docker run --rm myapp:prod dpkg -l

# Check for common debug tools that should not be present
docker run --rm myapp:prod which curl wget vim strace tcpdump 2>&1
```

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
