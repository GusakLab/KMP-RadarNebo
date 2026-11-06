---
title: Use .dockerignore to Exclude Unnecessary Files
impact: HIGH
impactDescription: reduces context transfer time and prevents spurious cache invalidation
tags: ctx, dockerignore, context, build-time
---

## Use .dockerignore to Exclude Unnecessary Files

Without `.dockerignore`, Docker sends whole build context directory to builder daemon before build starts: version control history, installed dependencies, test suites, editor configuration, every other file, even ones Dockerfile never references. Extra transfer slows every build. Any change to excluded-but-transferred file invalidates cache for `COPY . .` instructions.

**Incorrect (no .dockerignore, entire project directory sent as context):**

```dockerfile
# Project structure:
#   .git/           (150 MB of history)
#   node_modules/   (400 MB of installed deps)
#   coverage/       (20 MB of test reports)
#   .env            (secrets!)
#   dist/
#   src/
#   Dockerfile

FROM node:22-slim
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .
RUN npm run build
CMD ["node", "dist/server.js"]
```

(Docker transfers ~570 MB of context to builder every build. Editing README, switching git branch or running tests locally changes files in `.git/` or `coverage/`, invalidating `COPY . .` layer and forcing full rebuild of everything after.)

**Correct (comprehensive .dockerignore excludes irrelevant files):**

`.dockerignore`:

```gitignore
# Version control
.git
.gitignore

# Dependencies (installed inside container)
node_modules

# Build output (rebuilt inside container)
dist

# Test and coverage artifacts
test
tests
__tests__
coverage
*.test.js
*.spec.js
jest.config.*

# Documentation
*.md
LICENSE
docs

# Environment and secrets
.env
.env.*

# Editor and IDE files
.vscode
.idea
*.swp
*.swo
*~

# OS files
.DS_Store
Thumbs.db

# CI/CD configuration
.github
.gitlab-ci.yml
.circleci

# Docker files (avoid recursive context)
Dockerfile*
docker-compose*
.dockerignore
```

(Dockerfile unchanged. Docker now transfers only source and configuration files build needs, typically a few MB instead of hundreds. Git history, test result or documentation changes no longer invalidate any build cache layers.)

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
