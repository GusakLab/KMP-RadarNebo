---
title: Scan built images for vulnerabilities in CI
impact: HIGH
impactDescription: catches known CVEs in base images and OS packages before they ship
tags: sec, scanning, trivy, grype, scout, sarif, ci
---

## Scan built images for vulnerabilities in CI

Minimal base images still gain CVEs with age. Scan built image every build, and on schedule for published images. Makes "rebuild regularly" (see `base-rebuild-regularly`) an enforced gate.

**Incorrect (unpinned scanner action, results only in logs):**

```yaml
- uses: aquasecurity/trivy-action@master        # mutable branch ref
  with:
    image-ref: myapp:latest
```

**Correct (pinned action, fail on fixable critical/high, upload SARIF):**

```yaml
permissions:
  contents: read
  security-events: write   # only for the SARIF upload
steps:
  - name: Scan image
    uses: aquasecurity/trivy-action@<pinned-sha> # vX.Y.Z - resolve the current release SHA
    env:
      TRIVY_DISABLE_TELEMETRY: "true"   # trivy sends usage telemetry by default
      TRIVY_SKIP_VERSION_CHECK: "true"
    with:
      image-ref: ${{ env.IMAGE }}@${{ steps.build.outputs.digest }}
      format: sarif
      output: trivy-results.sarif
      severity: CRITICAL,HIGH
      ignore-unfixed: true
      exit-code: '1'
  - name: Upload results
    if: always()
    uses: github/codeql-action/upload-sarif@<pinned-sha> # v3 or later
    with:
      sarif_file: trivy-results.sarif
```

**Local equivalents:**

```bash
export TRIVY_DISABLE_TELEMETRY=true TRIVY_SKIP_VERSION_CHECK=true   # opt out of trivy telemetry
trivy image --severity CRITICAL,HIGH --ignore-unfixed myapp@sha256:<digest>
grype myapp@sha256:<digest>
docker scout cves myapp@sha256:<digest>
```

**Notes:**
- Scan exact digest you will push, not mutable tag.
- Record accepted findings in scanner ignore file with reason and expiry. Do not lower severity threshold instead.
- Pin every third-party action to full commit SHA; see `github-actions-advanced` skill.

Reference: [Docker Scout](https://docs.docker.com/scout/), [Trivy](https://trivy.dev/)
