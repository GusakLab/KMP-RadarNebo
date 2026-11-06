---
name: caveman-review
description: >
  Compressed code review - one line per finding with location, problem and fix.
  Use only when the user asks for it by name: /caveman-review or "caveman
  review". For ordinary PR or diff review use code-review.
---

Write terse, actionable code review comments. One line per finding: location, problem, fix.

## Rules

**Format:** `L<line>: <problem>. <fix>.` For multi-file diffs: `<file>:L<line>: ...`.

**Severity prefix (optional, when mixed):**
- `🔴 bug:` — broken behavior, will cause incident
- `🟡 risk:` — works but fragile (race, missing null check, swallowed error)
- `🔵 nit:` — style, naming, micro-optim. Author can ignore
- `❓ q:` — genuine question, not suggestion

**Drop:**
- "I noticed that...", "It seems like...", "You might want to consider..."
- "This is just a suggestion but..." — use `nit:` instead
- "Great work!", "Looks good overall but..." — say once at top, not per comment
- Restating what line does — reviewer can read diff
- Hedging ("perhaps", "maybe", "I think") — if unsure, use `q:`

**Keep:**
- Exact line numbers
- Exact symbol/function/variable names in backticks
- Concrete fix, not "consider refactoring this"
- *Why*, if fix isn't obvious from problem statement

## Examples

❌ "I noticed that on line 42 you're not checking if the user object is null before accessing the email property. This could potentially cause a crash if the user is not found in the database. You might want to add a null check here."

✅ `L42: 🔴 bug: user can be null after .find(). Add guard before .email.`

❌ "It looks like this function is doing a lot of things and might benefit from being broken up into smaller functions for readability."

✅ `L88-140: 🔵 nit: 50-line fn does 4 things. Extract validate/normalize/persist.`

❌ "Have you considered what happens if the API returns a 429? I think we should probably handle that case."

✅ `L23: 🟡 risk: no retry on 429. Wrap in withBackoff(3).`

## Auto-Clarity

Drop terse mode for security findings (CVE-class bugs need full explanation and reference), architectural disagreements (need rationale, not just one-liner), and onboarding where author is new and needs the "why". There, write normal paragraph, then resume terse.

## Boundaries

Reviews only. Does not write code fix, does not approve/request-changes, does not run linters. Output comment(s) ready to paste into PR. "stop caveman-review" or "normal mode": revert to verbose review style.