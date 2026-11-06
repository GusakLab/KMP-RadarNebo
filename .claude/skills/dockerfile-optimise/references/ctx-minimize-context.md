---
title: Keep Build Context Small
impact: MEDIUM
impactDescription: reduces data transfer to builder and speeds up build initialization
tags: ctx, context-size, performance, build-time
---

## Keep Build Context Small

Docker must serialize and transfer whole build context to builder daemon before first instruction runs. In monorepo or large project, repository root as context can mean gigabytes of unrelated code, assets and data, adding seconds or minutes to every build even when nothing relevant changed.

**Incorrect (monorepo root as build context):**

```text
project/
├── services/
│   ├── api/
│   │   ├── Dockerfile
│   │   ├── package.json
│   │   └── src/
│   ├── worker/
│   │   ├── Dockerfile
│   │   ├── package.json
│   │   └── src/
│   └── frontend/
│       ├── Dockerfile
│       ├── package.json
│       └── src/
├── data/            (500 MB of seed data)
├── ml-models/       (2 GB of model weights)
├── docs/
└── scripts/
```

```bash
# Building the API service from monorepo root
docker build -f services/api/Dockerfile .
```

(Docker sends whole 3+ GB monorepo to builder: ML models, seed data, other services, documentation.)

**Correct (context scoped to service directory):**

```bash
# Pass only the service directory as context
docker build -f services/api/Dockerfile services/api/
```

(Docker sends only `services/api/` directory, typically a few MB of source and configuration. Build starts almost instantly instead of waiting on gigabytes.)

**Correct (dedicated context path with shared dependencies):**

When services share code (e.g. common library), scope context to only what is needed:

```text
project/
├── libs/
│   └── shared/
│       ├── package.json
│       └── src/
└── services/
    └── api/
        ├── Dockerfile
        ├── package.json
        └── src/
```

```bash
# Context includes libs/ and services/api/ but not data/, ml-models/, etc.
docker build -f services/api/Dockerfile services/
```

```dockerfile
# services/api/Dockerfile
FROM node:22-slim
WORKDIR /app

# Copy shared library
COPY libs/shared/package.json libs/shared/
RUN cd libs/shared && npm ci

# Copy API service
COPY api/package.json api/package-lock.json api/
RUN cd api && npm ci --production

COPY libs/shared/src/ libs/shared/src/
COPY api/src/ api/src/

WORKDIR /app/api
RUN npm run build
CMD ["node", "dist/server.js"]
```

Reference: [Docker Build Cache - Optimize](https://docs.docker.com/build/cache/optimize/)
