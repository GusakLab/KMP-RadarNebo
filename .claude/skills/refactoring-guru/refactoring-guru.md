# OBEY Refactoring.Guru

## Source and Scope

Rule set derived from public Refactoring.Guru refactoring material:

- <https://refactoring.guru/refactoring>
- <https://refactoring.guru/refactoring/what-is-refactoring>
- <https://refactoring.guru/refactoring/technical-debt>
- <https://refactoring.guru/refactoring/when>
- <https://refactoring.guru/refactoring/how-to>
- <https://refactoring.guru/refactoring/smells>
- <https://refactoring.guru/refactoring/catalog>
- <https://refactoring.guru/refactoring/techniques>

Crawl intentionally excluded example code, images, premium course pages, design pattern pages, legal pages, and non-refactoring navigation.

Not a copy of the site: operational rule set for AI coding agents, paraphrased from the refactoring process, code-smell catalog, and refactoring technique catalog.

Binding engineering policy: `MUST` is binding, `SHOULD` is strong default, `MUST NOT` is forbidden.

---

## Purpose

Refactoring: controlled improvement of code structure without adding new functionality.

Clean code here means code that:

- is obvious to other programmers
- avoids duplicated knowledge and duplicated control flow
- has minimal moving parts
- passes relevant tests
- is easier and cheaper to maintain than code it replaced

---

## Primary Directive

When changing existing code, first diagnose the smell that makes the change hard.

Then choose smallest refactoring that removes or isolates that smell while preserving behavior.

Never treat refactoring as vague cleanup pass. Every refactoring MUST have:

- specific smell, friction, or maintenance cost it addresses
- bounded transformation
- verification path
- no hidden feature change

---

## Refactoring Process

### Keep Refactoring Separate

- MUST NOT mix direct feature development and refactoring in one indistinguishable edit.
- SHOULD separate refactoring from behavior changes, at least by commit, patch section, or clearly labeled step.
- MUST preserve existing behavior during refactoring.
- MUST call out any behavior change as feature work or bug fixing, not refactoring.
- SHOULD refactor after feature work when feature leaves new duplication, awkward names, or unnecessary structure.

### Work in Small Steps

- MUST apply refactoring as sequence of small changes.
- MUST keep program working after each meaningful step when practical.
- MUST stop and reduce scope when refactoring grows too large to reason about locally.
- SHOULD prefer several named transformations over one broad rewrite.
- MUST NOT use refactoring as cover for uncontrolled redesign.

### Verify Continuously

- MUST run all relevant existing tests after refactoring.
- If tests fail, MUST decide whether refactoring changed behavior or tests were too coupled to implementation details.
- MUST fix refactoring mistakes before continuing.
- SHOULD replace or lift brittle low-level tests when they block behavior-preserving structure changes.
- MUST NOT delete failing tests to make refactoring look successful.

### Keep the Result Cleaner

- MUST NOT perform refactoring that leaves code as unclear, duplicated, or bloated as before.
- SHOULD pause and re-diagnose when chain of small edits is not improving clarity.
- SHOULD consider planned rewrite only when code is extremely sloppy, tests exist or are added first, and enough time is explicitly allocated.

---

## When to Refactor

### Rule of Three

- MAY implement first occurrence directly.
- SHOULD tolerate second similar occurrence while abstraction is still uncertain.
- MUST consider refactoring on third similar occurrence.
- MUST NOT abstract coincidental similarity before repeated responsibility is clear.

### While Adding a Feature

- SHOULD refactor first when existing code is too dirty to understand the change safely.
- SHOULD reshape local structure so feature becomes straightforward.
- SHOULD use feature request to pay down specific debt that blocks it.

### While Fixing a Bug

- SHOULD inspect area around bug for hidden complexity, duplication, and unclear ownership.
- SHOULD clean structure that let bug hide when cleanup is small and local.
- MUST keep observed bug fix as behavior change separate from supporting refactor.

### During Code Review

- SHOULD use review as last chance to catch smells before code goes public.
- SHOULD fix simple smells immediately when review context and ownership allow.
- SHOULD estimate and isolate larger smells instead of smuggling them into reviewed change.
- SHOULD collaborate with author when smell needs judgment about intent.

---

## Technical Debt Rules

- MUST NOT justify patches, kludges, missing tests, or unclear structure as harmless if they make later changes slower or riskier.
- SHOULD expose debt source when it comes from business pressure, missing tests, weak modularity, delayed refactoring, poor documentation, isolated branches, or inconsistent standards.
- MUST prioritize debt hurting current change speed, correctness, or team understanding.
- SHOULD reduce debt incrementally through ordinary feature and bug work.
- MUST NOT defer all refactoring to future cleanup project unless current change cannot safely absorb it.

---

## Smell Detection Process

When touching existing code, scan in this order:

1. Bloaters: code grew too large to understand or change.
2. Object-orientation abusers: inheritance, type codes, or conditionals misuse object model.
3. Change preventers: one change forces edits in too many places, or one class changes for unrelated reasons.
4. Dispensables: code exists without earning its maintenance cost.
5. Couplers: classes know too much about each other, or delegate so much that responsibility disappears.
6. Library gaps: external classes force duplicated workarounds.

### Diagnose, Treat, Verify, Stop

Use for every non-trivial refactoring:

1. Diagnose the smell:
   - name visible symptom
   - name maintenance cost it creates
   - identify whether smell is local, repeated, or architectural
   - check whether smell is real or only style preference
2. Choose treatment:
   - pick catalog technique that directly addresses the smell
   - prefer smaller technique before larger structural move
   - note expected cleaner end state before editing
   - reject treatment if its own tradeoff is worse than the smell
3. Verify behavior:
   - identify existing tests, characterization checks, type checks, or manual checks before moving code
   - run relevant check after each risky step
   - if behavior changes, stop treating change as refactoring and isolate the behavior change
4. Decide the stop condition:
   - stop when named smell is gone or materially reduced
   - stop when next improvement requires different smell diagnosis
   - stop when refactoring would cross ownership, public API, or feature scope without explicit approval
   - stop when code is clean enough for requested change and further cleanup is speculative

MUST NOT keep refactoring only because another smell turned up. Record next smell separately unless it blocks current change.

### Smell Exception Rules

- MUST NOT treat every smell mechanically; confirm treatment improves clarity for this codebase.
- MUST document or report intentional non-treatment when visible smell stays in touched code.

---

## Bloaters

### Long Method

- Trigger: understanding method requires scrolling, comments, or mental bookkeeping.
- MUST question method once noticeably long; ten lines is warning threshold, not mechanical limit.
- SHOULD extract method when fragment needs comment to explain what it does.
- SHOULD extract loops, conditional branches, and coherent phases into named methods.
- SHOULD use `Replace Temp with Query`, `Introduce Parameter Object`, or `Preserve Whole Object` when locals block extraction.
- SHOULD use `Replace Method with Method Object` when many locals or tightly coupled algorithm block extraction.
- MUST NOT avoid extraction only because method call might add negligible performance cost.

### Large Class

- Trigger: class has too many fields, methods, responsibilities, or lines to grasp as one concept.
- SHOULD split class wearing multiple functional hats.
- SHOULD use `Extract Class` when subset of fields and methods forms separate responsibility.
- SHOULD use `Extract Subclass` when rare or variant behavior bloats main class.
- SHOULD use `Extract Interface` when clients need only stable subset of behavior.
- SHOULD move GUI-held domain data into domain classes when interface objects carry business state.
- MUST NOT split class only because it is large if extracted part has no stable responsibility.

### Primitive Obsession

- Trigger: primitives, strings, numbers, constants, or arrays stand in for meaningful concepts.
- SHOULD replace repeated primitive values with small objects carrying meaning and validation.
- SHOULD use `Replace Data Value with Object` for single primitive with domain behavior or constraints.
- SHOULD use `Replace Type Code with Class`, subclasses, or state/strategy when codes control behavior.
- SHOULD use `Replace Array with Object` when array positions have named meaning.
- SHOULD use `Replace Magic Number with Symbolic Constant` when literal carries domain meaning.
- MUST NOT wrap primitives in new types when wrapper adds no name, validation, behavior, or error prevention.

### Long Parameter List

- Trigger: method needs more than three or four parameters, or callers must memorize argument order.
- SHOULD replace derived arguments with `Replace Parameter with Method Call`.
- SHOULD pass existing object with `Preserve Whole Object` when callee needs several values from it.
- SHOULD introduce parameter object when parameters form recurring concept.
- MUST NOT remove parameters if that creates unwanted dependency between classes.

### Data Clumps

- Trigger: same group of values appears in multiple fields, signatures, or calls.
- SHOULD test whether values still make sense with one member removed; if not, model the group.
- SHOULD use `Extract Class` for repeated field groups.
- SHOULD use `Introduce Parameter Object` for repeated parameter groups.
- SHOULD pass whole object when methods keep receiving pieces of same concept.
- SHOULD move behavior using the clump onto new object when appropriate.
- MUST NOT pass whole object if that creates undesirable dependency on much larger collaborator.

---

## Object-Orientation Abusers

### Switch Statements

- Trigger: complex `switch` or repeated `if` chains branch on type, mode, or category.
- SHOULD suspect missing polymorphism when adding new case requires edits in multiple switch sites.
- SHOULD extract and move switch logic to class that owns the decision.
- SHOULD replace type-code branching with subclasses or state/strategy when behavior varies by type.
- SHOULD replace conditional dispatch with polymorphism once structure is explicit.
- SHOULD use explicit methods instead of polymorphism when only a few simple parameter variations exist.
- SHOULD use null object when branch exists only for null handling.
- MUST NOT replace simple honest conditional or factory selection with unnecessary polymorphism.

### Temporary Field

- Trigger: fields meaningful only in special circumstances, empty or invalid otherwise.
- SHOULD extract algorithm and its temporary state into separate class.
- SHOULD use method object when method needs temporary fields only to carry many intermediate values.
- SHOULD use null object when conditional checks around absent state dominate code.
- MUST NOT normalize half-initialized objects as ordinary design.

### Refused Bequest

- Trigger: subclass inherits behavior or data it does not use or cannot honor.
- SHOULD push unused methods or fields down to subclasses that need them.
- SHOULD replace inheritance with delegation when subclass relationship is misleading.
- SHOULD preserve inheritance only when unused inherited behavior is harmless and does not confuse clients.

### Alternative Classes with Different Interfaces

- Trigger: two classes do same job but expose different method names or signatures.
- SHOULD align names with `Rename Method`.
- SHOULD align signatures with `Move Method`, `Add Parameter`, or `Parameterize Method`.
- SHOULD extract superclass when duplicated behavior is only partial but real.
- SHOULD delete one alternative once common interface and behavior make it redundant.
- MAY leave alternatives separate when they live in separate external libraries and unification is impractical.

---

## Change Preventers

### Divergent Change

- Trigger: one class must change for many unrelated reasons.
- SHOULD split unrelated responsibilities with `Extract Class`.
- SHOULD separate product, display, persistence, and integration behavior when they evolve independently.
- SHOULD use superclass or subclass extraction only when shared behavior is genuine.

### Shotgun Surgery

- Trigger: one conceptual change forces many small edits across many classes.
- SHOULD centralize scattered responsibility.
- SHOULD move methods and fields to owner of changing concept.
- SHOULD inline or extract classes to put related changes in one place.
- MUST NOT leave knowledge scattered once pattern is visible.

### Parallel Inheritance Hierarchies

- Trigger: adding subclass in one hierarchy requires matching subclass in another.
- SHOULD merge duplicated hierarchy pressure by moving methods and fields so one hierarchy owns the variation.
- SHOULD collapse or replace parallel structures that exist only to mirror each other.
- MUST avoid creating new parallel hierarchies during extension work.

---

## Dispensables

### Comments

- Trigger: comments explain what unclear code does rather than why it exists.
- SHOULD replace explanatory comments with better names, extracted variables, extracted methods, or assertions.
- SHOULD keep comments for rationale, non-obvious constraints, external contracts, and algorithms that resisted simplification.
- MUST NOT use comments as deodorant for confusing structure.

### Duplicate Code

- Trigger: two fragments are identical or do same job with slightly different wording.
- SHOULD use `Extract Method` for duplicates in same class.
- SHOULD use pull-up or extract-superclass techniques for duplicates across sibling classes.
- SHOULD use `Extract Class` when duplicate behavior belongs to separate concept.
- SHOULD remove accidental duplication even when fragments are not textually identical.
- MAY leave duplication when merging would make code less intuitive or create wrong abstraction.
- MUST NOT merge duplicates that are only coincidentally similar and likely to diverge for different reasons.

### Lazy Class

- Trigger: class no longer does enough to justify its maintenance cost.
- SHOULD inline near-useless class.
- SHOULD collapse hierarchy when subclasses or superclasses no longer carry distinct behavior.
- MAY keep small class when it clearly communicates intended extension point and earns that clarity.

### Data Class

- Trigger: class only stores data and exposes crude getters or setters while clients perform the behavior.
- SHOULD encapsulate public fields.
- SHOULD encapsulate collections rather than expose mutable collection internals.
- SHOULD move client behavior onto data class when behavior operates on that data.
- SHOULD remove broad setters or accessors once meaningful behavior exists.

### Dead Code

- Trigger: unused variables, parameters, fields, methods, classes, files, or unreachable branches.
- SHOULD use IDE and compiler feedback to find dead code.
- MUST delete unused code and files when no compatibility reason remains.
- SHOULD inline or collapse empty classes or hierarchies before deletion when needed.
- SHOULD remove unused parameters from methods.
- MUST NOT delete public, serialized, reflected, or plugin-facing code without checking external compatibility.

### Speculative Generality

- Trigger: abstractions, parameters, hooks, fields, or classes exist only for imagined future needs.
- SHOULD inline unused abstractions.
- SHOULD remove unused parameters, methods, fields, and classes.
- SHOULD collapse unused hierarchies.
- MAY keep framework extension points only when real users need them.
- MUST check tests before deleting member that exists only for test access.

---

## Couplers

### Feature Envy

- Trigger: method uses another object's data more than its own.
- SHOULD move behavior to class owning the data it mainly uses.
- SHOULD extract envying fragment before moving it when only part of method envies another object.
- SHOULD split method across owners when it uses several data sources for separable purposes.
- MAY keep behavior separate when separation is intentional, such as interchangeable strategy-like behavior.

### Inappropriate Intimacy

- Trigger: classes rely on each other's internals or spend too much time together.
- SHOULD move methods and fields to reduce private knowledge crossing boundaries.
- SHOULD extract or hide delegates to reduce unnecessary knowledge of collaborator structure.
- SHOULD replace inheritance with delegation when intimacy comes from overexposed subclass relationship.

### Message Chains

- Trigger: client code navigates chain of objects to reach data or behavior.
- SHOULD hide delegate behind object client already knows.
- SHOULD move behavior closer to data instead of making clients navigate structure.
- MUST NOT expose object graph topology as routine calling convention.

### Middle Man

- Trigger: class mostly forwards calls and adds no policy, coordination, or protection.
- SHOULD remove middle man when direct collaboration is clearer.
- SHOULD inline class that exists only as pass-through.
- SHOULD keep delegating layer when it protects a boundary, hides volatile structure, or provides useful policy.

### Incomplete Library Class

- Trigger: external library class lacks methods you need and cannot be changed directly.
- SHOULD use foreign method for one or two missing operations.
- SHOULD use local extension when missing behavior is substantial.
- MUST NOT scatter repeated library workarounds throughout codebase.
- MUST NOT fork or wrap library broadly when one narrow foreign method would close the gap.

---

## Smell-to-Treatment Priority Map

Use after diagnosing the smell. Start with preferred treatment. Move to fallback only when preferred treatment is blocked. Risky options need stronger tests and explicit justification.

- `Long Method`: prefer `Extract Method`; fallback to `Replace Temp with Query`, `Introduce Parameter Object`, or `Preserve Whole Object` when locals block extraction; risky: `Replace Method with Method Object`, because it creates new object and changes algorithm shape.
- `Large Class`: prefer `Extract Class`; fallback to `Extract Subclass` for rare or variant behavior, or `Extract Interface` for client-facing subsets; risky: broad hierarchy extraction before responsibilities are stable.
- `Primitive Obsession`: prefer `Replace Data Value with Object`, `Replace Magic Number with Symbolic Constant`, or `Replace Array with Object`; fallback to type-code refactorings when behavior varies by code; risky: replacing type code with subclasses or state/strategy before variation is stable.
- `Long Parameter List`: prefer `Replace Parameter with Method Call` or `Preserve Whole Object`; fallback to `Introduce Parameter Object`; risky: removing parameters by creating hidden object dependencies.
- `Data Clumps`: prefer `Extract Class` or `Introduce Parameter Object`; fallback to `Preserve Whole Object`; risky: passing large owner object merely to avoid a parameter list.
- `Switch Statements`: prefer `Extract Method` and `Move Method` to isolate the decision; fallback to type-code replacement; risky: `Replace Conditional with Polymorphism` when conditional is simple or not based on stable variation.
- `Temporary Field`: prefer `Extract Class` or `Replace Method with Method Object`; fallback to `Introduce Null Object` for absence checks; risky: spreading optional half-state through more conditionals.
- `Refused Bequest`: prefer `Push Down Method` or `Push Down Field`; fallback to `Replace Inheritance with Delegation`; risky: preserving inheritance only to avoid changing callers.
- `Alternative Classes with Different Interfaces`: prefer `Rename Method` and signature alignment; fallback to `Extract Superclass`; risky: merging classes across library or ownership boundaries.
- `Divergent Change`: prefer `Extract Class`; fallback to `Extract Superclass` or `Extract Subclass` for genuine shared behavior; risky: inheritance used to avoid clear responsibility splits.
- `Shotgun Surgery`: prefer `Move Method` and `Move Field` to centralize ownership; fallback to `Inline Class` or `Extract Class`; risky: adding more forwarding layers without reducing edit sites.
- `Parallel Inheritance Hierarchies`: prefer moving methods and fields to collapse mirrored variation; fallback to hierarchy collapse; risky: adding next paired subclass without redesigning ownership.
- `Comments`: prefer `Extract Variable`, `Extract Method`, or `Rename Method`; fallback to `Introduce Assertion` for hidden state assumptions; risky: deleting comments before code is self-explanatory.
- `Duplicate Code`: prefer `Extract Method`; fallback to pull-up or `Extract Superclass` for sibling duplication, or `Extract Class` for separate concept; risky: merging coincidental similarity.
- `Lazy Class`: prefer `Inline Class`; fallback to `Collapse Hierarchy`; risky: keeping class only because future work might need it.
- `Data Class`: prefer `Encapsulate Field` and `Encapsulate Collection`; fallback to `Move Method` and `Extract Method` to bring behavior to the data; risky: stopping after trivial accessors.
- `Dead Code`: prefer deletion after usage checks; fallback to `Inline Class`, `Collapse Hierarchy`, or `Remove Parameter`; risky: deleting externally reachable API.
- `Speculative Generality`: prefer `Inline Method`, `Inline Class`, `Remove Parameter`, and field deletion; fallback to `Collapse Hierarchy`; risky: removing framework extension points without checking users.
- `Feature Envy`: prefer `Move Method`; fallback to `Extract Method` before moving envying fragment; risky: moving behavior deliberately separated for interchangeable strategy-like use.
- `Inappropriate Intimacy`: prefer `Move Method` and `Move Field`; fallback to `Hide Delegate` or `Replace Inheritance with Delegation`; risky: widening visibility to preserve the intimacy.
- `Message Chains`: prefer `Hide Delegate`; fallback to `Move Method` closer to data; risky: adding middle man that merely forwards without reducing knowledge.
- `Middle Man`: prefer `Remove Middle Man`; fallback to `Inline Class`; risky: removing boundary that hides volatile structure or policy.
- `Incomplete Library Class`: prefer `Introduce Foreign Method` for narrow missing operation; fallback to `Introduce Local Extension` for repeated substantial missing behavior; risky: broad library wrapping or forking.

---

## Technique Playbook

Each named technique MUST be applied with symptom, use condition, avoid condition, safe steps, and verification.

### Composing Methods Playbook

- `Extract Method`: Symptom: fragment has coherent purpose, needs comment, duplicates another fragment, or blocks local reasoning. Use when a name explains fragment better than inline detail. Do not use when fragment hides required side effects or depends on too much changing local state. Safe steps: identify inputs, outputs, mutated variables; extract; name by purpose; replace old fragment with call. Verify with tests around caller; check changed state flow.
- `Inline Method`: Symptom: method name adds no clarity beyond its body. Use when indirection obscures caller. Do not use when method is override point, public contract, or useful concept name. Safe steps: inspect all callers, substitute body, remove only when no caller remains. Verify dispatch/interface usage and tests around callers.
- `Extract Variable`: Symptom: expression hard to understand in place. Use when temporary name reveals intent. Do not use when variable merely repeats expression mechanically. Safe steps: introduce immutable local close to use, name the concept, keep evaluation order. Verify with tests; check no side effect now evaluates earlier or later.
- `Inline Temp`: Symptom: temporary variable blocks another refactoring or hides simple expression. Use when expression is cheap and clear. Do not use when name explains non-obvious concept or prevents repeated side effects. Safe steps: replace references with expression, then delete temp. Verify evaluation count and order.
- `Replace Temp with Query`: Symptom: temporary value blocks extraction or repeats meaningful calculation. Use when named query can compute same value without mutation. Do not use when calculation is expensive, stateful, or order-dependent without caching policy. Safe steps: create query, replace temp reads, remove temp. Verify result equivalence and performance-sensitive paths.
- `Split Temporary Variable`: Symptom: one variable has multiple meanings across assignments. Use when assignments represent separate concepts. Do not use when variable is intentional accumulator. Safe steps: create one variable per meaning, update uses. Verify each use points to intended value.
- `Remove Assignments to Parameters`: Symptom: parameter reused as scratch state. Use when mutation obscures caller intent. Do not use when language semantics intentionally model output parameters and callers rely on it. Safe steps: introduce local variable, replace assignments, keep parameter read-only. Verify caller-visible behavior.
- `Replace Method with Method Object`: Symptom: method too tangled with locals to extract smaller methods. Use when dedicated object can hold algorithm state and enable smaller methods. Do not use for simple long method that `Extract Method` can handle. Safe steps: create method object, move locals to fields, move algorithm, split internal steps. Verify algorithm output and side effects.
- `Substitute Algorithm`: Symptom: algorithm is confusing and clearer equivalent exists. Use only after behavior is well protected. Do not use to silently change semantics, performance guarantees, or edge-case behavior. Safe steps: capture current behavior, replace algorithm, compare results on edge cases. Verify with broad tests around expected and boundary inputs.

### Moving Features Playbook

- `Move Method`: Symptom: method uses another class more than its own. Use when behavior belongs with data it changes. Do not use when separation is deliberate for interchangeable behavior. Safe steps: inspect data usage, extract partial fragment if needed, add method to target, redirect callers, remove old method. Verify callers and access visibility.
- `Move Field`: Symptom: another class or concept uses field more. Use when ownership is clearer elsewhere. Do not use when moving creates circular knowledge or breaks lifecycle ownership. Safe steps: add field to target, migrate reads/writes, preserve initialization, delete old field. Verify construction, serialization, persistence, and mutation behavior.
- `Extract Class`: Symptom: one class does two jobs. Use when fields and methods form stable separate responsibility. Do not use for arbitrary size reduction. Safe steps: create class, move data and behavior together, delegate temporarily, update clients gradually. Verify behavior and that responsibility boundaries are clearer.
- `Inline Class`: Symptom: class no longer earns its maintenance cost. Use when its behavior fits naturally in another class. Do not use when it marks real boundary or extension point. Safe steps: move members to target, replace references, delete empty class. Verify construction and public API usage.
- `Hide Delegate`: Symptom: clients navigate collaborator structure. Use when current object can shield clients from that structure. Do not use if it creates pure pass-through layer without reducing knowledge. Safe steps: add forwarding method with meaningful policy, update clients, keep collaborator private. Verify clients no longer know the path.
- `Remove Middle Man`: Symptom: class mostly forwards calls. Use when direct collaboration is clearer. Do not use when middle layer protects volatility or policy. Safe steps: replace forwarding calls with direct calls, remove forwarding methods, then reassess class. Verify callers still have appropriate dependency.
- `Introduce Foreign Method`: Symptom: library class lacks one small operation. Use for narrow missing method you cannot add to library. Do not use when many operations are missing. Safe steps: create local helper near usage, name it as if it belonged to library type, replace duplicates. Verify behavior against library edge cases.
- `Introduce Local Extension`: Symptom: library class repeatedly lacks substantial behavior. Use when local wrapper/subclass reduces duplicated workarounds. Do not use for one small helper. Safe steps: create extension type, move repeated behavior, migrate callers deliberately. Verify compatibility with library construction and updates.

### Organizing Data Playbook

- `Self Encapsulate Field`: Symptom: direct field access prevents controlled access behavior. Use when access may need validation, lazy behavior, or override. Do not use when direct field access is intentionally simple and local. Safe steps: add access methods, replace internal reads/writes, then route future access through methods. Verify no recursive access or initialization breakage.
- `Replace Data Value with Object`: Symptom: primitive carries domain meaning or validation. Use when behavior or constraints belong with the value. Do not use for wrapper without added meaning. Safe steps: create value object, migrate construction, move validation/behavior, replace primitive usage. Verify equality, serialization, and boundary conversion.
- `Change Value to Reference`: Symptom: many equal objects should represent one mutable entity. Use when shared identity and current state matter. Do not use for naturally immutable values. Safe steps: introduce factory or repository lookup, return canonical instances, update creation paths. Verify identity sharing and missing-object handling.
- `Change Reference to Value`: Symptom: reference lifecycle is heavier than object deserves. Use when immutable value semantics fit. Do not use when identity or shared mutation matters. Safe steps: make object immutable, define equality, simplify construction. Verify comparisons and update flows.
- `Replace Array with Object`: Symptom: array indexes have hidden names. Use when positions represent fields. Do not use for true homogeneous sequences. Safe steps: create object with named fields, replace index access, add behavior if needed. Verify all index semantics are preserved.
- `Duplicate Observed Data`: Symptom: GUI classes hold domain data. Use when domain state should live outside UI with synchronization. Do not use when UI-only state has no domain meaning. Safe steps: create domain object, move domain data, synchronize UI/domain updates. Verify two-way update behavior.
- `Change Unidirectional Association to Bidirectional`: Symptom: both classes genuinely need navigation. Use when reverse lookup is complex or frequent. Do not use for convenience alone. Safe steps: choose dominant owner, add reverse field, centralize association updates. Verify add/remove consistency.
- `Change Bidirectional Association to Unidirectional`: Symptom: one side does not use the other. Use to reduce dependency and maintenance code. Do not use when behavior requires reverse navigation. Safe steps: replace reads with parameters/lookups if needed, remove update code, delete unused field. Verify navigation callers.
- `Replace Magic Number with Symbolic Constant`: Symptom: literal has hidden meaning. Use when a name explains the value. Do not use for obvious local literals. Safe steps: introduce named constant near owner, replace uses. Verify no unrelated same-value literals were captured.
- `Encapsulate Field`: Symptom: public field exposes representation. Use when access needs control. Do not stop at trivial accessors if behavior belongs inside. Safe steps: add accessor, migrate reads/writes, make field private. Verify callers and invariants.
- `Encapsulate Collection`: Symptom: callers mutate internal collection directly. Use when owner must preserve invariants. Do not expose settable mutable collection as replacement. Safe steps: return read-only view/copy, add add/remove methods, migrate callers. Verify mutation paths.
- `Replace Type Code with Class`: Symptom: code needs type safety or behavior but not polymorphic variants. Use for meaningful codes. Do not use for trivial constants. Safe steps: create class for code, replace primitives, centralize validation. Verify persistence and comparisons.
- `Replace Type Code with Subclasses`: Symptom: type code drives stable variant behavior. Use when behavior differs by type and type does not change often at runtime. Do not use for volatile states. Safe steps: create subclasses, move variant behavior, replace creation. Verify dispatch and construction.
- `Replace Type Code with State/Strategy`: Symptom: type or state controls behavior and may change at runtime. Use when runtime switching matters. Do not use when simple class code is enough. Safe steps: create state/strategy objects, move behavior, route transitions explicitly. Verify state transitions.
- `Replace Subclass with Fields`: Symptom: subclasses differ only by constant data. Use when hierarchy adds no behavior. Do not use when subclasses have distinct logic. Safe steps: add fields to superclass, replace subclass construction, remove empty subclasses. Verify type checks and serialization.

### Conditional and Method Call Playbook

- `Decompose Conditional`: Symptom: condition or branches need mental parsing. Use when names can clarify condition, then, or else parts. Do not use if extraction hides side effects. Safe steps: extract condition and branches into named methods. Verify branch behavior.
- `Consolidate Conditional Expression`: Symptom: multiple checks lead to one action. Use when checks are side-effect free. Do not use if checks differ in timing or side effects. Safe steps: combine expression, extract named query. Verify truth table.
- `Consolidate Duplicate Conditional Fragments`: Symptom: all branches repeat code. Use when repeated code can move before or after conditional without changing order. Do not use if branch-specific side effects change ordering. Safe steps: move common fragment, extract if longer. Verify branch outputs.
- `Remove Control Flag`: Symptom: flag variable only directs loop or branch flow. Use when direct break/return/continue is clearer. Do not use if flag represents durable domain state. Safe steps: replace flag checks with direct control flow. Verify loop exit behavior.
- `Replace Nested Conditional with Guard Clauses`: Symptom: special cases obscure normal path. Use when early exits make normal flow obvious. Do not use when nesting communicates required transaction or cleanup scope. Safe steps: identify special cases, move them first, keep normal path last. Verify all branches.
- `Replace Conditional with Polymorphism`: Symptom: behavior varies by stable type/state and conditionals repeat. Use after variation ownership is clear. Do not use for simple one-off conditionals or factory selection. Safe steps: create type/state structure, move variant behavior, replace conditional dispatch. Verify each variant.
- `Introduce Null Object`: Symptom: null checks dominate behavior. Use when neutral object can obey same interface. Do not use when absence is an error that should be explicit. Safe steps: create null object, replace null branches, preserve observable absence behavior. Verify absent and present cases.
- `Introduce Assertion`: Symptom: code depends on hidden state assumptions. Use to make invariants explicit. Do not use for normal validation or recoverable user errors. Safe steps: add assertion at boundary of assumption. Verify tests fail clearly when invariant is violated.
- `Rename Method`: Symptom: method name hides intent. Use when callers should understand behavior without reading body. Do not use if rename churn is unrelated to the change. Safe steps: rename definition and callers atomically. Verify references and public compatibility.
- `Add Parameter`: Symptom: method lacks data needed for its job. Use when passing occasional data beats storing it. Do not use if method should own or derive the data. Safe steps: add compatible signature, migrate callers, remove old signature when safe. Verify callers.
- `Remove Parameter`: Symptom: parameter no longer affects behavior. Use after confirming it is unused. Do not use if parameter is part of public compatibility. Safe steps: remove uses, migrate signatures, preserve compatibility path if needed. Verify callers.
- `Separate Query from Modifier`: Symptom: method both returns data and mutates state. Use when callers need clear intent. Do not use if atomic read-modify behavior is public contract. Safe steps: split query and command, update callers. Verify state changes and return values.
- `Parameterize Method`: Symptom: similar methods differ only by values. Use when one method with a parameter keeps intent clear. Do not use when parameter selects different behavior. Safe steps: create parameterized method, redirect old methods, remove duplicates if safe. Verify all value cases.
- `Replace Parameter with Explicit Methods`: Symptom: parameter selects distinct behavior. Use when separate names are clearer than flags or modes. Do not use for ordinary data. Safe steps: create explicit methods, route callers, remove selector parameter. Verify each behavior.
- `Preserve Whole Object`: Symptom: callers pass several values from one object. Use when callee naturally depends on whole concept. Do not use if it creates oversized dependency. Safe steps: change signature to object, update field reads, migrate callers. Verify dependency direction.
- `Replace Parameter with Method Call`: Symptom: caller passes data callee can obtain. Use to reduce redundant caller work. Do not use if it hides expensive or surprising dependency. Safe steps: move lookup to callee, remove parameter, update callers. Verify lookup behavior.
- `Introduce Parameter Object`: Symptom: parameters repeatedly travel together. Use when they form one concept. Do not use for random bag of unrelated arguments. Safe steps: create object, migrate signature, move related behavior. Verify construction and validation.
- `Remove Setting Method`: Symptom: field should not change after initialization. Use when immutability or lifecycle clarity matters. Do not use when mutation is valid domain behavior. Safe steps: set through constructor/factory, remove setter, update initialization. Verify object creation.
- `Hide Method`: Symptom: public method not intended for clients. Use to reduce interface surface. Do not use if external callers need it. Safe steps: check callers, reduce visibility, update tests. Verify public API.
- `Replace Constructor with Factory Method`: Symptom: creation needs naming, selection, caching, or controlled reference lookup. Use when `new` hides important creation policy. Do not use for simple construction. Safe steps: add factory, redirect construction, restrict constructor if safe. Verify creation paths.
- `Replace Error Code with Exception`: Symptom: status codes that callers must inspect represent exceptional failure. Use when failure should interrupt normal flow. Do not use for ordinary expected branch choices. Safe steps: throw exception, update callers, remove code checks. Verify failure handling.
- `Replace Exception with Test`: Symptom: callers use exceptions for avoidable expected conditions. Use when cheap pre-check exists. Do not use when failure is exceptional or race-prone. Safe steps: add query/test, update callers, keep exception for true violations. Verify normal and failure paths.

### Generalization Playbook

- `Pull Up Field`, `Pull Up Method`, `Pull Up Constructor Body`: Symptom: siblings duplicate members or setup. Use when superclass can honestly own shared part. Do not use when duplication is accidental or variants will diverge. Safe steps: move shared member up, update subclasses, remove duplicates. Verify all subclasses.
- `Push Down Field`, `Push Down Method`: Symptom: only some subclasses use superclass member. Use when superclass contract is too broad. Do not use if callers rely on superclass member. Safe steps: move member down, update references, narrow contract. Verify affected subtype callers.
- `Extract Subclass`: Symptom: only some instances need special behavior. Use when variation is stable and meaningful. Do not use for temporary flags or speculative categories. Safe steps: create subclass, move variant behavior, update construction. Verify base and variant behavior.
- `Extract Superclass`: Symptom: classes share real behavior or data. Use when common owner simplifies duplication. Do not use for coincidental method names. Safe steps: create superclass, pull up shared members, update inheritance. Verify all subclasses.
- `Extract Interface`: Symptom: clients use only common subset. Use when subset is real client contract. Do not use as generic abstraction habit. Safe steps: define interface, type clients to it, keep implementers honest. Verify client compilation and behavior.
- `Collapse Hierarchy`: Symptom: subclass and superclass are practically identical. Use when hierarchy adds no distinction. Do not use if remaining subclasses would violate substitutability. Safe steps: choose survivor, move members, replace references, delete empty type. Verify type expectations.
- `Form Template Method`: Symptom: similar algorithms share structure but vary steps. Use when skeleton and steps are stable. Do not use when algorithms are only superficially similar. Safe steps: align method names, pull up skeleton, push variant steps down. Verify all algorithms.
- `Replace Inheritance with Delegation`: Symptom: inheritance causes refused bequest or excessive coupling. Use when object uses another object rather than is that object. Do not use if subtype substitution is central. Safe steps: add delegate, forward needed behavior, replace inherited access. Verify public behavior.
- `Replace Delegation with Inheritance`: Symptom: class delegates nearly everything to object it truly is. Use rarely, when subtype relation is honest. Do not use if inheritance would create unused behavior. Safe steps: inherit, remove redundant delegate, update construction. Verify substitutability.

---

## Decision Anti-Patterns

- MUST NOT apply refactoring because its name sounds modern; apply it because it treats diagnosed smell.
- MUST NOT replace duplication with abstraction named worse than the duplicated code.
- MUST NOT keep forwarding class merely because deleting it requires caller updates.
- MUST NOT delete speculative or dead-looking code until generated, reflected, serialized, plugin-facing, and public usages are checked.
- MUST NOT add assertions for normal user input, expected absence, or recoverable errors.
- MUST NOT use exceptions as routine tests when callers can cheaply check the condition first.
- MUST NOT move behavior away from its data if that creates feature envy in opposite direction.

---

## Technique Execution Safety

### Extraction Safety

- Before `Extract Method`, MUST identify every variable fragment reads, writes, or returns.
- SHOULD leave variables local to extracted method when declared and used only inside fragment.
- SHOULD pass prior values as parameters only when extracted fragment genuinely needs them.
- MUST double-check any variable modified inside fragment; if later code needs changed value, return it explicitly or choose safer refactoring.
- SHOULD use `Replace Temp with Query` before extraction when temporary variables block clean method boundary.
- MUST name extracted method after its purpose, not mechanical steps it performs.
- MUST NOT extract fragment that hides important side effect behind harmless-sounding name.

### Inlining Safety

- Before `Inline Method`, MUST confirm method adds no useful name, abstraction, override point, or public contract.
- SHOULD inline only after checking all callers, especially where dynamic dispatch, inheritance, or interface calls may be involved.
- MUST NOT inline method if callers depend on it as part of public or test-facing API.
- Before `Inline Class`, MUST move all useful behavior and data to target class and update all references.
- MUST delete emptied class only after references, construction sites, tests, and documentation no longer require it.

### Moving Safety

- Before `Move Method`, MUST inspect which class owns most data the method uses.
- SHOULD extract moved fragment first when only part of method belongs elsewhere.
- MUST update all callers and preserve visibility intentionally; do not widen access only to make the move compile.
- Before `Move Field`, MUST migrate reads and writes through accessors or direct replacements in small sequence.
- MUST NOT move behavior away from its data if separation was deliberate and supports interchangeable behavior.

### Encapsulation Safety

- Before `Encapsulate Field`, SHOULD add access methods, migrate all direct readers and writers, then make field private.
- SHOULD review accessor callers after encapsulation; behavior may belong inside owning class rather than outside.
- Before `Encapsulate Collection`, MUST prevent callers from mutating internal collection directly.
- SHOULD expose add/remove operations that preserve invariants instead of settable collection.
- MUST NOT add trivial getters and setters as final design if they merely preserve public data under different names.

### Conditional Safety

- Before `Consolidate Conditional Expression`, MUST verify conditions are side-effect free.
- SHOULD extract consolidated condition into named query when expression is complex.
- Before `Consolidate Duplicate Conditional Fragments`, SHOULD move duplicate code before or after conditional only when that preserves execution order.
- Before `Replace Nested Conditional with Guard Clauses`, MUST identify normal path and preserve special-case behavior.
- Before `Replace Conditional with Polymorphism`, MUST confirm conditional varies by stable type, state, or strategy; otherwise prefer explicit methods or simpler conditional.

### Method Call Safety

- Before `Add Parameter`, MUST check whether method should instead own data as field or obtain it through existing collaborator.
- SHOULD preserve compatibility by creating new method or transition path before deleting old signature when callers are numerous or public.
- Before `Remove Parameter`, MUST confirm parameter is unused or no longer changes behavior.
- Before `Separate Query from Modifier`, MUST split state mutation from returned information and update callers to use the right method for each intent.
- Before `Replace Parameter with Explicit Methods`, MUST confirm parameter selects distinct behavior rather than ordinary data.
- Before `Introduce Parameter Object`, MUST confirm grouped parameters represent one concept, not arbitrary bag.
- MUST NOT simplify method call if simplification creates hidden dependencies between classes.

### Data Reorganization Safety

- Before `Replace Data Value with Object`, MUST define object's meaning, equality, validation, and allowed behavior.
- Before changing value/reference semantics, MUST decide whether identity, mutability, sharing, and lifecycle management are required.
- SHOULD make value objects immutable before replacing references with values.
- SHOULD use factory creation when replacing values with references, so callers receive canonical object.
- Before changing association direction, MUST identify which side owns updates and how consistency is maintained.
- MUST remove bidirectional association when one side does not need navigation.
- MUST NOT add bidirectional association unless both sides genuinely need it and consistency logic is explicit.

### Generalization Safety

- Before pulling members up, MUST confirm sibling duplication is real and superclass contract can honestly own the member.
- Before pushing members down, MUST confirm superclass no longer promises or needs the member.
- Before extracting superclass or interface, MUST identify real shared behavior or real client-facing subset.
- MUST NOT extract interface only because two classes happen to share method names.
- Before collapsing hierarchy, MUST check remaining subclasses for substitutability and public type expectations.
- Before replacing inheritance with delegation, MUST preserve delegated behavior and update construction and forwarding paths deliberately.
- MUST NOT replace delegation with inheritance unless delegating class truly is a subtype and inheritance will not create refused bequest.

---

## Safety and Tradeoff Rules

- MUST NOT introduce polymorphism, inheritance, bidirectional links, or new classes when simpler extraction or rename solves the problem.
- MUST NOT remove parameters, associations, or abstractions if that creates worse coupling or hides required variation.
- SHOULD prefer deleting unused structure before extending it.
- MUST preserve public compatibility or provide transition path when refactoring public interfaces.

---

## Refactoring Workflow for Agents

Before editing:

1. Identify requested behavior change or maintenance goal.
2. Scan touched area for smells using catalog above.
3. Name primary smell, its cost, and smallest useful refactoring.
4. Identify expected cleaner end state and stop condition.
5. Identify tests or checks that prove behavior is preserved.
6. Decide whether refactoring belongs before, after, or separate from feature work.

During editing:

1. Apply one named transformation at a time.
2. Keep code runnable after each meaningful step.
3. Rename, extract, move, inline, or encapsulate before introducing larger design structures.
4. Re-run relevant tests after risky movement, public interface changes, or changed state flow.
5. Re-check whether chosen technique is still smallest treatment.
6. Stop if refactoring exposes different, larger problem, and report new scope.

After editing:

1. Confirm behavior preservation.
2. Confirm original smell is reduced or removed.
3. Confirm no broader feature change hid in the refactor.
4. Confirm no new smell was introduced, especially middle-man, speculative generality, or inappropriate intimacy.
5. Confirm any intentionally untreated smell has a reason.
6. Report technique used, stop condition reached, and validation performed.
