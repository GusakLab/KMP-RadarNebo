---
name: grasp-knowledge
description: "GRASP responsibility-assignment audit and design guidance (Information Expert, Creator, Controller, Low Coupling, High Cohesion, Polymorphism, Pure Fabrication, Indirection, Protected Variations) for object-oriented code. Use when deciding which class should own a behavior or auditing responsibility placement; examples and search commands use PHP 8.4 and need translating to other languages."
---

# GRASP Principles Knowledge Base

## Overview

GRASP (General Responsibility Assignment Software Patterns, Craig Larman): guidelines for assigning responsibilities to classes and objects in object-oriented design. Principles are language-neutral. Examples here and in `references/` use PHP 8.4. Translate them to project's language and idioms.

| Principle | Core Question | Goal |
|-----------|---------------|------|
| **Information Expert** | Who has the data? | Assign to class with information |
| **Creator** | Who creates objects? | Assign creation responsibility |
| **Controller** | Who handles system events? | Coordinate use case flow |
| **Low Coupling** | How to reduce dependencies? | Minimize interconnections |
| **High Cohesion** | How to focus responsibilities? | Keep related things together |
| **Polymorphism** | How to handle type variations? | Use polymorphic operations |
| **Pure Fabrication** | What if no domain class fits? | Create artificial class |
| **Indirection** | How to decouple? | Add intermediate object |
| **Protected Variations** | How to handle change? | Hide variation points |

## Workflow

1. **Scope.** Pick module or use case to audit. For design question, start from responsibility in question ("who should compute the order total?").
2. **Gather signals.** Use violation signals below. Text search only produces leads. Confirm each by reading code and its callers.
3. **Map to a principle.** Name violated principle, evidence (`file:line`), and cost it causes today (change ripple, untestable unit, duplicated rule).
4. **Recommend the smallest move.** Move behavior to data owner, extract handler, inject abstraction, or replace type switch. Principles trade off: Indirection and Pure Fabrication add coupling points. Protected Variations pays off only at real, recurring variation points.
5. **Report.** Use [assets/report-template.md](assets/report-template.md). Report confirmed findings only. Do not compute scores.

## Violation Signals (language-neutral)

| Principle | Signals in the code | Search hints (adapt syntax) |
|---|---|---|
| Information Expert | Method mostly reads another object's fields or getters; chains like `a.getB().getC().x`; data classes with all logic in a "service" | two or more chained getter calls, e.g. `getB().getC()` |
| Creator | Objects constructed far from owning aggregate or container; same construction repeated in many places | `new [A-Z]\w+\(` (or language's constructor syntax) |
| Controller | Controllers or handlers holding business rules, persistence, and formatting; one controller handling unrelated use cases | file sizes under controller or handler directories |
| Low Coupling | Constructors with many dependencies; domain code depending on concrete infrastructure classes (ORM, SDK clients) | constructor parameter lists; infrastructure package imports in domain modules |
| High Cohesion | Vague names (`*Manager`, `*Helper`, `*Utils`, `*Processor`); methods sharing no fields; method names joined with "And" | class names; largest files |
| Polymorphism | `switch`/`match`/`if` chains on type or kind field repeated across several sites; `instanceof` ladders | `instanceof`, `switch`/`match` on `type`/`kind` |
| Protected Variations | Third-party SDKs or unstable APIs instantiated directly throughout code | direct SDK constructors |

Thresholds like "more than 7 dependencies" or "controllers over 100 lines" are inspection prompts, not rules. Judge against project norms. Runnable PHP search commands: appendix of [assets/report-template.md](assets/report-template.md).

## Quick Examples (PHP 8.4)

### Information Expert

```php
// BAD: Logic outside of object with data
final class OrderService
{
    public function calculateTotal(Order $order): Money
    {
        $total = Money::zero();
        foreach ($order->getLines() as $line) {
            $total = $total->add($line->getProduct()->getPrice()->multiply($line->getQuantity()));
        }
        return $total;
    }
}

// GOOD: Logic in class that has the data
final class Order
{
    public function total(): Money
    {
        return array_reduce(
            $this->lines,
            fn(Money $sum, OrderLine $line) => $sum->add($line->total()),
            Money::zero(),
        );
    }
}
```

### Low Coupling

```php
// BAD: Depends on concrete classes
final class ReportGenerator
{
    public function __construct(
        private DoctrineOrderRepository $orders,
        private SymfonyMailer $mailer,
    ) {}
}

// GOOD: Depends on abstractions
final readonly class ReportGenerator
{
    public function __construct(
        private OrderReader $orders,
        private Mailer $mailer,
    ) {}
}
```

### High Cohesion

```php
// BAD: Low cohesion - unrelated responsibilities
final class UserManager
{
    public function register(array $data): User { }
    public function sendEmail(User $user): void { }
    public function generateReport(): string { }
}

// GOOD: High cohesion - focused responsibilities
final readonly class UserRegistrationService
{
    public function register(RegistrationData $data): User { }
    public function confirmEmail(Token $token): void { }
}
```

## GRASP & DDD Integration

| GRASP | DDD Application |
|-------|-----------------|
| Information Expert | Entities hold own behavior |
| Creator | Aggregates create their entities |
| Controller | Application Services / Use Cases |
| Low Coupling | Bounded Context boundaries |
| High Cohesion | Aggregate consistency boundary |
| Polymorphism | Domain Services, Strategies |
| Pure Fabrication | Repositories, Factories, Specifications |
| Indirection | Anti-Corruption Layer, Adapters |
| Protected Variations | Ports & Adapters, Domain Events |

## References

Detailed patterns and examples in `references/`:
- `information-expert.md` — Tell Don't Ask, calculations in owner
- `creator.md` — Factory patterns, aggregation rules
- `controller.md` — Use case handlers, thin controllers
- `low-coupling.md` — Dependency injection, abstractions
- `high-cohesion.md` — Focused responsibilities
- `polymorphism.md` — Strategy pattern, type variations
- `pure-fabrication.md` — Repositories, specifications
- `indirection.md` — Adapters, mediators
- `protected-variations.md` — Stable interfaces
- `antipatterns.md` — Common GRASP violations
