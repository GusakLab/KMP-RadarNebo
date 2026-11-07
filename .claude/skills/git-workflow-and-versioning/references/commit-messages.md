# Commit Messages and Local History Cleanup

Read this when writing a non-trivial commit message, or when tidying unpushed commits before a push or PR.

## Repository convention first

Before writing a message, look at `git log --oneline -20`, plus any `CONTRIBUTING.md`, commitlint config or commit-msg hook. Match the existing convention (Conventional Commits, ticket prefixes, sign-off, required trailers). Use the defaults below only when there is no convention.

## Structure

```
<type>(<optional scope>): <subject>

<body: what changed and why, wrapped at ~72 columns>

<footer: trailers such as Refs:, Closes:, BREAKING CHANGE:, Co-Authored-By:>
```

### Subject line

- Use a type prefix such as `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore` or `revert`. Add `!` after the type or scope for a breaking change.
- Use the imperative mood: "add", not "added" or "adds".
- Keep it to about 50 characters (hard limit 72), with no trailing period.
- Be specific: "fix race in payment retry", not "fix bug" or "update stuff".
- Describe the outcome, not the mechanics. Leave file names out unless they are the point.

### Body

Use a body whenever the diff does not explain itself:

- **Why** the change was needed: the problem, the root cause, the constraint.
- **What** changed at a high level. The diff shows the details.
- **Side effects**, risks, and alternatives you rejected.
- How you verified it, when that is not obvious.

Separate the body from the subject with a blank line. Use bullets for lists.

### Footer

```
Refs: #123
Closes: #789
BREAKING CHANGE: /users now requires authentication
Co-Authored-By: Name <email>
```

Add attribution trailers only when the repository or the user's instructions call for them.

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

- A logical unit of work is complete and its tests pass.
- You are at a stable checkpoint before switching tasks or starting a risky step.
- You fixed one bug (one commit per bug).

Do not commit:

- Code that does not build, or failing tests (unless the test documents a known issue the user asked to record).
- Unrelated changes mixed together.
- Debug output or temporary comments.
- Secrets.

## Cleaning up local history (non-interactive)

Rewrite only commits that have **not been pushed** to a shared branch. Once others may have based work on a commit, add a new commit instead. Never force-push a shared branch. On your own feature branch, rewrite pushed commits only with the user's agreement, and push with `--force-with-lease`.

Agents cannot drive an interactive editor. Use these non-interactive forms:

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

`GIT_SEQUENCE_EDITOR=:` accepts the generated todo list unchanged, so the only effect is the autosquash reordering. For anything that needs hand editing (splitting a commit, reordering by hand), describe the plan and ask the user to run the interactive rebase.

### Recovery

```bash
git rebase --abort                  # while a rebase is in progress
git reflog                          # find the pre-rewrite commit
git branch rescue <sha-from-reflog> # keep it safe, then compare before resetting
```

Do not squash a whole feature into one commit by default. Keep the logical commits that explain how the change was built, unless the repository's merge policy squashes anyway.
