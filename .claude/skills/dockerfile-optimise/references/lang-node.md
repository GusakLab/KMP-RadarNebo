---
title: Node.js - install from the lockfile, ship production deps only, run node directly
impact: HIGH
impactDescription: drops devDependencies and build output intermediates; correct SIGTERM handling
tags: lang, node, npm, pnpm, yarn, multi-stage, signals
---

## Node.js - install from the lockfile, ship production deps only, run node directly

`npm install` can rewrite the lockfile and resolve different versions than CI tested. Shipping the builder's `node_modules` includes devDependencies (TypeScript, test runners). Starting via `npm start` puts npm in front of node as PID 1, which may not forward SIGTERM, so containers stop only after the kill timeout.

**Incorrect:**

```dockerfile
FROM node:22
WORKDIR /app
COPY . .
RUN npm install && npm run build
CMD ["npm", "start"]
# Non-reproducible install, devDependencies shipped, npm as PID 1, root user
```

**Correct (separate deps, build and runtime stages):**

```dockerfile
# syntax=docker/dockerfile:1
FROM node:22-slim AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --omit=dev

FROM node:22-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npm run build

FROM node:22-slim
ENV NODE_ENV=production
WORKDIR /app
COPY --from=deps  --chown=node:node /app/node_modules ./node_modules
COPY --from=build --chown=node:node /app/dist ./dist
COPY --chown=node:node package.json ./
USER node
EXPOSE 3000
CMD ["node", "dist/index.js"]
```

**Package-manager variants:**

| Manager | Install command | Cache mount target |
|---|---|---|
| npm | `npm ci` (`--omit=dev` for runtime deps) | `/root/.npm` |
| pnpm | `pnpm install --frozen-lockfile` (`--prod`) | the pnpm store dir (set it explicitly, e.g. `PNPM_STORE_DIR` / `pnpm config set store-dir`) |
| Yarn (Berry) | `yarn install --immutable` | `.yarn/cache` or the configured cache folder |

Install pnpm/Yarn at a pinned version (Corepack where the Node image still provides it, otherwise `npm install -g pnpm@<version>`).

**Pitfalls:**
- Native modules (`bcrypt`, `sharp`) compiled on Alpine (musl) fail on Debian (glibc) and vice versa. Build and run on the same libc.
- For a shell-free runtime, `gcr.io/distroless/nodejs<major>-debian12` works with the same deps/build stages; set `CMD ["dist/index.js"]` because the entrypoint is already `node` (see `base-distroless`).
- Next.js: use `output: "standalone"` and copy `.next/standalone`, `.next/static` and `public` instead of the whole `node_modules`.

Reference: [Docker: Node.js language guide](https://docs.docker.com/guides/nodejs/)
