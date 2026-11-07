---
title: Go - static binary, cached module and build caches, distroless or scratch runtime
impact: HIGH
impactDescription: 10-30MB images; incremental builds reuse module and compile caches; native cross-compilation
tags: lang, go, static, distroless, cross-compile, buildx
---

## Go - static binary, cached module and build caches, distroless or scratch runtime

A Go service needs neither the toolchain nor a shell at runtime. Without cache mounts, every build re-downloads modules and recompiles the standard library. Building under QEMU emulation for another architecture is many times slower than letting Go cross-compile.

**Incorrect:**

```dockerfile
FROM golang:1.23
WORKDIR /app
COPY . .
RUN go build -o server .
CMD ["./server"]
# ~1GB image with the toolchain; full rebuild on every change; root user
```

**Correct (cross-compiling builder, distroless static runtime):**

```dockerfile
# syntax=docker/dockerfile:1
FROM --platform=$BUILDPLATFORM golang:1.23 AS build
ARG TARGETOS TARGETARCH
WORKDIR /src
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=bind,source=go.mod,target=go.mod \
    --mount=type=bind,source=go.sum,target=go.sum \
    go mod download
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    --mount=type=bind,target=. \
    CGO_ENABLED=0 GOOS=$TARGETOS GOARCH=$TARGETARCH \
    go build -trimpath -ldflags="-s -w" -o /out/server ./cmd/server

FROM gcr.io/distroless/static-debian12:nonroot
COPY --from=build /out/server /server
EXPOSE 8080
ENTRYPOINT ["/server"]
```

Build multi-arch with `docker buildx build --platform linux/amd64,linux/arm64 .`; the builder runs natively and only the Go compiler targets each platform.

**Pitfalls:**
- `CGO_ENABLED=1` (for example sqlite via cgo) needs libc at runtime: use `gcr.io/distroless/base-debian12` instead of `static`, or a pure-Go driver.
- `scratch` has no CA certificates, timezone data or `/etc/passwd`; copy them in explicitly or use `distroless/static`, which includes them.
- Private modules: pass credentials with `--mount=type=secret` or `--mount=type=ssh` and set `GOPRIVATE`, never via `ARG` (see `sec-secret-mounts`).

Reference: [Docker: Go language guide](https://docs.docker.com/guides/golang/)
