---
title: Use Docker Official Images
impact: HIGH
impactDescription: reduces vulnerability exposure and ensures best-practice configuration
tags: base, official-images, security, trust
---

## Use Docker Official Images

Random community images may carry outdated packages, known vulnerabilities, misconfigured defaults, even intentional malware.

**Incorrect (unverified community image):**

```dockerfile
FROM random-user/node-custom:latest

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

CMD ["node", "server.js"]
```

(`random-user/node-custom` has unverified provenance, unknown update cadence, no guaranteed security scanning. May bundle unneeded tools or carry never-patched known CVEs.)

**Correct (Docker Official Image):**

```dockerfile
FROM node:22-alpine

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

CMD ["node", "server.js"]
```

(`node:22-alpine` is Docker Official Image from Node.js Docker team: regular security updates, Dockerfile best practices, vulnerability scanning on Docker Hub.)

### Docker Hub Trust Tiers

| Tier | Badge | Meaning |
|------|-------|---------|
| **Docker Official Image** | `docker-official-image` | Curated by Docker, reviewed by upstream maintainers, regularly scanned and rebuilt |
| **Verified Publisher** | `verified-publisher` | Verified commercial entity (e.g., Bitnami, Datadog, Nginx Inc.) |
| **Docker-Sponsored Open Source** | `open-source` | Open-source project sponsored through Docker's OSS programme |

Prefer Docker Official Images. If none exists for your runtime, try Verified Publisher images before community images. Always check image description, Dockerfile source and update frequency before trusting any image as base.

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
