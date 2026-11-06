# GoF Pattern Catalog: Detection and Fit

## Contents
- Quick lookup (signals table)
- Creational: Abstract Factory, Builder, Factory Method, Prototype, Singleton
- Structural: Adapter, Bridge, Composite, Decorator, Facade, Flyweight, Proxy
- Behavioral: Chain of Responsibility, Command, Interpreter, Iterator, Mediator, Memento, Observer, State, Strategy, Template Method, Visitor
- Commonly confused groups
- Sources

Fields per entry:
- **Intent**: purpose.
- **Canonical examples**: well-known real uses, mostly JDK. Tags like [CMU] or [Logica] point to Sources at end.
- **Confirm**: participants that must be present.
- **Signals**: grep and reading hints. Leads, not proof.
- **Idiomatic**: language-native forms that count.
- **Fits when**: forces justifying it.
- **Smells**: over-engineering or misuse signs.
- **Confused with**: lookalikes.

## Quick lookup

| Pattern | Name signals | Structural signals |
|---|---|---|
| Abstract Factory | `*Factory` interface with several `create*` methods | Parallel concrete factories, each making a family of related products |
| Builder | `*Builder`, `.build()`, `with*()` | Step-wise setters returning `this`, then terminal `build()` |
| Factory Method | `create*`, `make*`, `new*` (abstract/overridable) | Superclass calls overridable creation hook; subclasses decide concrete type |
| Prototype | `clone`, `copy`, `deepCopy`, `__copy__` | New objects copied from configured instance, often via prototype registry |
| Singleton | `getInstance`, `instance`, `shared`, `default` | Private constructor + static holder; module-level instance; `@lru_cache` getter |
| Adapter | `*Adapter`, `*Wrapper`, `to*` | Implements Target interface, delegates to incompatible Adaptee |
| Bridge | `*Impl`, `*Implementor`, `*Driver`, `*Backend` | Abstraction hierarchy references separate implementation hierarchy |
| Composite | `children`, `add/remove`, `Node`, `Component` | Leaf and container share interface; container runs same op over children |
| Decorator | `*Decorator`, `wrap`, `with*`, middleware | Wraps object of **same** interface, adds behavior, delegates |
| Facade | `*Facade`, `*Service`, `*Client`, `*Manager` | One class, simplified API over several subsystem objects |
| Flyweight | `*Pool`, `*Cache`, `intern`, `getOrCreate` | Shared immutable intrinsic state keyed in a factory; extrinsic state passed in |
| Proxy | `*Proxy`, `lazy`, `Remote*`, `Cached*`, `Guarded*` | Same interface as subject, controls **access** (lazy, remote, auth, cache) |
| Chain of Responsibility | `setNext`, `next`, `handle`, `middleware`, `pipeline` | Handlers linked; each handles or forwards |
| Command | `*Command`, `execute`, `undo`, `Action`, `Job` | Request objects with `execute()`; invoker queues/logs/undoes them |
| Interpreter | `interpret`, `evaluate`, `Expression`, `AST`, `Node` | Grammar classes, each evaluating itself recursively against a context |
| Iterator | `Iterator`, `next`, `hasNext`, `__iter__`, `[Symbol.iterator]`, `yield` | Traversal object separate from collection |
| Mediator | `*Mediator`, `*Coordinator`, `*Hub`, `*Controller`, `EventBus` | Colleagues talk only to mediator, not to each other |
| Memento | `*Memento`, `snapshot`, `save/restore`, `getState/setState` | Originator exports opaque state; caretaker stores it without inspecting it |
| Observer | `subscribe`, `addListener`, `on`, `emit`, `notify`, `*Listener` | Subject holds observer list, notifies on change |
| State | `*State`, `transitionTo`, `setState`, `context.state` | Context delegates to state object; states trigger transitions themselves |
| Strategy | `*Strategy`, `*Policy`, `*Rule`, `*Algorithm` | Context holds interchangeable algorithm chosen by client |
| Template Method | `abstract` hooks, `do*`, `on*`, `before*/after*` | Base class defines fixed algorithm skeleton calling overridable steps |
| Visitor | `*Visitor`, `accept(visitor)`, `visit*` | Double dispatch: element calls `visitor.visitX(this)` |

---

## Creational

### Abstract Factory
- **Intent:** Create families of related objects without naming concrete classes.
- **Canonical examples:** GUI toolkit look-and-feel families [CMU]. Insurance product families [Logica]. Rare in JDK [CMU].
- **Confirm:** AbstractFactory interface with several creation methods. Two or more ConcreteFactories, one per family. AbstractProduct interfaces. Clients depend only on abstractions.
- **Signals:** `UIFactory.createButton()/createCheckbox()`, `DbFactory.createConnection()/createCommand()`. One factory picked at startup by config or platform.
- **Idiomatic:** Dict or module of constructors per family (for example `themes["dark"].Button`). DI container profiles.
- **Fits when:** Several real product families whose products must be used together (platform themes, DB vendors, cloud providers).
- **Smells:** Only one concrete factory: over-engineered. Factories making unrelated products: grab bag. Each new product type forces edits to every factory; if frequent, family boundary is wrong.
- **Confused with:** Factory Method (single product, chosen by subclassing). Builder (one complex product, built step by step).

### Builder
- **Intent:** Separate constructing a complex object from its representation, so one process can yield different results.
- **Canonical examples:** `StringBuilder` (one representation) [CMU]. Effective Java's `NutritionFacts.Builder` [CMU]. Director building wood or brick houses [Logica]. Compiler parsers [Logica].
- **Confirm:** Builder with step methods and terminal `build()`/`getResult()`. Optional Director sequencing steps. Full GoF form: several ConcreteBuilders, different representations.
- **Signals:** Fluent `with*()`/`set*()` methods returning `this`, then `build()`. Validation in `build()`.
- **Idiomatic:** Fluent "Effective Java" builder (no Director, one representation): common legitimate variant, classify as **Partial/Idiomatic**. Kotlin `apply {}`, named or default arguments, Python kwargs or dataclasses often make a builder unnecessary.
- **Fits when:** Many optional parameters; invariants must be checked before object exists; immutable object assembled incrementally; or one process yields several representations (SQL, HTML, documents). Bloch: Builder emulates named parameters where a language lacks them. Turns O(2ⁿ) constructor explosion into O(n) combinable setters, costing one intermediate object [CMU].
- **Smells:** Builder for 2–3 required fields, or in a language with named or default arguments: over-engineered. Object half-built or mutable after `build()`. Builder copying every target field with no validation.
- **Confused with:** Abstract Factory. Fluent interfaces generally: method chaining alone is not Builder.

### Factory Method
- **Intent:** Interface for creating an object; subclasses decide which class to instantiate.
- **Canonical examples:** `Iterable.iterator()`: each collection decides which `Iterator` to return [CMU]. `java.sql.Connection.createStatement()` returns vendor-driver-specific statement [Logica].
- **Confirm:** Creator with overridable creation method called by Creator's **own logic**. ConcreteCreators override it. Products share an interface.
- **Signals:** An `abstract createX()` in a base class whose other methods use the result. Framework hooks like `createView()` or `newThread()`.
- **Idiomatic:** Passing factory function or callback (`threadFactory`, `default_factory`): Factory Method by composition.
- **Fits when:** Framework or base class must create objects whose concrete type comes from subclasses or plugins.
- **Smells:** Static `create()` with `switch` on a string: **Simple Factory** or Static Factory. Not GoF, but "close enough" and very common [CMU]; name it so. Often fine, but not Factory Method. Sources disagree: Logica presents parameterized `createProduct(type)` as "the Factory pattern". GoF counts *parameterized factory methods* as a Factory Method variant only when subclasses can override them. Non-overridable static switch stays Simple Factory. Factory method with only one override, or returning `new X()` with no variation: over-engineered.
- **Confused with:** Simple Factory (not GoF), Abstract Factory, Template Method (Factory Method is a specialized Template Method step).

### Prototype
- **Intent:** Create objects by copying a prototypical instance.
- **Canonical examples:** `clone()` on arrays [CMU]. Copy account object, modify copy in a transaction, swap it in [Logica].
- **Confirm:** `clone()` on prototype interface. Clients clone configured instances instead of calling constructors, often via prototype registry.
- **Signals:** `clone()`, `copy.deepcopy`, `structuredClone`, spread-copy of templates, `prototypes[key].clone()` registry.
- **Idiomatic:** JS prototypal inheritance (`Object.create`) is a language mechanism, not automatically the pattern. Records or data classes with `copy(...)`/`with`-expressions.
- **Fits when:** Construction expensive, or configuration complex or runtime-driven, and many variants needed. Also dynamically loaded classes.
- **Smells:** Shallow clones accidentally sharing mutable state. Circular references, or classes that can't support copying [Logica]. Java's `Cloneable`/`clone()` especially: poor fit; prefer copy constructors or static copy factories [CMU]. `clone()` nothing calls. Cloning where plain constructor or factory is clearer.
- **Confused with:** Memento (copies state for restore, not creation). Flyweight (shares instances, no copying).

### Singleton
- **Intent:** Exactly one instance, with a global access point.
- **Canonical examples:** `java.lang.Runtime` [CMU]. Enum singleton (`enum Elvis { ELVIS }`) [CMU].
- **Confirm:** Instantiation restricted (private constructor, guard, or module scope) **and** global accessor **that returns an instance**. Class with only static members whose private constructor is never called (for example `final class Ids { private static final AtomicLong NEXT; … }`) is a **static utility class**: not a Singleton, not even "Partial", even with shared static state. List it under "considered and rejected".
- **Signals:** `getInstance()`, `static instance`, `INSTANCE`, `shared`, `@Singleton`, module-level `instance = X()`, memoized `get_x()`.
- **Idiomatic:** Module-level instance in Python, JS, or Go. Kotlin `object`. Java enum singleton. DI container singleton scope: healthier, since consumers get it by injection, not global lookup.
- **Fits when:** Truly process-unique (hardware handle, process-wide registry, logger) **and** global access acceptable. "Compelling uses are rare but they do exist" [CMU]. Some call it an anti-pattern, overused where one instance isn't required [Logica]. Singleton is one of the *instance-controlled* classes. Others: static utility class (no instances), enum (one instance per value, known at compile time), interned class (one canonical instance per value, created at runtime) [CMU]. Rule these out before calling something Singleton.
- **Smells:** Convenient global for mutable state: hides dependencies, hinders test isolation. `getInstance()` deep in business logic. Non-thread-safe lazy initialization in concurrent code. Several "singletons" needing reset in tests: prefer DI with single instance. Most often **over-engineered or misused** pattern; evaluate critically.
- **Confused with:** Monostate. Static utility classes (no instance at all). Flyweight factory.

---

## Structural

### Adapter
- **Intent:** Convert a class's interface into the one clients expect.
- **Canonical examples:** `Arrays.asList(T[])` [CMU].
- **Confirm:** Target interface client uses, Adaptee with **incompatible** interface, Adapter implementing Target by translating calls to Adaptee. Object adapter delegates; class adapter inherits.
- **Signals:** `*Adapter`, third-party SDK wrapper implementing an internal interface, `toDomain()`/`fromDto()` conversions inside it.
- **Idiomatic:** Function adapting a callback signature. Ports-and-adapters (hexagonal) architecture adapters.
- **Fits when:** Must use existing or third-party class whose interface does not match, or isolating a vendor dependency behind your own interface.
- **Smells:** Name trap: Java AWT's `WindowAdapter`, `MouseAdapter`, `KeyAdapter` and similar are convenience base classes with empty listener methods. *Not* GoF Adapters: they convert no interface, though some sources list them [Logica]. Treat similar `*Adapter` classes as name-only unless they translate between two interfaces. "Adapter" matching adaptee one-to-one with no translation: pass-through, over-engineered unless it exists only as test-double seam (then say so). Adapter accumulating business logic.
- **Confused with:** Decorator (same interface, adds behavior). Proxy (same interface, controls access). Facade (simplifies many objects, does not convert one). Bridge (designed in up front, not retrofitted).

### Bridge
- **Intent:** Decouple abstraction from implementation so both vary independently.
- **Canonical examples:** JDBC and JCE. In Service Provider Interface (SPI) frameworks, SPI is the Bridge's Implementor [CMU]. AWT peers [Logica].
- **Confirm:** Abstraction hierarchy (often refined by subclasses) referencing an Implementor interface with its own ConcreteImplementor hierarchy. **Both** sides vary.
- **Signals:** `Shape` → `Renderer`, `RemoteControl` → `Device`, `Logger` → `Sink/Transport`, JDBC-style drivers.
- **Idiomatic:** Composition with injected backend, when front side also has variants.
- **Fits when:** Otherwise M×N subclasses (for example shapes × renderers), and both dimensions change independently.
- **Smells:** Only one dimension varies: plain composition or Strategy; "Bridge" overstates it. Abstraction is one class, no refinements. Every call pays double indirection, abstraction to implementor [Logica]: usually negligible, real on hot paths.
- **Confused with:** Adapter (retrofitted to make things fit; Bridge designed up front). Strategy (algorithm swap, not two parallel hierarchies).

### Composite
- **Intent:** Compose objects into trees; treat single objects and compositions uniformly.
- **Canonical examples:** `javax.swing.JComponent` [CMU]. Expression trees [CMU].
- **Confirm:** Component interface. Leaf objects. Composite holding child Components, implementing operations by delegating to children, usually recursively.
- **Signals:** `children: Component[]`, `add/remove/getChild`, recursive `render()`/`size()`/`evaluate()` over a tree.
- **Idiomatic:** Recursive data types and algebraic data types (ADTs) with recursive functions. UI component trees, file-system nodes, AST nodes.
- **Fits when:** Domain genuinely part–whole hierarchical; clients gain from not distinguishing leaves from containers.
- **Smells:** Leaves forced to implement meaningless `add()` methods that throw: "transparency" cost, often acceptable, worth noting. Clients still checking `instanceof Composite`: degraded. Used for flat lists. Overly general: hard to restrict which component types a container may hold, so look for runtime type checks enforcing it [Logica].
- **Confused with:** Decorator (one wrapped child, adds behavior). Interpreter (built on Composite).

### Decorator
- **Intent:** Attach responsibilities dynamically by wrapping an object in one with **same interface**.
- **Canonical examples:** `java.io` streams, `Collections.synchronized*` / `unmodifiable*` wrappers, Swing borders [CMU].
- **Confirm:** Component interface. ConcreteComponent. Decorator implementing Component, holding a Component, delegating to it, adding behavior before or after. Decorators stack.
- **Signals:** `new Buffered(new Compressed(new FileStream()))`, `wrap()`, middleware wrapping a handler and returning a handler, `with*` HOCs.
- **Idiomatic:** Higher-order functions and Python `@decorators` (Decorator for functions). React HOCs. Java annotations or TS decorators used only as metadata are **not** this pattern.
- **Fits when:** Optional, combinable cross-cutting behaviors (logging, caching, retry, compression, auth) that would otherwise explode subclasses.
- **Smells:** Wide component interface: every decorator must forward every method, so each becomes error-prone [Logica]. Long chains add per-call overhead [Logica]. Deep stacks where order matters but is not documented. Decorator changing the interface (that is an Adapter). Decorators that unwrap or downcast to reach inner object. Only one decorator ever applied, always: inline it.
- **Confused with:** Proxy (controls access, usually manages subject's lifecycle; Decorator adds behavior, composed by client). Authoritative sources overlap: CMU lists `Collections.synchronized*` wrappers as *both* Decorator and Proxy (smart reference). When code supports both readings, say so; decide by dominant intent. Adapter. Composite. Chain of Responsibility (decorators always delegate; handlers may stop the chain).

### Facade
- **Intent:** Unified, simplified interface over a subsystem's interfaces.
- **Canonical examples:** `java.util.concurrent.Executors` [CMU]. GoF compiler subsystem example [CMU].
- **Confirm:** One class or module coordinating **several** subsystem objects behind smaller API. Subsystem stays accessible for advanced use.
- **Signals:** `*Facade`, `*Service`, `*Client`, or `*Manager` with high-level methods calling several lower-level components. SDK top-level clients.
- **Idiomatic:** Package `index` or `__init__` re-exporting curated API, only if it simplifies something.
- **Fits when:** Complex subsystem has common use cases callers would otherwise reimplement, or clients need layering and decoupling from subsystem churn.
- **Smells:** A "god facade" growing unrelated methods. Variant parameters or near-duplicate methods for callers each needing small variation; facade "ends up a mess" [Logica]. Facade wrapping one class one-to-one: Adapter or needless pass-through. Clients bypassing facade anyway: degraded.
- **Confused with:** Adapter (converts one interface). Mediator (Facade is one-directional; Mediator's colleagues know it, and it coordinates them both ways).

### Flyweight
- **Intent:** Share to support huge numbers of fine-grained objects efficiently.
- **Canonical examples:** All enums, `TimeUnit` (number of units is extrinsic state) [CMU]. Word-processor glyphs [Logica].
- **Confirm:** Intrinsic state (immutable, shared) separated from extrinsic state (client passes in). FlyweightFactory caches and returns shared instances by key.
- **Signals:** `getOrCreate(key)`, `intern`, glyph or tile or sprite caches, `Integer.valueOf` caching, object pools of immutable values.
- **Idiomatic:** String interning, enum instances, memoized immutable value objects.
- **Fits when:** Very many objects, most state can be extrinsic, memory is a measured problem.
- **Smells:** No evidence of memory pressure (premature optimization). Mutable shared instances: correctness bug. Plain cache of expensive results mistaken for it: a cache, not Flyweight. Object pool of *mutable, reused* objects is also not Flyweight.
- **Confused with:** Singleton (one instance versus many shared keyed instances). Prototype. Object Pool (not GoF).

### Proxy
- **Intent:** Surrogate controlling access to another object.
- **Canonical examples:** RMI stubs (remote proxy), collection wrappers (smart reference) [CMU]. Lazily loaded images (virtual proxy) [CMU, Logica].
- **Confirm:** Proxy implementing RealSubject's interface, holding or creating RealSubject, **controlling access**. Four GoF variants: virtual proxy (lazy creation), remote proxy (local stand-in for remote object), protection proxy (denies some operations to some callers), smart reference (locking, reference counting, or loading on first access) [CMU]. Caching proxies: common modern addition.
- **Signals:** `Lazy*`, `Remote*`, `*Stub`, `Cached*`, `Guarded*`, JS `Proxy` object, ORM lazy-loading entities, RPC stubs.
- **Idiomatic:** JS `Proxy`, Python `__getattr__` forwarding, generated RPC client stubs, Spring AOP proxies.
- **Fits when:** Access must be deferred, remote, guarded, or cached, transparently to caller.
- **Smells:** Proxy hiding expensive remote calls behind seemingly cheap local ones: N+1 or latency surprises. Transparency here can harm caller: note it. **Mixed access:** some code uses proxy, other code reaches real subject directly, so paths behave differently (for example, one skips the authorization check) [Logica]. Search for direct construction or injection of RealSubject. Proxy adding no control, only forwarding.
- **Confused with:** Decorator (see above). Adapter (different interface).

---

## Behavioral

### Chain of Responsibility
- **Intent:** Pass request along a handler chain. Each handles it or forwards to next.
- **Canonical examples:** `ClassLoader` delegation, `Properties` defaults. Exception propagation is a form of it [CMU]. Chain of error-logging handlers [Logica].
- **Confirm:** Handler interface with `handle(request)` and link to next handler (or list the dispatcher walks). Each handler decides independently: process, forward, or stop.
- **Signals:** `setNext`, `next()`, middleware pipelines, interceptor chains, `handlers.find(h => h.canHandle(x))` loop.
- **Idiomatic:** Express/Koa middleware, servlet filters, DOM event bubbling. List of predicate→handler pairs walked in order.
- **Fits when:** More than one handler might handle a request, handler set or order configured at runtime, sender should not know receiver.
- **Smells:** Hidden or undocumented order dependence. Requests silently falling off chain end with no default. Exactly one handler always matching by type: map lookup is simpler.
- **Confused with:** Decorator (always delegates). Command. Observer (all observers get notified; in a chain, typically one handler handles it).

### Command
- **Intent:** Encapsulate request as object, to parameterize, queue, log, or undo operations.
- **Canonical examples:** `Runnable`, executor framework [CMU]. Menu, button, popup actions sharing one command; undo and redo [Logica].
- **Confirm:** Command interface with `execute()` (optionally `undo()`). ConcreteCommands binding Receiver plus arguments. Invoker triggering them without knowing what they do. Client creating them.
- **Signals:** `*Command`, `execute`, `undo/redo` stacks, job and task queues, `Action` objects in editors, CQRS command handlers.
- **Idiomatic:** Closures or lambdas stored for later execution. Redux actions plus reducers (Command-like data object). Serialized jobs (Sidekiq, Celery).
- **Fits when:** Undo or redo, queuing or scheduling, macro recording, audit logging, retrying operations, or decoupling UI triggers from operations.
- **Smells:** Command hierarchy whose `execute()` runs immediately, never stored or queued: only indirection; use a method or function. Many small classes [Logica]: fair trade only when reification buys undo, queuing, logging, or binding one action to many triggers. Fuzzy boundary: does a reusable `Runnable` taking an argument and returning a value still count? [CMU] Judge by whether request is *treated as data*. Commands holding large mutable state.
- **Confused with:** Strategy (Strategy is *how* to do something, swapped into a context; Command is *what* to do, stored or executed by an invoker).

### Interpreter
- **Intent:** Grammar as classes; interpret sentences by evaluating a tree of them.
- **Canonical examples:** Regular expression matching, expression evaluators [CMU].
- **Confirm:** AbstractExpression with `interpret(context)`. TerminalExpressions and NonterminalExpressions (latter compose others: a Composite). A Context.
- **Signals:** `Expression`, `interpret`/`evaluate`, `AST`, rule engines, filter or query DSLs, `And/Or/Not` spec classes.
- **Idiomatic:** ADTs plus a recursive `eval` function. Specification pattern (domain-level interpreter).
- **Fits when:** Small, stable grammar; evaluation speed not critical; grammar changes should stay local.
- **Smells:** Large or growing grammar: use parser generator plus Visitor-based passes instead. Poor performance on hot paths. Hand-rolled string parsing scattered across code that should be a small grammar: missing-pattern signal.
- **Confused with:** Composite (Interpreter uses it). Visitor (alternative for adding evaluation operations over an AST).

### Iterator
- **Intent:** Access aggregate elements sequentially without exposing representation.
- **Canonical examples:** `Iterable`/`Iterator`, for-each statement [CMU, Logica].
- **Confirm:** Separate traversal object or protocol (`next`/`hasNext`, `__iter__`/`__next__`, `Symbol.iterator`) supplied by collection.
- **Signals:** Custom `Iterator` class, generators (`yield`), `Iterable` implementations, cursor or paginator classes.
- **Idiomatic:** GoF also counts *internal* iteration (collection drives loop, calls your function: `forEach`, `each`, `map`) as Iterator [CMU]. Almost always language-native. Generators and iterables count as **Idiomatic**. Do not report every `for` loop; report only *custom* iterators the user wrote.
- **Fits when:** Custom collection or data source (tree traversal, paginated API, stream) needs uniform or lazy traversal.
- **Smells:** Not safe under concurrent modification unless designed for it; one remedy: snapshot iteration state with a Memento [Logica]. Custom iterator duplicating a built-in. Iterator exposing internal indices. Modification during iteration with no fail-fast behavior.
- **Confused with:** Visitor (operations over heterogeneous elements versus sequential access).

### Mediator
- **Intent:** Object encapsulating how a set of objects interact, so they don't refer to each other directly.
- **Canonical examples:** Dialog box whose widgets affect each other [CMU, Logica]. Chat rooms and list servers [Logica].
- **Confirm:** Colleagues reference Mediator only and call it. Mediator knows colleagues, coordinates them. No colleague-to-colleague coupling.
- **Signals:** `*Mediator`, `*Coordinator`, dialog or form controllers coordinating widgets, chat rooms, air-traffic-control style classes. MediatR-style request dispatch is mostly Command dispatch; note the difference.
- **Idiomatic:** Event bus can act as mediator but is often closer to Observer or pub-sub; decide by whether central coordination logic exists.
- **Fits when:** Many-to-many interactions in a fixed component set got tangled. Mediator turns O(n²) pairwise couplings into O(n) [CMU].
- **Smells:** God mediator holding all business logic: trades interaction complexity for mediator complexity [Logica]. All traffic through one object: possible performance bottleneck [Logica]. Colleagues still referencing each other directly (degraded). Used for only two objects.
- **Confused with:** Facade (one-directional). Observer (distributed, no coordination logic). Event Aggregator or Pub-Sub (not GoF).

### Memento
- **Intent:** Capture and externalize object's internal state without violating encapsulation, for later restore.
- **Canonical examples:** Undo stack for line-art edits [CMU]. Database transactions [Logica]. Serialization alone is *not* Memento [CMU].
- **Confirm:** Originator creates Memento of its state and can restore from one. Caretaker stores mementos **without inspecting them**.
- **Signals:** `snapshot()/restore()`, `createMemento/setMemento`, undo history holding state blobs, `saveState/loadState`.
- **Idiomatic:** Immutable state snapshots (Redux history, persistent data structures). Serialization used for checkpoints.
- **Fits when:** Undo or rollback, checkpoints, transactional "try then revert".
- **Smells:** Caretaker reads or mutates memento internals, breaking encapsulation. Originator also changes *other* objects or resources the memento doesn't capture, so restore is only partial [Logica]. Full snapshots of huge state on every change: consider Command-based undo or diffs instead.
- **Confused with:** Command undo (inverse operations versus state snapshots). Prototype.

### Observer
- **Intent:** One-to-many dependency: when one object changes, all dependents get notified.
- **Canonical examples:** Swing listeners throughout [CMU]. Publish/subscribe [Logica].
- **Confirm:** Subject with subscribe and unsubscribe methods and observer list. Observers with update or callback method. Subject notifies them on state change without knowing their concrete types.
- **Signals:** `subscribe`, `addListener/removeListener`, `on/off/emit`, `notify`, `*Listener`, `EventEmitter`, signals, observables.
- **Idiomatic:** EventEmitter, RxJS, signals and stores, C# events, Kotlin Flow, reactive frameworks: count as Idiomatic. Evaluate how user's code uses them. GoF is agnostic on many details: push versus pull updates, who triggers notification, per-aspect subscriptions [CMU]. Do not mark down an implementation for choosing one of these variants.
- **Fits when:** Changes must propagate to a variable, unknown set of dependents, and loose coupling matters.
- **Smells:** Every subscriber notified of every change though most don't care: wasted communication [Logica]. Memory leaks from subscriptions never removed. Cascading or cyclic updates. Notification order relied on but not guaranteed. Hard-to-trace control flow ("who changed this?"). Single, permanent observer: direct call is simpler.
- **Confused with:** Mediator, Chain of Responsibility, Pub-Sub (decoupled through a broker, not GoF).

### State
- **Intent:** Object alters behavior when internal state changes; appears to change its class.
- **Canonical examples:** TCP connection [CMU]. Editor drawing-tool modes [Logica]. In Java: enum constants as states, held in an `AtomicReference<State>` [CMU].
- **Confirm:** Context delegates behavior to current State object. ConcreteStates implement that state's behavior **and usually trigger the transitions themselves** (or context does, based on state results).
- **Signals:** `*State` classes, `context.setState(new X())`, `transitionTo`, state-machine libraries (XState) or enum-plus-transition tables.
- **Idiomatic:** Java enum constants with constant-specific method bodies are per-state objects, so they count as State [CMU]. Enum plus transition table, or sealed class with `when`/`match` and no per-state behavior: state machine, maybe State pattern, maybe not. Use State pattern label only when behavior is delegated to per-state objects; otherwise call it a "state machine (non-GoF form)".
- **Fits when:** Behavior differs substantially per state, many states and transitions, and alternative is a large `switch (state)` repeated across several methods.
- **Smells:** Only 2–3 trivial states: enum plus `if` is simpler. States needing deep access to Context internals. Transitions scattered between Context and States, no single source of truth.
- **Confused with:** Strategy (structurally identical; in Strategy the *client* chooses and strategies don't know each other, whereas States know and trigger transitions).

### Strategy
- **Intent:** Family of interchangeable algorithms, selectable independently of clients using them.
- **Canonical examples:** `Comparator` [CMU]. Text-layout line-breaking algorithms [CMU].
- **Confirm:** Strategy interface. Two or more ConcreteStrategies. Context holding a strategy, delegating to it. **Client or config selects** the strategy.
- **Signals:** `*Strategy`, `*Policy`, `*Rule`, `*Comparator`, `*Algorithm`, `strategies[key]` map, dependency-injected algorithm interfaces.
- **Idiomatic:** Passing function or lambda (`sort(key=...)`, `Comparator`), or dict of functions. Count as real Strategy usage.
- **Fits when:** Several algorithm variants actually used, selected at runtime or by configuration, or a test seam needed.
- **Smells:** Clients must understand strategy differences to choose one [Logica]. If choosing is itself complex, centralize it in a factory or config, not in every caller. Single implementation, no test double: over-engineered. Context checking `instanceof` on strategy (degraded). Strategies knowing about each other or switching context's strategy: really State.
- **Confused with:** State, Command, Template Method (inheritance-based step variation versus composition-based whole-algorithm swap), Bridge.

### Template Method
- **Intent:** Algorithm skeleton in a base class; subclasses redefine certain steps without changing structure.
- **Canonical examples:** Skeletal collection implementations (`AbstractList`, etc.) [CMU].
- **Confirm:** Base-class method (ideally non-overridable or `final`) calling abstract or hook methods in fixed order. Subclasses override only the steps.
- **Signals:** Abstract class with one public driver method plus `protected abstract` steps. `beforeX/afterX/onX` hooks. Framework lifecycle methods.
- **Idiomatic:** Higher-order function taking step functions (closer to Strategy by composition). Framework lifecycles (`setUp/tearDown`, React class lifecycle).
- **Fits when:** Several classes share an invariant algorithm skeleton, differing only in specific steps.
- **Smells:** Deep inheritance chains. Subclasses overriding the template method itself, defeating its purpose. Many hooks most subclasses leave empty. Only one subclass. Consider composition (Strategy) when steps vary independently.
- **Confused with:** Strategy, its dual: one converts mechanically into the other, inheritance swapped for composition [CMU]. Factory Method.

### Visitor
- **Intent:** Represent an operation on elements of an object structure, so new operations can be added without changing element classes.
- **Canonical examples:** Type checking and pretty-printing over a parse tree [CMU].
- **Confirm:** Elements with `accept(visitor)` calling `visitor.visitConcreteX(this)`. Essence: **double dispatch**, first dynamically on visitor, then on visited element. Walking a structure and applying a function is *not* Visitor; that is Iterator [CMU]. Visitor interface with one `visit` method per element type. Two or more ConcreteVisitors (operations).
- **Signals:** `accept(`, `visit*(`, `*Visitor`, AST or compiler passes, document exporters.
- **Idiomatic:** Pattern matching over sealed types or ADTs (`match`, `when`, `switch` with exhaustiveness): modern replacement. Count as Idiomatic Visitor if used as "operations over a closed hierarchy".
- **Fits when:** Element hierarchy **stable**, new operations added often (compilers, linters, serializers).
- **Smells:** Element hierarchy changes often: every new type forces edits to every visitor; check git history. Single visitor: use a method. Visitors needing private element state, breaking encapsulation. Language with exhaustive pattern matching: Visitor ceremony may be unnecessary.
- **Confused with:** Iterator, Composite (Visitor often walks a Composite), Interpreter.

---

## Commonly confused groups

**Wrappers: Adapter, Decorator, Proxy, Facade, Bridge.** Ask:
- Same interface as wrapped object? No, converts one interface: **Adapter**. No, simplifies several objects: **Facade**.
- Same interface: why does wrapper exist? To add composable behavior: **Decorator**. To control access, lifetime, location, or permission: **Proxy**.
- Designed up front so two hierarchies vary independently? **Bridge**.

**Polymorphic swap: Strategy, State, Command, Template Method.**
- Client picks algorithm and keeps it: **Strategy**.
- Object switches own behavior as its state changes: **State**.
- Request reified so it can be stored, queued, or undone: **Command**.
- Steps vary via subclass overrides inside a fixed skeleton: **Template Method**.

**Creation: Factory Method, Abstract Factory, Builder, Prototype, Simple Factory.**
- Subclasses decide one product type: **Factory Method**.
- Product family from one swappable factory: **Abstract Factory**.
- One complex object assembled step by step: **Builder**.
- Copy a configured instance: **Prototype**.
- Static method with a `switch`: **Simple Factory**, not GoF.

**Communication: Observer, Mediator, Chain of Responsibility.**
- One-to-many broadcast, subject unaware of listeners: **Observer**.
- Central coordinator for many-to-many interactions: **Mediator**.
- Request passed along until something handles it: **Chain of Responsibility**.

---

## Sources

Catalog synthesized from these references. Paraphrases them; does not reproduce their text.

- **[GoF]** Gamma, Helm, Johnson, Vlissides. *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley, 1994. Primary definition of the 23 patterns.
- **[CMU]** Josh Bloch and Charlie Garrod. "23 Patterns in 80 Minutes: a Whirlwind Java-centric Tour of the Gang-of-Four Design Patterns". CMU 15-214, Spring 2016. https://www.cs.cmu.edu/~charlie/courses/15-214/2016-spring/slides/24%20-%20All%20the%20GoF%20Patterns.pdf. Intent, use case, key types, and JDK examples for all 23 patterns, plus Bloch's views on Builder and Singleton.
- **[Logica]** Benneth Christiansson (ed.) et al. *GoF Design Patterns – with examples using Java and UML2*. Logica Java Architects Training Crew, 2008. CC BY-SA 3.0. https://edeleastar.github.io/design-patterns/topic00/pdf/c-logica-gof-catalogue.pdf. Where to use, benefits, drawbacks, and usage examples for 20 patterns; does not cover Interpreter, Template Method, or Visitor.

Where sources disagree, catalog follows GoF and records the disagreement inline. Examples: Logica's "Factory pattern" (parameterized or simple factory) and AWT's `*Adapter` listener classes.

As CMU notes, "definitions can be vague, coverage is incomplete". So this skill reports confidence and relies on intent, not names.
