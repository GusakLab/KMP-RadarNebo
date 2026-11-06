---
title: Run as Non-Root User
impact: HIGH
impactDescription: reduces container escape blast radius from full root access to unprivileged user scope
tags: sec, user, non-root, privilege
---

## Run as Non-Root User

Container processes run as root (UID 0) by default. Attacker exploiting application and escaping container gains root on host. Non-root user confines process to unprivileged account, limiting damage from container escape, path traversal or arbitrary file write.

**Incorrect (no USER instruction -- container runs as root):**

```dockerfile
FROM node:22-slim

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

EXPOSE 3000
CMD ["node", "server.js"]
```

**Correct (dedicated non-root user with explicit UID/GID):**

```dockerfile
FROM node:22-slim

# Create a dedicated group and user with fixed IDs for consistency
# across rebuilds and environments
RUN groupadd -r -g 1001 appuser && \
    useradd --no-log-init -r -u 1001 -g appuser appuser

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --production

COPY --chown=appuser:appuser . .

# Switch to non-root user for all subsequent instructions and CMD
USER appuser

EXPOSE 3000
CMD ["node", "server.js"]
```

**Correct (multi-stage build with non-root runtime user):**

```dockerfile
# -- Build stage (root is acceptable here) --
FROM golang:1.23 AS build

WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -ldflags="-s -w" -o /bin/server ./cmd/server

# -- Runtime stage --
FROM gcr.io/distroless/static-debian12

COPY --from=build /bin/server /bin/server

# Distroless includes a nonroot user at UID 65532
USER nonroot:nonroot

EXPOSE 8080
ENTRYPOINT ["/bin/server"]
```

(Distroless ships built-in `nonroot` user; no package install or user creation needed.)

### Guidelines

- **Assign explicit UID/GID**, not auto-assignment. Prevents UID drift across rebuilds; volume permissions stay predictable.
- **Avoid installing `sudo`** in container. If process needs elevated privileges at startup (e.g., binding to port 80), drop them after initialization with [`gosu`](https://github.com/tianon/gosu). Do not keep `sudo` at runtime.
- **Set `USER` as late as possible**, so earlier `RUN` instructions needing root (like `apt-get install`) avoid permission errors.
- **Use `--chown` on COPY instructions**: assigns files to non-root user without separate `chown` layer.

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
