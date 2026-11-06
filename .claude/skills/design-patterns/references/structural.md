# Structural Design Patterns

## Adapter
*Also known as: Wrapper*

**Intent:** Let objects with incompatible interfaces collaborate.

**Problem:** Useful class (e.g., 3rd-party analytics lib) doesn't match interface your code expects.

**Solution:** Adapter wraps incompatible object and translates calls. Two variants:
- **Object Adapter** — wraps adaptee via composition (preferred; more flexible)
- **Class Adapter** — inherits from both target and adaptee (requires multiple inheritance)

**Roles:**
- **Target** — interface client code expects
- **Adaptee** — existing class with incompatible interface
- **Adapter** — implements Target, wraps Adaptee, translates calls

**When to use:**
- Existing class has incompatible interface
- Reuse existing subclasses lacking common functionality (wrap in adapter instead of duplicating)

**Pros:** SRP; OCP; add adapters without changing existing code
**Cons:** More complexity (new classes); sometimes simpler refactor is better

---

## Bridge

**Intent:** Split large class (or closely related classes) into two hierarchies — abstraction and implementation — developed independently.

**Problem:** Multiple orthogonal variation dimensions (e.g., `Shape` × `Color`) cause combinatorial class explosion.

**Solution:** Move one dimension into separate class hierarchy. Original class references it and delegates.

**Roles:**
- **Abstraction** — high-level control layer; references Implementation
- **RefinedAbstraction** — optional subclasses of Abstraction
- **Implementation** — interface for implementation classes
- **ConcreteImplementation** — platform-specific implementations

**Example:** `Shape` (abstraction) holds `Renderer` (implementation). `Circle` × `VectorRenderer` and `Circle` × `RasterRenderer` are only two objects, not four classes.

**When to use:**
- Divide monolithic class with several functionality variants
- Extend class in several orthogonal (independent) dimensions
- Switch implementations at runtime

**Pros:** Platform-independent abstractions; OCP; SRP
**Cons:** Harder to understand for highly cohesive classes

---

## Composite
*Also known as: Object Tree*

**Intent:** Compose objects into trees for part-whole hierarchies. Treat individual objects and compositions uniformly.

**Problem:** Represent hierarchies (file systems, UI widgets, org charts) so client code treats leaves and branches alike.

**Solution:** Common `Component` interface for `Leaf` and `Composite` (container). Container delegates to children, aggregates results.

**Roles:**
- **Component** — common interface with `execute()` (and optionally `add/remove/getChild`)
- **Leaf** — no children; does real work
- **Composite** — has children; delegates to them; may aggregate results
- **Client** — works only with `Component` interface

**When to use:**
- Tree-like object structures
- Client code should treat simple and complex elements uniformly

**Pros:** Handle complex trees through polymorphism; OCP
**Cons:** Hard to restrict which components can be children; may need runtime type checks

---

## Decorator
*Also known as: Wrapper*

**Intent:** Attach behaviors to objects by placing them in wrapper objects holding those behaviors.

**Problem:** Optional behaviors needed at runtime. Subclassing explodes (e.g., `CoffeeWithMilkAndSugarAndCaramel`).

**Solution:** Wrap object in decorator with same interface. Decorator adds its behavior, then delegates to wrapped object.

**Roles:**
- **Component** — common interface
- **ConcreteComponent** — base object being decorated
- **Decorator** — implements Component; references wrapped Component; delegates calls
- **ConcreteDecorator** — adds specific behavior before/after delegation

**Example:** `TextDataSource` → wrapped in `EncryptionDecorator` → wrapped in `CompressionDecorator`. Stacked at runtime.

**When to use:**
- Add behaviors at runtime without breaking existing code
- Inheritance is awkward or impossible (e.g., `final` class)
- Many combinations of optional behaviors

**Pros:** Extend behavior without subclassing; combine behaviors; SRP
**Cons:** Hard to remove specific decorator from stack; order-dependent

**Relations:** Differs from Proxy (Proxy controls access; Decorator adds behavior). Both use same structure.

---

## Facade

**Intent:** Simplified interface to complex subsystem.

**Problem:** Subsystem has many classes with many dependencies. Clients must know and configure all.

**Solution:** Facade class offers simple methods for common use cases. Delegates to subsystem classes, hides complexity.

**Roles:**
- **Facade** — simple interface to subsystem; knows which subsystem classes to use
- **Additional Facade** — optional; keeps one facade from growing too complex
- **Subsystem Classes** — do actual work; unaware of facade
- **Client** — uses only facade

**When to use:**
- Need limited but straightforward interface to complex subsystem
- Layer subsystem (facades as entry points between layers)

**Pros:** Isolate code from subsystem complexity
**Cons:** Can become "god object" coupled to all subsystem classes

**Relations:** Facade creates new interface; Adapter makes existing interfaces work together. Facade can be Singleton.

---

## Flyweight
*Also known as: Cache*

**Intent:** Fit more objects in RAM by sharing common state instead of storing it in each.

**Problem:** Millions of fine-grained objects (e.g., game particles) hold too much duplicate state (texture, color, sprite) → out of RAM.

**Solution:** Move **intrinsic state** (shared, immutable; e.g., texture) into Flyweight object. Keep **extrinsic state** (unique per context; e.g., position) outside. Flyweight Factory ensures flyweights are shared, not duplicated.

**Roles:**
- **Flyweight** — holds intrinsic state; takes extrinsic state as method parameters
- **FlyweightFactory** — creates/caches flyweights; returns existing ones when possible
- **Context** — holds extrinsic state + Flyweight reference
- **Client** — computes or stores extrinsic state; calls flyweight methods

**When to use:**
- Program must support huge number of similar objects that barely fit in RAM
- Objects hold duplicate state that can be extracted and shared

**Pros:** Saves significant RAM
**Cons:** Trades RAM for CPU (recompute extrinsic state); much more complex code

---

## Proxy
*Also known as: Surrogate*

**Intent:** Substitute or placeholder for another object, controlling access to it.

**Problem:** Add behavior (lazy init, access control, logging, caching) around real object without changing its interface or client.

**Solution:** Proxy class shares real service's interface. It wraps real object and intercepts calls, doing extra work before/after delegating.

**Common Proxy Types:**
- **Virtual Proxy** — lazy initialization (heavy object created only when needed)
- **Protection Proxy** — access control (check credentials before delegating)
- **Remote Proxy** — local representative for remote service
- **Logging Proxy** — logs requests
- **Caching Proxy** — caches results of expensive operations
- **Smart Reference** — tracks references; dismiss heavy object when no clients

**Pros:** Control service without clients knowing; manage lifecycle; OCP
**Cons:** Adds latency; complex

**Relations:** Unlike Decorator (adds behavior client knows about), Proxy manages lifecycle/access of subject object.
