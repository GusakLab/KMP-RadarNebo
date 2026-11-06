# Refactoring Technique Catalog

Read this after you have diagnosed a smell (workflow step 1 in SKILL.md) and need the mechanics of a specific treatment. Open only the matching file. Each entry shows incorrect and correct code. The examples are mostly TypeScript, so translate them to the project's language and idioms.

The priority labels come from the original catalog and describe typical impact, not mandatory order. Always prefer the smallest treatment that removes the diagnosed smell (see [../refactoring-guru.mini.md](../../refactoring-guru.mini.md)). Some entries state fixed thresholds (for example "functions under 20 lines"). Treat those as inspection signals, not rules.

## 1. Structure & Decomposition (CRITICAL)

- [Extract Method for Long Functions](struct-extract-method.md) (`struct-extract-method`)
- [Apply Single Responsibility Principle](struct-single-responsibility.md) (`struct-single-responsibility`)
- [Extract Class from Large Class](struct-extract-class.md) (`struct-extract-class`)
- [Compose Method for Readable Flow](struct-compose-method.md) (`struct-compose-method`)
- [Keep Functions Under 20 Lines](struct-function-length.md) (`struct-function-length`)
- [Replace Method with Method Object](struct-replace-method-with-object.md) (`struct-replace-method-with-object`)
- [Introduce Parameter Object](struct-parameter-object.md) (`struct-parameter-object`)

## 2. Coupling & Dependencies (CRITICAL)

- [Use Dependency Injection](couple-dependency-injection.md) (`couple-dependency-injection`)
- [Hide Delegate to Reduce Coupling](couple-hide-delegate.md) (`couple-hide-delegate`)
- [Remove Middle Man When Excessive](couple-remove-middle-man.md) (`couple-remove-middle-man`)
- [Fix Feature Envy by Moving Methods](couple-feature-envy.md) (`couple-feature-envy`)
- [Apply Interface Segregation Principle](couple-interface-segregation.md) (`couple-interface-segregation`)
- [Preserve Whole Object Instead of Fields](couple-preserve-whole-object.md) (`couple-preserve-whole-object`)

## 3. Naming & Clarity (HIGH)

- [Use Intention-Revealing Names](name-intention-revealing.md) (`name-intention-revealing`)
- [Avoid Abbreviations and Acronyms](name-avoid-abbreviations.md) (`name-avoid-abbreviations`)
- [Use Consistent Vocabulary](name-consistent-vocabulary.md) (`name-consistent-vocabulary`)
- [Use Searchable Names](name-searchable-names.md) (`name-searchable-names`)
- [Avoid Type Encodings in Names](name-avoid-encodings.md) (`name-avoid-encodings`)

## 4. Conditional Logic (HIGH)

- [Replace Nested Conditionals with Guard Clauses](cond-guard-clauses.md) (`cond-guard-clauses`)
- [Replace Conditional with Polymorphism](cond-polymorphism.md) (`cond-polymorphism`)
- [Decompose Complex Conditionals](cond-decompose.md) (`cond-decompose`)
- [Consolidate Duplicate Conditional Fragments](cond-consolidate.md) (`cond-consolidate`)
- [Introduce Special Case Object](cond-special-case.md) (`cond-special-case`)
- [Replace Conditional with Lookup Table](cond-lookup-table.md) (`cond-lookup-table`)

## 5. Abstraction & Patterns (MEDIUM-HIGH)

- [Extract Strategy for Algorithm Variants](pattern-strategy.md) (`pattern-strategy`)
- [Use Template Method for Shared Skeleton](pattern-template-method.md) (`pattern-template-method`)
- [Use Factory for Complex Object Creation](pattern-factory.md) (`pattern-factory`)
- [Apply Open-Closed Principle](pattern-open-closed.md) (`pattern-open-closed`)
- [Prefer Composition Over Inheritance](pattern-composition-over-inheritance.md) (`pattern-composition-over-inheritance`)
- [Extract Superclass for Common Behavior](pattern-extract-superclass.md) (`pattern-extract-superclass`)

## 6. Data Organization (MEDIUM)

- [Encapsulate Collection](data-encapsulate-collection.md) (`data-encapsulate-collection`)
- [Replace Primitive with Object](data-replace-primitive.md) (`data-replace-primitive`)
- [Encapsulate Record into Class](data-encapsulate-record.md) (`data-encapsulate-record`)
- [Split Variable with Multiple Assignments](data-split-variable.md) (`data-split-variable`)
- [Replace Temp with Query](data-replace-temp-with-query.md) (`data-replace-temp-with-query`)

## 7. Error Handling (MEDIUM)

- [Use Exceptions Instead of Error Codes](error-exceptions-over-codes.md) (`error-exceptions-over-codes`)
- [Create Domain-Specific Exception Types](error-custom-exceptions.md) (`error-custom-exceptions`)
- [Fail Fast with Preconditions](error-fail-fast.md) (`error-fail-fast`)
- [Separate Error Handling from Business Logic](error-separate-concerns.md) (`error-separate-concerns`)

## 8. Micro-Refactoring (LOW)

- [Remove Dead Code](micro-remove-dead-code.md) (`micro-remove-dead-code`)
- [Inline Trivial Variables](micro-inline-variable.md) (`micro-inline-variable`)
- [Simplify Boolean Expressions](micro-simplify-expressions.md) (`micro-simplify-expressions`)
- [Rename for Clarity](micro-rename-for-clarity.md) (`micro-rename-for-clarity`)

