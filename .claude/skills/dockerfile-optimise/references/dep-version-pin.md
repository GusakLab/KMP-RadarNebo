---
title: Pin Package Versions for Reproducibility
impact: MEDIUM
impactDescription: prevents unexpected breakage from upstream package updates
tags: dep, version-pinning, reproducibility, deterministic
---

## Pin Package Versions for Reproducibility

Unpinned packages resolve to whatever version is current at build time. New major or minor release of system dependency can bring breaking API changes, binary incompatibilities or subtle behaviour differences, with no Dockerfile change. Pinned versions give same result on every build.

**Incorrect (unpinned, non-deterministic installs):**

```dockerfile
FROM ubuntu:24.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    nginx \
    postgresql-client \
    && rm -rf /var/lib/apt/lists/*
```

(Installs whatever Ubuntu mirror resolves today. Monday build may get `nginx 1.24`; Friday build gets `nginx 1.26` after archive update, silently breaking reverse-proxy configuration.)

**Correct (pinned to exact versions):**

```dockerfile
FROM ubuntu:24.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3=3.12.3-1ubuntu2 \
    nginx=1.24.0-2ubuntu7 \
    postgresql-client-16=16.2-1ubuntu4 \
    && rm -rf /var/lib/apt/lists/*
```

### pip — use a locked requirements file

**Incorrect (loose constraints allow version drift):**

```text
# requirements.txt
flask>=3.0
sqlalchemy
requests
```

(Each build may resolve different versions. Breaking `sqlalchemy` release can fail application with no code change.)

**Correct (exact versions pinned):**

```text
# requirements.txt
flask==3.0.2
sqlalchemy==2.0.29
requests==2.31.0
```

(Generate pins with `pip freeze > requirements.txt` or `pip-compile`.)

### Discovering Available Versions

Find available apt package versions in target base image:

```bash
docker run --rm ubuntu:24.04 apt-cache policy nginx
```

Availability varies by distribution release. Always verify against your specific base image.

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
