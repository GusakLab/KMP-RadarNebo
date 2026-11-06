# Creational Design Patterns

Creational patterns deal with **object creation mechanisms**, increasing flexibility and reuse.

---

## Factory Method
*Also known as: Virtual Constructor*

**Intent:** Define an interface for creating an object, but let subclasses decide which class to instantiate.

**Problem:** Code is tightly coupled to `new Truck()`. Adding `Ship` means touching everything.

**Solution:** Replace `new ConcreteClass()` with a call to a `createProduct()` factory method. Subclasses override this method to return different product types. All products share a common interface.

**Roles:**
- **Product** — common interface (e.g., `Transport` with `deliver()`)
- **ConcreteProduct** — `Truck`, `Ship`
- **Creator** — declares the factory method; has core business logic
- **ConcreteCreator** — `RoadLogistics`, `SeaLogistics` — override factory method

**When to use:**
- You don't know the exact type of object to create until runtime
- You want library users to extend internal components via subclassing
- You want to save system resources by reusing existing objects

**Pros:** Avoids tight coupling between creator and products; SRP; OCP
**Cons:** Requires one subclass per product type — can grow large

**Relations:** Often evolves into Abstract Factory. Template Method uses Factory Method internally.

---

## Abstract Factory

**Intent:** Produce families of related objects without specifying their concrete classes.

**Problem:** You need to ensure product families are consistent (e.g., a macOS button should pair with a macOS checkbox, not a Windows checkbox).

**Solution:** Declare interfaces for each distinct product type. Then declare an Abstract Factory interface with creation methods for each. Concrete factories implement the abstract factory per product family.

**Roles:**
- **AbstractFactory** — `GUIFactory` with `createButton()` and `createCheckbox()`
- **ConcreteFactory** — `WinFactory`, `MacFactory`
- **AbstractProduct** — `Button`, `Checkbox`
- **ConcreteProduct** — `WinButton`, `MacButton`, etc.
- **Client** — works only with abstract types

**When to use:**
- Code must work with multiple families of related products
- A class has a set of Factory Methods that blur its primary responsibility

**Pros:** Products are guaranteed compatible; avoids coupling to concrete classes; SRP; OCP
**Cons:** Adds many interfaces and classes — complex

**Relations:** Often implemented using Factory Methods. Can be a Singleton.

---

## Builder

**Intent:** Construct complex objects step by step, allowing different representations using the same construction process.

**Problem:** A constructor with 10 optional parameters ("telescoping constructor") — most calls pass `null` for unused params.

**Solution:** Extract object construction into a separate `Builder` class with individual setter methods. An optional `Director` class orchestrates common build sequences.

**Roles:**
- **Builder** — declares construction steps (`buildWalls()`, `buildDoors()`)
- **ConcreteBuilder** — implements steps; provides `getResult()`
- **Director** — optional; knows how to call builder steps in the right order
- **Product** — the resulting complex object

**When to use:**
- Eliminate "telescoping constructor" anti-pattern
- Code needs to create different representations of the same product
- Want step-by-step construction with deferred final assembly

**Pros:** Construct objects step-by-step; reuse construction code; SRP
**Cons:** Overall complexity increases (many new classes)

**Relations:** Can be combined with Composite to build complex trees. Compare with Prototype (clones instead of building from scratch).

---

## Prototype
*Also known as: Clone*

**Intent:** Copy existing objects without making code dependent on their concrete classes.

**Problem:** Need to duplicate an object, but some fields may be private. Also, the code that wants to copy shouldn't know the object's class.

**Solution:** Delegate cloning to the object itself. Declare a `clone()` method in a `Prototype` interface. Concrete classes implement it.

**Roles:**
- **Prototype** — interface declaring `clone()`
- **ConcretePrototype** — implements `clone()`; handles copying even private fields
- **Prototype Registry** — optional; stores pre-built prototypes by name/key

**When to use:**
- Code shouldn't depend on the concrete classes of objects to copy
- Want to reduce the number of subclasses that differ only in initialization
- Reduce costly initialization (e.g., DB queries) by cloning pre-configured objects

**Pros:** Clone complex objects without coupling; alternative to subclassing for pre-configuration
**Cons:** Cloning circular references is tricky; deep vs. shallow copy decisions

**Relations:** Prototype Registry resembles Flyweight. Abstract Factory can use prototypes internally.

---

## Singleton

**Intent:** Ensure a class has only one instance and provide a global access point to it.

**Problem:** Two separate issues packaged together: (1) ensure single instance; (2) provide global access.

**Solution:** Make the constructor private. Create a static method that controls access to the single instance (lazy-initialize it on first call, cache it, return it on subsequent calls).

**Roles:**
- **Singleton** — private constructor; static `getInstance()` method; single private static instance field

**Thread safety:** In multithreaded contexts, protect `getInstance()` with locking or use static initialization.

**When to use:**
- A class should have exactly one instance (e.g., shared DB connection, logger, config)
- Need stricter control than global variables

**Pros:** Guaranteed single instance; global access point; lazy initialization
**Cons:** Violates SRP (manages its own lifecycle); hard to unit test; can mask bad design (disguised global state)

**Relations:** Many patterns (Facade, Abstract Factory) are often Singletons. Monostate pattern is an alternative.

---

## Creational Patterns Comparison

| Pattern | Key Question | Output |
|---------|-------------|--------|
| Factory Method | Which *subclass* creates the object? | One product, extensible via subclassing |
| Abstract Factory | Which *family* of products? | Suite of related products |
| Builder | How is the object *assembled*? | One complex object, step-by-step |
| Prototype | Should we *copy* an existing object? | Clone of an existing instance |
| Singleton | Should only *one* instance exist? | Single shared instance |