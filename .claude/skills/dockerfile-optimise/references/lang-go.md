---
title: Go - static binary, cached module and build caches, distroless or scratch runtime
impact: HIGH
impactDescription: 10-30MB images; incremental builds reuse module and compile caches; native cross-compilation
tags: lang, go, static, distroless, cross-compile, buildx
---

## Go - static binary, cached module and build caches, distroless or scratch runtime

Go service needs no toolchain or shell at runtime. Without cache mounts, each build re-downloads modules and recompiles standard library. QEMU emulation for another architecture is many times slower than Go cross-compiling.

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

Build multi-arch with `docker buildx build --platform linux/amd64,linux/arm64 .`. Builder runs natively; only Go compiler targets each platform.

**Pitfalls:**
- `CGO_ENABLED=1` (e.g. sqlite via cgo) needs libc at runtime. Use `gcr.io/distroless/base-debian12` instead of `static`, or pure-Go driver.
- `scratch` lacks CA certificates, timezone data and `/etc/passwd`. Copy them in, or use `distroless/static`, which has them.
- Private modules: pass credentials with `--mount=type=secret` or `--mount=type=ssh` and set `GOPRIVATE`. Never via `ARG` (see `sec-secret-mounts`).

Reference: [Docker: Go language guide](https://docs.docker.com/guides/golang/)
