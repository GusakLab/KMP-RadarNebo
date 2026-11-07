# Structural Design Patterns

Structural patterns explain how to assemble objects and classes into larger structures while keeping those structures **flexible and efficient**.

---

## Adapter
*Also known as: Wrapper*

**Intent:** Allow objects with incompatible interfaces to collaborate.

**Problem:** You have a useful class (e.g., a 3rd-party analytics lib) but its interface doesn't match what your code expects.

**Solution:** Create an Adapter that wraps the incompatible object and translates calls. Two variants:
- **Object Adapter** — wraps the adaptee via composition (preferred; more flexible)
- **Class Adapter** — inherits from both target and adaptee (requires multiple inheritance)

**Roles:**
- **Target** — the interface your client code expects
- **Adaptee** — the existing class with incompatible interface
- **Adapter** — implements Target, wraps Adaptee, translates calls

**When to use:**
- Use an existing class but its interface is incompatible
- Reuse existing subclasses that lack common functionality (wrap them in adapter instead of duplicating)

**Pros:** SRP; OCP; can introduce adapters without changing existing code
**Cons:** Increases complexity (new classes); sometimes a simpler refactor is better

---

## Bridge

**Intent:** Split a large class (or set of closely related classes) into two separate hierarchies — abstraction and implementation — that can be developed independently.

**Problem:** A class with multiple orthogonal dimensions of variation (e.g., `Shape` × `Color`) leads to a combinatorial class explosion.

**Solution:** Extract one dimension into a separate class hierarchy. The original class holds a reference to it and delegates work.

**Roles:**
- **Abstraction** — high-level control layer; holds a reference to Implementation
- **RefinedAbstraction** — optional subclasses of Abstraction
- **Implementation** — interface for implementation classes
- **ConcreteImplementation** — platform-specific implementations

**Example:** `Shape` (abstraction) holds a `Renderer` (implementation). `Circle` × `VectorRenderer` and `Circle` × `RasterRenderer` are just two objects, not four classes.

**When to use:**
- Divide a monolithic class with several variants of functionality
- Extend a class in several orthogonal (independent) dimensions
- Switch implementations at runtime

**Pros:** Platform-independent abstractions; OCP; SRP
**Cons:** Harder to understand for highly cohesive classes

---

## Composite
*Also known as: Object Tree*

**Intent:** Compose objects into tree structures to represent part-whole hierarchies. Treat individual objects and compositions uniformly.

**Problem:** Need to represent hierarchies (file systems, UI widgets, org charts) and want client code to treat leaves and branches the same way.

**Solution:** Define a common `Component` interface for both `Leaf` and `Composite` (container). The container delegates work to its children and aggregates results.

**Roles:**
- **Component** — common interface with `execute()` (and optionally `add/remove/getChild`)
- **Leaf** — no children; does real work
- **Composite** — has children; delegates to them; may aggregate results
- **Client** — works only with `Component` interface

**When to use:**
- Implement tree-like object structures
- Client code should treat simple and complex elements uniformly

**Pros:** Work with complex tree structures using polymorphism; OCP
**Cons:** Hard to restrict which components can be children; may require runtime type checks

---

## Decorator
*Also known as: Wrapper*

**Intent:** Attach new behaviors to objects by placing them inside wrapper objects that contain the behaviors.

**Problem:** You need to add optional behaviors to objects at runtime. Subclassing explodes (e.g., `CoffeeWithMilkAndSugarAndCaramel`).

**Solution:** Wrap the object in a decorator that implements the same interface, adds its behavior, then delegates to the wrapped object.

**Roles:**
- **Component** — common interface
- **ConcreteComponent** — base object being decorated
- **Decorator** — implements Component; holds a reference to a Component; delegates calls
- **ConcreteDecorator** — adds specific behavior before/after delegation

**Example:** `TextDataSource` → wrapped in `EncryptionDecorator` → wrapped in `CompressionDecorator`. Stacked at runtime.

**When to use:**
- Add behaviors to objects at runtime without breaking existing code
- Extension via inheritance is awkward or impossible (e.g., `final` class)
- Need many combinations of optional behaviors

**Pros:** Extend behavior without subclassing; combine multiple behaviors; SRP
**Cons:** Hard to remove a specific decorator from a stack; order-dependent

**Relations:** Differs from Proxy (Proxy controls access; Decorator adds behavior). Both use same structure.

---

## Facade

**Intent:** Provide a simplified interface to a complex subsystem.

**Problem:** A complex subsystem has many classes with many dependencies. Clients must know and configure all of them.

**Solution:** Create a Facade class that provides simple methods for common use cases. It delegates to subsystem classes but hides the complexity.

**Roles:**
- **Facade** — simple interface to subsystem; knows which subsystem classes to use
- **Additional Facade** — optional; prevents one facade from becoming too complex
- **Subsystem Classes** — do actual work; unaware of the facade
- **Client** — uses only the facade

**When to use:**
- Need a limited but straightforward interface to a complex subsystem
- Structure a subsystem into layers (facades as entry points between layers)

**Pros:** Isolate code from subsystem complexity
**Cons:** Can become a "god object" coupled to all subsystem classes

**Relations:** Facade creates a new interface; Adapter makes existing interfaces work together. Facade can be a Singleton.

---

## Flyweight
*Also known as: Cache*

**Intent:** Fit more objects in RAM by sharing common state between multiple objects instead of storing it in each.

**Problem:** Millions of fine-grained objects (e.g., particles in a game) hold too much duplicate state (texture, color, sprite) → out of RAM.

**Solution:** Extract **intrinsic state** (shared, immutable; e.g., texture) into a Flyweight object. Keep **extrinsic state** (unique per context; e.g., position) outside. The Flyweight Factory ensures flyweights are shared, not duplicated.

**Roles:**
- **Flyweight** — holds intrinsic state; accepts extrinsic state as method parameters
- **FlyweightFactory** — creates/caches flyweights; returns existing ones when possible
- **Context** — holds extrinsic state + a reference to a Flyweight
- **Client** — computes or stores extrinsic state; calls flyweight methods

**When to use:**
- Program must support a huge number of similar objects that barely fit in RAM
- Objects contain duplicate states that can be extracted and shared

**Pros:** Saves significant RAM
**Cons:** Trades RAM for CPU (recompute extrinsic state); much more complex code

---

## Proxy
*Also known as: Surrogate*

**Intent:** Provide a substitute or placeholder for another object to control access to it.

**Problem:** Need to add behavior (lazy init, access control, logging, caching) around a real object without changing its interface or the client.

**Solution:** Create a Proxy class with the same interface as the real service. The Proxy wraps the real object and intercepts calls, doing extra work before/after delegating.

**Common Proxy Types:**
- **Virtual Proxy** — lazy initialization (heavy object created only when needed)
- **Protection Proxy** — access control (check credentials before delegating)
- **Remote Proxy** — local representative for a remote service
- **Logging Proxy** — logs requests
- **Caching Proxy** — caches results of expensive operations
- **Smart Reference** — tracks references; dismiss heavy object when no clients

**When to use:**
- Lazy init of heavyweight service object
- Control access to service object
- Execute something on a remote server
- Log requests; cache results
- Track lifecycle / dismiss unneeded objects

**Pros:** Control service without clients knowing; manage lifecycle; OCP
**Cons:** Adds latency; complex

**Relations:** Unlike Decorator (which adds behavior the client knows about), Proxy manages the lifecycle/access of a subject object.

---

## Structural Patterns Comparison

| Pattern | Core Mechanism | Key Difference |
|---------|---------------|----------------|
| Adapter | Wraps one interface to look like another | Compatibility fix for existing code |
| Bridge | Splits abstraction from implementation hierarchies | Two separate, independently extensible hierarchies |
| Composite | Recursive tree structure, uniform interface | Leaves and containers treated the same |
| Decorator | Wraps and adds behavior at runtime | Stacks of behavior enhancements |
| Facade | Single entry point to a subsystem | Simplification, not extension |
| Flyweight | Shared intrinsic state across many objects | RAM optimization for large numbers of objects |
| Proxy | Surrogate with same interface | Controls/intercepts access to the real object |