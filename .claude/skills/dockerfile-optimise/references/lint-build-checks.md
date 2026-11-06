---
title: Enable Docker Build Checks
impact: MEDIUM
impactDescription: prevents 20+ categories of silent Dockerfile defects from reaching production
tags: lint, build-checks, validation, buildkit
---

## Enable Docker Build Checks

Docker build checks catch common Dockerfile anti-patterns: shell-form CMD, secrets leaked through ARG, duplicate stage names and more. Without checks, these issues pass build silently. They surface as runtime failures or security vulnerabilities in production.

**Incorrect (building without checks -- anti-patterns silently pass):**

```dockerfile
# No check directive -- build proceeds even with issues
FROM ubuntu:24.04

ARG DB_PASSWORD
ENV DB_PASSWORD=$DB_PASSWORD

RUN apt-get update && apt-get install -y curl

# Shell form CMD -- PID 1 runs as /bin/sh -c wrapper, breaks signal handling
CMD node server.js
```

(Secrets in `ARG`, missing apt list cleanup and shell-form `CMD` go undetected.)

**Correct (build checks enabled to catch anti-patterns before build):**

Validation-only mode from CLI checks Dockerfile without producing image:

```bash
docker build --check .
```

Or embed check directive in Dockerfile to enforce checks on every build:

```dockerfile
# syntax=docker/dockerfile:1
# check=error=true

FROM ubuntu:24.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

CMD ["node", "server.js"]
```

(`# check=error=true` fails build on any check violation. Warnings become hard errors, so non-compliant images never build.)

### Skipping Specific Checks

Some checks may not fit your case. Skip individual ones with `skip` parameter:

```dockerfile
# check=error=true;skip=JSONArgsRecommended
# syntax=docker/dockerfile:1

FROM ubuntu:24.04

# Shell form is intentional here -- we need variable expansion
CMD echo "Starting $APP_NAME" && exec node server.js
```

Skip several with comma-separated list:

```dockerfile
# check=error=true;skip=JSONArgsRecommended,SecretsUsedInArgOrEnv
```

### Using the Build Arg

Enable checks via `BUILDKIT_DOCKERFILE_CHECK` build argument, without editing Dockerfile:

```bash
# Run checks only (no image output)
docker build --check --build-arg BUILDKIT_DOCKERFILE_CHECK=error=true .

# Build normally but fail on check violations
docker build --build-arg BUILDKIT_DOCKERFILE_CHECK=error=true -t myapp .
```

### Requirements

Build checks require Docker Engine 27.0+ or Docker Desktop 4.33+. Older versions silently ignore `# check` directive and `--check` flag.

Reference: [Docker Build Checks](https://docs.docker.com/build/checks/)
