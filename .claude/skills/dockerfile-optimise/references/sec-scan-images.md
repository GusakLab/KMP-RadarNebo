---
title: Scan built images for vulnerabilities in CI
impact: HIGH
impactDescription: catches known CVEs in base images and OS packages before they ship
tags: sec, scanning, trivy, grype, scout, sarif, ci
---

## Scan built images for vulnerabilities in CI

A minimal base image still accumulates CVEs as it ages. Scanning the built image on every build, and on a schedule for already-published images, turns "rebuild regularly" (see `base-rebuild-regularly`) into an enforced gate.

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
trivy image --severity CRITICAL,HIGH --ignore-unfixed myapp@sha256:<digest>
grype myapp@sha256:<digest>
docker scout cves myapp@sha256:<digest>
```

**Notes:**
- Scan the exact digest you will push, not a mutable tag.
- Record accepted findings in the scanner's ignore file with a reason and expiry instead of lowering the severity threshold.
- Pin every third-party action to a full commit SHA; see the `github-actions-advanced` skill.

Reference: [Docker Scout](https://docs.docker.com/scout/), [Trivy](https://trivy.dev/)
