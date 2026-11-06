---
title: Pin Base Image Versions with Digests
impact: HIGH
impactDescription: guarantees reproducible builds and prevents supply chain attacks
tags: base, version-pinning, digest, reproducibility, security
---

## Pin Base Image Versions with Digests

Mutable tags like `:latest` or `:3.12` can resolve to different image over time. Build that worked yesterday may break today after silent base image update. Worse, compromised tag could inject malicious code into supply chain with no Dockerfile change.

**Incorrect (mutable tag, non-deterministic builds):**

```dockerfile
FROM python:latest

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

CMD ["python", "app.py"]
```

(`python:latest` moves with every new upstream Python release.)

**Better (pinned minor version, still mutable):**

```dockerfile
FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

CMD ["python", "app.py"]
```

(`python:3.12-slim` limits drift to patch-level updates within 3.12 series. Image still changes when Debian packages update or security patches are applied.)

**Correct (immutable digest, fully reproducible):**

```dockerfile
FROM python:3.12-slim@sha256:a866731a6b71c4a194a845d86e06568725e430ed21271324873d91eb9e2a0c81

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

CMD ["python", "app.py"]
```

(`@sha256:...` digest is immutable: every build, anywhere, anytime, gets exact same base image. Tag `python:3.12-slim` stays for readability; digest takes precedence.)

### The Trade-off: Reproducibility vs. Automatic Updates

Pinned digest opts out of automatic security patches. If upstream fixes critical CVE, builds keep old, vulnerable image until you manually update digest.

To manage it:

- **Use Docker Scout** or similar to monitor pinned images for new vulnerabilities and get automated remediation pull requests.
- **Automate digest updates** in CI: periodically resolve latest digest for pinned tag, open PR when it changes.
- **Pin in production Dockerfiles** where reproducibility matters most. Use mutable tags in development or CI images where freshness matters more.

### Finding the Digest

```bash
# Get the digest for a specific tag
docker inspect --format='{{index .RepoDigests 0}}' python:3.12-slim

# Or pull and inspect in one step
docker pull python:3.12-slim
docker images --digests python
```

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
