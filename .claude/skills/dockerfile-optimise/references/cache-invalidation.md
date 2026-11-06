---
title: Avoid Unnecessary Cache Invalidation
impact: HIGH
impactDescription: prevents cascading rebuilds from irrelevant file changes
tags: cache, invalidation, dockerignore, bind-mount
---

## Avoid Unnecessary Cache Invalidation

Docker's `COPY` and `ADD` checksum copied files to decide cache validity. `COPY . .` without `.dockerignore` checksums every build context file: READMEs, test suites, IDE configurations, git history. Changing any of them invalidates layer and rebuilds everything below.

**Incorrect (no .dockerignore, broad COPY):**

```dockerfile
FROM node:22-slim
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --production

# Copies everything: .git/, node_modules/, README.md, docs/, tests/, .env
COPY . .
RUN npm run build
```

(Editing `README.md` or adding test file invalidates `COPY . .` layer and reruns `npm run build` with unchanged application source.)

**Correct (same Dockerfile, plus .dockerignore excluding irrelevant files):**

```dockerfile
# .dockerignore
.git
node_modules
npm-debug.log*
Dockerfile
docker-compose*.yml
.dockerignore
README.md
CHANGELOG.md
LICENSE
docs/
tests/
__tests__/
coverage/
.env
.env.*
.vscode/
.idea/
*.swp
*.swo
```

(Only production-relevant source files enter checksum. Changes to documentation, tests or editor config do not invalidate any build layers.)

**Alternative (specific COPY paths instead of broad wildcard):**

```dockerfile
FROM node:22-slim
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --production

# Copy only the directories that matter for the build
COPY src/ ./src/
COPY public/ ./public/
COPY tsconfig.json ./

RUN npm run build
```

(Explicit source directories give tighter control. Only changes in `src/`, `public/` or `tsconfig.json` invalidate build layer.)

Reference: [Docker Build Cache](https://docs.docker.com/build/cache/)
