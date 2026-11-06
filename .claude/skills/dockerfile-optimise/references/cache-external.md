---
title: Use External Cache for CI/CD Builds
impact: HIGH
impactDescription: shares cache across ephemeral CI runners, eliminating cold builds
tags: cache, ci-cd, registry-cache, github-actions
---

## Use External Cache for CI/CD Builds

CI/CD runners are typically ephemeral: each job starts with no local Docker cache and rebuilds every layer from scratch. Export build cache to external backend (like container registry), import it on later builds, and unchanged layers get reused across runners and pipelines.

**Incorrect (no cache sharing in CI):**

```yaml
# .github/workflows/build.yml
name: Build
on: push

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Build image
        run: docker build -t myapp:${{ github.sha }} .
```

**Correct (registry-backed cache with GitHub Actions):**

```yaml
# .github/workflows/build.yml
name: Build
on: push
permissions:
  contents: read
  packages: write   # push image + cache to ghcr.io only

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push
        uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: ghcr.io/${{ github.repository }}:${{ github.sha }}
          cache-from: type=registry,ref=ghcr.io/${{ github.repository }}:buildcache
          cache-to: type=registry,ref=ghcr.io/${{ github.repository }}:buildcache,mode=max
```

(`mode=max` exports cache for all intermediate layers, not just final image. Any runner then pulls cached layers from registry: 5-minute build drops under 30 seconds when only source code changed.)

**Direct docker build equivalent:**

```bash
# Build with external cache (CLI usage)
docker buildx build \
  --cache-from type=registry,ref=ghcr.io/myorg/myapp:buildcache \
  --cache-to type=registry,ref=ghcr.io/myorg/myapp:buildcache,mode=max \
  --tag ghcr.io/myorg/myapp:latest \
  --push .
```

(Same `--cache-from` and `--cache-to` flags work with any CI system, e.g. GitLab CI, CircleCI, Jenkins, pointed at accessible registry.)

Reference: [Docker Build Cache - Optimize](https://docs.docker.com/build/cache/optimize/)
