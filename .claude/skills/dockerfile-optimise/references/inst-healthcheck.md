---
title: Define HEALTHCHECK for Container Orchestration
impact: MEDIUM
impactDescription: enables orchestrators to detect and replace unhealthy containers
tags: inst, healthcheck, orchestration, reliability
---

## Define HEALTHCHECK for Container Orchestration

Without HEALTHCHECK, Docker calls container healthy while main process runs. Deadlocked application, exhausted connection pool or unresponsive web server all report "healthy", because process has not exited. Orchestrators like Docker Swarm and Docker Compose keep routing traffic to broken containers.

**Incorrect (no HEALTHCHECK -- container looks healthy even when application is unresponsive):**

```dockerfile
FROM node:22-slim

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

EXPOSE 3000
CMD ["node", "server.js"]
```

(Docker Swarm will not replace it. Docker Compose `depends_on` with `condition: service_healthy` will hang.)

**Correct (HEALTHCHECK with curl for Debian-based images):**

```dockerfile
FROM node:22-slim

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD ["curl", "-f", "http://localhost:3000/health"]

CMD ["node", "server.js"]
```

(Docker probes `/health` every 30 seconds. After 3 consecutive failures (5-second timeout each), container is marked `unhealthy`. 10-second start period lets application initialize before failures count.)

**Correct (HEALTHCHECK with wget for Alpine images that lack curl):**

```dockerfile
FROM node:22-alpine

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD ["wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:3000/health"]

CMD ["node", "server.js"]
```

(Alpine ships wget by default, not curl. `--spider` sends HEAD request without downloading body. `--no-verbose` keeps container logs clean.)

**Correct (HEALTHCHECK with a zero-dependency custom binary):**

```dockerfile
FROM gcr.io/distroless/static-debian12

COPY --from=build /bin/server /bin/server
COPY --from=build /bin/healthcheck /bin/healthcheck

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD ["/bin/healthcheck"]

ENTRYPOINT ["/bin/server"]
```

(Distroless images have no shell, curl or wget. Static healthcheck binary from build stage adds no runtime dependencies.)

### HEALTHCHECK Parameters

| Parameter | Default | Purpose |
|-----------|---------|---------|
| `--interval` | 30s | Time between probes |
| `--timeout` | 30s | Max probe duration before it counts as failed |
| `--start-period` | 0s | Grace period for container initialization (failures during it do not count toward retries) |
| `--retries` | 3 | Consecutive failures needed to mark container unhealthy |

### When NOT to Use HEALTHCHECK

In Kubernetes, prefer Pod spec **liveness and readiness probes** over Dockerfile HEALTHCHECK. Kubernetes does not use Docker HEALTHCHECK. Its probes give finer control: separate liveness, readiness and startup probes, with HTTP, TCP and exec checks. HEALTHCHECK stays essential for **Docker Compose** and **Docker Swarm**, where Kubernetes probes are not available.

Reference: [Dockerfile reference - HEALTHCHECK](https://docs.docker.com/reference/dockerfile/)
