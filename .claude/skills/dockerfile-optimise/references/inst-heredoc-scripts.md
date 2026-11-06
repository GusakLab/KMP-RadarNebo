---
title: Use Heredocs for Multi-Line Scripts
impact: MEDIUM
impactDescription: improves readability and eliminates backslash-continuation errors
tags: inst, heredoc, readability, buildkit, run
---

## Use Heredocs for Multi-Line Scripts

Long RUN instructions with backslash continuations are hard to read, easy to break, and allow no inline comments between continued lines. Missing trailing backslash silently splits one command into two, causing subtle, hard-to-diagnose build failures. BuildKit heredoc syntax (`RUN <<EOF`) allows multi-line scripts in natural shell syntax with full comment support.

**Incorrect (backslash continuation -- fragile, hard to read):**

```dockerfile
FROM python:3.12-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*
```

(Missing `\` after any line silently splits command. Drop backslash after `ca-certificates` and `libpq-dev` runs as standalone command, failing build with confusing error.)

**Correct (heredoc syntax -- readable, resilient):**

```dockerfile
# syntax=docker/dockerfile:1
FROM python:3.12-slim

RUN <<EOF
apt-get update
apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    libpq-dev
rm -rf /var/lib/apt/lists/*
EOF
```

(Each command is own line. Missing backslash inside `apt-get install` list still fails clearly, because next line is separate command. Comments fit anywhere between lines without breaking script.)

**Correct (heredoc with explicit shell and error handling):**

```dockerfile
# syntax=docker/dockerfile:1
FROM node:22-slim

RUN <<EOF
set -eux
apt-get update
apt-get install -y --no-install-recommends tini
rm -rf /var/lib/apt/lists/*
# Verify tini was installed correctly
tini --version
EOF
```

(`set -eux` gives strict error handling: `-e` exits on any failure, `-u` treats unset variables as errors, `-x` prints each command for build log debugging.)

**Correct (COPY heredoc for creating configuration files inline):**

```dockerfile
# syntax=docker/dockerfile:1
FROM nginx:1.27-alpine

COPY <<EOF /etc/nginx/conf.d/default.conf
server {
    listen 8080;
    server_name _;

    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files \$uri \$uri/ /index.html;
    }

    location /health {
        access_log off;
        return 200 "ok";
    }
}
EOF
```

(COPY heredoc creates config file inline, no separate file in build context. Simple configs stay with Dockerfile for self-contained builds.)

**Correct (multiple files in a single layer with heredocs):**

```dockerfile
# syntax=docker/dockerfile:1
FROM python:3.12-slim

COPY <<requirements.txt <<gunicorn.conf.py /app/
Flask==3.0.3
gunicorn==22.0.0
psycopg2-binary==2.9.9
requirements.txt
import multiprocessing

bind = "0.0.0.0:8000"
workers = multiprocessing.cpu_count() * 2 + 1
timeout = 120
gunicorn.conf.py
```

(Several heredocs in one COPY create several files in one layer. Each ends at own delimiter: `requirements.txt` and `gunicorn.conf.py`.)

### Requirements

Heredocs require BuildKit and `# syntax=docker/dockerfile:1` at top of Dockerfile. BuildKit is default builder in Docker Engine 23.0+ and Docker Desktop 4.0+. On older Docker, enable it with `DOCKER_BUILDKIT=1`.

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
