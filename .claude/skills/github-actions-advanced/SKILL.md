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

Expert guidance for designing, writing, debugging, and securing **production-grade** GitHub Actions workflows.

---

## Detailed Guide

Read [the detailed guide](references/detailed-guide.md) before executing this skill. It holds the full procedure and reference material (triggers, matrices, reusable workflows, OIDC, caching, releases). For focused work, load the relevant sections; for end-to-end work, read it completely.

Read [runners and verification](references/runners-and-verification.md) when adding a self-hosted runner, separating validation from publication jobs, or verifying that a new workflow blocks failing changes.

Commit SHAs in examples are illustrative. Resolve the SHA of the current release of each action yourself (for example `gh api repos/actions/checkout/git/ref/tags/vX.Y.Z`) and keep the `# vX.Y.Z` comment so Dependabot or Renovate can update it.

## When to Use This Skill

- User mentions GitHub Actions, `.github/workflows`, CI/CD pipelines, runners, jobs, steps, or actions
- User wants to automate builds, tests, deployments, or releases via GitHub
- User asks about matrix builds, reusable workflows, composite actions, or self-hosted runners
- User needs help with OIDC authentication, caching strategies, or secrets management
- User says "set up CI for my repo"
- User asks about workflow security, hardening, or environment protection rules

## When NOT to Use This Skill

- GitLab CI, Argo Workflows or cross-platform pipeline design → `building-ci-pipelines`
- A specific failing run to diagnose from logs → `github-actions-debugger`
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
# npx pin-github-action .github/workflows/*.yml
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

Never place `${{ ... }}` directly inside `run:` when the value can come from
PR metadata, workflow inputs, repository files, matrix JSON, or earlier job
outputs. Put it in `env:` first, validate allowlisted values where possible, and
reference the shell variable with quotes.

### 4. Restrict `pull_request_target` Usage

```yaml
# Only run when a maintainer adds a specific label — prevents untrusted execution
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

### 5. Harden with StepSecurity

```yaml
# Add to every workflow — hardens runner, monitors outbound traffic
- uses: step-security/harden-runner@4d991eb9995541a0b71d1b66f1f98a5f1bef422c  # v2.11.0
  with:
    egress-policy: audit          # Start with 'audit', move to 'block' after confirming allowlist
    allowed-endpoints: >
      api.github.com:443
      registry.npmjs.org:443
      objects.githubusercontent.com:443
```

---

## Done When

- Every workflow declares `permissions:` and each job has `timeout-minutes`.
- Every third-party action is pinned to a full commit SHA with a version comment.
- No `${{ }}` expression from untrusted input is interpolated directly into `run:`.
- Untrusted (pull request) jobs hold no secrets or write tokens; publication runs in a separate trusted job.
- `actionlint` passes, and a passing and a deliberately failing run on a topic branch behave as expected (see [runners and verification](references/runners-and-verification.md)).
