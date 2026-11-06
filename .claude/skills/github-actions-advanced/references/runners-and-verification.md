# Self-hosted runners, trust boundaries and workflow verification

## Self-hosted runners

Use self-hosted runners only for private repositories or trusted workloads. Public repository lets fork PRs run arbitrary code on machine.

### Register a runner

Get current runner version and checksum from repository's **Settings > Actions > Runners > New self-hosted runner** page or `actions/runner` releases page. Do not copy hard-coded version from old docs.

```bash
# Short-lived registration token (needs admin on the repo; use orgs/ORG/... for an org runner)
TOKEN=$(GH_TELEMETRY=false gh api -X POST repos/OWNER/REPO/actions/runners/registration-token --jq .token)  # gh sends usage telemetry by default

mkdir actions-runner && cd actions-runner
curl -fsSLo runner.tar.gz -L "https://github.com/actions/runner/releases/download/v${RUNNER_VERSION}/actions-runner-linux-x64-${RUNNER_VERSION}.tar.gz"
echo "${RUNNER_SHA256}  runner.tar.gz" | sha256sum -c -   # checksum from the release page
tar xzf runner.tar.gz

# Token via env, not argv (visible in ps)
ACTIONS_RUNNER_INPUT_TOKEN="$TOKEN" ./config.sh --unattended \
  --url https://github.com/OWNER/REPO \
  --name "build-$(hostname)" \
  --labels linux,x64,build \
  --ephemeral            # one job per registration; re-register from a clean image

# Long-lived service instead of --ephemeral (less isolation):
# sudo ./svc.sh install && sudo ./svc.sh start
```

Autoscaling on Kubernetes: prefer Actions Runner Controller (ARC) runner scale sets over hand-managed VMs.

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

- Ephemeral runners (or fresh VM/container per job) so one job cannot poison next.
- Run as dedicated unprivileged user. No Docker socket or cloud instance credentials unless job needs them.
- Use runner groups to restrict which repositories and workflows may use runner.
- Restrict egress where possible. Do not mount long-lived production credentials on host.

## Validation vs publication boundary

- Run pull request tests and builds in unprivileged jobs: `permissions: contents: read`, no production secrets, `persist-credentials: false`.
- Put publication (registry push, release, deploy) in separate trusted job: triggered only from protected refs, minimum write permission, own environment.
- Build artifact once from tested commit. Record its identity (digest, attestation). Publishing job consumes that exact artifact and rejects missing or mismatched provenance.
- Never execute pull-request code in `pull_request_target` or `workflow_run` job that holds write tokens or secrets. Treat artifacts downloaded from such runs as untrusted input.
- Named GitHub environment does not configure reviewers by itself. Check its protection rules in repository settings.

## Matrices

Add matrix only for runtimes and operating systems project supports. Do not hide failures with blanket `continue-on-error`. When jobs skip conditionally, confirm required status checks still report. Skipped required job can leave PR unmergeable or, with some setups, silently passing.

## Verify a new or changed workflow

1. Push passing change on topic branch. Confirm every expected check runs and required check names are stable.
2. Push deliberate test failure. Confirm failing check blocks downstream publication jobs.
3. Inspect effective token permissions in job log ("GITHUB_TOKEN Permissions"). Confirm untrusted jobs have no secrets.
4. Review caches and uploaded artifacts as data-exposure paths (no secrets, no `.env`, no credentials in logs).
5. Lint workflow with `actionlint`. Audit it with `zizmor` if available.

Report files changed, runs used as evidence and anything left unverified (for example, protected-environment approvals that need maintainer).
