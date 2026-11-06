---
title: Never Pass Secrets via ARG or ENV
impact: HIGH
impactDescription: prevents credential exposure in docker history and image metadata
tags: sec, arg, env, secrets, vulnerability
---

## Never Pass Secrets via ARG or ENV

`ARG` and `ENV` both persist secrets in image. `ARG` values land in build history, readable via `docker history --no-trunc`. `ENV` values bake into image configuration, visible in every running container via `/proc/1/environ` or `docker inspect`. Built-in build check `SecretsUsedInArgOrEnv` flags this as security violation.

**Incorrect (secret passed as build argument):**

```dockerfile
FROM python:3.12-slim

# ARG value is recorded in build history in plain text
ARG AWS_SECRET_ACCESS_KEY

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

# The secret is visible via: docker history --no-trunc <image>
RUN AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
    python manage.py collectstatic --noinput

CMD ["gunicorn", "app.wsgi:application"]
```

**Incorrect (secret persisted via ENV):**

```dockerfile
FROM node:22-slim

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

# ENV persists in every layer and in the running container
ENV ADMIN_PASS=s3cret-p@ssw0rd

# Even unsetting it in a later layer does NOT remove it --
# the value is already committed in the ENV instruction's layer
RUN unset ADMIN_PASS

CMD ["node", "server.js"]
```

**Correct (secret mount with environment variable injection):**

```dockerfile
# syntax=docker/dockerfile:1
FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

# Secret is injected as an env var for this RUN instruction only
# It is never written to any layer or image metadata
RUN --mount=type=secret,id=aws-key,env=AWS_SECRET_ACCESS_KEY \
    python manage.py collectstatic --noinput

CMD ["gunicorn", "app.wsgi:application"]
```

Build command (secret from host environment variable):

```bash
docker build \
    --secret id=aws-key,env=AWS_SECRET_ACCESS_KEY \
    -t myapp .
```

**Correct (secret mount from file):**

```dockerfile
# syntax=docker/dockerfile:1
FROM node:22-slim

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

# Secret mounted as a file, read at build time, never persisted
RUN --mount=type=secret,id=admin-pass \
    ADMIN_PASS=$(cat /run/secrets/admin-pass) \
    node scripts/seed-admin.js

CMD ["node", "server.js"]
```

Build command (secret from file):

```bash
docker build \
    --secret id=admin-pass,src=./admin-password.txt \
    -t myapp .
```

### Runtime Secrets

For runtime secrets (not only build time), use orchestrator-level secret management, not `ENV`:

- **Docker Swarm:** `docker service create --secret db_password ...` mounts secrets at `/run/secrets/` inside running containers.
- **Kubernetes:** `Secret` resources mounted as volumes or injected via Secrets Store CSI driver.
- **Docker Compose:** `secrets` top-level key mounts secrets from files or environment into containers at runtime.

Reference: [SecretsUsedInArgOrEnv](https://docs.docker.com/reference/build-checks/secrets-used-in-arg-or-env/)
