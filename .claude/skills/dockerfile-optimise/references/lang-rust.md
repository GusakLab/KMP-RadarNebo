---
title: Rust - cache registry and target dir, copy one binary into a minimal runtime
impact: HIGH
impactDescription: incremental release builds instead of full recompiles; 10-40MB runtime images
tags: lang, rust, cargo, musl, distroless, scratch
---

## Rust - cache registry and target dir, copy one binary into a minimal runtime

Release builds of a dependency tree take minutes; without caching, every source edit recompiles all crates. The Rust toolchain image is over 1GB and must not ship.

**Incorrect:**

```dockerfile
FROM rust:1
WORKDIR /app
COPY . .
RUN cargo build --release
CMD ["./target/release/app"]
# Toolchain shipped; every change recompiles every crate
```

**Correct (glibc build, distroless/cc runtime):**

```dockerfile
# syntax=docker/dockerfile:1
FROM rust:<version>-slim AS build
WORKDIR /app
COPY . .
# target/ is a cache mount, so copy the binary out within the same RUN
RUN --mount=type=cache,target=/usr/local/cargo/registry \
    --mount=type=cache,target=/app/target \
    cargo build --release --locked && \
    cp target/release/app /app/app-bin

FROM gcr.io/distroless/cc-debian12:nonroot
COPY --from=build /app/app-bin /app
ENTRYPOINT ["/app"]
```

**Alternative (fully static musl binary on scratch):**

```dockerfile
FROM rust:<version>-alpine AS build
RUN apk add --no-cache musl-dev
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/usr/local/cargo/registry \
    --mount=type=cache,target=/app/target \
    cargo build --release --locked && cp target/release/app /app/app-bin

FROM scratch
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=build /app/app-bin /app
USER 10001:10001
ENTRYPOINT ["/app"]
```

**When NOT to use the musl/scratch variant:**
- Crates linking C libraries (OpenSSL, some allocators) may not build statically, and musl's default allocator can slow allocation-heavy services; prefer `rustls` or the glibc/distroless variant.

For heavy dependency trees, `cargo-chef` (separate "recipe" stage) caches compiled dependencies in a normal image layer, which also works on CI builders without persistent cache mounts.

Reference: [Docker: Rust language guide](https://docs.docker.com/guides/rust/)
