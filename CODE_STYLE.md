# Kotlin Code Style

How we write Kotlin in RadarNebo. Every code change and every code review is held to this guide.

It goes beyond formatting. It covers API design, module and package boundaries, error handling, coroutines,
testing and naming. The goal is code that the next engineer can change safely.

This guide merges the worxbend [CODE_STYLE.md](https://github.com/worxbend/worxbend/blob/main/CODE_STYLE.md) and
[SCALA_CODE_STYLE.md](https://github.com/worxbend/worxbend/blob/main/SCALA_CODE_STYLE.md), translated from Scala to
Kotlin and fitted to this codebase. Where Scala habit and Kotlin convention disagree, Kotlin convention wins: constants
are `SCREAMING_SNAKE_CASE`, indentation is 4 spaces, default arguments and early `return` are idiomatic. When this
guide says nothing, follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html), then
the surrounding code.

Examples use the repo's vocabulary and, where it helps, real class names. Every snippet shows code as these rules
require it, so a snippet can differ in detail from the current source.

"Must", "never", "no" and "only" mark rules. Rules have no exceptions, and they apply to existing code as much as to
new code. "Prefer" and "aim" mark guidance: a reviewer may ask for the reason when code deviates from it. See
[Violations](#violations).

## Non-Negotiables

| Area | Rule |
| --- | --- |
| Formatting | Kotlin official code style, 4-space indentation, 120 columns for every line, trailing commas in multiline lists. |
| Packages | Code lives under `com.gusak.lab.radarnebo`, then the module area, then the concept. |
| Types | Public and protected functions and properties state their types, as Kotlin's explicit API mode requires. |
| Size | A function is at most 50 lines. |
| Mutation | `val` by default. No shared mutable state without one clear owner. |
| Errors | Recoverable failures are values: a sealed result type, not an exception. |
| Exceptions | Throw only for defects, broken invariants or failed startup. Catch narrowly, at boundaries. |
| Cancellation | Suspend code never swallows `CancellationException`. |
| Nulls | `T?` means absence. No `!!`, in production or test code. Java platform types never leak past the adapter. |
| Concurrency | Structured coroutines in owned scopes. No `GlobalScope`. No `runBlocking` outside `main` and tests. |
| Resources | Every resource has one owner and a visible release path. |
| Time, randomness | Read only through an injected clock or `Random`. |
| Tests | One behavior per test, deterministic, no real sleeps. |
| Warnings | Kotlin compilation reports zero warnings, and every module's build sets `allWarningsAsErrors`. Warnings are fixed, never suppressed. |
| Violations | None are tolerated. A violation is fixed, never exempted, suppressed or baselined. |

## Tooling

The build is the authority. Editor settings help only when they match it.

### Build and test

Run Gradle from the repository root. The CI gate is:

```bash
./gradlew --continue :core:jvmTest :shared:jvmTest :mission-control:server:test :mission-control:ui:test :app:desktop:test
./gradlew --continue :app:android:assembleDebug :mission-control:android:assembleDebug \
    :core:compileKotlinIosArm64 :core:compileKotlinIosSimulatorArm64 \
    :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64
```

The first line runs every test, including the Compose screenshot and contrast tests in `:shared:jvmTest`. The second
builds the Android apps and the iOS klibs; linking the iOS app needs macOS and Xcode. The README lists the run
commands for each app.

### Formatting

The IDE's Kotlin official style is the reference formatter.

- A whole-file reformat goes in its own commit, apart from behavior changes.
- 120 columns is a hard limit for every line in `.kt` and `.kts` files: code, comments, KDoc, string literals and
  generated data alike. Split long string literals, wrap long comments, and break data across lines.
- Trailing commas in multiline parameter, argument, collection and `when` lists. The codebase already does this.
- No wildcard imports. The codebase has none; keep it that way. Let the IDE order imports.
- Break long call chains before the `.`, one call per line.

```kotlin
// Good
val activeTracks = tracks
    .filter { it.isActive }
    .sortedBy { it.firstSeenAt }
    .take(MAX_TRACKS)

// Bad
val activeTracks = tracks.filter { it.isActive }.sortedBy { it.firstSeenAt }.take(MAX_TRACKS)
```

Multiline declarations put each parameter on its own line, with the closing parenthesis on its own line:

```kotlin
class TelegramChannelSource(
    private val channel: String,
    private val client: HttpClient = AlertsInUaSource.defaultClient(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val activeFor: Duration = 20.minutes,
    override val pollInterval: Duration = 60.seconds,
) : AlertsDataSource
```

Use an expression body for a function that is one expression. Use braces for every multiline `if`, `when` branch or
loop. A one-line `if` expression without braces is fine when it fits.

## Modules, Packages and Files

Module and package structure is design. It tells a reader which concept they are in and where a boundary begins.

### Modules

| Module | Holds | May depend on |
| --- | --- | --- |
| `core` | Tracking, geography and API client code shared by the server and every client. | Nothing in this repo. |
| `shared` | Compose UI, view models, repositories, use cases and Koin wiring for the client apps. | `core` |
| `mission-control:server` | The Ktor server. | `core` |
| `mission-control:ui` | The desktop server console. | `mission-control:server`, `shared` |
| `app:*`, `mission-control:android` | Platform entry points: thin. | `shared` or nothing |

Keep `core` free of UI, Ktor server, Exposed and platform code. Both the server and the apps depend on it, so its
public API is the widest promise in the repo.

### Server layers

The server is layered, and each layer has a direction:

| Package | Owns | Must not import |
| --- | --- | --- |
| `server.domain.<concept>` | Domain rules and ports (interfaces such as `AlertArchive`). | Ktor, Exposed, JDBC, kotlinx.serialization wire types. |
| `server.application` | Use-case orchestration: polling, situation building. | Ktor routing, Exposed. |
| `server.infrastructure.<adapter>` | Port implementations: `persistence`, `upstream`, `simulation`, `tracking`. | `server.api`. |
| `server.api` | HTTP routes, request and response mapping. | Exposed. |
| `server.contract` | Upstream wire formats (alerts.in.ua). | Domain logic. |
| `server.config` | Typed configuration, loaded once at startup. | — |

The domain package has no Ktor or Exposed imports today. Keep it that way.

### Kotlin Multiplatform source sets

- Write code in `commonMain` first. Move it to a platform source set only when it needs a platform API.
- For a platform service, prefer an interface in common code plus a binding in the platform Koin module (as
  `KeyValueStorage` does) over `expect`/`actual`. Reserve `expect`/`actual` for small functions and types.
- `commonMain` must not call JVM-only APIs (`java.*`, `System.currentTimeMillis`). Use `kotlin.time`, kotlinx-datetime
  and kotlinx-coroutines.

### Package names

Package names are lowercase words separated by dots, with no underscores or camel case:

```kotlin
// Good
package com.gusak.lab.radarnebo.server.infrastructure.upstream

// Bad
package com.gusak.lab.radarnebo.server.infrastructure_upstream
package com.gusak.lab.radarnebo.server.infrastructureUpstream
```

Name a package after its concept (`tracking`, `alert`, `persistence`, `geo`) or its architectural layer (`ui`,
`data`, `domain`, `di`, `api`, `application`, `infrastructure`). Never name one after a grab-bag (`utils`, `helpers`,
`common`, `misc`).

### Files

- One primary public type per file. The file is named after it: `SituationPoller.kt`.
- A sealed family that is read as one concept lives in one file.
- A file of top-level functions is named after their concept: `Format.kt`, not `Utils.kt`.
- No `Types.kt`, `Models.kt`, `Helpers.kt` or `Extensions.kt`. If the only thing the contents share is "these are
  types", split the file by concept.

### Visibility

Public is a promise. Private is a design tool. Choose visibility when you create a declaration.

- `private` for file or class internals.
- `internal` for module internals. In `core` and `shared`, anything not used outside the module is `internal`.
- Public only for API that other modules call. Start narrow, then widen when a real caller appears.

```kotlin
internal object Golden { /* test helper, module-only */ }

private const val USER_AGENT = "Mozilla/5.0 (RadarNebo)"
```

Kotlin classes are final by default. Keep them final. Make a class `open` only when it is designed for extension.

## Naming

Names are part of the API. Saving a few characters is rarely worth less searchability or review clarity.

### Types

`UpperCamelCase` for classes, interfaces, objects, enums and type aliases. Use nouns or noun phrases. An interface
for a capability may be an adjective (`Closeable`).

| Prefer | Avoid |
| --- | --- |
| `ExposedTrackArchive` | `TrackArchiveImpl`, `DbTrackRepo` |
| `InMemoryAlertArchive` | `FakeArchive2`, `MemArchive` |
| `AlertsInUaSource` | `AlertsSourceHelper` |
| `RetryPolicy` | `RetryManager`, `RetryUtil` |
| `FetchResult` | `Result` outside a narrow scope |

Do not put the language construct in the name: no `Interface`, `Class`, `Object` or `Impl` suffixes, and no `I`
prefix. Name an implementation after what makes it different: `Exposed…`, `InMemory…`, `Simulated…`.

### Functions and properties

`lowerCamelCase` for functions, properties, parameters and local values.

| Intent | Prefer | Avoid |
| --- | --- | --- |
| Accessor | `track.lastPosition` (property) | `track.getLastPosition()` |
| Predicate | `alert.isActive`, `track.hasForecast` | `alert.active`, `track.forecastFlag` |
| Side effect | `archive.save(track)` | `archive.setTrack(track)` |
| Query | `archive.findById(id)` | `archive.getTrackById(id)` |
| Nullable lookup with a throwing sibling | `findOrNull(id)` | `findMaybe(id)` |
| Collection | `tracks` | `trackList` |

Use an active verb for a function with side effects. Use a noun for a pure value. Do not repeat the receiver:
`archive.findById(id)`, not `archive.findTrackById(id)`.

A property must be cheap, free of side effects, non-throwing, and must return the same value until the state changes.
Anything else is a function.

### Constants

Kotlin convention, which the codebase already follows: `SCREAMING_SNAKE_CASE` for `const val` and for top-level or
`object` `val`s that hold deeply immutable data.

```kotlin
companion object {
    const val POSITIONS_PATH: String = "/v1/drones/positions.json"
    const val TRACK_ARCHIVE_PATH: String = "/v1/drones/archive.json"
    const val HEALTH_PATH: String = "/health"
}
```

Put a constant next to the concept that owns it, usually in the owner's `companion object` or as a `private`
top-level value in its file. A deployment-specific value belongs in typed configuration (`ServerConfig`), not in code.
A raw number with a unit carries the unit in its name: `timeoutMs`, `radiusKm`, `epochMillis`.

### Acronyms

Treat acronyms as words: `HttpClient`, `JsonCodec`, `UiState`. An established platform name keeps its spelling
(`IOException`, `URL`). In public API, avoid abbreviations that are less recognizable than the full word. Short names
are fine for locals in tiny scopes.

### Type parameters

Single upper-case letters (`T`, `K`, `V`) for simple generics. Descriptive names when there are several roles:
`Cache<Key, Value>`.

### Tests

Test functions are camelCase sentences that state the behavior. This is the codebase's style:

```kotlin
@Test
fun aRequestWithoutTheTokenIsIgnored() { /* ... */ }

@Test
fun anExtraSourceThatKeepsFailingIsLeftOut() { /* ... */ }
```

No backtick names with spaces. Common tests also compile for non-JVM targets, and the codebase does not use them.

## Types and Domain Modeling

Make invalid states hard to represent. If a reviewer has to ask "can this be empty?" or "what does `true` mean?", the
type needs work.

### Explicit types

Public and protected functions and properties declare their types, exactly where Kotlin's explicit API mode
(`-Xexplicit-api`) would demand a type. That mode is the checker for this rule; this repo does not adopt its
visibility-modifier requirement. Local values may rely on inference when the type is obvious from the right-hand side.
Add a type when inference would produce something wider, a platform type, or a type the reader needs to see.

```kotlin
// Good
fun findTrack(id: TrackId): Track? = tracks[id]
private val tracks = mutableMapOf<TrackId, Track>()

// Bad: public API with an inferred type
fun findTrack(id: TrackId) = tracks[id]
```

### Value classes

Wrap primitive domain values that cross a boundary. A raw `String`, `Int` or `Long` rarely says what it holds. Use
value classes for identifiers, quantities, codes and external IDs. Validate at construction:

```kotlin
@JvmInline
value class TrackId private constructor(val value: String) {
    companion object {
        fun parse(raw: String): TrackId? = raw.trim().takeIf { it.isNotEmpty() }?.let(::TrackId)
    }
}
```

`@JvmInline` is required on the JVM and accepted on every other target, so this works in `commonMain`.
kotlinx.serialization encodes a `@Serializable` value class as its underlying value, so the wire format does not
change.

### Sealed types and enums

Use a `sealed interface` with `data class` and `data object` cases for a closed set whose members carry different
data. Use an `enum class` for a closed set of plain values. Put the whole sealed family in one file.

```kotlin
/** What one fetch from an [AlertsDataSource] produced. */
sealed interface FetchResult {
    data class Fresh(val report: SituationReport, val lastModified: String? = null) : FetchResult

    data object NotModified : FetchResult

    data class Failed(
        val message: String,
        val minWaitMs: Long,
        val retryAfterMs: Long? = null,
        val maxWaitMs: Long = 60_000,
    ) : FetchResult
}
```

Model lifecycle states as separate cases, not as one class with many nullable fields.

Match sealed types with an exhaustive `when` and no `else` branch. Then the compiler flags every `when` that misses
a newly added case.

```kotlin
val waitMs = when (result) {
    is FetchResult.Fresh -> pollInterval.inWholeMilliseconds
    FetchResult.NotModified -> pollInterval.inWholeMilliseconds
    is FetchResult.Failed -> result.retryAfterMs ?: result.minWaitMs
}
```

### Boolean blindness

Avoid a Boolean parameter whose meaning the caller has to remember. Use an enum, or two functions:

```kotlin
// Good
enum class Delivery { Immediate, Batched }
fun publish(report: SituationReport, delivery: Delivery)

// Bad: what does true mean at the call site?
fun publish(report: SituationReport, immediate: Boolean)
```

A Boolean is fine for a simple predicate such as `alert.isActive`. When a Boolean argument is unavoidable (for
example in an existing or third-party API), pass it as a named argument: `connect(host, secure = true)`.

### Absence, results and errors

- `T?` for absence that needs no explanation.
- A sealed result type when the caller needs to know why an operation failed, or must branch on the outcome.
- Do not use `kotlin.Result` for domain errors: its error is an untyped `Throwable`. The codebase has no `Result<`
  in its APIs. Keep it that way.
- Avoid nesting such as `FetchResult?` or `List<Track?>`. Each dimension should mean something different, or the
  model needs another case.
- Do not mix absence and failure: `findById` returns `Track?`, while `fetch` returns a result type.

## Functions

A function performs one logical operation, or orchestrates a short sequence of named steps. A long function usually
hides a concept that has no name yet.

### One concern per function

An orchestration function should read like the process it runs:

```kotlin
suspend fun refreshOnce(): Duration {
    val result = source.fetch(lastModified)
    val report = reportFrom(result) ?: return waitAfter(result)
    archive.save(report)
    publish(report)
    return waitAfter(result)
}
```

Extract a step when its name makes the workflow easier to audit, even if the step has a single caller.

- A function is at most 50 lines, counted from its signature to its closing brace. Aim for under 20.
- Split a long function by job: each extracted function has one purpose and a name that says it. Never split at an
  arbitrary line just to meet the limit.
- Composables follow the same limit. Split them by visual region or state. Keep each composable's parameters to the
  state it draws and the events it emits.

### Parameters

- From four parameters up, consider a parameter object, unless named arguments keep the call site clear.
- Default arguments are idiomatic in Kotlin. Use them for real defaults and for test seams, for example
  `clock: () -> Long = System::currentTimeMillis`.
- Use named arguments at call sites for Booleans, for adjacent parameters of the same type, and for any literal whose
  meaning is not obvious.

### Early return

Early `return` is idiomatic in Kotlin. Use guard clauses (`?: return`, `if (...) return`) to keep the main path flat.
Prefer expression bodies when the function is one expression. Avoid non-local `return` from inside lambdas. Use a
labeled return (`return@forEach`) only when the lambda stays short.

### Purity and side effects

Prefer pure functions. Make side effects visible in the name and in the signature: `suspend`, a verb, a port
parameter. A function named like a query must not write.

```kotlin
// Good: pure
fun totalDistanceKm(points: List<GeoPoint>): Double = points.zipWithNext(::distanceKm).sum()

// Bad: a query-shaped name that hides two side effects
fun trackOf(id: TrackId): Track {
    log.info("Loading $id")
    archive.touch(id)
    return archive.load(id)
}
```

### Scope functions and extensions

- Use `let`, `apply`, `also`, `run` and `with` when they shorten code without hiding it. Do not nest them more than one
  level deep.
- Do not build control flow from `?.let { } ?: run { }` when an `if` or `when` reads better.
- Extension functions suit types you do not own, and keep data classes lean. Declare them next to the concept, not in
  an `Extensions.kt` grab bag.
- Do not put domain logic in an extension on a primitive. `String.toTrackId()` belongs at the parsing boundary only.

## Classes and Objects

### Data classes

Use data classes for immutable values. Declare all properties as `val`. Keep data classes free of behavior, apart
from small derived properties.

```kotlin
// Good
data class TrackPoint(
    val position: GeoPoint,
    val observedAt: Instant,
)

// Bad: mutable and with an identity crisis
data class TrackPoint(var position: GeoPoint, var observedAt: Instant)
```

Change a value with `copy(...)`, not with mutation.

### Companion objects and factories

Use a companion object for constants, named constructors and factories. A factory that chooses an implementation
returns the interface:

```kotlin
class Archives(val alerts: AlertArchive, val tracks: TrackArchive) {
    companion object {
        fun inMemory(): Archives = Archives(InMemoryAlertArchive(), InMemoryTrackArchive())

        fun create(config: ServerConfig): Archives = /* Exposed or in-memory, from config */
    }
}
```

### Member order

1. Properties, public first.
2. `init` blocks.
3. Secondary constructors.
4. Public functions, in the order a reader needs them.
5. Private functions, next to the public function that uses them or at the end.
6. Companion object last.

## Collections and Data Flow

Choose a collection by what it means. Its type tells the reader how the data is used.

| Need | Prefer |
| --- | --- |
| Ordered finite values | `List<T>` |
| Unique values, lookup | `Set<T>`, `Map<K, V>` |
| Long chains on large input, lazily | `Sequence<T>` |
| Asynchronous stream | `Flow<T>` |
| Binary data | `ByteArray` |
| Java interop | A Java collection at the adapter boundary only |
| Large or unbounded data | Paging, `Flow`, or a bounded window |

- APIs expose read-only types (`List`, `Map`, `Set`). They never return `MutableList` or `ArrayList`. Copy mutable
  data from a library into an immutable value at the boundary.
- Use collection operations over hand-written loops when they read better. A plain loop is fine when it is clearer,
  for example with an early exit or several accumulators.
- Never load unbounded data into memory. Archives and queries take an explicit window or limit, as the archive does
  with its maximum window.
- Use `Pair` and `Triple` only for tiny local transformations. A public API uses a named data class. Destructure with
  names (`val (lat, lon) = point`) instead of `.first` and `.second`.

## Error Handling

Error handling is API design. It decides what callers can recover from and what is safe to retry.

### Recoverable failures are values

When the caller can do something meaningful, return a result, as `AlertsDataSource.fetch` returns `FetchResult`.
When a caller branches on the error, use a sealed error type, not a string. Strings are for human-readable messages at
the edge.

### Exceptions

Exceptions are for defects, broken invariants, failed startup, and third-party APIs that cannot report failure as a
value. Catch the most specific exception at the boundary and convert it right away.

- State invariants with `require` (bad arguments), `check` (bad state) and `error()` (unreachable branches).
- In suspend code, rethrow `CancellationException` before any broad `catch`. This is the codebase's pattern:

```kotlin
override suspend fun fetch(lastModified: String?): FetchResult = try {
    // ...
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    FetchResult.Failed("t.me/s/$channel: ${e.message}", minWaitMs = pollInterval.inWholeMilliseconds)
}
```

- Never catch `Throwable` except at a process or thread boundary, and only to clean up and rethrow.
- `runCatching` is for best-effort, non-suspending cleanup such as `runCatching { it.close() }`. Never wrap a suspend
  call in it, because it swallows cancellation.
- Do not log and rethrow. Log once, where the error is handled.
- Do not send internal details to clients. Client-visible errors name the failure class and point to the server log,
  as `SituationPoller` does.

### Null safety

- No `!!` anywhere. In production code, use `?:` with a fallback, an early `return`, `requireNotNull` or
  `checkNotNull` with a message, or a type that proves presence.
- In tests, use `assertNotNull(value)`: it fails clearly and returns the smart-cast non-null value.
- A Java API returns platform types. Give them an explicit Kotlin type (`String?` or `String`) in the adapter, so the
  nullability decision is made once.
- `lateinit` only for framework-initialized fields. Never use it for dependencies: inject those through the
  constructor.

## Configuration, Time and External State

Hidden dependencies make tests unreliable and production behavior surprising.

### Typed configuration

Load configuration once at startup, validate it into typed values (`ServerConfig`), and pass it through constructors.
Use `kotlin.time.Duration` for time spans (`60.seconds`), not raw numbers. When a raw number is unavoidable (a wire
format, epoch milliseconds), its name carries the unit.

### Time and randomness

Inject time, randomness and ID generation. `Clock.System`, `System.currentTimeMillis()` and an unseeded `Random`
appear only as the default value of an injected parameter, at the point where a dependency is created:

```kotlin
class AlertsRepository(
    private val api: RadarNeboApi,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
)
```

Randomness follows the same rule: the simulation receives its `Random`, so a seeded run is reproducible.

View models and repositories take a clock in their constructor. Composables get the time from their caller: a
`now` parameter, or a `clock` parameter on the screen-level composable whose default is the system clock. Previews
and test fixtures use a fixed instant, so screenshots and tests are deterministic.

### Secrets

Secrets never appear in source code, tests, logs, error messages or generated docs. A `data class` generates a
`toString` that prints every property, so a class that holds a secret overrides `toString` or keeps the secret out of
its constructor. Mask keys in traffic logs, as `RadarNeboApi` does with the access key.

## Concurrency and Lifecycle

Concurrency is an ownership problem before it is a performance problem. Every coroutine, channel, client and file
handle needs a parent scope and a shutdown path.

### Structured coroutines

| Need | Default pattern |
| --- | --- |
| A few concurrent calls | `coroutineScope { listOf(async { a() }, async { b() }).awaitAll() }` |
| Collection or stream processing | `Flow` operators; bounded fan-out with `flatMapMerge(concurrency = n)` |
| Producer and consumer | `Channel` with an explicit capacity, closed by the producer |
| Serialized mutable state | One owning coroutine, a `Mutex`, or `MutableStateFlow.update { }` |
| UI state | `StateFlow` in a view model, launched in `viewModelScope` |
| App-lifetime work | A `CoroutineScope` owned by the app or server and cancelled on shutdown |
| Blocking I/O (JDBC, Exposed, files) | `withContext(Dispatchers.IO)` |

- No `GlobalScope`.
- No `runBlocking` except in `main` and in tests. Tests use `runTest`.
- Use `delay`, never `Thread.sleep`, in coroutine code. Long loops check `isActive` or call a suspending function.
- Raw threads only to bridge a blocking API, such as a blocking socket accept loop. Name them, make them daemons where
  that is safe, and say why in a comment.
- Use `Atomic*` and `synchronized` only for small, local state, never as a substitute for structure.

### Scope ownership

A `suspend` function finishes its work before it returns. Use `coroutineScope { }` for internal concurrency.

A class that starts background work owns its scope. It exposes `start()` and `close()` (or implements `Closeable`), and
cancels the scope when it closes. A function that launches work which outlives it takes the `CoroutineScope` as a
parameter, so the caller decides the lifetime.

```kotlin
class AlertsWatcher(
    private val source: AlertsDataSource,
    private val clock: () -> Long = System::currentTimeMillis,
) : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch { watchForever() }
    }

    override fun close() {
        scope.cancel()
    }
}
```

### Compose

- No work in composition. Side effects go in `LaunchedEffect`, `DisposableEffect` or `produceState`, with keys that
  change when their inputs do.
- Collect flows with `collectAsStateWithLifecycle`, never `collectAsState`.
- Hoist state. A composable takes state and emits events. View models own business logic.

### Resources

- Use `use { }` for every `Closeable` you open and close in one place.
- Release resources in reverse order of acquisition.
- Best-effort cleanup during close uses `runCatching { it.close() }`, so one failing close does not leak the rest.
- Never rely on finalizers. Never return a resource whose owner has already closed it.

## HTTP, JSON and Boundaries

A boundary translates between the outside world and the domain. Keep that translation explicit and in one place.

### DTOs and domain types

- `@Serializable` DTOs describe wire formats. Domain types describe rules. Convert between them at the boundary:
  routes in `server.api`, upstream formats in `server.contract` and `server.infrastructure.upstream`.
- Normalize an upstream quirk once, in its adapter. Do not let it leak into the domain.
- Pin wire names with `@SerialName` for enum constants and for properties whose Kotlin name may change.

### API compatibility

Apps in the field lag behind the server. Old clients keep calling `/v1` for months.

- Changes to `/v1` responses are additive. Never remove, rename or retype a field.
- Clients decode with `ignoreUnknownKeys = true` and `coerceInputValues = true` (`RadarNeboApi.json`), so a new
  response field is safe. A new field that a client needs must have a default.
- A breaking change gets a new path version, and the old one keeps serving until clients have moved.
- The golden tests (`ApiContractGoldenTest`, `SimulationGoldenTest`) pin the contract. When a golden fails, diff the
  text under `build/golden-actual/` and update the digest only for an intended change. Say why in the PR.

### Dependency injection

Use constructor injection everywhere. Dependencies are visible in the constructor and easy to replace in tests.

- **Client apps:** Koin. Modules are built by functions (`fun dataModule(): Module = module { ... }`), not held in
  `val`s.
  Koin keeps singletons inside the module object, so a shared module would share state between containers. Use
  `singleOf` for stateful classes and `factoryOf` for stateless use cases. Platform-specific bindings go in the
  platform module.
- **Server:** plain constructor wiring in `Main.kt` and its factories (`Archives.create`). Do not add a DI framework.
- No field injection, and no `KoinComponent` / `by inject()` in domain, data or use-case classes.

## Design Patterns in Kotlin

Use a pattern when it removes a real problem, not because it has a name. Prefer the language feature when one fits.

| Pattern | Kotlin form in this repo |
| --- | --- |
| Repository / port | An interface in the domain (`AlertArchive`), implemented in infrastructure (`ExposedAlertArchive`, `InMemoryAlertArchive`). |
| Factory | A companion `create(...)` or a top-level function that returns the interface. |
| Builder | Usually unnecessary: named and default arguments plus `copy()`. Use a DSL (`buildList`, Ktor builders) for nested configuration. |
| Strategy | An interface, a `fun interface`, or a function type `(Input) -> Output`. |
| Decorator | Interface delegation: `class Logged(private val inner: Source) : Source by inner`, overriding only what changes. |
| Composite | A class that implements the interface over a list, as `CompositeAlertsSource` does. |

Keep interfaces small and focused. Do not add a method to an interface for one caller. Prefer composition over
inheritance.

## Testing

Tests make behavior obvious. A test that checks everything usually explains nothing.

### Layout

Test source sets mirror production packages: `commonTest` and `jvmTest` in multiplatform modules, `src/test/kotlin`
in JVM modules. Use `kotlin.test` (`@Test`, `assertEquals`, `assertNotNull`).

### Unit tests

Each test covers one scenario. Set up only what the test needs. Assert in domain terms. Separate arrange, act and
assert with blank lines:

```kotlin
@Test
fun aTrackWithASinglePointHasNoForecast() {
    val track = Fixtures.track(points = listOf(Fixtures.point()))

    val forecast = track.forecast()

    assertNull(forecast)
}
```

- Prefer fakes over mocks: in-memory implementations (`InMemoryAlertArchive`, `InMemoryStorage`), Ktor `MockEngine`
  for HTTP clients, `testApplication` for server routes.
- Share fixtures through top-level functions with default arguments (`Fixtures.kt`), not through base classes.
- Use `runTest` for coroutines, with virtual time. Inject the clock. No real sleeps: use virtual time, controlled
  channels or explicit completion signals.

### Unit, integration and UI

| Test type | Owns |
| --- | --- |
| Unit | Domain rules, tracking and trajectory math, parsing, state transitions, error mapping. |
| Integration | Exposed with SQLite, Ktor routes through `testApplication`, serialization, upstream parsers on recorded pages. |
| Golden | The API contract and simulation output, pinned by digest. |
| UI | Compose screenshot and contrast tests in `shared` `jvmTest`. |

Test failure paths as deliberately as success paths. A bug fix starts with a test that fails without the fix.

## Documentation and Comments

Good names and types carry intent, so good code needs fewer comments. A comment is still valuable when it explains a
decision the code cannot express.

### Comments

Write comments for:

- Business rules that are not obvious.
- Upstream quirks.
- Performance constraints.
- Security constraints.
- Concurrency and resource ownership assumptions.

Explain why, not what. The codebase has good examples: the note on why Koin modules are functions, and the note on
why client errors hide upstream messages.

```kotlin
// Good
// Modules are built by functions, not held in vals: Koin keeps singleton instances inside the module
// object, so two containers (an app and a preview, or two tests) sharing one would share repositories.

// Bad
// Create the data module
```

- No commented-out code. Git keeps history.
- A deliberate shortcut gets a comment in this form: `shortcut: <the limit>, <when to upgrade>`. Use it instead of a
  bare `TODO`. `ponytail-debt` lists them.
- Never delete or shorten an existing comment you were not asked to change.

### KDoc

Write KDoc for public API that other modules use: `core`, the server's domain ports, and the HTTP contract. Document
the contract, failure behavior, resource ownership and threading. Link symbols with `[Name]`.

```kotlin
/**
 * Wait at least [minWaitMs] (or the source's [retryAfterMs]) before trying again.
 * [maxWaitMs] caps the exponential backoff, so a transient outage doesn't delay recovery for long.
 */
data class Failed(
    val message: String,
    val minWaitMs: Long,
    val retryAfterMs: Long? = null,
    val maxWaitMs: Long = 60_000,
) : FetchResult
```

Do not document private functions unless they encode a subtle algorithm or boundary condition.

## Gradle Build Scripts

Build scripts are Kotlin code and follow this guide.

- Versions live only in `gradle/libs.versions.toml`. Apply plugins with `alias(libs.plugins.x)`. Never put a version
  in a module script. Gradle cannot read the catalog in the `plugins {}` block of `settings.gradle.kts`, so a settings
  plugin states its version there. That block is the only other place a version may appear.
- Module dependencies follow the module table above. A new dependency between modules needs a reason in the PR.
- Configuration repeated across three or more modules moves to `buildSrc`.
- Do not change a module's `jvmTarget`, Android SDK levels or Kotlin opt-ins in passing. Each is its own change, with
  a reason.
- Opt in to an experimental API at the narrowest scope that covers its use: an expression, a file or a module.
- Never weaken compiler settings or add `@Suppress` to silence a warning. Fix the code.

## Review Checklist

Use this before opening a pull request, and as a reviewer.

### Design

- The package names a concept, not a mechanism, and sits in the right module and layer.
- Public declarations are public on purpose. Module internals are `internal`.
- Domain invariants are in the types. Raw primitives do not cross important boundaries.
- No Boolean argument makes the caller remember what `true` means.
- Time, randomness, clients and storage are explicit dependencies.
- `/v1` changes are additive. Goldens changed only on purpose.

### Implementation

- Formatting matches the Kotlin official style. No line over 120 columns. No unrelated reformatting.
- Public declarations state their types.
- Functions do one thing, or orchestrate named steps, in at most 50 lines.
- Zero compiler warnings. No `@Suppress`, baseline or exclusion added.
- Recoverable errors are values. Exceptions are converted at boundaries.
- Suspend code rethrows `CancellationException`. No `runCatching` around suspend calls.
- No `!!`. No `GlobalScope`. No `runBlocking` outside `main` and tests.
- Collections are bounded, streamed or paged according to size.
- Resource and coroutine-scope ownership is visible.
- No secrets in code, logs or errors.

### Tests

- Each test covers one scenario and is named as a sentence.
- Business rules are testable without infrastructure.
- Integration tests target real boundaries.
- Time and randomness are deterministic.
- Failure paths are tested. A bug fix comes with a test that fails without it.

## Violations

The rules have no exceptions, and none are grandfathered.

- Code that breaks a rule is fixed. Fix it in the same PR when the PR touches that code. Otherwise open a dedicated
  fix PR right away, so the violation does not ride along with unrelated work.
- Never add an exemption: no `@Suppress`, no lint baseline, no ignore list, no excluded path, no "fix when touched"
  note.
- A fix keeps behavior. It keeps the `/v1` contract and the golden digests unchanged, and the tests prove it.
- A rule that turns out to be wrong is changed here first, in its own PR, with the reason. Then the code follows the
  new rule. Code never quietly deviates from the written rule.

Style work must make future changes safer. A refactor that improves neither readability, type safety, testability
nor operational confidence does not belong in a pull request.

## References

- [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Coroutines guide](https://kotlinlang.org/docs/coroutines-guide.html) and
  [exception handling](https://kotlinlang.org/docs/exception-handling.html)
- [Compose API guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md)
- [kotlinx.serialization guide](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md)
- Source guides: worxbend [CODE_STYLE.md](https://github.com/worxbend/worxbend/blob/main/CODE_STYLE.md) and
  [SCALA_CODE_STYLE.md](https://github.com/worxbend/worxbend/blob/main/SCALA_CODE_STYLE.md)
