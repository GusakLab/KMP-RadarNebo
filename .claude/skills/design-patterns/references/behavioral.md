# Behavioral Design Patterns

## Chain of Responsibility
*Also known as: CoR, Chain of Command*

**Intent:** Pass requests along handler chain. Each handler processes request or passes it on.

**Problem:** Request goes through series of checks/handlers (auth → validation → caching → ...); exact chain unknown in advance.

**Solution:** Link handlers in chain; each references next. Handler processes request or forwards it.

**Roles:**
- **Handler** — interface with `setNext(handler)` and `handle(request)`
- **BaseHandler** — optional abstract class; stores next handler; delegates by default
- **ConcreteHandler** — process or pass
- **Client** — assembles chain; can trigger any handler, not only first

**When to use:**
- Handle different request types in various ways; order/types unknown
- Several handlers must run in specific order
- Handler set should change at runtime

**Pros:** Decouple sender from receivers; OCP; add/remove handlers at runtime
**Cons:** Some requests may go unhandled

---

## Command
*Also known as: Action, Transaction*

**Intent:** Turn request into stand-alone object holding all request info. Enables parameterizing methods, queuing, logging, undo.

**Problem:** UI elements trigger operations hardcoded in button handlers. Copy/paste gets duplicated across menu, toolbar, keyboard shortcut.

**Solution:** Extract operation into `Command` object with `execute()` method. Sender holds command and calls it; Receiver does work.

**Roles:**
- **Invoker** — stores Command; triggers it (doesn't know what it does)
- **Command** — interface with `execute()`
- **ConcreteCommand** — implements `execute()`; delegates to Receiver; stores undo state
- **Receiver** — holds business logic
- **Client** — creates and configures ConcreteCommands

**When to use:**
- Parameterize objects with operations
- Queue or schedule operations
- Reversible operations (undo/redo stack)
- Transactional behavior

**Pros:** SRP; OCP; undo/redo; deferred execution; compose commands
**Cons:** Code grows complex (many classes for simple operations)

---

## Iterator

**Intent:** Traverse collection without exposing its structure (list, tree, stack, etc.).

**Problem:** Traverse different collection types (arrays, trees, graphs) without coupling client code to internal structure.

**Solution:** Extract traversal into Iterator object. Collection creates its own iterator.

**Roles:**
- **Iterator** — interface: `hasNext()`, `next()`, optionally `currentItem()`
- **ConcreteIterator** — tracks position; implements traversal logic
- **IterableCollection** — interface with `createIterator()`
- **ConcreteCollection** — returns its specific iterator

**When to use:**
- Abstract traversal of complex data structure
- Cut duplicated traversal code across app
- Multiple traversal algorithms for same collection

**Pros:** SRP; OCP; parallel iteration (multiple iterators); delay traversal
**Cons:** Overkill for simple collections

---

## Mediator
*Also known as: Intermediary, Controller*

**Intent:** Cut chaotic dependencies; objects collaborate only through mediator.

**Problem:** Tightly-coupled form: each field (checkbox, textbox, button) knows and directly updates every other — dependency web.

**Solution:** Components communicate only through `Mediator` interface, which coordinates interaction.

**Roles:**
- **Mediator** — interface with `notify(sender, event)`
- **ConcreteMediator** — knows all components; implements coordination logic
- **Component** — knows mediator; calls `mediator.notify()` instead of referencing others directly

**When to use:**
- Classes tightly coupled through references to many others
- Component not reusable elsewhere because it depends on many others
- Many if-conditionals react to same set of object changes

**Pros:** SRP; OCP; reuse components easily; replaces many-to-many with one-to-many
**Cons:** Mediator can become god object

**Relations:** Mediator vs. Observer — Mediator centralizes control; Observer distributes it. MVC's Controller is a Mediator.

---

## Memento
*Also known as: Snapshot*

**Intent:** Save and restore object's previous state without revealing its implementation.

**Problem:** Undo needs snapshots, but snapshotting reads private fields and breaks encapsulation.

**Solution:** Object snapshots itself (Originator creates Memento). Caretaker stores/restores Mementos, never inspects contents.

**Roles:**
- **Originator** — creates Mementos from current state; restores from Memento
- **Memento** — stores snapshot; restricted interface, so only Originator reads it
- **Caretaker** — keeps Memento history; asks Originator to create/restore

**When to use:**
- Snapshot object state to restore later
- Direct access to object's fields would violate encapsulation

**Pros:** Doesn't break encapsulation; simplifies Originator
**Cons:** RAM-heavy if clients create Mementos often; Caretaker must track Originator's lifecycle

---

## Observer
*Also known as: Event-Subscriber, Listener, EventEmitter*

**Intent:** Subscription mechanism that notifies multiple objects of events on another object.

**Problem:** Customers must hear when product is back in stock. Either store spams all customers or customers spam store (polling).

**Solution:** Publisher (subject) keeps subscriber list. On event, it notifies all subscribers via common interface.

**Roles:**
- **Publisher** (Subject) — keeps subscriber list; `subscribe()`, `unsubscribe()`, `notifySubscribers()`
- **Subscriber** — interface with `update(context)`
- **ConcreteSubscriber** — reacts to notification
- **Client** — creates publishers and subscribers; registers subscribers

**When to use:**
- Change in one object may require changing others (number unknown)
- Objects should observe others without tight coupling

**Pros:** OCP; dynamic relationships at runtime; loose coupling
**Cons:** Subscribers notified in random order; memory leaks if not unsubscribed

---

## State

**Intent:** Object alters behavior when internal state changes; appears to change class.

**Problem:** `Document` behaves differently in Draft vs. Moderation vs. Published. If/switch everywhere is brittle, grows with each new state.

**Solution:** Extract state behaviors into separate State classes. Context delegates to State object for its current state.

**Roles:**
- **Context** — references current State; delegates behavior; exposes `setState()`
- **State** — interface with all state-specific methods
- **ConcreteState** — implements behavior for that state; may trigger state transitions on Context

**When to use:**
- Behavior depends on state, many states exist, and state-specific code changes often
- Class polluted with massive conditionals on current state
- Many duplicate switch/if statements

**Pros:** SRP; OCP; eliminate conditionals
**Cons:** Overkill for few states; states aware of each other = coupling

**Relations:** Differs from Strategy (State can replace itself; strategies don't know about each other).

---

## Strategy

**Intent:** Define algorithm family, each in own class, with interchangeable objects.

**Problem:** Route planner needs walking, car, public transit directions, all crammed into `Navigator` class. Adding cycling changes tested class.

**Solution:** Each algorithm becomes Strategy class with common interface. Context holds Strategy, delegates algorithm call.

**Roles:**
- **Context** — references Strategy; calls `strategy.execute(data)`
- **Strategy** — interface with `execute(data)`
- **ConcreteStrategy** — implements an algorithm

**When to use:**
- Switch algorithm variants at runtime
- Many similar classes differ only in execution behavior
- Isolate business logic from algorithm details
- Replace massive conditionals selecting variants of same algorithm

**Pros:** Swap algorithms at runtime; isolate implementation; OCP
**Cons:** Clients must know differences between strategies; function callbacks can replace simple strategies

**Relations:** Decorator changes the skin, Strategy changes the guts. Compare with State (strategies don't know each other).

---

## Template Method

**Intent:** Algorithm skeleton in base class; some steps deferred to subclasses.

**Problem:** Two classes have similar algorithms differing in details. Duplicate code is hard to maintain.

**Solution:** Move common skeleton into base class method. Differing steps are abstract (subclasses must implement) or hooks (subclasses may override).

**Roles:**
- **AbstractClass** — declares template method + abstract/hook steps
- **ConcreteClass** — implements abstract steps; optionally overrides hooks

**When to use:**
- Clients extend only particular steps, not whole algorithm
- Several classes hold nearly identical algorithms with minor differences

**Pros:** Pull duplicate code into base class; subclasses extend partial algorithm
**Cons:** May violate LSP; harder to maintain as steps grow; inversed control ("Hollywood Principle")

**Relations:** Factory Method specializes Template Method. Template Method varies parts of algorithm via inheritance; Strategy swaps whole algorithm via composition.

---

## Visitor

**Intent:** Separate algorithms from objects they operate on.

**Problem:** Add operations (export to XML, calculate area) to existing class hierarchy without modifying those classes. Operation in each class violates SRP.

**Solution:** Move operations into `Visitor` class; each concrete visitor implements operation for every element type. Elements `accept(visitor)`, passing `this` to visitor.

**Roles:**
- **Visitor** — interface with `visit(ConcreteElementA)`, `visit(ConcreteElementB)`, etc.
- **ConcreteVisitor** — implements operations per element type
- **Element** — interface with `accept(visitor)`
- **ConcreteElement** — calls `visitor.visit(this)` in `accept()`
- **Client** — creates visitors and elements; runs visitor over element tree

**Double dispatch:** `element.accept(visitor)` → `visitor.visit(element)` — runtime picks method by both visitor type and element type.

**When to use:**
- Operation on all elements of complex structure (e.g., AST)
- Clean auxiliary behaviors out of business logic
- New operations on class hierarchy that shouldn't change existing classes

**Pros:** OCP (new operations via new visitor classes); SRP; accumulate state across tree
**Cons:** Adding element class means updating all visitors; visitors may need private field access
