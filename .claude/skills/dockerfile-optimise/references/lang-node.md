---
title: Node.js - install from the lockfile, ship production deps only, run node directly
impact: HIGH
impactDescription: drops devDependencies and build output intermediates; correct SIGTERM handling
tags: lang, node, npm, pnpm, yarn, multi-stage, signals
---

## Node.js - install from the lockfile, ship production deps only, run node directly

`npm install` can rewrite lockfile and resolve versions CI never tested. Shipping builder's `node_modules` ships devDependencies (TypeScript, test runners). `npm start` makes npm PID 1 in front of node. It may not forward SIGTERM, so containers stop only after kill timeout.

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
| pnpm | `pnpm install --frozen-lockfile` (`--prod`) | pnpm store dir (set explicitly, e.g. `PNPM_STORE_DIR` / `pnpm config set store-dir`) |
| Yarn (Berry) | `yarn install --immutable` | `.yarn/cache` or configured cache folder |

Install pnpm/Yarn at pinned version: Corepack where Node image still provides it, otherwise `npm install -g pnpm@<version>`.

**Pitfalls:**
- Native modules (`bcrypt`, `sharp`) built on Alpine (musl) fail on Debian (glibc), and vice versa. Build and run on same libc.
- Shell-free runtime: `gcr.io/distroless/nodejs<major>-debian12` works with same deps/build stages. Set `CMD ["dist/index.js"]`, since entrypoint is already `node` (see `base-distroless`).
- Next.js: use `output: "standalone"`; copy `.next/standalone`, `.next/static` and `public` instead of whole `node_modules`.

Reference: [Docker: Node.js language guide](https://docs.docker.com/guides/nodejs/)
