# Commit Messages and Local History Cleanup

Read when writing non-trivial commit message or tidying unpushed commits before push or PR.

## Repository convention first

Before writing, check `git log --oneline -20` plus any `CONTRIBUTING.md`, commitlint config or commit-msg hook. Match existing convention (Conventional Commits, ticket prefixes, sign-off, required trailers). Use defaults below only when no convention exists.

## Structure

```
<type>(<optional scope>): <subject>

<body: what changed and why, wrapped at ~72 columns>

<footer: trailers such as Refs:, Closes:, BREAKING CHANGE:, Co-Authored-By:>
```

### Subject line

- Type prefix such as `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore` or `revert`. Add `!` after type or scope for breaking change.
- Imperative mood: "add", not "added" or "adds".
- About 50 characters (hard limit 72), no trailing period.
- Be specific: "fix race in payment retry", not "fix bug" or "update stuff".
- Describe outcome, not mechanics. Omit file names unless they are the point.

### Body

Add body when diff does not explain itself:

- **Why** needed: problem, root cause, constraint.
- **What** changed, high level. Diff shows details.
- **Side effects**, risks, rejected alternatives.
- How you verified it, if not obvious.

Blank line between subject and body. Bullets for lists.

### Footer

```
Refs: #123
Closes: #789
BREAKING CHANGE: /users now requires authentication
Co-Authored-By: Name <email>
```

Add attribution trailers only when repository or user instructions require them.

## Examples

Bug fix with root cause:

```
fix: remove leaked event listeners on unmount

Listeners added on mount were never removed, so memory grew with each
navigation and the app slowed down after 10+ page transitions.

Root cause: the useEffect hook returned no cleanup function. Return one
that removes every listener registered by the component.

Closes: #892
```

Breaking change with migration notes:

```
feat!: switch API client to v2 response format

BREAKING CHANGE: responses changed from { data, status: "ok" } to
{ result, success: true }.

Migration:
1. Read `result` instead of `data`.
2. Check the `success` boolean instead of the `status` string.
3. Error responses now include `error.code`.

Refs: #1001
```

Security fix:

```
fix: sanitize profile bio and comments before rendering

User-supplied HTML in the bio, comment and status fields was rendered
unescaped, which allowed stored XSS. Sanitize on write and escape on
render.

Severity: high. No known exploitation.
Closes: #778
```

## When to commit

Commit when:

- Logical unit of work complete, tests pass.
- Stable checkpoint before switching tasks or starting risky step.
- One bug fixed (one commit per bug).

Do not commit:

- Code that does not build, or failing tests (unless test documents known issue user asked to record).
- Unrelated changes mixed together.
- Debug output or temporary comments.
- Secrets.

## Cleaning up local history (non-interactive)

Rewrite only commits that have **not been pushed** to shared branch. Once others may have built on a commit, add new commit instead. Never force-push shared branch. On own feature branch, rewrite pushed commits only with user's agreement; push with `--force-with-lease`.

Agents cannot drive interactive editor. Use non-interactive forms:

```bash
# See what is unpushed
git log --oneline @{upstream}..HEAD        # or: origin/main..HEAD

# Fold a correction into an earlier commit
git commit --fixup=<sha>
GIT_SEQUENCE_EDITOR=: git rebase -i --autosquash <base>

# Reword only the latest commit
git commit --amend -m "fix: precise subject"

# Collapse all unpushed WIP into fresh logical commits
git reset --soft <base>          # keeps every change staged
git restore --staged .           # optional: unstage, then stage per commit
git add <paths> && git commit -m "feat: ..."
```

`GIT_SEQUENCE_EDITOR=:` accepts generated todo list unchanged; only effect is autosquash reordering. For anything needing hand editing (splitting commit, reordering by hand), describe plan and ask user to run interactive rebase.

### Recovery

```bash
git rebase --abort                  # while a rebase is in progress
git reflog                          # find the pre-rewrite commit
git branch rescue <sha-from-reflog> # keep it safe, then compare before resetting
```

Do not squash whole feature into one commit by default. Keep logical commits showing how change was built, unless repository's merge policy squashes anyway.
