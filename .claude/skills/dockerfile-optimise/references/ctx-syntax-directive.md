---
title: Use Syntax Directive for Latest BuildKit Features
impact: MEDIUM
impactDescription: unlocks cache mounts, secret mounts, heredocs, and other BuildKit features
tags: ctx, syntax-directive, buildkit, parser
---

## Use Syntax Directive for Latest BuildKit Features

Syntax directive makes BuildKit pull specific Dockerfile parser image from registry, not whatever version ships with local Docker daemon. Without it, features depend entirely on daemon version on build machine, so builds may silently fail or behave differently across environments.

**Incorrect (no syntax directive, limited to local daemon parser):**

```dockerfile
FROM golang:1.23 AS build
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go build -o /app/server ./cmd/server

FROM gcr.io/distroless/static-debian12
COPY --from=build /app/server /server
CMD ["/server"]
```

(On daemon older than feature's introduction, cannot use `RUN --mount=type=cache`, `RUN --mount=type=secret`, `RUN --mount=type=bind` or heredocs. May work on developer machine, fail in CI with different Docker version.)

**Correct (syntax directive pins latest stable parser):**

```dockerfile
# syntax=docker/dockerfile:1
FROM golang:1.23 AS build
WORKDIR /app

COPY go.mod go.sum ./
RUN --mount=type=cache,target=/go/pkg/mod \
    go mod download

RUN --mount=type=bind,target=. \
    --mount=type=cache,target=/root/.cache/go-build \
    go build -o /out/server ./cmd/server

FROM gcr.io/distroless/static-debian12
COPY --from=build /out/server /server
CMD ["/server"]
```

(`# syntax=docker/dockerfile:1` pulls latest `1.x` parser from Docker Hub every build, so all stable BuildKit features work regardless of local Docker daemon version.)

### Features Unlocked by the Syntax Directive

| Feature | Syntax | Purpose |
|---------|--------|---------|
| Cache mounts | `RUN --mount=type=cache,target=/path` | Persist package manager caches across builds |
| Bind mounts | `RUN --mount=type=bind,target=/path` | Mount context files without creating layer |
| Secret mounts | `RUN --mount=type=secret,id=mysecret` | Inject secrets without leaking them into layers |
| SSH mounts | `RUN --mount=type=ssh` | Forward SSH agent for private repo access |
| Heredocs | `RUN <<EOF` | Multi-line scripts without backslash continuation |

### Important Notes

Syntax directive is parser directive, not comment. It **must** be very first line of Dockerfile, before any comments, `ARG` instructions or blank lines. Any content before it makes Docker treat it as regular comment and ignore it:

```dockerfile
# This comment causes the syntax directive below to be ignored!
# syntax=docker/dockerfile:1
FROM node:22-slim
```

Tag `docker/dockerfile:1` follows semver: always resolves to latest stable `1.x.x` release. Bug fixes and new features arrive with no Dockerfile change.

Reference: [Dockerfile frontend](https://docs.docker.com/build/dockerfile/frontend/)
