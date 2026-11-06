---
name: design-patterns
description: "Choose or explain a Gang of Four pattern for a stated new design problem, including when plain code, a function, or the language's built-in feature is the better answer. Use when designing or teaching; to judge patterns already in existing code, use gof-patterns."
---

# Design Patterns Skill

Based on: *Dive Into Design Patterns* by Alexander Shvets (Refactoring.Guru, 2019)

## When to use

- Someone describes a design problem and asks which pattern, if any, fits.
- Someone asks you to explain a pattern, compare two patterns, or sketch one in their language.

To audit patterns already present in code (detection, over-engineering, erosion), use `gof-patterns`. For smell-driven restructuring of existing code, use `refactoring-guru`.

## First: is a pattern needed at all?

Every pattern adds indirection. Recommend one only when the force it addresses exists **today**:

- There are two or more real variants in use, or a concrete extension point with real clients. One implementation plus "we might add more" is not a force.
- The variation point actually changes. Check how the code has evolved, not how it might.
- A plain function, a map or lookup table, a closure or function parameter, a module, or a language feature (enum with methods, sum types, generators, decorators, default arguments) solves it with fewer moving parts.

If plain code is enough, say so and show it. If you do recommend a pattern, name the simpler alternative you rejected and why. Verdicts to borrow when evaluating a proposed design: **appropriate** (forces present, benefit worth the indirection), **over-engineered** (solves variation that doesn't exist, such as a factory returning one type or an abstract factory with one family; name the simpler replacement), **misapplied** (a different pattern or none fits better).

## Core OOP Pillars (Quick Reference)

| Pillar | Definition |
|--------|-----------|
| **Abstraction** | Model only the attributes/behaviors relevant to the current context — ignore the rest |
| **Encapsulation** | Hide implementation details behind a public interface; protect internals from the outside |
| **Inheritance** | Build new classes on top of existing ones; subclasses inherit state and behavior |
| **Polymorphism** | A program detects the real class of an object at runtime and calls its actual implementation |

**Object Relations (weakest → strongest):**
- **Dependency** — object uses another temporarily (method param / return)
- **Association** — object has a permanent link to another (field)
- **Aggregation** — "has-a" but component can exist independently
- **Composition** — "whole-part"; component only exists inside the container

---

## Software Design Principles

### The Three Core Principles
1. **Encapsulate What Varies** — Identify what changes; isolate it so changes don't ripple everywhere
2. **Program to an Interface, not an Implementation** — Depend on abstractions, not concrete classes
3. **Favor Composition over Inheritance** — Compose behaviors at runtime rather than baking them in via subclasses

### SOLID
See `references/solid.md` for detailed examples and checklist.

| Letter | Principle | One-liner |
|--------|-----------|-----------|
| **S** | Single Responsibility | A class has just one reason to change |
| **O** | Open/Closed | Open for extension, closed for modification |
| **L** | Liskov Substitution | Subclasses must be substitutable for their superclass |
| **I** | Interface Segregation | Don't force clients to depend on methods they don't use |
| **D** | Dependency Inversion | High-level and low-level modules both depend on abstractions |

---

## Pattern Catalog Overview

Patterns split into three families:

| Family | Purpose | Patterns |
|--------|---------|---------|
| **Creational** | Object creation mechanisms | Factory Method, Abstract Factory, Builder, Prototype, Singleton |
| **Structural** | Assembling objects into larger structures | Adapter, Bridge, Composite, Decorator, Facade, Flyweight, Proxy |
| **Behavioral** | Communication & responsibility between objects | Chain of Responsibility, Command, Iterator, Mediator, Memento, Observer, State, Strategy, Template Method, Visitor |

**For detailed pattern references, see:**
- `references/creational.md`
- `references/structural.md`
- `references/behavioral.md`

---

## Pattern Selection Guide

Use this when the user describes a problem and needs to pick a pattern:

### "I need to create objects but…"
| Problem | Pattern |
|---------|---------|
| Don't know type until runtime / want subclasses to decide | **Factory Method** |
| Need families of related objects | **Abstract Factory** |
| Constructor has too many params / need step-by-step construction | **Builder** |
| Need to copy complex objects | **Prototype** |
| Need exactly one instance of a shared resource (first consider creating one instance at startup and passing it in) | **Singleton** |

### "I need to structure classes but…"
| Problem | Pattern |
|---------|---------|
| Interfaces are incompatible / working with legacy code | **Adapter** |
| Class has multiple independent dimensions of variation | **Bridge** |
| Need to treat individual objects and trees of objects uniformly | **Composite** |
| Need to add behavior to objects at runtime without subclassing | **Decorator** |
| Subsystem is too complex; need a simple entry point | **Facade** |
| Huge number of similar fine-grained objects eating RAM | **Flyweight** |
| Need lazy init, access control, logging, caching around a real object | **Proxy** |

### "I need objects to communicate/behave but…"
| Problem | Pattern |
|---------|---------|
| Requests should pass through a chain of handlers | **Chain of Responsibility** |
| Need to parameterize objects with operations / support undo | **Command** |
| Need to traverse a collection without exposing its structure | **Iterator** |
| Objects are too tightly coupled to each other | **Mediator** |
| Need to save/restore object state (undo/snapshot) | **Memento** |
| Objects need to be notified when another object changes | **Observer** |
| Object behavior depends on state and changes at runtime | **State** |
| Need to switch algorithms/behaviors at runtime | **Strategy** |
| Subclasses should fill in the blanks of an algorithm skeleton | **Template Method** |
| Need to add operations to objects without modifying their classes | **Visitor** |

---

## How to Present a Pattern

When explaining any pattern, always cover:
1. **Intent** — one-sentence definition
2. **Problem** — the design pain it solves
3. **Solution** — the structural approach
4. **Structure** — the roles (Creator, Product, etc.) and how they relate
5. **When to use (Applicability)**
6. **Pros & Cons**
7. **Code example** (use user's language; pseudocode is fine if unknown)
8. **Relations** — similar/complementary patterns to mention

---

## Common Anti-Patterns & Pattern Remedies

| Smell | Remedy |
|-------|--------|
| God class doing too many things | Apply **SRP** + split into collaborators |
| Construction of one of several real variants is duplicated across call sites | **Factory Method** (or a plain constructor function); **Abstract Factory** only for real families |
| Deeply nested if/switch on type | **Strategy** or **State** |
| Subclass explosion (one class per feature combination) | **Decorator** or **Bridge** |
| Global mutable state | Pass the state explicitly or inject the dependency. **Singleton** does not fix global state; it is global state. Use it only for a genuinely single, stateless-or-synchronized resource |
| Tight coupling between subsystems | **Facade** or **Mediator** |
| Duplicate code in similar algorithms | **Template Method** |
| UI button/menu logic scattered | **Command** (for undo queues too) |
| Observers manually wired everywhere | **Observer** / event bus |
| Hard to test because of rigid dependencies | **Dependency Inversion**: inject the dependency (an interface only when there is more than one implementation or a test seam is needed) |