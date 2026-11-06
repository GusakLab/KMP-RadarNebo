# RadarNebo: Agent Instructions

RadarNebo maps air alerts and tracks drones and missiles over Ukraine. It is a Kotlin Multiplatform repo: Compose
Multiplatform clients (Android, iOS, desktop), a Ktor server with a desktop console, and an Android server monitor.

These instructions bind every agent session in this repository. [AGENTS.md](AGENTS.md) points other agents here.

## Policies (always on)

1. **Code style.** [CODE_STYLE.md](CODE_STYLE.md) governs every line of Kotlin and Gradle Kotlin DSL that you write,
   change or review. Read the sections a task touches before editing. Run its
   [review checklist](CODE_STYLE.md#review-checklist) before handing work back.
2. **No exceptions.** A rule violation is fixed, never exempted: no `@Suppress`, baseline, ignore list, excluded path
   or "fix later" note. Fix a violation inside the task's scope in the same change. Report one outside it, and open
   its fix PR right away when asked to. A wrong rule is changed in CODE_STYLE.md first, in its own PR. See
   [Violations](CODE_STYLE.md#violations).
3. **Development policy.** [CONTRIBUTING.md](CONTRIBUTING.md) covers branches, commits, pull requests, reviews, CI,
   dependencies and releases.
4. **Ponytail, standing opt-in.** Every session runs the `ponytail` skill at level `full`. Make the smallest change
   that fully solves the task, finished across callers, tests, fixtures and docs. End each reply with what was
   skipped or not checked. Never cut validation, error handling, security or the tests a change needs.
5. **Caveman, standing opt-in.** Chat replies use the `caveman` skill's voice.
6. **Correspondence.** PR descriptions, PR comments, review comments, issue comments and release notes follow
   [CONTRIBUTING.md#correspondence](CONTRIBUTING.md#correspondence). The caveman rules apply, with grammar kept. This
   overrides caveman's plain-prose exemption for persisted text. Review comments use the `caveman-review` line format,
   and review summaries use the `ponytail-review` verdict.
7. **Commits** follow [CONTRIBUTING.md#commits](CONTRIBUTING.md#commits): an imperative sentence subject, not
   Conventional Commits.
8. **Code comments, KDoc and docs** use plain, concise prose, as
   [CODE_STYLE.md#documentation-and-comments](CODE_STYLE.md#documentation-and-comments) says. Caveman's compressed
   voice does not apply to them.

This file is the explicit request that the `caveman`, `caveman-review`, `ponytail` and `ponytail-review` skills
require. A user can switch caveman or ponytail off for a session ("normal mode", "stop ponytail"). Policies 1-3 and 6-8
still apply.

Precedence: the user's explicit instruction in the session, then this file, then CODE_STYLE.md and CONTRIBUTING.md,
then a skill's own defaults. When a skill disagrees with the repo docs, the repo docs win.

## Commands

| Purpose | Command |
| --- | --- |
| CI gate | `./gradlew --continue :core:jvmTest :mission-control:server:test :mission-control:ui:test :app:desktop:test` |
| UI screenshot and contrast tests | `./gradlew :shared:jvmTest` |
| iOS klibs (compile on Linux too) | `./gradlew :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64` |
| Android app | `./gradlew :app:android:assembleDebug` |
| Desktop app | `./gradlew :app:desktop:run` |
| Server / server console | `./gradlew :mission-control:server:run` / `./gradlew :mission-control:ui:run` |

Incremental builds hide warnings from compiled-before files. To see every warning, run the compile tasks with
`--rerun`.

## Repo Facts That Bite

- Module dependency directions and server layers are fixed: [CODE_STYLE.md#modules](CODE_STYLE.md#modules).
- `/v1` responses change additively only. Golden tests pin the API and simulation output by digest. Never edit a
  digest to make a test pass; diff `build/golden-actual/` and explain an intended change in the PR.
- One server runs per machine: a server started later takes over from the running one.
- Swift calls exactly one Kotlin symbol, `MainViewControllerKt.MainViewController`. Keep its name and file.
- Koin `singleOf(::X)`, `factoryOf(::X)` and `viewModelOf(::X)` resolve every constructor parameter from the
  container, including defaulted ones. After adding a parameter, check the binding. `DependencyInjectionTest` checks
  the graph.
- Versions live only in `gradle/libs.versions.toml`. The server deploys from `main` only.
- Leave unrelated uncommitted files alone. Commit only the paths your task changed.

## Skill Routing

Project skills live in [`.claude/skills/`](.claude/skills/), copied from dot-agents. Provenance and licenses are in
[`.claude/THIRD_PARTY_SKILLS.md`](.claude/THIRD_PARTY_SKILLS.md). Do not edit a copied skill here.

### Always

| When | Use |
| --- | --- |
| Every session | `ponytail` (full) and `caveman` |
| Before a non-trivial edit: state assumptions and success criteria | `andrej-karpathy` |
| After writing or editing production code, before handing back | `clean-code-guard`, judged against CODE_STYLE.md |

### Kotlin and platform

| When | Use |
| --- | --- |
| Coroutines, Flow, StateFlow, cancellation, `runTest` | `kotlin-coroutines` |
| Nullability, removing `!!`, Java platform types | `kotlin-null-safety` |
| Gradle Kotlin DSL, `buildSrc`, type-safe builders | `kotlin-dsl-patterns` |
| Compose UI for phones and tablets: touch, navigation, platform conventions | `mobile-design` |
| Library behavior at the pinned versions (Kotlin, Compose Multiplatform, Ktor, Koin, Exposed, AGP) | `source-driven-development` |

### Plan and build

| When | Use |
| --- | --- |
| Requirements are vague or span several modules | `spec-driven-development`, then `planning-and-task-breakdown` |
| A change over ~100 lines or across several components | `incremental-implementation` |
| New behavior, or a reproducible bug | `test-driven-development` |
| Designing or rebalancing a test suite | `testing-strategies` |
| A bug fix or small behavior change | `surgical-patch` |
| Unknown cause, intermittent failure, regression | `investigate-first`, then `debugging-and-error-recovery` |
| Prove that work is done, validation only | `verify-and-stop` |

### Design and quality

| When | Use |
| --- | --- |
| Routine and class construction, naming, defensive checks | `code-complete` |
| Readability cleanup of working code | `code-simplification` |
| A named code smell, or a structural refactor | `refactoring-guru` |
| Choosing a design pattern | `design-patterns` |
| Auditing patterns already in the code | `gof-patterns` |
| Deciding which class owns a responsibility | `grasp-knowledge` |
| Server layering, ports and adapters | `architecture-patterns` |
| Module APIs in `core` and `shared`, interface contracts | `api-and-interface-design` |
| The HTTP API: `/v1` contract, versioning, errors | `designing-apis` |
| Exposed and SQLite: schema, indexes, migrations | `database-design` |
| Auth, client keys, rate limits, CORS, input parsing, secrets | `security-and-hardening` |

### Review

| When | Use |
| --- | --- |
| Review a PR, branch or diff | `code-review` (standards: CODE_STYLE.md and CONTRIBUTING.md) with the `code-review-and-quality` rubric. Write findings in the `caveman-review` format and end with the `ponytail-review` verdict. |
| Is a change over-engineered or bloated? | `ponytail-review` |
| Quality or bloat audit of the whole repo | `ponytail-audit` |
| List deferred `shortcut:` comments | `ponytail-debt` |

### Delivery

| When | Use |
| --- | --- |
| Commits, branches, tags, versions | `git-workflow-and-versioning`, with the commit style from CONTRIBUTING.md |
| Writing or changing `.github/workflows` | `github-actions-advanced` |
| A failing or slow CI run | `github-actions-debugger` |
| `mission-control/server/Dockerfile` | `dockerfile-optimise` |
| ADRs, README, `docs/`, KDoc | `documentation-and-adrs` |
