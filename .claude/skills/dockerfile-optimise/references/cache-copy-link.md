---
title: Use COPY --link for Cache-Efficient Layer Copying
impact: HIGH
impactDescription: enables layer cache reuse even when preceding layers change
tags: cache, copy-link, buildkit, multi-stage
---

## Use COPY --link for Cache-Efficient Layer Copying

Regular `COPY` stacks files on previous layer, creating dependency chain: if any earlier layer changes, `COPY` layer must rebuild, even with identical content. `COPY --link` creates independent layer not tied to predecessors, so cache reuses it even when earlier layers in same stage changed. Biggest win in multi-stage builds where final images copy artifacts from build stages.

**Incorrect (regular COPY, any upstream change invalidates cache):**

```dockerfile
# syntax=docker/dockerfile:1
FROM golang:1.23 AS build
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go build -o /out/server ./cmd/server

FROM alpine:3.20
RUN apk add --no-cache ca-certificates tzdata
COPY --from=build /out/server /usr/local/bin/server
CMD ["server"]
```

(Changing `RUN apk add` layer forces `COPY --from=build` rebuild even though copied binary is identical.)

**Correct (COPY --link creates independent layer):**

```dockerfile
# syntax=docker/dockerfile:1
FROM golang:1.23 AS build
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go build -o /out/server ./cmd/server

FROM alpine:3.20
RUN apk add --no-cache ca-certificates tzdata
COPY --link --from=build /out/server /usr/local/bin/server
CMD ["server"]
```

(Changing `apk add` line no longer invalidates copied binary layer. BuildKit can also resolve this layer without base image present locally, speeding builds.)

### When to Use --link

Default to `COPY --link` for all `COPY` instructions unless you depend on symlinks or need files merged into existing directory contents. Performance always equals or beats regular `COPY`.

**Static file copies into image:**

```dockerfile
COPY --link package.json package-lock.json ./
COPY --link . .
```

### When NOT to Use --link

- `COPY` relies on symlinks in source or destination (linked copies cannot create symlinks referencing existing content)
- Copied files must merge into directory that must already exist from prior layer

### Requirements

`COPY --link` requires `# syntax=docker/dockerfile:1` directive (or BuildKit with Dockerfile frontend 1.4+).

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
