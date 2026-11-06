---
name: gof-patterns
description: "Detect Gang of Four patterns already present in existing code (including idiomatic forms) and judge each as appropriate, over-engineered, misapplied, or degraded, with file:line evidence. Use for a pattern audit of specified code; to choose a pattern for a new problem use design-patterns. Don't turn ordinary edits into a whole-codebase audit."
license: MIT (see LICENSE)
metadata:
  author: jaeleeps
  version: "1.2.1"
  repository: https://github.com/jaeleeps/gof-patterns
---

# GoF Design Pattern Analysis

## Workflow

### 0. Read the catalog (required)
Read [references/patterns.md](references/patterns.md) (all 23 patterns) in full before classifying. It holds close-call rules not repeated here: Singleton versus static utility class, which idiomatic forms count, Proxy versus Decorator, Strategy versus State. Read it even when the question names one pattern. You can read it in parallel with the code.

### 1. Scope
Pick target: file, directory, module, or whole repo. If user named nothing, use current repo. Large repo: survey directories first (entry points, core domain, extension points), go deep only where candidates show. Tell user what you covered and what you skipped.

Note language and paradigm; they decide which forms count as idiomatic (step 3).

Pick reading mode by target size, not whole-repo size:
- **Small target (roughly 40 source files or 3,000 lines or fewer):** read every source file in full before analyzing. List files once, then read all with your file-reading tool, **issuing the reads in parallel in a single step** if your environment supports parallel tool calls. Prefer the dedicated file-reading tool over one shell command dumping the tree (`cat` loops, `xargs`, `grep -n ''`). Permission checks often block those, or big output gets truncated, forcing a full re-read. Many patterns only show across files (Bridge, Mediator, Template Method bypassed by a subclass, Strategy eroded by `instanceof` in the context), so reading everything beats searching first on cost and accuracy. Then go straight to step 3.
- **Larger target:** survey and search first (step 2), then read candidate files in parallel batches.

### 2. Gather candidates (large targets)
Cheap pass first. A hit is only a lead:
- Grep name and structural signals from the catalog's lookup table (for example `getInstance`, `accept(`, `Builder`, `subscribe`, `clone`, `handle(`/`setNext`).
- Check type structure: interfaces or abstract classes with several implementations, classes wrapping an object of their own interface, polymorphic dispatch, registries.
- Read code itself. Much real pattern use carries no pattern name.

### 3. Confirm each candidate
Pattern is present when both its **participants and its intent** are present. Name alone proves nothing. Per candidate:
- Map every participant in the catalog entry to code (`file:line`).
- Check intent. Adapter, Decorator, Proxy, Facade can look structurally identical; they differ in *why* the wrapper exists. Strategy and State differ in *who* triggers the switch.
- Assign confidence:
  - **Confirmed**: all participants present, intent matches.
  - **Partial**: core present, a participant missing or collapsed (often fine; say whether it matters).
  - **Idiomatic**: expressed through a language feature: function passed as strategy, module-level instance as singleton, generator as iterator, event emitter as observer. Do not mark down for skipping class ceremony.
  - **Name-only**: named after a pattern but does not implement it (for example a `UserFactory` that only wraps a constructor, no polymorphism). Report these; the name misleads.

### 4. Evaluate fit
Every pattern costs indirection. Judge whether its force **exists in this code**, not whether it might someday. Use catalog's "Fits when" and "Smells" lists. In general:
- Look for real variation: two or more implementations in use, test doubles relying on the seam, plugin or extension point third parties use, or external constraint (for example incompatible third-party API, for Adapter).
- Check cost: added files, indirection hops a reader must follow, testability (Singleton is the usual problem), hidden control flow (Observer chains, Chain of Responsibility ordering).
- Check direction of change: Visitor makes new operations cheap and new types expensive. Check git history or code for which one changes.

One verdict per finding:
- **Appropriate**: forces present, benefit worth the indirection.
- **Over-engineered**: solves variation that does not exist (single implementation, factory returning one type, Abstract Factory with one family). Name simpler replacement.
- **Misapplied**: pattern does not match problem, or another fits better (for example State implemented as Strategy, so callers must manage transitions). Name better fit.
- **Degraded**: pattern was right but has eroded (for example `instanceof` checks bypassing a Strategy, Visitor missing new node types).

Flag **Missing pattern** only with concrete pain in code: repeated `switch` on type across several sites, duplicated algorithm skeletons, constructor with many optional parameters. Do not suggest patterns speculatively.

### 5. Report
Use this structure:

```
## Design pattern analysis: <scope>

<1–2 sentences: overall shape, e.g. "Plugin system built on Strategy + Factory Method; one over-engineered Singleton.">

| Pattern | Where | Confidence | Verdict |
|---|---|---|---|
| Strategy | src/pricing/ (PricingRule + 4 impls) | Confirmed | Appropriate |
| Singleton | src/config.ts:12 | Confirmed | Over-engineered |

### <Pattern> — <location>
**Evidence:** participant → `file:line` mapping (short).
**Verdict:** <verdict>. <Why, citing the forces present or absent in this code.>
**Recommendation:** <keep / simplify to X / replace with Y / fix erosion>. Include a short sketch only when it clarifies.

### Not detected / considered and rejected
<Candidates you ruled out and why, especially name-only matches.>

### Opportunities (only with concrete evidence)

### Other issues noticed
<Correctness bugs you saw while reading, one line each with `file:line`.>
```

Keep report proportional to findings. One file with one pattern needs only a short answer, not every section. Reference code as `path:line` so user can jump to it. Asked about a specific pattern: answer that directly first, then add other notable findings.

## Ground rules
- Idiomatic forms are real pattern uses. Put each in summary table with confidence **Idiomatic** and a verdict. Never move one to "considered and rejected" only because it lacks a class hierarchy. Examples: Python generator hiding pagination is an Iterator; `@decorator` wrapping a function to add behavior is a Decorator; function passed as `key=` is a Strategy.
- Analysis is about patterns, but don't stay silent about bugs. Correctness bug noticed while reading (ignored error result, inverted undo, race): list under "Other issues noticed", even if unrelated to any pattern. Don't hunt bugs beyond what you read for the pattern analysis.
- Every claim must trace to code you read. Never infer a pattern from file or directory names alone.
- Framework-provided patterns get one line, but are not user's design decision. Examples: Spring beans as Singletons, React context as a form of Observer, Express middleware as Chain of Responsibility. Evaluate how user's code *uses* them, not the framework.
- Non-GoF patterns (Repository, Dependency Injection, MVC, Simple Factory, Null Object) may come up. Name them as non-GoF; do not force them into a GoF category.
- Base verdict on code as it is. If right verdict depends on future plans you can't see, state the assumption and ask.
