---
title: Use Distroless or Scratch Images for Production
impact: MEDIUM-HIGH
impactDescription: eliminates shell, package managers, and all non-essential binaries from production
tags: base, distroless, scratch, minimal, security
---

## Use Distroless or Scratch Images for Production

Even slim images ship shell (`/bin/sh`), package manager (`apt`/`apk`) and dozens of system utilities. Attacker with code execution in container uses them for privilege escalation, data exfiltration and lateral movement. Distroless and scratch keep only application and runtime dependencies: nothing left to exploit.

**Incorrect (slim image for compiled binary):**

```dockerfile
FROM golang:1.23 AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o /bin/server ./cmd/server

FROM python:3.12-slim
COPY --from=build /bin/server /bin/server
EXPOSE 8080
CMD ["/bin/server"]
```

(Statically-linked Go binary lands in Python slim image carrying 150MB+ of Python runtime, apt, bash and hundreds of unused utilities. Final image ~170MB instead of ~12MB.)

**Correct (scratch for statically-linked Go binaries):**

```dockerfile
FROM golang:1.23 AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o /bin/server ./cmd/server

FROM scratch
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=build /bin/server /bin/server
EXPOSE 8080
ENTRYPOINT ["/bin/server"]
```

(`scratch` is empty. Image holds only static binary and CA certificates for TLS.)

**Correct (distroless for Java applications):**

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM gcr.io/distroless/java21-debian12
COPY --from=build /src/build/libs/app.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

(`gcr.io/distroless/java21-debian12` holds only JRE and its dependencies. ~220MB, against ~450MB for slim JDK image.)

**Correct (distroless for Node.js applications):**

```dockerfile
FROM node:22-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY . .

FROM gcr.io/distroless/nodejs22-debian12
WORKDIR /app
COPY --from=build /app /app
EXPOSE 3000
CMD ["server.js"]
```

(Distroless Node.js image ships only Node.js runtime. No npm, no shell, no OS utilities.)

### Choosing the Right Minimal Base

| Base Image | Contents | Best For | Size |
|------------|----------|----------|------|
| `scratch` | Nothing | Statically-linked binaries (Go, Rust) | Binary size only |
| `gcr.io/distroless/static-debian12` | CA certs, timezone data, `/etc/passwd` | Static binaries needing TLS + user IDs | ~2MB |
| `gcr.io/distroless/base-debian12` | Above + glibc | Dynamically-linked C/C++ binaries | ~20MB |
| `gcr.io/distroless/java21-debian12` | Above + JRE 21 | Java applications | ~220MB |
| `gcr.io/distroless/nodejs22-debian12` | Above + Node.js 22 | Node.js applications | ~170MB |
| `gcr.io/distroless/python3-debian12` | Above + Python 3 | Python applications | ~50MB |

### When NOT to Use Distroless or Scratch

Distroless has no shell, which makes exec-based debugging of running containers hard. When you need it, use multi-stage build with debug target:

```dockerfile
FROM golang:1.23 AS build
# ... build steps ...

# Debug target with shell access
FROM alpine:3.20 AS debug
COPY --from=build /bin/server /bin/server
ENTRYPOINT ["/bin/server"]

# Production target without shell
FROM scratch AS production
COPY --from=build /bin/server /bin/server
ENTRYPOINT ["/bin/server"]
```

Build with `--target debug` for development, `--target production` for deployment.

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
