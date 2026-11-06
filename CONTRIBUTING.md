# Contributing

How we develop RadarNebo. These policies apply to everyone, people and AI agents alike.

| Policy | Where |
| --- | --- |
| How code is written and reviewed | [CODE_STYLE.md](CODE_STYLE.md) |
| Branches, commits, pull requests, reviews, CI, releases | This file |
| How agents work in this repo | [CLAUDE.md](CLAUDE.md) |
| Server deploy, Android release | [docs/deployment.md](docs/deployment.md), [docs/android-release-build.md](docs/android-release-build.md), [docs/mission-control-android-release.md](docs/mission-control-android-release.md) |

## Ground Rules

1. Code follows [CODE_STYLE.md](CODE_STYLE.md). Reviews check against it.
2. No exceptions to the rules. A violation is fixed, never exempted: no `@Suppress`, baseline, ignore list, excluded
   path or "fix later" note. A rule that is wrong is changed in CODE_STYLE.md first, in its own PR.
3. Make the smallest change that fully solves the task. Finish every part the change touches: callers, tests,
   fixtures, docs.
4. Write all correspondence short and exact: PR descriptions, PR comments, reviews and issue comments. See
   [Correspondence](#correspondence).
5. `main` is always releasable. The server deploys from `main` only.
6. Never commit secrets, keystores or `keystore.properties`.

## Branches

- Branch from `main`. Name the branch `<type>/<short-kebab-topic>`.
- Types in use: `feat/`, `fix/`, `refactor/`, `ci/`, `docs/`, `chore/`. A planned series uses
  `followup/NN-topic`, numbered in merge order.
- One concern per branch. Unrelated cleanup goes on its own branch.
- Keep the branch current by merging `main` into it. Do not rewrite history that others have pulled.

## Commits

Follow the repository's existing style. It is not Conventional Commits.

- The subject is an imperative sentence in sentence case, with no prefix and no trailing period, at most 72
  characters. It says what the change does for the product, not which files changed.
- Add a body only when the subject cannot carry the reason. The body says why, wrapped at 72 columns.
- One logical change per commit. A whole-file reformat or a file move is its own commit.
- Never commit build output, local IDE files or generated screenshots that the change does not need.

```text
Good:  Deploy the server from main only, through a production environment
Good:  Move the server image back to the 21 LTS JRE so the Trivy scan passes
Bad:   fix: updated files
Bad:   WIP
```

## Pull Requests

### Before opening

- CI's test command passes locally (see [Tests and CI](#tests-and-ci)).
- You have reviewed your own diff against the [CODE_STYLE.md checklist](CODE_STYLE.md#review-checklist).
- Every behavior change has a test. A bug fix starts with a test that fails without the fix.

### Size

Aim for under 400 changed lines, not counting golden digests, lockfiles and generated files. Split anything larger
into a series (`followup/01-…`, `followup/02-…`). Each part must still build and pass tests on its own.

### Description

Use the [template](.github/pull_request_template.md): **Why**, **What**, **Test**, **Risk**, **Not done**. Follow the
[correspondence rules](#correspondence). A reviewer should understand the change in one read.

```markdown
## Why
Telegram warnings arrive minutes before alerts.in.ua marks a region. Tracking ignored them.

## What
- Add `TelegramChannelSource` as an extra source behind `CompositeAlertsSource`.
- Extra sources feed tracking only. `/v1/alerts/active.json` still serves the primary source.

## Test
- `./gradlew :mission-control:server:test`: pass.
- New: `TelegramChannelSourceTest`, `CompositeAlertsSourceTest`.

## Risk
t.me markup can change without notice. A parse failure returns `FetchResult.Failed`, and tracking runs on.

## Not done
No retry budget per extra source yet.
```

### Merging

- Open the PR as a draft until CI passes and the description is complete.
- A PR needs one approving review from someone other than the author.
- The author merges, with a merge commit, after approval and green CI.
- An agent-written change has a human owner. The owner reviews the diff before asking anyone else to.

## Reviews

### What to check

In priority order:

1. **Correctness:** wrong results, missed edge cases, broken callers.
2. **Safety:** secrets, input validation, auth, data loss, swallowed errors and cancellation.
3. **Contract:** `/v1` changes are additive. Golden digests changed only on purpose, with the reason stated.
4. **Tests:** risky logic has a test that fails when it breaks.
5. **Style:** [CODE_STYLE.md](CODE_STYLE.md), using its review checklist.
6. **Lean:** dead code, speculative options, duplicated helpers, unneeded dependencies.

Every rule violation in the changed code blocks the merge, whatever its severity label. A violation that you find
outside the PR's scope gets its own fix PR: the reviewer names it, and the author opens that PR right away.

### Comment format

Use one line per finding: location, severity, problem, fix.

```text
<file>:L<line>: <severity>: <problem>. <fix>.
```

| Severity | Meaning | Blocks merge |
| --- | --- | --- |
| `bug:` | Broken behavior. Will cause an incident. | Yes |
| `risk:` | Works today, but fragile: a race, a missing guard, a swallowed error. | Yes, unless the author justifies it |
| `nit:` | Taste, where CODE_STYLE.md sets no rule. The author may ignore it. | No |
| `rule:` | Breaks a CODE_STYLE.md rule. Cite the rule. | Yes |
| `q:` | A genuine question, not a disguised request. | Until answered |

```text
Good:  AlertsWatcher.kt:L48: bug: catch (e: Exception) swallows CancellationException. Rethrow it first.
Good:  AlertsWatcher.kt:L61: rule: `!!` (CODE_STYLE Null safety). Use `?: return`.
Good:  WatcherTest.kt:L30: nit: "works" says nothing. Name the behavior: anOldTrackIsDroppedAfterTheWindow.
Bad:   I noticed that maybe this might potentially swallow cancellation? You might want to consider looking into it.
```

Write a security finding or an architecture disagreement in full sentences, with the reason and a reference.

### Review summary

Start with the verdict, then number the blocking findings:

```text
Verdict: fix 1 and 2 first.
1. AlertsWatcher.kt:L48: bug: …
2. WatcherRoutes.kt:L120: risk: …
Not checked: iOS build.
```

When nothing blocks: `Verdict: Ship.`, plus one line on what you checked.

### Replies

Answer each thread with its outcome: `Fixed in <sha>.`, `Won't fix: <reason>.` or the answer to the `q:`. Do not
resolve a reviewer's `bug:` or `risk:` thread yourself without a fix or the reviewer's agreement.

## Correspondence

This covers PR descriptions, PR comments, review comments, issue comments and release notes. Write the way a busy
senior engineer reads.

1. **Answer first.** Lead with the result, the decision or the ask.
2. **No ceremony.** No greetings, thanks in advance, hedging, recaps or sign-offs. Drop "just", "really",
   "basically", "I think".
3. **Short words, exact names.** Write "fix", not "implement a solution for". Put code names, paths and commands in
   backticks, exactly as they are. Numbers carry their units.
4. **One idea per sentence.** At most 20 words, active voice, imperative for instructions.
5. **Keep the meaning words.** Never drop "not", "never", "no", "only" or "except". Keep the grammar in descriptions;
   cut filler, not articles. Fragments are fine only in one-line review findings.
6. **Payload verbatim.** Quote errors exactly, using the shortest line that decides the issue. Paste commands so they
   can be copied.
7. **Say what was not done.** End with anything skipped, deferred or not checked, plus any risk the reader must know.
8. **Spell it out when it matters.** Use full sentences for security issues, irreversible steps, step order, and
   questions to the reader.

These are the [caveman](.claude/skills/caveman/SKILL.md) and [ponytail](.claude/skills/ponytail/SKILL.md) rules,
adapted for text that others read later.

## Tests and CI

CI runs on every pull request and on every push to `main`:

```bash
./gradlew --continue :core:jvmTest :mission-control:server:test :mission-control:ui:test :app:desktop:test
```

- A UI change also runs `./gradlew :shared:jvmTest` locally (Compose screenshot and contrast tests).
- New non-trivial logic gets a test: a branch, a parser, tracking math, security, or a data write.
- When a golden fails, diff the text written under `build/golden-actual/` against the old output. Update the digest
  only for an intended change, and say why in the PR.
- Never disable or skip a test to get CI green. A flaky test is a bug: fix it in its own PR, right away.
- Never silence a compiler warning. Fix it (see CODE_STYLE.md, Warnings).
- Mobile and desktop builds that CI does not run (`:app:android:assembleDebug`, the iOS app) are the author's job to
  build when the change touches them. Note it under **Test**.

## Dependencies

- Versions live in `gradle/libs.versions.toml` only.
- Dependabot opens one grouped PR per ecosystem each week: Gradle, GitHub Actions, Docker and Docker Compose. Review
  it like any other PR: read the changelogs of major bumps, and run the tests.
- JDK base images stay on LTS releases. A major JDK move is done by hand (Dependabot ignores those).
- Pin GitHub Actions to a full commit SHA, with the version in a comment, as `ci.yml` does.
- A new dependency needs a reason in the PR: why the standard library or an existing dependency is not enough. Do not
  add a dependency for a few lines of code.

## Releases and Deploys

- **Server:** every push to `main` deploys through the `production` environment, which only `main` can use and which
  holds the deploy secrets. Image scans (Trivy) must pass. Details: [docs/deployment.md](docs/deployment.md).
- **Android:** [docs/android-release-build.md](docs/android-release-build.md) and
  [docs/mission-control-android-release.md](docs/mission-control-android-release.md).
- Versions follow SemVer. A breaking change to the `/v1` contract is not allowed. Add a new versioned path instead.

## Security

- Secrets live in GitHub environment secrets and on the server, never in the repository, logs, error messages or
  screenshots.
- Report a vulnerability privately to a maintainer, not in a public issue.
- A change to auth, client keys, rate limits, CORS or input parsing needs a reviewer who checks the security impact
  specifically. Say so in the PR's **Risk** section.
