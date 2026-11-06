# SOLID Principles — Detailed Reference

## S — Single Responsibility Principle
> "A class should have just one reason to change."

**Goal:** Reduce complexity. One class = one concern.

**Smell:** You must change a class for two different reasons (e.g., business logic AND report format).

**Fix:** Extract the second concern into its own class.

**Example:**
- BAD: `Employee` class contains both employee data management AND timesheet formatting
- GOOD: Split into `Employee` (data) + `TimesheetReport` (formatting)

---

## O — Open/Closed Principle
> "Classes should be open for extension but closed for modification."

**Goal:** Add new features without breaking existing tested code.

**Approach:** Depend on abstractions. New variants are new subclasses/implementations, not edits.

**Example:**
- BAD: `Order` class has hardcoded shipping methods; adding a new one breaks the class
- GOOD: Extract a `Shipping` interface; each method is its own class. `Order` never changes.

**Related pattern:** Strategy — the canonical implementation of OCP.

---

## L — Liskov Substitution Principle
> "Objects of a subclass must be replaceable for objects of the superclass without breaking client code."

**Checklist for subclass methods:**
- Parameter types must be the **same or more abstract** than the superclass method (contravariance)
- Return types must be the **same or more specific** (covariance)
- Must not throw new exceptions the base method doesn't throw
- Must not **strengthen preconditions** (e.g., adding `if value < 0: throw`)
- Must not **weaken postconditions** (e.g., leaving DB connections open when base always closes them)
- Must **preserve invariants** of the superclass
- Must not modify private fields of the superclass (via reflection)

**Classic violation:**
- `ReadOnlyDocument extends Document` and overrides `save()` by throwing an exception — breaks client code that calls `save()` on any `Document`
- Fix: Make `ReadOnlyDocument` the base; `WritableDocument` extends it and adds `save()`

---

## I — Interface Segregation Principle
> "Clients shouldn't be forced to depend on methods they do not use."

**Goal:** Keep interfaces narrow and focused.

**Smell:** A class implementing an interface is forced to leave some methods empty or throw `UnsupportedOperationException`.

**Fix:** Split the fat interface into smaller, role-specific interfaces.

**Example:**
- BAD: `CloudProvider` interface has 20 methods; `SimpleStorage` cloud only supports 5 of them
- GOOD: Break into `StorageProvider`, `CDNProvider`, `ComputeProvider`, etc.

**Warning:** Don't go too far — over-splitting creates its own complexity.

---

## D — Dependency Inversion Principle
> "High-level classes shouldn't depend on low-level classes. Both should depend on abstractions."

**Two levels:**
- **Low-level** — basic ops: disk I/O, DB, network
- **High-level** — business logic that orchestrates low-level ops

**Problem without DIP:** Business logic is tightly coupled to a specific `MySQLDatabase` class. Changing DB = rewriting business logic.

**Solution:** Introduce a `Database` interface. Business logic depends on `Database`. `MySQLDatabase` and `PostgresDatabase` both implement it.

**Flow:**
```
HIGH-LEVEL → [Interface] ← LOW-LEVEL
```

**Related:** This principle is the foundation of Dependency Injection containers.

---

## SOLID + Patterns Quick Map

| SOLID Principle | Primary Supporting Patterns |
|----------------|----------------------------|
| SRP | Most patterns (patterns extract responsibilities) |
| OCP | Strategy, Template Method, Decorator |
| LSP | (Design correctness; violated by improper inheritance) |
| ISP | Adapter (adapts fat interfaces), role interfaces |
| DIP | Factory Method, Abstract Factory, Dependency Injection |