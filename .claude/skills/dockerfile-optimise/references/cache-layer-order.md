---
title: Order Layers by Change Frequency
impact: CRITICAL
impactDescription: prevents full rebuilds on every code change
tags: cache, layer-ordering, build-time, invalidation
---

## Order Layers by Change Frequency

Docker reuses cached layer while instruction and inputs are unchanged. Invalidated layer invalidates every later layer too, and all must rebuild. Frequently-changing instructions (like copying application source) before stable ones (like installing system packages) force Docker to redo stable work every build.

**Incorrect (frequently-changing layer before stable layers):**

```dockerfile
FROM node:22-slim

WORKDIR /app

# Copying source code first means ANY file edit invalidates
# the npm install layer below, triggering a full reinstall
COPY . .

RUN npm install --production

RUN npm run build

EXPOSE 3000
CMD ["node", "dist/server.js"]
```

**Correct (layers ordered from least to most frequently changing):**

```dockerfile
FROM node:22-slim

WORKDIR /app

# 1. System-level setup (rarely changes)
RUN apt-get update && apt-get install -y --no-install-recommends \
    tini \
    && rm -rf /var/lib/apt/lists/*

# 2. Dependency manifest (changes only when deps change)
COPY package.json package-lock.json ./

# 3. Dependency install (cached until manifests change)
RUN npm ci --production

# 4. Application source (changes most frequently)
COPY . .

RUN npm run build

EXPOSE 3000
ENTRYPOINT ["tini", "--"]
CMD ["node", "dist/server.js"]
```

(Source changes invalidate only `COPY . .` layer and build step. Dependency installation stays cached.)

Reference: [Docker Build Cache - Optimize](https://docs.docker.com/build/cache/optimize/)
