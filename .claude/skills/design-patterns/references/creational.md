# Creational Design Patterns

## Factory Method
*Also known as: Virtual Constructor*

**Intent:** Interface for creating object; subclasses decide which class to instantiate.

**Problem:** Code tightly coupled to `new Truck()`. Adding `Ship` means touching everything.

**Solution:** Replace `new ConcreteClass()` with `createProduct()` factory method call. Subclasses override it to return other product types. Products share common interface.

**Roles:**
- **Product** — common interface (e.g., `Transport` with `deliver()`)
- **ConcreteProduct** — `Truck`, `Ship`
- **Creator** — declares factory method; holds core business logic
- **ConcreteCreator** — `RoadLogistics`, `SeaLogistics` — override factory method

**When to use:**
- Exact object type unknown until runtime
- Library users extend internal components via subclassing
- Save system resources by reusing existing objects

**Pros:** No tight coupling between creator and products; SRP; OCP
**Cons:** One subclass per product type; can grow large

**Relations:** Often evolves into Abstract Factory. Template Method uses Factory Method internally.

---

## Abstract Factory

**Intent:** Produce families of related objects without specifying their concrete classes.

**Problem:** Product families must stay consistent (e.g., macOS button pairs with macOS checkbox, not Windows checkbox).

**Solution:** Declare interface per product type, then Abstract Factory interface with creation method for each. One concrete factory per product family.

**Roles:**
- **AbstractFactory** — `GUIFactory` with `createButton()` and `createCheckbox()`
- **ConcreteFactory** — `WinFactory`, `MacFactory`
- **AbstractProduct** — `Button`, `Checkbox`
- **ConcreteProduct** — `WinButton`, `MacButton`, etc.
- **Client** — works only with abstract types

**When to use:**
- Code must work with multiple families of related products
- Class has set of Factory Methods blurring its primary responsibility

**Pros:** Products guaranteed compatible; no coupling to concrete classes; SRP; OCP
**Cons:** Many interfaces and classes; complex

**Relations:** Often built from Factory Methods. Can be Singleton.

---

## Builder

**Intent:** Build complex objects step by step; same process yields different representations.

**Problem:** Constructor with 10 optional parameters ("telescoping constructor"); most calls pass `null` for unused params.

**Solution:** Move construction into separate `Builder` class with individual setter methods. Optional `Director` class runs common build sequences.

**Roles:**
- **Builder** — declares construction steps (`buildWalls()`, `buildDoors()`)
- **ConcreteBuilder** — implements steps; provides `getResult()`
- **Director** — optional; knows right order of builder steps
- **Product** — resulting complex object

**When to use:**
- Eliminate "telescoping constructor" anti-pattern
- Create different representations of same product
- Step-by-step construction with deferred final assembly

**Pros:** Step-by-step construction; reuse construction code; SRP
**Cons:** More overall complexity (many new classes)

**Relations:** Combine with Composite to build complex trees. Compare with Prototype (clones instead of building from scratch).

---

## Prototype
*Also known as: Clone*

**Intent:** Copy existing objects without making code depend on their concrete classes.

**Problem:** Duplicating object, but some fields may be private, and copying code shouldn't know object's class.

**Solution:** Object clones itself. Declare `clone()` method in `Prototype` interface; concrete classes implement it.

**Roles:**
- **Prototype** — interface declaring `clone()`
- **ConcretePrototype** — implements `clone()`; copies even private fields
- **Prototype Registry** — optional; stores pre-built prototypes by name/key

**When to use:**
- Code shouldn't depend on concrete classes of objects to copy
- Fewer subclasses that differ only in initialization
- Skip costly initialization (e.g., DB queries) by cloning pre-configured objects

**Pros:** Clone complex objects without coupling; alternative to subclassing for pre-configuration
**Cons:** Cloning circular references is tricky; deep vs. shallow copy decisions

**Relations:** Prototype Registry resembles Flyweight. Abstract Factory can use prototypes internally.

---

## Singleton

**Intent:** Ensure class has only one instance; provide global access point to it.

**Problem:** Two separate issues packaged together: (1) ensure single instance; (2) provide global access.

**Solution:** Private constructor. Static method controls access to single instance (lazy-initialize on first call, cache, return on later calls).

**Roles:**
- **Singleton** — private constructor; static `getInstance()` method; single private static instance field

**Thread safety:** In multithreaded contexts, guard `getInstance()` with locking or use static initialization.

**When to use:**
- Class should have exactly one instance (e.g., shared DB connection, logger, config)
- Need stricter control than global variables

**Pros:** Guaranteed single instance; global access point; lazy initialization
**Cons:** Violates SRP (manages own lifecycle); hard to unit test; can mask bad design (disguised global state)

**Relations:** Many patterns (Facade, Abstract Factory) are often Singletons. Monostate pattern is alternative.
