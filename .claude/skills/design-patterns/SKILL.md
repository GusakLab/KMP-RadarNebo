---
name: design-patterns
description: "Choose or explain a Gang of Four pattern for a stated new design problem, including when plain code, a function, or the language's built-in feature is the better answer. Use when designing or teaching; to judge patterns already in existing code, use gof-patterns."
---

# Design Patterns Skill

Based on: *Dive Into Design Patterns* by Alexander Shvets (Refactoring.Guru, 2019)

## When to use

- User describes design problem, asks which pattern (if any) fits.
- User asks to explain pattern, compare two, or sketch one in their language.

To audit patterns already in code (detection, over-engineering, erosion), use `gof-patterns`. For smell-driven restructuring of existing code, use `refactoring-guru`.

## First: is a pattern needed at all?

Every pattern adds indirection. Recommend one only when its force exists **today**:

- Two or more real variants in use, or concrete extension point with real clients. One implementation plus "we might add more" is not a force.
- Variation point actually changes. Check how code evolved, not how it might.
- Plain function, map or lookup table, closure or function parameter, module, or language feature (enum with methods, sum types, generators, decorators, default arguments) solves it with fewer moving parts.

If plain code is enough, say so and show it. If recommending pattern, name rejected simpler alternative and why. Verdicts for evaluating proposed design: **appropriate** (forces present, benefit worth indirection), **over-engineered** (solves variation that doesn't exist, such as factory returning one type or abstract factory with one family; name simpler replacement), **misapplied** (different pattern or none fits better).

## Core OOP Pillars (Quick Reference)

| Pillar | Definition |
|--------|-----------|
| **Abstraction** | Model only attributes/behaviors relevant to current context; ignore rest |
| **Encapsulation** | Hide implementation details behind public interface; protect internals from outside |
| **Inheritance** | Build new classes on existing ones; subclasses inherit state and behavior |
| **Polymorphism** | Program detects object's real class at runtime and calls its actual implementation |

**Object Relations (weakest → strongest):**
- **Dependency** — object uses another temporarily (method param / return)
- **Association** — object has permanent link to another (field)
- **Aggregation** — "has-a", but component can exist independently
- **Composition** — "whole-part"; component exists only inside container

---

## Software Design Principles

### The Three Core Principles
1. **Encapsulate What Varies** — Identify what changes; isolate it so changes don't ripple everywhere
2. **Program to an Interface, not an Implementation** — Depend on abstractions, not concrete classes
3. **Favor Composition over Inheritance** — Compose behaviors at runtime; don't bake them in via subclasses

### SOLID
Examples and checklist: `references/solid.md`.

| Letter | Principle | One-liner |
|--------|-----------|-----------|
| **S** | Single Responsibility | Class has only one reason to change |
| **O** | Open/Closed | Open for extension, closed for modification |
| **L** | Liskov Substitution | Subclasses must be substitutable for their superclass |
| **I** | Interface Segregation | Don't force clients to depend on methods they don't use |
| **D** | Dependency Inversion | High-level and low-level modules both depend on abstractions |

---

## Pattern Catalog Overview

| Family | Purpose | Patterns |
|--------|---------|---------|
| **Creational** | Object creation mechanisms | Factory Method, Abstract Factory, Builder, Prototype, Singleton |
| **Structural** | Assembling objects into larger structures | Adapter, Bridge, Composite, Decorator, Facade, Flyweight, Proxy |
| **Behavioral** | Communication & responsibility between objects | Chain of Responsibility, Command, Iterator, Mediator, Memento, Observer, State, Strategy, Template Method, Visitor |

**Detailed pattern references:**
- `references/creational.md`
- `references/structural.md`
- `references/behavioral.md`

---

## Pattern Selection Guide

### "I need to create objects but…"
| Problem | Pattern |
|---------|---------|
| Type unknown until runtime / subclasses should decide | **Factory Method** |
| Families of related objects | **Abstract Factory** |
| Constructor has too many params / step-by-step construction needed | **Builder** |
| Copy complex objects | **Prototype** |
| Exactly one instance of shared resource (first consider creating one instance at startup and passing it in) | **Singleton** |

### "I need to structure classes but…"
| Problem | Pattern |
|---------|---------|
| Incompatible interfaces / legacy code | **Adapter** |
| Class has several independent dimensions of variation | **Bridge** |
| Treat individual objects and object trees uniformly | **Composite** |
| Add behavior to objects at runtime without subclassing | **Decorator** |
| Subsystem too complex; need simple entry point | **Facade** |
| Huge number of similar fine-grained objects eating RAM | **Flyweight** |
| Lazy init, access control, logging, caching around real object | **Proxy** |

### "I need objects to communicate/behave but…"
| Problem | Pattern |
|---------|---------|
| Requests pass through chain of handlers | **Chain of Responsibility** |
| Parameterize objects with operations / support undo | **Command** |
| Traverse collection without exposing its structure | **Iterator** |
| Objects too tightly coupled to each other | **Mediator** |
| Save/restore object state (undo/snapshot) | **Memento** |
| Objects need notice when another object changes | **Observer** |
| Object behavior depends on state, changes at runtime | **State** |
| Switch algorithms/behaviors at runtime | **Strategy** |
| Subclasses fill in blanks of algorithm skeleton | **Template Method** |
| Add operations to objects without modifying their classes | **Visitor** |

---

## How to Present a Pattern

Explaining any pattern, always cover:
1. **Intent** — one-sentence definition
2. **Problem** — design pain it solves
3. **Solution** — structural approach
4. **Structure** — roles (Creator, Product, etc.) and how they relate
5. **When to use (Applicability)**
6. **Pros & Cons**
7. **Code example** (user's language; pseudocode fine if unknown)
8. **Relations** — similar/complementary patterns to mention

---

## Common Anti-Patterns & Pattern Remedies

| Smell | Remedy |
|-------|--------|
| God class doing too many things | Apply **SRP**; split into collaborators |
| Construction of one of several real variants duplicated across call sites | **Factory Method** (or plain constructor function); **Abstract Factory** only for real families |
| Deeply nested if/switch on type | **Strategy** or **State** |
| Subclass explosion (one class per feature combination) | **Decorator** or **Bridge** |
| Global mutable state | Pass state explicitly or inject dependency. **Singleton** does not fix global state; it is global state. Use it only for genuinely single, stateless-or-synchronized resource |
| Tight coupling between subsystems | **Facade** or **Mediator** |
| Duplicate code in similar algorithms | **Template Method** |
| UI button/menu logic scattered | **Command** (for undo queues too) |
| Observers manually wired everywhere | **Observer** / event bus |
| Hard to test due to rigid dependencies | **Dependency Inversion**: inject dependency (interface only when more than one implementation exists or test seam is needed) |
