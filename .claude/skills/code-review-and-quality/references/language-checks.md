# Language-Specific Review Checks and Always-Flag Anti-Patterns

Read in Step 3 when diff is in a language below or matches an anti-pattern. Project's own linters, type checker, and conventions override these lists. Don't report what a configured tool already enforces.

## Anti-patterns to always flag

| Anti-pattern | Signal | Usual remedy |
|---|---|---|
| God function | One function mixes phases (parse, validate, compute, persist) or needs scrolling to follow | Extract named steps; separate orchestration from logic |
| Magic values | Unnamed literals encoding business rule, limit, or unit | Named constant, typed value, or config |
| Copy-paste logic | Same rule in two or more places | Reuse or extract canonical helper (rule of three when similarity uncertain) |
| Premature optimization | Complex code justified by speed, no measurement | Simplest correct version; optimize against benchmark |
| Over-engineering | Interface, factory, or registry with one implementation, no present need | Inline to the concrete code |
| Swallowed errors | `except: pass`, `catch {}`, `.catch(() => {})`, `_ = err` | Handle specific error or propagate; never silently continue |
| Global mutable state | Module-level mutable variables shared across callers | Pass state explicitly or inject dependency |

## Python

- Mutable default arguments (`def f(items=[])`): list shared across calls. Use `None`, create list inside.
- Bare `except:` or `except Exception: pass`: catches `KeyboardInterrupt`/`SystemExit` (bare form) or hides failures. Catch specific exceptions; re-raise when unrecoverable.
- Mutable class attributes (`permissions = []` on class) shared by all instances. Initialize in `__init__`.
- Resources opened without `with` (files, locks, connections).
- Blocking calls in `async def` functions.
- Missing or `Any`-heavy annotations on public functions, when project type-checks.

## JavaScript / TypeScript

- `==` where `===` meant (unless project deliberately uses `== null`).
- Floating promises, unhandled rejections: `await` promise, or return it and handle error.
- `fetch` responses used without checking `response.ok`.
- `any`, unchecked casts (`as Foo`), non-null assertions (`!`) papering over unclear invariant.
- Event listeners, subscriptions, intervals without cleanup (memory leaks; React effects missing cleanup return).
- Mutating React props or state objects instead of producing new values.

## Go

- Ignored errors (`_ = f()`, unchecked `err`), or errors wrapped without context.
- Goroutines without termination path (context cancellation, closed channel).
- Unbuffered channel sends with no guaranteed receiver (deadlock risk).
- `context.Context` not propagated to I/O calls.
- Mutex misuse: copying struct containing mutex, or deadlock-prone lock ordering.

## Rust

- `unwrap()`/`expect()` on paths production input can reach.
- Needless `.clone()` to satisfy borrow checker where borrow works.
- Errors not propagated with `?`, or converted to lossy strings.
- `unsafe` blocks without `// SAFETY:` justification of upheld invariants.
- Lifetime annotations more complex than ownership requires.
