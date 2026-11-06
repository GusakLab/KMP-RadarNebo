---
title: Prefer COPY Over ADD
impact: LOW-MEDIUM
impactDescription: prevents unintended archive extraction and unverified remote fetches in 100% of cases
tags: inst, copy, add, clarity, security
---

## Prefer COPY Over ADD

ADD has two hidden behaviors beyond copying. It auto-extracts recognized archives (tar, gzip, bzip2, xz) and fetches remote URLs. Reader cannot tell whether `ADD archive.tar.gz /app/` extracts on purpose or by accident. COPY does one thing: copy files from build context into image.

**Incorrect (ADD auto-extracts archives -- intent ambiguous):**

```dockerfile
FROM python:3.12-slim

WORKDIR /app
ADD ./app.tar.gz /app/
ADD ./config.yaml /app/
```

(`ADD ./app.tar.gz /app/` silently extracts archive into `/app/`; maintainer cannot tell if intended. `ADD ./config.yaml /app/` does plain copy, because YAML is not recognized archive format. One instruction for two different operations misleads.)

**Correct (COPY for files, explicit extraction when needed):**

```dockerfile
FROM python:3.12-slim

WORKDIR /app
COPY ./app.tar.gz /staging/
RUN tar -xzf /staging/app.tar.gz -C /app/ && rm /staging/app.tar.gz
COPY ./config.yaml /app/
```

(COPY moves archive in, RUN extracts it with visible flags, archive is removed in same layer.)

**Incorrect (ADD for remote URLs -- no checksum verification, no caching control):**

```dockerfile
FROM debian:bookworm-slim

ADD https://example.com/bin/tool-v1.2.3 /usr/local/bin/tool
RUN chmod +x /usr/local/bin/tool
```

(ADD fetches at build time with no checksum verification. Compromised or changed file at URL lands silently in image. Layer caching also works poorly, because ADD always checks remote URL.)

**Correct (curl/wget with explicit checksum verification):**

```dockerfile
FROM debian:bookworm-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*

RUN curl -fsSL -o /usr/local/bin/tool https://example.com/bin/tool-v1.2.3 && \
    echo "<published-sha256>  /usr/local/bin/tool" | sha256sum -c - && \
    chmod +x /usr/local/bin/tool
```

(Tampered file fails sha256sum check and aborts build.)

### When ADD is Appropriate

Use ADD when auto-extraction is intentional and context shows it:

```dockerfile
# ADD with checksum for remote files (BuildKit feature)
ADD --checksum=sha256:24454f830cdb571e2c4ad15481119c43b3cafd48dd869a9b2015d0c6e5c848e4 \
    https://example.com/releases/app-v2.1.0.tar.gz /tmp/

# ADD for intentional local archive extraction in a build stage
FROM debian:bookworm-slim AS extract
ADD rootfs.tar.gz /
```

(`--checksum` (requires BuildKit) gives verified remote fetch in one instruction. Local archive extraction in dedicated build stage is also valid when intent is unambiguous.)

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
