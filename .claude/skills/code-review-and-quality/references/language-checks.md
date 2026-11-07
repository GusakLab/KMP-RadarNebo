# Language-Specific Review Checks and Always-Flag Anti-Patterns

Read this during Step 3 of the review process, when the diff is in one of the languages below or matches one of the anti-patterns. The project's own linters, type checker, and conventions take precedence over these lists. Don't report something a configured tool already enforces.

## Anti-patterns to always flag

| Anti-pattern | Signal | Usual remedy |
|---|---|---|
| God function | One function mixes several phases (parse, validate, compute, persist) or needs scrolling to follow | Extract named steps; separate orchestration from logic |
| Magic values | Unnamed literals that encode a business rule, limit, or unit | Named constant, typed value, or config |
| Copy-paste logic | The same rule implemented in two or more places | Reuse or extract the canonical helper (rule of three for uncertain similarity) |
| Premature optimization | Complex code justified by speed with no measurement | Simplest correct version; optimize with a benchmark |
| Over-engineering | Interface, factory, or registry with one implementation and no present need | Inline to the concrete code |
| Swallowed errors | `except: pass`, `catch {}`, `.catch(() => {})`, `_ = err` | Handle the specific error or propagate it; never silently continue |
| Global mutable state | Module-level mutable variables shared across callers | Pass state explicitly or inject the dependency |

## Python

- Mutable default arguments (`def f(items=[])`): the list is shared across calls. Use `None` and create the list inside.
- Bare `except:` or `except Exception: pass`: catches `KeyboardInterrupt`/`SystemExit` (bare form) or hides failures. Catch specific exceptions and re-raise when you can't recover.
- Mutable class attributes (`permissions = []` on the class) are shared by all instances. Initialize them in `__init__`.
- Resources opened without `with` (files, locks, connections).
- Blocking calls inside `async def` functions.
- Missing or `Any`-heavy type annotations on public functions, when the project type-checks.

## JavaScript / TypeScript

- `==` where `===` is meant (unless the project deliberately uses `== null`).
- Floating promises and unhandled rejections: `await` the promise, or return it and handle the error.
- `fetch` responses used without checking `response.ok`.
- `any`, unchecked casts (`as Foo`), and non-null assertions (`!`) that paper over an unclear invariant.
- Event listeners, subscriptions, or intervals without cleanup (memory leaks; React effects missing a cleanup return).
- Mutating props or state objects in React instead of producing new values.

## Go

- Ignored errors (`_ = f()`, or an unchecked `err`), or errors wrapped without context.
- Goroutines without a termination path (context cancellation, closed channel).
- Unbuffered channel sends with no guaranteed receiver (deadlock risk).
- `context.Context` not propagated to I/O calls.
- Mutex misuse: copying a struct that contains a mutex, or lock ordering that can deadlock.

## Rust

- `unwrap()`/`expect()` on paths that production input can reach.
- Unnecessary `.clone()` to satisfy the borrow checker where a borrow would do.
- Errors not propagated with `?`, or converted into lossy strings.
- `unsafe` blocks without a `// SAFETY:` justification of the upheld invariants.
- Lifetime annotations more complex than the ownership actually requires.
