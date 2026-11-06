---
title: Use Absolute Paths with WORKDIR
impact: LOW-MEDIUM
impactDescription: prevents path confusion and improves Dockerfile readability
tags: inst, workdir, paths, clarity
---

## Use Absolute Paths with WORKDIR

Each RUN runs in new shell, so `cd /path` inside RUN does not affect later instructions. `RUN cd /path && command` is fragile: forget `&&` chain and command silently runs in wrong directory. Relative WORKDIR paths (e.g., `WORKDIR src`) resolve against previous WORKDIR. Resulting implicit chain is hard to follow and breaks easily in refactoring.

**Incorrect (cd does not persist across RUN instructions):**

```dockerfile
FROM node:22-slim

RUN mkdir -p /usr/src/app
RUN cd /usr/src/app && npm install
RUN npm run build
```

(`RUN npm run build` runs in `/` (default working directory), not `/usr/src/app`. Earlier `cd` affected only its own RUN shell. Build fails: no `package.json` in `/`.)

**Incorrect (relative WORKDIR creates implicit dependency chain):**

```dockerfile
FROM node:22-slim

WORKDIR app
WORKDIR src
WORKDIR ../config
```

(Each resolves against previous: `app` becomes `/app`, `src` becomes `/app/src`, `../config` becomes `/app/config`.)

**Correct (absolute WORKDIR persists across all subsequent instructions):**

```dockerfile
FROM node:22-slim

WORKDIR /usr/src/app

COPY package.json package-lock.json ./
RUN npm ci --production

COPY . .
RUN npm run build

EXPOSE 3000
CMD ["node", "dist/server.js"]
```

(WORKDIR applies to all later RUN, CMD, ENTRYPOINT, COPY and ADD instructions. `npm ci` and `npm run build` both run in `/usr/src/app` with no `cd`. `COPY . .` copies into `/usr/src/app`, because relative `.` resolves against WORKDIR.)

**Correct (WORKDIR with multi-stage builds):**

```dockerfile
# -- Build stage --
FROM golang:1.23 AS build

WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -ldflags="-s -w" -o /bin/server ./cmd/server

# -- Runtime stage --
FROM gcr.io/distroless/static-debian12

WORKDIR /app
COPY --from=build /bin/server ./server

ENTRYPOINT ["./server"]
```

(Each stage has own WORKDIR: build in `/src`, runtime in `/app`.)

### Guidelines

- **Always use absolute paths** with WORKDIR (e.g., `WORKDIR /app`, not `WORKDIR app`).
- **Set WORKDIR once per stage**, near top, after FROM. Avoid changing it within stage unless there is clear reason.
- **WORKDIR creates the directory** if missing. No need for preceding `RUN mkdir -p /app`.
- **Use WORKDIR instead of `cd`** in RUN. For temporary directory change, use `cd` within one chained command: `RUN cd /tmp && wget ... && tar -xzf ...`.

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
