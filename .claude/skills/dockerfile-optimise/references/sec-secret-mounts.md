---
title: Use Secret Mounts for Sensitive Data
impact: HIGH
impactDescription: prevents credentials from persisting in image layers
tags: sec, secrets, mount, buildkit
---

## Use Secret Mounts for Sensitive Data

Secrets passed via `COPY` or `ARG` land in image layers, visible via `docker history` or extracted layer tarballs. Anyone with image access (registries, CI caches, downstream consumers) can recover plaintext. BuildKit secret mounts inject sensitive data into one `RUN` instruction, never writing it to layer.

**Incorrect (secret baked into image layer via COPY):**

```dockerfile
FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Secret is written into a layer and persists forever
COPY credentials.json /app/credentials.json

COPY . .
CMD ["python", "app.py"]
```

(Later `RUN rm credentials.json` does not remove it from earlier layer; `docker save` extracts it.)

**Incorrect (secret passed via build argument):**

```dockerfile
FROM node:22-slim

# ARG values are recorded in docker history
ARG DB_PASSWORD
ENV DB_PASSWORD=${DB_PASSWORD}

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

CMD ["node", "server.js"]
```

(`ENV` also persists it into every running container.)

**Correct (file-based secret mount):**

```dockerfile
# syntax=docker/dockerfile:1
FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

# Secret is mounted at /run/secrets/db_password for this RUN only
# It never appears in any image layer
RUN --mount=type=secret,id=db_password \
    cat /run/secrets/db_password | python manage.py migrate

CMD ["python", "app.py"]
```

Build command:

```bash
docker build --secret id=db_password,src=./db_password.txt -t myapp .
```

**Correct (environment variable-based secret mount):**

```dockerfile
# syntax=docker/dockerfile:1
FROM node:22-slim

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

# Secret injected as an environment variable for this RUN only
RUN --mount=type=secret,id=api-key,env=API_KEY \
    node scripts/validate-api-key.js

CMD ["node", "server.js"]
```

Build command:

```bash
# API_KEY already exported from CI/secret store; never type the value inline (shell history)
docker build --secret id=api-key,env=API_KEY -t myapp .
```

(`API_KEY` not in `docker history`, not persisted in image metadata.)

Reference: [Build secrets](https://docs.docker.com/build/building/secrets/)
