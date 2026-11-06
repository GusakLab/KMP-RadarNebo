---
title: Enable SBOM and Provenance Attestations
impact: MEDIUM
impactDescription: reduces CVE audit time from hours of manual inspection to seconds via machine-readable SBOM
tags: sec, sbom, provenance, attestations, supply-chain
---

## Enable SBOM and Provenance Attestations

Without attestations, no machine-readable record of image packages or how image was built. SBOM (Software Bill of Materials) and provenance attestations embed this metadata in image index. Enables automated scanning and verifiable build provenance.

**Incorrect (no attestation metadata):**

```bash
docker build -t registry.example.com/myapp:latest .
docker push registry.example.com/myapp:latest
```

**Correct (SBOM and provenance attestations enabled):**

```bash
docker buildx build \
    --sbom=true \
    --provenance=true \
    --push \
    -t registry.example.com/myapp:latest .
```

(SBOM lists all OS packages and application dependencies. Provenance records build source, builder identity, build parameters. Both attach to image index as OCI referrers.)

**Correct (inspecting attestations after push):**

```bash
# View the SBOM for a pushed image
docker buildx imagetools inspect \
    registry.example.com/myapp:latest \
    --format '{{ json .SBOM }}'

# View provenance data
docker buildx imagetools inspect \
    registry.example.com/myapp:latest \
    --format '{{ json .Provenance }}'
```

(Attestations sit beside image manifest in registry. Query without pulling image layers.)

**Correct (CI pipeline with attestations and Docker Scout scanning):**

```dockerfile
# syntax=docker/dockerfile:1
FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

USER nobody
CMD ["gunicorn", "app.wsgi:application"]
```

```bash
# Build, attest, and push
docker buildx build \
    --sbom=true \
    --provenance=mode=max \
    --push \
    -t registry.example.com/myapp:v1.2.3 .

# Scan the image using the attached SBOM
docker scout cves registry.example.com/myapp:v1.2.3
```

(`mode=max` captures full build definition, Dockerfile source included. Docker Scout finds known CVEs from SBOM without unpacking every layer.)

### Key Details

- **Attestations require `--push`**: Stored as extra manifests in image index. `--load` cannot attach them (produces single-platform image without index).
- **SBOM generators**: BuildKit defaults to Syft, scanning filesystem into CycloneDX or SPDX SBOM.
- **Provenance levels**: `mode=min` records basic build metadata. `mode=max` adds full build source and reproducibility data.
- **Registry support**: Stored as OCI artifacts, supported by most modern registries (Docker Hub, GitHub Container Registry, Amazon ECR, Google Artifact Registry).

Reference: [Build attestations](https://docs.docker.com/build/metadata/attestations/)
