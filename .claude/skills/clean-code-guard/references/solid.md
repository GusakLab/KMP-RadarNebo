# SOLID — the five principles

Source: Robert C. Martin. Principles collected on Uncle Bob's "Principles of OOD" page on objectmentor.com (mirrored at butunclebob.com), updated on blog.cleancoder.com. Original papers: *C++ Report*, circa 1995–1996.

## Contents

- S: Single Responsibility Principle
- O: Open/Closed Principle
- L: Liskov Substitution Principle
- I: Interface Segregation Principle
- D: Dependency Inversion Principle
- How AI-generated code typically breaks SOLID
- Self-check for SOLID

---

## S — Single Responsibility Principle

**Definition (Martin 2014, hardened from original).** *"A module should be responsible to one, and only one, actor."*

Older form: "A class should have only one reason to change."

Source: blog.cleancoder.com — SRP, 2014.

### Why

Axis is *people*. Different stakeholders (Accounting, HR, DBA) want different things from same class. When needs change, they edit same file, conflict, break each other.

### Smells to flag

- One class has methods touching unrelated subsystems (persistence + presentation + business rules).
- Class methods serve disjoint stakeholder groups.
- Git history shows two distinct clusters of co-changing methods inside one class.

### Common misinterpretation

*"A class should do one thing."* No. SRP means **cohesion around an actor**, not method count. A 12-method `InvoiceRepository` answerable only to data-access layer satisfies SRP. A 3-method class with one HTTP call, one Jinja render, one DB write does not.

### Bad

```text
EmployeeReport
  calculatePay()    // Accounting
  reportHours()     // HR
  save()            // Data storage owner
```

### Good

```text
PayCalculator       // Accounting
HoursReporter       // HR
EmployeeRepository  // Data storage owner
```

---

## O — Open/Closed Principle

**Definition.** *"Software entities (classes, modules, functions) should be open for extension, but closed for modification."*

Originally Bertrand Meyer (1988, *Object-Oriented Software Construction*). Martin refocused it on polymorphic abstraction, not implementation inheritance.

Source: blog.cleancoder.com — OCP, 2014; Martin's 1996 paper PDF (Duke mirror).

### Why

New behavior should arrive as new code, not edits to working code.

### Smells to flag

- Branch dispatching on type tag or runtime type check — every new type means editing same function.
- New feature requires modifying N existing files instead of adding one.
- `match`/`enum` switches crossing module boundaries (policy reaching into details).

### Common misinterpretation

*"Never modify code."* Real rule: *modules containing high-level policy* should not change to accommodate new variants. Leaf code changes freely.

### Bad

```text
export(record, kind):
  if kind == "pdf":  return toPdf(record)
  if kind == "csv":  return toCsv(record)
  if kind == "json": return toJson(record)
  // adding "xml" requires editing this function
```

### Good

```text
exporters = {
  "pdf":  toPdf,
  "csv":  toCsv,
  "json": toJson,
}

export(record, kind):
  return exporters[kind](record)
// adding "xml" is one line in the table
```

---

## L — Liskov Substitution Principle

**Definition (Liskov & Wing, 1994).** *"If for each object o1 of type S there is an object o2 of type T such that for all programs P defined in terms of T, the behavior of P is unchanged when o1 is substituted for o2, then S is a subtype of T."*

Source: Martin's LSP paper PDF (LaBRI mirror).

### Why

Substitutability. Callers written against base type must keep working when handed a subtype. Otherwise polymorphism leaks abstraction.

### Smells to flag

- Subclass overrides a method to signal "not implemented" or "unsupported operation."
- Subclass **strengthens preconditions** (rejects inputs parent accepts).
- Subclass **weakens postconditions** (returns something parent guarantees against).
- Callers check runtime subtype to decide whether to call a method.

### Common misinterpretation

*"Subclasses must have the same methods."* That's signature compatibility: plain type-checking. LSP is **behavioral**:
- Preconditions can only **weaken** in subtype.
- Postconditions and invariants can only **strengthen**.
- Parameter types contravariant; return types covariant.

### The Rectangle/Square classic

```text
Rectangle
  setWidth(width)
  setHeight(height)
  area()

Square extends Rectangle
  setWidth(width):
    this.width = width
    this.height = width   // invariant: width == height
  setHeight(height):
    this.width = height   // invariant: width == height
    this.height = height
```
Caller holding `Rectangle` reference does `r.set_width(5); r.set_height(4); assert r.area() == 20`. With `Square`, assertion fails — LSP violated.

Fix is *not* patching the methods. Fix: `Square is-not-a Rectangle` in behavioral sense. Compose, don't inherit.

---

## I — Interface Segregation Principle

**Definition.** *"Clients should not be forced to depend on methods they do not use."* Equivalently: many client-specific interfaces beat one general-purpose interface.

Source: Martin's 1996 ISP paper, catalogued at butunclebob.com/ArticleS.UncleBob.PrinciplesOfOod.

### Why

Fat interfaces create transitive coupling. Clients get dragged into recompiles and test-fixtures for methods they never call.

### Smells to flag

- `Service` / `Manager` / `Repository` interface with 10+ methods; any given caller uses one or two.
- Implementations stubbing half the methods with no-op bodies, null/empty placeholders, or unimplemented failures (usually co-occurs with LSP violation).
- One mock object reconfigured differently across tests because interface is too broad.

### Common misinterpretation

*"Make interfaces small."* Not as count rule. ISP is **client-centric**: segregation follows *the set of methods a particular client uses*, not arbitrary method-count ceiling. Two clients with identical method needs can share one interface, even at 20 methods.

### Bad

```text
UserService
  create(...)
  read(...)
  update(...)
  delete(...)
  email(...)
  notify(...)
  audit(...)
  export(...)
```
Audit logger only needs `audit`. It now depends transitively on email and export subsystems.

### Good

```text
UserAuditor
  audit(...)

UserNotifier
  notify(...)
```

Implementations can satisfy multiple protocols. Callers depend only on what they use.

---

## D — Dependency Inversion Principle

**Definition (verbatim, two clauses).**
*(a) High-level modules should not depend on low-level modules. Both should depend on abstractions.*
*(b) Abstractions should not depend on details. Details should depend on abstractions.*

Source: Martin's 1996 *C++ Report* paper, archived at Wayback / objectmentor.com.

### Why

Policy must not transitively `import` mechanism, or policy becomes un-reusable and untestable.

### Smells to flag

- High-level module imports concrete low-level client inside business logic.
- Constructor that `new`/instantiates concrete collaborators instead of accepting them as parameters.
- Abstractions defined in *low-level* package (interface beside its database or service implementation) — ownership reversed. Interface belongs in **client's** package.
- Function signatures typed against concrete classes instead of interfaces, protocols, or abstract contracts.

### Common misinterpretation

*"DIP means use a DI container."* No. DIP concerns **the direction of source-code dependencies**. Plain constructor injection with no framework can satisfy DIP. Code using Spring can still violate it.

### Bad

```text
// billing/charge — high-level policy
import SqlUserRepository  // concrete import

chargeUser(userId, amount):
  repository = new SqlUserRepository()  // concrete instantiation
  user = repository.get(userId)
  ...
```

### Good

```text
// billing/user-repository — abstraction lives WITH the client (billing)
UserRepository
  get(userId) -> User

// billing/charge
chargeUser(userId, amount, repository: UserRepository):
  user = repository.get(userId)
  ...

// sql/user-repository — detail depends on the abstraction
SqlUserRepository satisfies UserRepository
  get(userId) -> User
```

Import arrows go `sql → billing` (detail → abstraction). They do not go `billing → sql`.

---

## How AI-generated code typically breaks SOLID

1. **God-module** from "do everything in one file" prompts — SRP + DIP + usually OCP.
2. **Type-tag dispatch chains** (`if kind == "pdf": ...`) — OCP.
3. **Unsupported-operation stubs in subclasses** when asked to "implement only the methods we need" — LSP + ISP.
4. **Concrete SDK/client imports at module load time** — DIP, hard to test without patching runtime.
5. **Mega-`Service` interfaces** with create/read/update/delete/email/notify/audit/export — ISP, usually SRP too.
6. **Silent precondition strengthening on override** — defensive-looking, breaks LSP: callers holding base type now crash on previously-valid inputs.
7. **Invariant-breaking "convenience" subclasses** (e.g., `ReadOnlyList(list)` overriding `append` to no-op) — LSP.
8. **Inverted ownership of abstractions** — interface/protocol/abstract contract in same file as concrete implementation. Cosmetic DIP fix, real dependency graph unchanged.

---

## Self-check for SOLID

Before shipping code:

1. (SRP) Any class in diff answering to more than one stakeholder group?
2. (OCP) Any change adding type-tag branch to existing function? Could it be data-driven (registry/strategy) instead?
3. (LSP) Any new subclass signaling "not implemented", tightening preconditions, or weakening postconditions?
4. (ISP) Any interface with a method your concrete client does not use?
5. (DIP) High-level package importing low-level concrete? Where do new abstractions live — with client or with implementation?
