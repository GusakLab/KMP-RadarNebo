---
name: ponytail-debt
description: >
  List every `shortcut:` comment (and older `ponytail:` ones) as a debt ledger. One-shot report,
  changes nothing. Use for "ponytail debt", "what did ponytail defer", "list the
  shortcuts", /ponytail-debt.
---

## Scan

Grep repo for comment markers. Skip `.git` and anything `.gitignore` files ignore (dependencies, build output, caches, any stack):

`git grep --no-index --exclude-standard -nE '(#|//|/[*]) ?(shortcut|ponytail):'`  (add other comment prefixes if stack uses them)

If user names own marker word (`/ponytail-debt TODO`), grep that word instead.

Each hit is one ledger row. Comment prefix keeps prose mentioning the convention out of ledger.
Skip hits that are not a deferral, like a note about a keyboard shortcut.

## Output

One row per marker, grouped by file:

`<file>:<line>, <what was simplified>. ceiling: <the limit named>. upgrade: <the trigger to revisit>.`

Convention: `shortcut: <ceiling>, <upgrade path>`. Pull ceiling and trigger straight from comment.
Want owner per row? Add `git blame -L<line>,<line>`.

Flag rot risk: marker comment naming no upgrade path or trigger gets `no-trigger` tag.

End with `<N> markers, <M> with no trigger.` Nothing found: `No shortcut debt. Clean ledger.`

## Boundaries

Reads and reports only, changes nothing. To persist it, ask and it writes ledger to a file (e.g. `PONYTAIL-DEBT.md`). One-shot.
