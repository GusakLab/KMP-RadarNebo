---
title: Use pipefail for Piped RUN Commands
impact: MEDIUM
impactDescription: prevents silent failures in piped commands
tags: lint, pipefail, shell, error-handling
---

## Use pipefail for Piped RUN Commands

In pipeline (`cmd1 | cmd2`), shell checks only last command's exit code. If earlier command fails but last succeeds, whole `RUN` counts as success. Failed download, broken compile step or corrupted stream passes silently. Image ends up with missing or incomplete content.

**Incorrect (piped command without pipefail -- silent failure):**

```dockerfile
FROM ubuntu:24.04

RUN apt-get update && apt-get install -y --no-install-recommends wget

# If wget fails (DNS error, 404, timeout), sort receives empty input
# but exits 0 -- build continues silently with an empty allowlist
RUN wget -O - https://example.com/allowed-hosts.txt | sort -u > /etc/allowed-hosts
```

**Correct (pipefail causes the pipeline to fail on any command error):**

```dockerfile
FROM ubuntu:24.04

RUN apt-get update && apt-get install -y --no-install-recommends wget

# set -o pipefail makes the pipeline return the exit code of the
# first command that fails, not just the last command
RUN set -o pipefail && wget -O - https://example.com/allowed-hosts.txt | sort -u > /etc/allowed-hosts
```

**Correct (explicit shell form for images where /bin/sh is dash):**

```dockerfile
FROM debian:12-slim

RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl gnupg

# dash (the default /bin/sh on Debian) does not support pipefail.
# Explicitly invoke bash to ensure the option is available.
# Install the repository signing key instead of piping a setup script into a shell.
RUN ["/bin/bash", "-c", "set -o pipefail && mkdir -p /etc/apt/keyrings && curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg"]

RUN echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_22.x nodistro main" \
      > /etc/apt/sources.list.d/nodesource.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*
```

Never pipe downloaded script straight into shell (`curl ... | bash`). Prefer package repository with signing key, or pinned release verified against published checksum.

(Exec form `["/bin/bash", "-c", "..."]` guarantees bash, whatever `/bin/sh` points to.)

### Combining with set -e

For maximum safety, combine `pipefail` with `set -e` (exit on error) in multi-line scripts:

```dockerfile
RUN set -euo pipefail && \
    curl -fsSL https://example.com/gpg-key.asc | gpg --dearmor -o /usr/share/keyrings/example.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/example.gpg] https://example.com/repo stable main" \
      > /etc/apt/sources.list.d/example.list && \
    apt-get update && \
    apt-get install -y --no-install-recommends example-package && \
    rm -rf /var/lib/apt/lists/*
```

### Shell Compatibility

`set -o pipefail` is bash feature. Default `/bin/sh` on Debian-based and Ubuntu-based images is `dash`, which lacks it. Shell-form `RUN` (not exec form) uses `/bin/sh` by default. Use exec form with explicit bash, or set default shell for later instructions:

```dockerfile
SHELL ["/bin/bash", "-o", "pipefail", "-c"]

# All subsequent RUN instructions now use bash with pipefail enabled
RUN curl -fsSL https://example.com/release.tar.gz | tar -xz -C /opt
```

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
