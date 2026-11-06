# Self-hosted runners, trust boundaries and workflow verification

Read this when adding a self-hosted runner, splitting a workflow into validation and publication jobs, or proving a new workflow actually blocks bad changes.

## Self-hosted runners

Use self-hosted runners only for private repositories or trusted workloads. A public repository lets fork pull requests run arbitrary code on the machine.

### Register a runner

Get the current runner version and checksum from the repository's **Settings > Actions > Runners > New self-hosted runner** page or from the `actions/runner` releases page; do not copy a hard-coded version from old docs.

```bash
# Short-lived registration token (needs admin on the repo; use orgs/ORG/... for an org runner)
TOKEN=$(gh api -X POST repos/OWNER/REPO/actions/runners/registration-token --jq .token)

mkdir actions-runner && cd actions-runner
curl -fsSLo runner.tar.gz -L "https://github.com/actions/runner/releases/download/v${RUNNER_VERSION}/actions-runner-linux-x64-${RUNNER_VERSION}.tar.gz"
echo "${RUNNER_SHA256}  runner.tar.gz" | sha256sum -c -   # checksum from the release page
tar xzf runner.tar.gz

./config.sh --unattended \
  --url https://github.com/OWNER/REPO \
  --token "$TOKEN" \
  --name "build-$(hostname)" \
  --labels linux,x64,build \
  --ephemeral            # one job per registration; re-register from a clean image

# Long-lived service instead of --ephemeral (less isolation):
# sudo ./svc.sh install && sudo ./svc.sh start
```

For autoscaling on Kubernetes, prefer Actions Runner Controller (ARC) runner scale sets over hand-managed VMs.

### Target it from a workflow

```yaml
jobs:
  build:
    runs-on: [self-hosted, linux, x64, build]   # all labels must match
    timeout-minutes: 30
    steps:
      - uses: actions/checkout@<pinned-sha> # vX.Y.Z
        with:
          persist-credentials: false
```

Runner hardening:

- Ephemeral runners (or a fresh VM/container per job) so one job cannot poison the next.
- Run as a dedicated unprivileged user; no Docker socket or cloud instance credentials unless the job needs them.
- Use runner groups to restrict which repositories and workflows may use a runner.
- Restrict egress where possible; do not mount long-lived production credentials on the host.

## Validation vs publication boundary

- Run tests and builds for pull requests in unprivileged jobs: `permissions: contents: read`, no production secrets, `persist-credentials: false`.
- Put publication (registry push, release, deploy) in a separate trusted job, triggered only from protected refs, with the minimum write permission and its own environment.
- Build the artifact once from the tested commit and record its identity (digest, attestation). The publishing job consumes that exact artifact and rejects missing or mismatched provenance.
- Never execute pull-request code in a `pull_request_target` or `workflow_run` job that holds write tokens or secrets; treat downloaded artifacts from such runs as untrusted input.
- A named GitHub environment does not configure reviewers by itself; check its protection rules in repository settings.

## Matrices

Add a matrix only for runtimes and operating systems the project supports. Do not hide failures with blanket `continue-on-error`. When jobs are skipped conditionally, confirm required status checks still report (a skipped required job can leave a PR unmergeable or, with some setups, silently passing).

## Verify a new or changed workflow

1. Push a passing change on a topic branch; confirm every expected check runs and the required check names are stable.
2. Push a deliberate test failure; confirm the failing check blocks downstream publication jobs.
3. Inspect effective token permissions in the job log ("GITHUB_TOKEN Permissions") and confirm untrusted jobs have no secrets.
4. Review caches and uploaded artifacts as data-exposure paths (no secrets, no `.env`, no credentials in logs).
5. Lint the workflow with `actionlint`, and audit it with `zizmor` if available.

Report the files changed, the runs used as evidence and anything left unverified (for example, protected-environment approvals that need a maintainer).
