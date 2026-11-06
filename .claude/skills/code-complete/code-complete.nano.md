# OBEY Code Complete by Steve McConnell

## When to use

Compact always-on construction discipline: implementation, review, debugging, refactoring, tuning.

## Primary bias to correct

Working code is not enough. Construction must lower defect risk, control complexity, keep human inspection cheap.

## Decision rules

- Before coding from solution idea, clarify requirements, architecture fit, risks, conventions, major construction decisions.
- Choose clarity, locality, explicitness, simple control flow, consistent style over clever compactness or fashionable idioms.
- Routines, classes, modules: cohesive, precisely named, small interface, encapsulated, hard to misuse.
- Make data meaning visible: names, constants, stronger types, closed states, deliberate initialization, units, ownership.
- Validate trust boundaries. Use assertions, invariants, contracts for programmer assumptions. Keep error handling explicit, diagnosable.
- Keep branches, loops, exits, exceptions, table-driven logic simple enough to verify.
- Build, test, review, debug, refactor, integrate, tune in small evidence-based loops: root cause before fixes, behavior protection before refactoring, measurement before optimization.
- Use comments, documentation, tools, standards to reduce reader or manual effort, never to hide poor structure.

## Trigger rules

- When readers must decode names, flags, primitives, units, states, or layout, model meaning explicitly.
- When routine mixes phases or has hard-to-use interface, split concerns or change data model.
- When input crosses trust boundary, decide validation, rejection, recovery, assertion, diagnostics.
- When control flow, loops, exceptions, or branching tables are hard to inspect, simplify before adding logic.
- When tests prove only happy paths, add boundary, invalid-input, defensive-check, contract, data-driven cases.
- When comments repeat obvious mechanics, rewrite code or delete comment. Keep comments for intent and constraints.

## Final checklist

- Clear construction context?
- Inspectable code shape?
- Explicit data meaning?
- Defended boundaries and diagnosable failures?
- Simple flow?
- Defect-finding tests and reviews?
- Evidence before fixes, refactors, and tuning?
