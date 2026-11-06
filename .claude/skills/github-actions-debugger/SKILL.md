---
name: github-actions-debugger
description: "Diagnose a failing or slow GitHub Actions run from its logs and workflow YAML, find the root cause (secrets, permissions, versions, cache, runner environment, flaky tests) and propose a minimal diff. Use when a specific workflow run failed or misbehaves; use github-actions-advanced to author or harden workflows."
category: devops
risk: safe
source: community
source_type: community
date_added: "2026-06-25"
author: Owais
tags: [github-actions, ci-cd, devops, debugging, workflows]
---

# GitHub Actions Pipeline Debugger

Find why a run failed, prove it, and change as little as possible.

## 1. Get the evidence

```bash
gh run list --workflow <file.yml> --limit 10          # recent runs: is it new, flaky or permanent?
gh run view <run-id>                                   # jobs, steps, conclusion
gh run view <run-id> --log-failed                      # logs of failed steps only
gh run view <run-id> --job <job-id> --log > job.log    # full log for one job
gh run download <run-id> -n <artifact>                 # test reports, screenshots
```

Read the workflow file at the **same commit** as the run (`gh run view <run-id> --json headSha`), not the current branch tip.

Logs can contain tokens, internal hostnames and customer data that GitHub did not mask. Redact before pasting them anywhere else.

## 2. Classify the failure

| Symptom in log | Likely cause | Check |
|---|---|---|
| `Resource not accessible by integration`, 403 on push/comment | `GITHUB_TOKEN` permissions | Job `permissions:`; "GITHUB_TOKEN Permissions" section at the top of the job log; fork PRs get read-only tokens |
| Empty variable, `required` input missing, auth failure | Secret not available | Secret name/scope (repo, env, org); fork PRs and Dependabot runs do not receive normal secrets; reusable workflows need `secrets: inherit` or explicit passing |
| `Unable to resolve action`, `node12/node16 ... deprecated` | Action ref or runtime | Move to the action's current major release and pin it by full SHA |
| Works locally, fails in CI | Environment drift | Runner image (`ubuntu-latest` moved), tool versions, timezone/locale, CPU/memory, parallelism |
| Passes on rerun | Flaky test or transient network | Compare several runs; quarantine with an issue rather than adding blind retries |
| `No space left on device` | Runner disk | Clean caches/images, use a larger runner |
| Job killed after N minutes | `timeout-minutes` or hung process | Step timing in the log; add a per-step timeout |
| Cache restored but install fails | Stale or poisoned cache | Change the cache key or run once without cache |
| `OIDC ... id-token` errors | Missing `id-token: write` or trust policy mismatch | Job permissions; cloud role trust condition (`sub` claim: repo, branch, environment) |
| Workflow did not run at all | Trigger/filter mismatch | `on:` branches/paths filters, `if:` conditions, workflow disabled, `[skip ci]` in the commit message |

## 3. Reproduce and narrow

```bash
gh run rerun <run-id> --failed            # rerun only failed jobs (tests flakiness)
gh run rerun <run-id> --failed --debug    # rerun with runner and step debug logging
gh workflow run <file.yml> --ref <branch> -f key=value   # manual dispatch for workflow_dispatch
```

Debug logging can also be enabled by setting the repository variable (or secret) `ACTIONS_STEP_DEBUG=true`. Lint the workflow with `actionlint`; `act` can run some jobs locally but does not reproduce GitHub-hosted runners exactly.

## 4. Fix with a minimal diff

Example - an outdated action runtime:

```diff
       - name: Checkout
-        uses: actions/checkout@v2
+        uses: actions/checkout@<full-commit-sha> # vX.Y.Z (current release)
```

Resolve the SHA from the action's release (for example `gh api repos/actions/checkout/git/ref/tags/vX.Y.Z --jq .object.sha`; if the tag is annotated, dereference it to the commit). Do not hard-code a "latest" major from memory; check the action's releases page.

Example - a secret that was never passed to the step:

```diff
       - name: Deploy
         run: npm run deploy
+        env:
+          DEPLOY_API_KEY: ${{ secrets.DEPLOY_API_KEY }}
```

Rules:

- Never fix auth by hard-coding a token or widening `permissions:` beyond what the failing step needs.
- Never move untrusted pull-request code into `pull_request_target` to "get secrets".
- Do not add `continue-on-error: true` to make a red check green.
- Separate transient failures (registry outage, rate limit) from structural ones before changing YAML.

## Done when

- The root cause is stated with the log line that proves it.
- The fix is a minimal diff, and a rerun (or a new run on a topic branch) passes.
- For flaky failures, several consecutive runs pass or the flaky test is tracked in an issue.

## Related skills

- `github-actions-advanced` - authoring and hardening workflows.
- `building-ci-pipelines` - pipeline design, caching and parallelization across CI platforms.
- `debugging-and-error-recovery` - when the failure is in the application code, not the pipeline.
