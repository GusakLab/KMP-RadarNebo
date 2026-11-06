---
title: Use SSH Mounts for Private Repository Access
impact: HIGH
impactDescription: enables private repo cloning without exposing SSH keys in image
tags: sec, ssh, git, private-repo
---

## Use SSH Mounts for Private Repository Access

Cloning private repositories during build usually needs SSH key. Copying key in and deleting it later leaves it in original layer: images are append-only, every layer independently extractable. BuildKit SSH mounts forward host SSH agent into one `RUN` instruction, never writing key material to any layer.

**Incorrect (SSH key copied into image layer):**

```dockerfile
FROM node:22-slim

WORKDIR /app

# Key is written into this layer permanently
COPY id_rsa /root/.ssh/id_rsa
RUN chmod 600 /root/.ssh/id_rsa && \
    ssh-keyscan github.com >> /root/.ssh/known_hosts && \
    git clone git@github.com:acme-corp/private-lib.git /app/lib

# Deleting the key does NOT remove it from the COPY layer above
RUN rm -rf /root/.ssh

COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

CMD ["node", "server.js"]
```

**Correct (SSH agent forwarded via mount):**

```dockerfile
# syntax=docker/dockerfile:1
FROM node:22-slim

WORKDIR /app

# SSH agent is forwarded for this RUN only -- no key touches the filesystem
RUN --mount=type=ssh \
    mkdir -p /root/.ssh && \
    ssh-keyscan github.com >> /root/.ssh/known_hosts && \
    git clone git@github.com:acme-corp/private-lib.git /app/lib && \
    rm -rf /root/.ssh

COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

CMD ["node", "server.js"]
```

Build command:

```bash
# Ensure the SSH agent is running and has the required key loaded
eval $(ssh-agent -s)
ssh-add ~/.ssh/id_ed25519

# Pass the SSH agent socket to the build
docker build --ssh default -t myapp .
```

(BuildKit forwards host SSH agent into `RUN` via Unix socket; private key never exists as file in build container. Afterward socket is removed.)

(`ssh-keyscan` trusts whichever host answers at build time. Stricter: check fingerprints once against GitHub's published list, commit that `known_hosts`, and replace keyscan with `--mount=type=bind,source=known_hosts,target=/etc/ssh/ssh_known_hosts`.)

**Correct (multi-stage build isolating the clone):**

```dockerfile
# syntax=docker/dockerfile:1

# -- Clone stage: fetches private dependencies --
# Official, version-pinned image: this stage gets the SSH agent (add digest, see base-pin-versions)
FROM alpine:3.24 AS source

RUN apk add --no-cache git openssh-client
RUN --mount=type=ssh \
    mkdir -p /root/.ssh && \
    ssh-keyscan github.com >> /root/.ssh/known_hosts && \
    git clone --depth 1 git@github.com:acme-corp/private-lib.git /private-lib && \
    rm -rf /root/.ssh

# -- Build stage: compiles application --
FROM node:22-slim AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
COPY --from=source /private-lib ./lib/private-lib
RUN npm run build

# -- Runtime stage: minimal production image --
FROM node:22-slim

WORKDIR /app
COPY --from=build /app/dist ./dist
COPY --from=build /app/node_modules ./node_modules
COPY package.json ./

USER node
EXPOSE 3000
CMD ["node", "dist/server.js"]
```

(SSH mount confined to `source` stage. Final image holds only compiled output: no Git history, no `.ssh` directory, no SSH agent reference.)

### CI/CD Integration

Most CI platforms provide SSH agent forwardable to Docker builds:

```bash
# GitHub Actions example
docker build --ssh default=$SSH_AUTH_SOCK -t myapp .
```

With Docker Compose and BuildKit:

```yaml
services:
  app:
    build:
      context: .
      ssh:
        - default
```

Reference: [Build secrets](https://docs.docker.com/build/building/secrets/)
