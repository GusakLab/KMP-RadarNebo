---
title: Rebuild Images Regularly with --pull
impact: MEDIUM
impactDescription: prevents running images with known vulnerabilities
tags: base, rebuild, pull, security-patches
---

## Rebuild Images Regularly with --pull

Docker caches base image locally. Later builds reuse that copy even after upstream releases security patches, OS updates or critical bug fixes. Over time it piles up unpatched vulnerabilities fixed months ago.

**Incorrect (building without --pull):**

```bash
docker build -t myapp:latest .
```

**Correct (force-pull latest base image on every build):**

```bash
docker build --pull -t myapp:latest .
```

(`--pull` makes Docker check registry for newer version of every `FROM` image and download it before building, so base image has latest security patches.)

**Correct (full freshness for security-critical builds):**

```bash
docker build --pull --no-cache -t myapp:latest .
```

(`--no-cache` re-executes every layer from scratch, so `apt-get update`, `apk upgrade` and similar commands fetch latest package lists instead of reusing stale cached metadata.)

### Recommended Rebuild Strategy

| Environment     | Frequency          | Flags                    |
|-----------------|--------------------|--------------------------|
| Development     | On dependency change | `--pull`                |
| CI / Staging    | Every build         | `--pull`                |
| Production      | Weekly or on CVE    | `--pull --no-cache`     |

### Automating Rebuilds in CI

Schedule weekly CI pipeline that rebuilds and redeploys production images with `--pull`. Catches base image updates without manual intervention:

```yaml
# Example: GitHub Actions scheduled rebuild
on:
  schedule:
    - cron: '0 3 * * 1'  # Every Monday at 03:00 UTC
jobs:
  rebuild:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: docker build --pull -t myapp:latest .
      - run: docker push myapp:latest
```

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
