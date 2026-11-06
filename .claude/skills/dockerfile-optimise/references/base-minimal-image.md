---
title: Use Minimal Base Images
impact: HIGH
impactDescription: 2-18× image size reduction depending on runtime and variant
tags: base, alpine, slim, image-size
---

## Use Minimal Base Images

Full distribution images like `ubuntu` or `debian` bundle hundreds of packages application never uses: compilers, man pages, documentation, system utilities. Result: bigger image, slower pull and deploy, wider attack surface.

**Incorrect (full distribution base image):**

```dockerfile
FROM python:3.12

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

CMD ["python", "app.py"]
```

(`python:3.12` builds on full Debian with gcc, make, man pages and hundreds of system packages. Exceeds 900MB before any application code.)

**Correct (slim variant drops non-essential packages):**

```dockerfile
FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

CMD ["python", "app.py"]
```

(`python:3.12-slim` strips compilers, documentation and rarely-used system packages. Base layer drops from ~900MB to ~150MB, no code changes.)

**Correct (Alpine variant for maximum size reduction):**

```dockerfile
FROM python:3.12-alpine

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

CMD ["python", "app.py"]
```

(`python:3.12-alpine` uses Alpine Linux with musl libc. Base layer ~50MB, but some Python packages with C extensions may need extra build dependencies.)

### Approximate Base Image Sizes

| Variant            | Base Size  | Use Case                                |
|--------------------|------------|-----------------------------------------|
| `python:3.12`      | ~900MB     | Development or when full toolchain needed |
| `python:3.12-slim` | ~150MB     | Production default for most applications  |
| `python:3.12-alpine` | ~50MB    | Maximum size reduction for compatible apps |

### When NOT to Use Minimal Images

Alpine uses musl libc, not glibc. Libraries relying on glibc-specific behaviour (certain scientific computing packages, DNS resolution edge cases, pre-compiled binary wheels) may fail to build or behave differently there. Then use `-slim`: keeps glibc compatibility, still drops non-essential packages.

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
