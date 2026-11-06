# Behavioral Design Patterns

Behavioral patterns deal with **algorithms and the assignment of responsibilities** between objects.

---

## Chain of Responsibility
*Also known as: CoR, Chain of Command*

**Intent:** Pass requests along a chain of handlers. Each handler decides to process the request or pass it to the next.

**Problem:** A request needs to go through a series of checks/handlers (auth → validation → caching → ...) but the exact chain is unknown in advance.

**Solution:** Link handlers in a chain. Each handler has a reference to the next. Upon receiving a request, a handler either processes it or forwards it.

**Roles:**
- **Handler** — interface with `setNext(handler)` and `handle(request)`
- **BaseHandler** — optional abstract class; stores next handler; delegates by default
- **ConcreteHandler** — process or pass
- **Client** — assembles the chain; can trigger any handler, not just the first

**When to use:**
- Handle different request types in various ways with unknown order/types
- Must execute several handlers in a specific order
- Set of handlers should be dynamic at runtime

**Pros:** Decouple sender from receivers; OCP; add/remove handlers at runtime
**Cons:** Some requests may go unhandled

---

## Command
*Also known as: Action, Transaction*

**Intent:** Turn a request into a stand-alone object containing all request info. Enables parameterizing methods, queuing, logging, and undo.

**Problem:** UI elements trigger operations. Operations are hardcoded in button handlers. Copy/paste has to be duplicated across menu, toolbar, keyboard shortcut.

**Solution:** Extract the operation into a `Command` object with an `execute()` method. Sender holds a command and calls it. Receiver does the actual work.

**Roles:**
- **Invoker** — stores a Command; triggers it (doesn't know what it does)
- **Command** — interface with `execute()`
- **ConcreteCommand** — implements `execute()`; delegates to Receiver; stores undo state
- **Receiver** — contains business logic
- **Client** — creates and configures ConcreteCommands

**When to use:**
- Parameterize objects with operations
- Queue operations or schedule execution
- Implement reversible operations (undo/redo stack)
- Implement transactional behavior

**Pros:** SRP; OCP; undo/redo; deferred execution; compose commands
**Cons:** Code becomes complex (many classes for simple operations)

---

## Iterator

**Intent:** Traverse elements of a collection without exposing its underlying structure (list, tree, stack, etc.).

**Problem:** Need to traverse different collection types (arrays, trees, graphs) without coupling client code to the internal structure.

**Solution:** Extract traversal into an Iterator object. The collection provides a method to create its own iterator.

**Roles:**
- **Iterator** — interface: `hasNext()`, `next()`, optionally `currentItem()`
- **ConcreteIterator** — tracks position; implements traversal logic
- **IterableCollection** — interface with `createIterator()`
- **ConcreteCollection** — returns its specific iterator

**When to use:**
- Abstract traversal of a complex data structure
- Reduce duplication of traversal code across the app
- Support multiple traversal algorithms for the same collection

**Pros:** SRP; OCP; parallel iteration (multiple iterators); delay traversal
**Cons:** Overkill for simple collections

---

## Mediator
*Also known as: Intermediary, Controller*

**Intent:** Reduce chaotic dependencies by forcing objects to collaborate only through a mediator.

**Problem:** A tightly-coupled form: each field (checkbox, textbox, button) knows about and directly updates every other — a dependency web.

**Solution:** All components communicate only through a `Mediator` interface. The mediator coordinates the interaction.

**Roles:**
- **Mediator** — interface with `notify(sender, event)`
- **ConcreteMediator** — knows all components; implements coordination logic
- **Component** — knows the mediator; calls `mediator.notify()` instead of directly referencing others

**When to use:**
- Classes are tightly coupled because they refer to many others
- Can't reuse a component in a different context because it depends on many others
- Multiple if-conditionals reacting to the same set of object changes

**Pros:** SRP; OCP; reuse components easily; replaces many-to-many with one-to-many
**Cons:** Mediator can become a god object

**Relations:** Mediator vs. Observer — Mediator centralizes control; Observer distributes it. MVC's Controller is a Mediator.

---

## Memento
*Also known as: Snapshot*

**Intent:** Save and restore an object's previous state without revealing its implementation.

**Problem:** Implement undo. But to snapshot an object's state, you must access its private fields — breaking encapsulation.

**Solution:** Delegate state storage to the object itself (Originator creates a Memento). Caretaker stores/restores Mementos but never inspects their contents.

**Roles:**
- **Originator** — creates Mementos from current state; restores from a Memento
- **Memento** — stores a snapshot; interface is restricted so only the Originator can read it
- **Caretaker** — stores the history of Mementos; asks Originator to create/restore

**When to use:**
- Produce snapshots of an object's state to restore it later
- Direct access to object's fields would violate encapsulation

**Pros:** Doesn't break encapsulation; simplifies Originator
**Cons:** RAM-heavy if clients create Mementos frequently; Caretaker must track Originator's lifecycle

---

## Observer
*Also known as: Event-Subscriber, Listener, EventEmitter*

**Intent:** Define a subscription mechanism to notify multiple objects about events happening to another object.

**Problem:** A store's customers must be notified when a product is back in stock. Either the store spams all customers or customers spam the store (polling).

**Solution:** The publisher (subject) holds a list of subscribers. When an event occurs, it iterates and notifies all subscribers via a common interface.

**Roles:**
- **Publisher** (Subject) — maintains list of subscribers; `subscribe()`, `unsubscribe()`, `notifySubscribers()`
- **Subscriber** — interface with `update(context)`
- **ConcreteSubscriber** — reacts to the notification
- **Client** — creates publishers and subscribers; registers subscribers

**When to use:**
- Changes in one object may require changing others (number unknown)
- Objects should be able to observe others without being tightly coupled

**Pros:** OCP; dynamic relationships at runtime; loose coupling
**Cons:** Subscribers notified in random order; memory leaks if not unsubscribed

---

## State

**Intent:** Allow an object to alter its behavior when its internal state changes. Appears to change its class.

**Problem:** A `Document` behaves differently when Draft vs. Moderation vs. Published. Using if/switch everywhere is brittle and grows with every new state.

**Solution:** Extract state-related behaviors into separate State classes. The original object (Context) delegates behavior to a State object that represents its current state.

**Roles:**
- **Context** — holds a reference to current State; delegates behavior; exposes `setState()`
- **State** — interface with all state-specific methods
- **ConcreteState** — implements behavior for that state; may trigger state transitions on Context

**When to use:**
- Object behaves differently depending on state, state count is large, and state-specific code changes frequently
- Class is polluted with massive conditionals based on current state
- Code has many duplicate switch/if statements

**Pros:** SRP; OCP; eliminate conditionals
**Cons:** Overkill for few states; states aware of each other = coupling

**Relations:** Differs from Strategy (State can replace itself; strategies don't know about each other).

---

## Strategy

**Intent:** Define a family of algorithms, put each in a separate class, and make their objects interchangeable.

**Problem:** A route-planning app needs walking, car, and public transit directions. All crammed into `Navigator` class. Adding cycling means changing a tested class.

**Solution:** Extract each algorithm into its own Strategy class with a common interface. Context holds a Strategy and delegates the algorithm call.

**Roles:**
- **Context** — holds reference to Strategy; calls `strategy.execute(data)`
- **Strategy** — interface with `execute(data)`
- **ConcreteStrategy** — implements an algorithm

**When to use:**
- Use different variants of an algorithm at runtime
- Have many similar classes differing only in execution behavior
- Isolate business logic from implementation details of algorithms
- Replace massive conditionals that select variants of the same algorithm

**Pros:** Swap algorithms at runtime; isolate implementation; OCP
**Cons:** Clients must know differences between strategies; function callbacks can replace simple strategies

**Relations:** Decorator changes the skin, Strategy changes the guts. Compare with State (strategies don't know each other).

---

## Template Method

**Intent:** Define the skeleton of an algorithm in a base class, deferring some steps to subclasses.

**Problem:** Two classes have similar algorithms but differ in details. Duplicate code is hard to maintain.

**Solution:** Extract the common algorithm skeleton into a base class method. Make the differing steps either abstract (subclasses must implement) or hooks (subclasses may optionally override).

**Roles:**
- **AbstractClass** — declares the template method + abstract/hook steps
- **ConcreteClass** — implements abstract steps; optionally overrides hooks

**When to use:**
- Let clients extend only particular steps, not the whole algorithm
- Several classes contain nearly identical algorithms with minor differences

**Pros:** Pull duplicate code into base class; let subclasses extend partial algorithm
**Cons:** May violate LSP; harder to maintain as more steps are added; inversed control ("Hollywood Principle")

**Relations:** Factory Method is a specialization of Template Method. Strategy differs: Template Method uses inheritance to vary parts of algorithm; Strategy uses composition to swap whole algorithm.

---

## Visitor

**Intent:** Separate algorithms from the objects on which they operate.

**Problem:** Want to add new operations (export to XML, calculate area) to an existing class hierarchy without modifying those classes. But putting the operation in each class violates SRP.

**Solution:** Extract operations into a `Visitor` class. Each concrete visitor implements the operation for every element type. Elements `accept(visitor)`, passing `this` to the visitor.

**Roles:**
- **Visitor** — interface with `visit(ConcreteElementA)`, `visit(ConcreteElementB)`, etc.
- **ConcreteVisitor** — implements operations for each element type
- **Element** — interface with `accept(visitor)`
- **ConcreteElement** — calls `visitor.visit(this)` in `accept()`
- **Client** — creates visitors and elements; runs visitor over element tree

**Double dispatch:** `element.accept(visitor)` → `visitor.visit(element)` — the right method is chosen at runtime based on both the visitor type and the element type.

**When to use:**
- Perform an operation on all elements of a complex structure (e.g., AST)
- Clean up business logic of auxiliary behaviors
- New operations on a class hierarchy that shouldn't change existing classes

**Pros:** OCP (add new operations via new visitor classes); SRP; accumulate state across tree
**Cons:** Update all visitors when adding a new element class; visitors may need access to private fields

---

## Behavioral Patterns Comparison

| Pattern | Core Idea |
|---------|-----------|
| Chain of Responsibility | Request flows down a handler chain |
| Command | Request is an object; supports undo/queue |
| Iterator | Traverse a collection without knowing its structure |
| Mediator | All components go through a central coordinator |
| Memento | Snapshot and restore state without breaking encapsulation |
| Observer | Automatic notification of subscribers on events |
| State | Object changes behavior when its state changes |
| Strategy | Swap interchangeable algorithm implementations |
| Template Method | Algorithm skeleton in base class; steps in subclasses |
| Visitor | Add operations to class hierarchy without modifying it |