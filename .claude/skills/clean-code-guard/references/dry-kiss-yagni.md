# DRY, KISS, YAGNI

## Contents

- DRY: do not duplicate knowledge
- KISS: keep complexity low and local
- YAGNI: avoid speculative configurability
- Ranked list: where AI agents over-engineer
- Self-check for DRY, KISS, YAGNI

---

## DRY — Don't Repeat Yourself

**Definition (Hunt & Thomas, *The Pragmatic Programmer*, verbatim).** *"Every piece of knowledge must have a single, unambiguous, authoritative representation within a system."*

Source: pragprog official DRY excerpt PDF; Wikipedia summary; O'Reilly *97 Things Every Programmer Should Know*, Ch. 30.

### The misreading

*"Don't have any duplicate code."* No. Hunt and Thomas mean duplication "of knowledge, of intent... expressing the same thing in two different places, possibly in two totally different ways." Two functions that **look alike but encode different rules** are not DRY violation. **One rule** expressed in code + database schema + documentation **is**.

### Smells worth flagging

Textual signals that *probably* mean knowledge duplication; verify meaning before refactoring:

- Identical token sequence of ≥5 non-trivial lines in ≥2 functions.
- Same regex, SQL fragment, or URL literal in ≥3 sites.
- Same magic number or string ≥3 times outside constants module.
- Same validation branch (for example, "if value is missing, raise") duplicated across siblings of one module.

### The Rule of 3 — wait for the third occurrence

Do not extract abstraction at first or second duplication. Wait for third: by then *real* shape of shared knowledge is clear enough to abstract correctly. Rule of 3 is refactoring folklore; underlying principle (don't abstract too early) is in Fowler's *Refactoring* (refactoring.com/catalog) and in wrong-abstraction work below.

### The Sandi Metz corollary — wrong abstraction is worse than duplication

From Sandi Metz, "The Wrong Abstraction" (Jan 2016): *"duplication is far cheaper than the wrong abstraction."*

Abstraction with accumulated per-caller branches and special cases is wrong. Remedy:

1. Re-inline it into each caller.
2. Delete per-caller dead branches.
3. Live with honest duplication for a while.
4. Re-abstract only when *real* shared knowledge becomes obvious.

**Rule.** No abstraction to remove three duplicated lines unless you can name underlying *knowledge* they represent. If you cannot name it, keep duplication.

---

## KISS — Keep It Simple, Stupid

### Operationalizing KISS for code review

KISS is fuzzy without numbers. Defaults:

- **Cognitive Complexity ≤10 per function.** SonarSource's metric: *how hard the code is to understand*, not count of independent paths. Dominant 2026 metric, adopted by SonarQube, Biome, ReSharper. Target <7 for greenfield code; <15 is upper bound before refactor becomes mandatory.
- **Cyclomatic complexity ≤10 per function.** McCabe's original 1976 metric; still useful structural floor. 11–20 moderate risk; 21–50 high; >50 untestable. Source: McCabe NIST 235r; JetBrains ReSharper threshold guidance. Cognitive Complexity judges human readability, cyclomatic judges "is this testable". When they disagree, prefer Cognitive Complexity.
- **Nesting depth ≤5.** Source: Aivosto, Project Metrics: Complexity.
- **Function length:** no canonical absolute; common static-analysis defaults (SonarQube, PMD) flag >50–60 lines. Pair with complexity ceiling; do not rely on LOC alone.

### Self-check

If function exceeds cyclo 10 or nest depth 5, refactor *before* finishing, not "later." Extract helper; replace nested `if/else` with early returns or lookup table.

---

## YAGNI — You Aren't Gonna Need It

**Canonical reference.** Martin Fowler, *bliki: Yagni* (May 2015). *"A mantra from ExtremeProgramming... capabilities we presume our software needs in the future should not be built now because 'you aren't gonna need it.'"*

### Fowler's four cost categories

Building presumptive feature costs:

1. **Cost of build.** Analysis, coding, testing of feature that ends up unused.
2. **Cost of delay.** Opportunity cost: revenue-generating work not done instead.
3. **Cost of carry.** Added complexity slows every later change and debug.
4. **Cost of repair.** When presumed feature proves wrong, you pay to rip it out plus technical debt accumulated against it.

Source: martinfowler.com/bliki/Yagni.html; InfoQ summary.

### AI-specific YAGNI traps

LLMs over-produce speculative surface area. Watch for:

1. **Config flags / env vars nobody asked for.** `enable_x_v2`, `legacy_mode`, toggles for single code path with no second variant.
2. **Plugin / strategy systems for 2 known cases.** Registry + base class + 2 subclasses where direct conditional is shorter and clearer.
3. **Generic helpers with one caller.** `normalizeAnything(value, strict=false, mode="default")` invoked from exactly one site.
4. **Optional parameters never passed.** `send(value, retries=3, backoff=null, jitter=false, logger=null)` where every call site uses defaults. Delete them until real caller exists.
5. **Speculative async / batching / caching.** Async wrappers, queues, batch endpoints at single-digit RPS load.
6. **Premature interfaces/protocols with one implementation.** `FooRepository` paired with one concrete `SqlFooRepository`. Inline until second implementation exists.

### Self-check

For every parameter, class, file, or abstraction you add, ask: *who calls this today?* If "nobody yet," delete it.

---

## Ranked list — where AI agents over-engineer

By observed frequency (engineering-blog-grade, not from controlled study):

1. **Premature interfaces/protocols** with one implementation.
2. **Factory classes for trivial constructors** — `UserFactory.create(...)` wrapping `User(...)`.
3. **DI containers in small apps** — wiring frameworks for 3–5 services where explicit construction in `main()` is shorter and traceable.
4. **Try/catch wrappers that change nothing** — adds lines, hides tracebacks.
5. **Speculative config surface** — settings objects with 15 fields where 3 are read.
6. **Plugin / registry scaffolding for two cases.**
7. **`utils.py` / `common.py` modules** — magnets for unrelated functions; violate DRY's "single authoritative representation" by location.
8. **Re-implementing what the platform already gives you** — custom retry loops, enums, or record-like types standard library provides; hand-rolled validation a database/schema constraint, type system, or framework's declarative rule would enforce; native platform feature rebuilt in hand-written app code. Prefer cheapest existing solution: stdlib or native feature over new code, declarative constraint over imperative check. Flip side: mode 15 in [ai-failure-modes.md](ai-failure-modes.md); do not add *new* dependency for what a few lines cover either.
9. **Excessive layering** (Controller → Service → Manager → Repository) for CRUD — four files to read one row.
10. **Wrapping libraries "to make them swappable"** — thin pass-through adapters around HTTP, database, or SDK client you will never swap.

---

## Self-check for DRY, KISS, YAGNI

Before shipping code:

1. (DRY) Did you remove duplicated *knowledge*, or only duplicated *text*? Can you name underlying rule?
2. (DRY/Metz) If you added abstraction, do at least two callers today have structurally identical code? Or is it speculative?
3. (KISS) Any function over cyclomatic 10 or nest depth 5?
4. (YAGNI) Any optional parameter, config flag, env var, interface, factory, or base class without caller using it today?
5. (YAGNI) Wrapped library to "make it swappable"? Delete wrapper.
