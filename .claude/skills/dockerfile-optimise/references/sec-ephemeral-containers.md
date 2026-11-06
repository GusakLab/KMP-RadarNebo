---
title: Design Ephemeral, Stateless Containers
impact: MEDIUM
impactDescription: enables safe container replacement and horizontal scaling
tags: sec, ephemeral, stateless, twelve-factor
---

## Design Ephemeral, Stateless Containers

Containers keeping application state, uploads or logs on local filesystem cannot be safely stopped, replaced or horizontally scaled. Crash or reschedule loses data; replicas see diverging state. Twelve-Factor App: containers are disposable processes, startable or stoppable any moment.

**Incorrect (state written to the container filesystem):**

```dockerfile
FROM node:22-slim

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

# Application writes uploads, sessions, and logs to local directories
# All data is lost when the container stops or is replaced
RUN mkdir -p /app/uploads /app/sessions /app/logs

EXPOSE 3000
CMD ["node", "server.js"]
```

**Correct (state externalized, container is disposable):**

```dockerfile
# syntax=docker/dockerfile:1
FROM node:22-slim

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

# /data/uploads is expected to be provided as an external mount
# at runtime (docker run -v or compose volumes). Do not use
# the VOLUME instruction — it creates anonymous volumes that
# are hard to track and prevents downstream images from
# modifying this directory.
RUN mkdir -p /data/uploads && chown node:node /data/uploads

USER node
EXPOSE 3000
CMD ["node", "server.js"]
```

(Mount `/data/uploads` at runtime via `docker run -v` or compose volumes, never via `VOLUME` Dockerfile instruction (untracked anonymous volumes).)

**Correct (Docker Compose with externalized state):**

```yaml
services:
  app:
    build: .
    environment:
      - S3_BUCKET=myapp-uploads
      - REDIS_URL=redis://cache:6379
      - DATABASE_URL=postgres://db:5432/myapp
    volumes:
      # Named volume for data that must persist locally (e.g., processing queue)
      - upload-queue:/data/uploads
    deploy:
      replicas: 3

  cache:
    image: redis:7-alpine

  db:
    image: postgres:16-alpine
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  upload-queue:
  pgdata:
```

### Ephemeral Container Checklist

| Pattern | Incorrect | Correct |
|---------|-----------|---------|
| File uploads | Container filesystem | Object storage (S3, GCS, MinIO) |
| Sessions | In-memory or filesystem store | Redis, Memcached, or database |
| Application logs | File inside container | Stdout/stderr (12-factor logging) |
| Cache | Local filesystem | Redis, Memcached, or CDN |
| Scheduled state | Cron job writing to local file | Database or message queue |

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
