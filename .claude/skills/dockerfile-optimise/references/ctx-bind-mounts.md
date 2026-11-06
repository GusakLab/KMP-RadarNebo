---
title: Use Bind Mounts Instead of COPY for Build-Only Files
impact: HIGH
impactDescription: eliminates large source files from build cache layers
tags: ctx, bind-mount, buildkit, cache-efficiency
---

## Use Bind Mounts Instead of COPY for Build-Only Files

`COPY` persists every copied file into image layer, even files needed only during compilation when final artifact is single binary. For compiled languages like Go, C, C++ or Rust, whole source tree and intermediate object files stay in layer cache permanently. `RUN --mount=type=bind` exposes files to one `RUN` step without writing them to any layer; only build output remains.

### Go

**Incorrect (entire source tree persisted in layer):**

```dockerfile
FROM golang:1.23
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go build -o /app/server ./cmd/server
```

(`COPY . .` layer permanently stores every `.go` file, test file and internal package in build cache. Large Go project can carry 50-200 MB of source unused after compilation.)

**Correct (source mounted temporarily via bind mount):**

```dockerfile
FROM golang:1.23 AS build
RUN --mount=type=bind,target=. \
    --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    go build -o /out/server ./cmd/server

FROM gcr.io/distroless/static-debian12
COPY --from=build /out/server /server
CMD ["/server"]
```

(Source tree is mounted read-only into build step and disappears when `RUN` finishes. Cache mounts persist Go module and build caches across builds. Only compiled binary lands in layer.)

### C / C++

**Incorrect (source and object files persisted in layer):**

```dockerfile
FROM gcc:14
WORKDIR /build
COPY . .
RUN make -j$(nproc) && make install
```

(`COPY . .` layer stores all source files, headers and Makefiles. `RUN make` layer stores intermediate `.o` files next to final binary. Both persist in cache indefinitely.)

**Correct (bind mount keeps source out of layers):**

```dockerfile
FROM gcc:14 AS build
RUN --mount=type=bind,target=/src \
    cd /src && make -j$(nproc) DESTDIR=/out install

FROM debian:bookworm-slim
COPY --from=build /out/ /
CMD ["/usr/local/bin/myapp"]
```

(Source and object files exist only during `RUN` step. Final image holds only installed binaries and libraries from `DESTDIR`.)

Reference: [Docker Build Cache - Optimize](https://docs.docker.com/build/cache/optimize/)
