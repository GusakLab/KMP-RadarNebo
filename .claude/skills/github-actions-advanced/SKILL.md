---
name: github-actions-advanced
description: >
  Author and harden GitHub Actions workflows - SHA-pinned actions, least-privilege
  permissions, OIDC, reusable workflows, matrices, caching, self-hosted runners,
  environments and releases. Use when writing or changing anything under
  .github/workflows; use github-actions-debugger for a failing run and
  building-ci-pipelines for platform-agnostic pipeline design.
category: devops
risk: safe
source: community
date_added: "2026-05-30"
---

# GitHub Actions Advanced Skill

## Detailed Guide

Read [the detailed guide](references/detailed-guide.md) before executing skill: full procedure and reference (triggers, matrices, reusable workflows, OIDC, caching, releases). Focused work: load relevant sections. End-to-end work: read all.

Read [runners and verification](references/runners-and-verification.md) for self-hosted runners, splitting validation from publication jobs, or proving new workflow blocks failing changes.

Example commit SHAs are illustrative. Resolve each action's current release SHA yourself (for example `GH_TELEMETRY=false gh api repos/actions/checkout/git/ref/tags/vX.Y.Z`; gh sends usage telemetry by default). Keep `# vX.Y.Z` comment so Dependabot or Renovate can update it.

## When to Use This Skill

- Mentions GitHub Actions, `.github/workflows`, CI/CD pipelines, runners, jobs, steps or actions
- Automating builds, tests, deployments or releases via GitHub
- Matrix builds, reusable workflows, composite actions or self-hosted runners
- OIDC authentication, caching strategies or secrets management
- User says "set up CI for my repo"
- Workflow security, hardening or environment protection rules

## When NOT to Use This Skill

- GitLab CI, Argo Workflows or cross-platform pipeline design → `building-ci-pipelines`
- Specific failing run to diagnose from logs → `github-actions-debugger`
- Dockerfile content without GitHub context → `dockerfile-optimise`
- Kubernetes manifests or cluster configuration → `operating-kubernetes`

---

## Security Hardening

### 1. Always Declare Permissions (Least Privilege)

```yaml
# Workflow-level default — restrict everything
permissions:
  contents: read

jobs:
  publish:
    # Job-level override — only expand what's needed
    permissions:
      contents: write        # Only for release/publish jobs
      packages: write        # Only for container push jobs
      pull-requests: write   # Only for PR comment jobs
      id-token: write        # Only for OIDC auth jobs
```

### 2. Pin Third-Party Actions to Full Commit SHA

```yaml
# ❌ UNSAFE — tag can be mutated or hijacked
- uses: actions/checkout@v4

# ✅ SAFE — commit SHA is immutable
- uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683  # v4.2.2

# Tool to automate SHA pinning:
# npx pin-github-action@3.5.2 .github/workflows/*.yml
# or sethvargo/ratchet (a Go binary - NOT the unrelated `ratchet` package on PyPI):
#   go install github.com/sethvargo/ratchet@v0.12.0
#   ratchet pin .github/workflows/*.yml
```

### 3. Prevent Script Injection

```yaml
# ❌ UNSAFE — attacker controls PR title, which gets expanded in shell
- run: echo "${{ github.event.pull_request.title }}"

# ✅ SAFE — pass through environment variable (shell doesn't evaluate it)
- env:
    PR_TITLE: ${{ github.event.pull_request.title }}
  run: echo "$PR_TITLE"

# ✅ SAFE — expressions in if: conditions are evaluated by Actions, not shell
- if: github.event.pull_request.draft == false
  run: echo "Not a draft"
```

Never put `${{ ... }}` directly in `run:` when value can come from PR metadata, workflow inputs, repository files, matrix JSON or earlier job outputs. Move it to `env:` first, validate allowlisted values where possible, reference quoted shell variable.

### 4. Restrict `pull_request_target` Usage

```yaml
# Only run when a maintainer adds a specific label — limits who can trigger it
on:
  pull_request_target:
    types: [labeled]

jobs:
  validate:
    # Double-guard: check label name AND author_association
    if: |
      github.event.label.name == 'safe-to-test' &&
      (github.event.pull_request.author_association == 'COLLABORATOR' ||
       github.event.pull_request.author_association == 'MEMBER' ||
       github.event.pull_request.author_association == 'OWNER')
```

Label gate is not sandbox: commits can land after labeling. Never check out or run PR head code in this job (it holds secrets and write token).

### 5. Optional: StepSecurity Harden-Runner

Third-party SaaS: uploads runner egress/process telemetry to StepSecurity API by default; `audit` mode cannot turn it off. Add only if user opts in. `block` + explicit allowlist + `disable-telemetry: true` stops telemetry upload; setup still sends repo, run and job names to StepSecurity API. Needs v2.11.1+: older versions silently fall back to `audit`.

```yaml
- uses: step-security/harden-runner@46b06cea07f9e64621fdf066ba77ac6915163d43  # v2.22.3
  with:
    egress-policy: block          # 'audit' builds allowlist but always uploads to StepSecurity
    disable-telemetry: true       # only valid with block
    allowed-endpoints: >
      api.github.com:443
      github.com:443
      registry.npmjs.org:443
      objects.githubusercontent.com:443
```

---

## Done When

- Every workflow declares `permissions:`. Each job has `timeout-minutes`.
- Every third-party action pinned to full commit SHA with version comment.
- No `${{ }}` expression from untrusted input interpolated directly into `run:`.
- Untrusted (pull request) jobs hold no secrets or write tokens. Publication runs in separate trusted job.
- `actionlint` passes. Passing run and deliberately failing run on topic branch behave as expected (see [runners and verification](references/runners-and-verification.md)).
