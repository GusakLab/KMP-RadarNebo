---
name: kotlin-coroutines
user-invocable: false
description: Write and debug Kotlin coroutines and Flow - structured scopes, dispatchers, StateFlow/SharedFlow, channels, cancellation, exception handling, and runTest-based testing. Use for async Kotlin on Android or the server.
allowed-tools: []
---

# Kotlin Coroutines

## Suspend Functions

```kotlin
// Basic suspend function
suspend fun fetchUser(id: Int): User {
    delay(1000) // Suspends without blocking
    return User(id, "Alice")
}

data class User(val id: Int, val name: String)

// Calling suspend functions
suspend fun loadUserProfile(id: Int): UserProfile {
    val user = fetchUser(id)
    val posts = fetchPosts(user.id)
    return UserProfile(user, posts)
}

suspend fun fetchPosts(userId: Int): List<Post> {
    delay(500)
    return emptyList()
}

data class Post(val id: Int, val title: String)
data class UserProfile(val user: User, val posts: List<Post>)

// Sequential vs concurrent execution
suspend fun loadDataSequential(): Pair<User, List<Post>> {
    val user = fetchUser(1)
    val posts = fetchPosts(1)
    return user to posts
}

suspend fun loadDataConcurrent(): Pair<User, List<Post>> = coroutineScope {
    val userDeferred = async { fetchUser(1) }
    val postsDeferred = async { fetchPosts(1) }
    userDeferred.await() to postsDeferred.await()
}

// Suspend functions with callbacks
suspend fun fetchData(url: String): String = suspendCoroutine { continuation ->
    fetchDataWithCallback(url) { result, error ->
        if (error != null) {
            continuation.resumeWithException(error)
        } else {
            continuation.resume(result)
        }
    }
}

fun fetchDataWithCallback(
    url: String,
    callback: (String, Exception?) -> Unit
) {
    // Simulated callback-based API
    callback("data", null)
}

// Cancellable suspend functions
suspend fun downloadFile(url: String): ByteArray =
    suspendCancellableCoroutine { continuation ->
        val request = startDownload(url) { data, error ->
            if (error != null) {
                continuation.resumeWithException(error)
            } else {
                continuation.resume(data)
            }
        }

        continuation.invokeOnCancellation {
            request.cancel()
        }
    }

class DownloadRequest {
    fun cancel() {}
}

fun startDownload(url: String, callback: (ByteArray, Exception?) -> Unit):
    DownloadRequest {
    return DownloadRequest()
}

// Using withContext for dispatcher switching
suspend fun saveToDatabase(user: User) {
    withContext(Dispatchers.IO) {
        // Database operation on IO dispatcher
        println("Saving user: ${user.name}")
    }
}

suspend fun updateUI(user: User) {
    withContext(Dispatchers.Main) {
        // UI update on main thread
        println("Updating UI for: ${user.name}")
    }
}
```

Mark with `suspend` modifier. Callable only from other suspend functions or
coroutines.

## Coroutine Builders

```kotlin
// launch: fire-and-forget child of a scope you own (viewModelScope,
// lifecycleScope, a request scope, or an injected application scope)
fun CoroutineScope.launchExample() = launch {
    val user = fetchUser(1)
    println("User: ${user.name}")
}

// async: coroutine with result
fun CoroutineScope.asyncExample() {
    launch {
        val deferredUser = async { fetchUser(1) }
        val deferredPosts = async { fetchPosts(1) }

        val user = deferredUser.await()
        val posts = deferredPosts.await()

        println("Loaded ${posts.size} posts for ${user.name}")
    }
}

// runBlocking: bridges blocking and suspending worlds - only in main() or
// legacy blocking entry points, never inside a coroutine or on a UI thread
fun runBlockingExample() = runBlocking {
    val user = fetchUser(1)
    println("User loaded: ${user.name}")
}

// coroutineScope: structured concurrency
suspend fun loadMultipleUsers(ids: List<Int>): List<User> = coroutineScope {
    ids.map { id ->
        async { fetchUser(id) }
    }.awaitAll()
}

// supervisorScope: independent child failures
suspend fun loadDataWithSupervisor(): List<User> = supervisorScope {
    val user1 = async { fetchUser(1) }
    val user2 = async {
        delay(100)
        throw Exception("Failed")
    }

    // user1 succeeds even if user2 fails
    listOfNotNull(
        try { user1.await() } catch (e: Exception) { null }
    )
}

// withTimeout: time-limited coroutines
suspend fun fetchWithTimeout(id: Int): User? {
    return try {
        withTimeout(2000) {
            fetchUser(id)
        }
    } catch (e: TimeoutCancellationException) {
        null
    }
}

// Structured concurrency with lifecycle (Android): viewModelScope is
// SupervisorJob + Dispatchers.Main.immediate and is cancelled in onCleared()
class UserViewModel(private val repo: UserRepository) : ViewModel() {
    fun loadData() {
        viewModelScope.launch {
            val user = repo.fetchUser(1)
            // Update UI state
        }
    }
}

// Outside Android: own the scope explicitly and cancel it on shutdown
class SyncService(
    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    fun start() = scope.launch { /* periodic work */ }
    fun close() = scope.cancel()
}
```

Never use `GlobalScope`: no owner, so its coroutines leak, ignore caller
cancellation, cannot be awaited in tests. If work must outlive a screen or
request, inject application-level `CoroutineScope` instead.

`coroutineScope` waits for children before exiting: no leaks, proper cleanup.

## Coroutine Context and Dispatchers

```kotlin
// Dispatchers for thread pools
suspend fun dispatcherExamples() {
    // Main: UI thread (Android/JavaFX)
    withContext(Dispatchers.Main) {
        println("On main thread: ${Thread.currentThread().name}")
    }

    // IO: for blocking I/O operations
    withContext(Dispatchers.IO) {
        println("On IO thread: ${Thread.currentThread().name}")
    }

    // Default: CPU-intensive work
    withContext(Dispatchers.Default) {
        println("On default thread: ${Thread.currentThread().name}")
    }

    // Unconfined: starts in caller thread, resumes where suspended
    withContext(Dispatchers.Unconfined) {
        println("Unconfined: ${Thread.currentThread().name}")
    }
}

// Coroutine context elements
fun contextExample() {
    val scope = CoroutineScope(
        Dispatchers.Main +
        SupervisorJob() +
        CoroutineName("MyCoroutine") +
        CoroutineExceptionHandler { _, throwable ->
            println("Caught: $throwable")
        }
    )

    scope.launch {
        println("Context: $coroutineContext")
    }
}

// Inheriting context
fun inheritContextExample() {
    CoroutineScope(Dispatchers.Main).launch {
        println("Parent: ${Thread.currentThread().name}")

        launch {
            // Inherits Dispatchers.Main
            println("Child: ${Thread.currentThread().name}")
        }

        launch(Dispatchers.IO) {
            // Overrides with IO dispatcher
            println("Override: ${Thread.currentThread().name}")
        }
    }
}

// ThreadLocal context element
val threadLocalValue = ThreadLocal<String>()

suspend fun threadLocalExample() {
    threadLocalValue.set("initial")

    withContext(threadLocalValue.asContextElement("new value")) {
        println("In context: ${threadLocalValue.get()}")
    }

    println("After context: ${threadLocalValue.get()}")
}

// Custom context element
data class UserId(val id: Int) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<UserId>
}

suspend fun customContextExample() {
    withContext(UserId(42)) {
        val userId = coroutineContext[UserId]
        println("User ID: ${userId?.id}")
    }
}
```

Child coroutines inherit all context elements (not only dispatcher); can override them.

## Flow for Reactive Streams

```kotlin
// Basic Flow
fun numberFlow(): Flow<Int> = flow {
    for (i in 1..5) {
        delay(100)
        emit(i)
    }
}

suspend fun collectFlow() {
    numberFlow().collect { value ->
        println("Received: $value")
    }
}

// Flow builders
fun flowBuilders() {
    // flowOf: emit fixed values
    val fixedFlow = flowOf(1, 2, 3, 4, 5)

    // asFlow: convert collections
    val listFlow = listOf(1, 2, 3).asFlow()

    // flow: custom emission logic
    val customFlow = flow {
        repeat(3) {
            emit(it)
            delay(100)
        }
    }
}

// Flow transformations
suspend fun flowTransformations() {
    numberFlow()
        .map { it * 2 }
        .filter { it > 5 }
        .take(3)
        .collect { println(it) }
}

// Flow combining
suspend fun combineFlows() {
    val flow1 = flowOf(1, 2, 3)
    val flow2 = flowOf("A", "B", "C")

    // zip: pairs elements
    flow1.zip(flow2) { num, letter ->
        "$num$letter"
    }.collect { println(it) }

    // combine: latest from each
    flow1.combine(flow2) { num, letter ->
        "$num$letter"
    }.collect { println(it) }
}

// Flow exception handling
suspend fun flowExceptionHandling() {
    flow {
        emit(1)
        emit(2)
        throw Exception("Error!")
    }.catch { e ->
        println("Caught: ${e.message}")
        emit(-1)
    }.collect { println(it) }
}

// StateFlow and SharedFlow
class DataRepository {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events

    suspend fun loadUsers() {
        val loaded = fetchUsers()
        _users.value = loaded
    }

    suspend fun emitEvent(event: Event) {
        _events.emit(event)
    }

    private suspend fun fetchUsers(): List<User> {
        delay(100)
        return listOf(User(1, "Alice"))
    }
}

data class Event(val type: String)

// Typical search pipeline: debounce input, cancel stale requests, do I/O off main
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
fun searchResults(query: Flow<String>, repo: SearchRepository): Flow<List<Item>> =
    query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { q -> repo.search(q) }   // previous search is cancelled
        .flowOn(Dispatchers.IO)                  // affects upstream only

// Inside a ViewModel: expose hot state; WhileSubscribed stops upstream when no UI
val uiState: StateFlow<UiState> = repository.users
    .map { UiState.Loaded(it) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

// Flow on different dispatchers
suspend fun flowWithContext() {
    flow {
        emit(1)
        emit(2)
    }
    .flowOn(Dispatchers.IO)
    .collect { value ->
        // Collected on caller's context
        println("Value: $value")
    }
}

// Channel Flow for hot streams
fun channelFlowExample() = channelFlow {
    launch {
        repeat(3) {
            send(it)
            delay(100)
        }
    }

    launch {
        repeat(3) {
            send(it * 10)
            delay(150)
        }
    }
}
```

Flow is cold: does not run until collected. StateFlow holds state, SharedFlow
broadcasts events, channelFlow allows concurrent emissions.

## Channels for Communication

Channels pass values between coroutines. Like BlockingQueue, but suspending.

```kotlin
// Basic channel usage
suspend fun channelExample() = coroutineScope {
    val channel = Channel<Int>()

    launch {
        for (x in 1..5) {
            channel.send(x)
        }
        channel.close()
    }

    for (y in channel) {
        println("Received: $y")
    }
}

// Buffered channels
suspend fun bufferedChannelExample() = coroutineScope {
    val channel = Channel<Int>(capacity = 4)

    launch {
        for (x in 1..10) {
            println("Sending $x")
            channel.send(x)
        }
        channel.close()
    }

    launch {
        delay(1000)
        for (y in channel) {
            println("Received: $y")
            delay(200)
        }
    }
}

// Channel types
fun channelTypes() {
    // Rendezvous: no buffer, sender suspends until receiver
    val rendezvous = Channel<Int>()

    // Buffered: specified capacity
    val buffered = Channel<Int>(10)

    // Unlimited: unlimited buffer
    val unlimited = Channel<Int>(Channel.UNLIMITED)

    // Conflated: keeps only latest value
    val conflated = Channel<Int>(Channel.CONFLATED)
}

// Produce builder (extension on the caller's scope)
fun CoroutineScope.produceNumbers(): ReceiveChannel<Int> = produce {
    for (x in 1..5) {
        send(x * x)
        delay(100)
    }
}

suspend fun consumeNumbers() = coroutineScope {
    val channel = produceNumbers()
    channel.consumeEach { println(it) }
}

// Multiple consumers
suspend fun multipleConsumers() = coroutineScope {
    val channel = Channel<Int>()

    repeat(3) { id ->
        launch {
            for (value in channel) {
                println("Consumer $id received: $value")
            }
        }
    }

    repeat(10) {
        channel.send(it)
        delay(100)
    }

    channel.close()
}

// Select expression for multiple channels
suspend fun selectExample() = coroutineScope {
    val channel1 = produce { send("A") }
    val channel2 = produce { send("B") }

    select<Unit> {
        channel1.onReceive { value ->
            println("From channel1: $value")
        }
        channel2.onReceive { value ->
            println("From channel2: $value")
        }
    }
    coroutineContext.cancelChildren() // stop the producer that lost the race
}
```

Patterns: fan-out (multiple consumers), fan-in (multiple producers), pipelines.

## Cancellation and Exception Handling

```kotlin
// Basic cancellation
suspend fun cancellationExample() = coroutineScope {
    val job = launch {
        repeat(1000) { i ->
            println("Working: $i")
            delay(500)
        }
    }

    delay(2000)
    println("Cancelling...")
    job.cancel()
    job.join()
    println("Cancelled")
}

// Checking for cancellation in CPU loops (no suspension points)
fun CoroutineScope.checkCancellation() = launch(Dispatchers.Default) {
    var i = 0
    while (isActive) {          // or ensureActive() / yield()
        println("Computing: ${i++}")
    }
}

// Non-cancellable work
suspend fun nonCancellableCleanup() = coroutineScope {
    val job = launch {
        try {
            repeat(1000) {
                delay(500)
            }
        } finally {
            withContext(NonCancellable) {
                println("Cleanup in non-cancellable context")
                delay(1000)
                println("Cleanup complete")
            }
        }
    }

    delay(1000)
    job.cancelAndJoin()
}

// Timeout handling
suspend fun timeoutExample() {
    try {
        withTimeout(1000) {
            repeat(100) {
                delay(100)
                println("Working...")
            }
        }
    } catch (e: TimeoutCancellationException) {
        println("Timed out")
    }
}

// CoroutineExceptionHandler: last-resort logging for uncaught failures in a
// root launch. It has no effect on async (exceptions surface on await) or on
// child coroutines. Use try/catch inside suspend code for real handling.
val handler = CoroutineExceptionHandler { _, exception ->
    println("Caught: $exception")
}

class Screen(private val scope: CoroutineScope) {
    fun refresh() = scope.launch(handler) {
        try {
            riskyOperation()
        } catch (e: IOException) {
            // handle the specific, expected failure here
        }
    }
}

// Structured exception handling
suspend fun structuredExceptions() = coroutineScope {
    val job1 = launch {
        delay(100)
        throw Exception("Job 1 failed")
    }

    val job2 = launch {
        delay(200)
        println("Job 2 completed")
    }

    // job2 is cancelled when job1 fails
}

// SupervisorScope for independent failures
suspend fun supervisorExample() = supervisorScope {
    val job1 = launch {
        delay(100)
        throw Exception("Job 1 failed")
    }

    val job2 = launch {
        delay(200)
        println("Job 2 completed")
    }

    // job2 continues even if job1 fails
}

// Error handling with try-catch
suspend fun errorHandling() {
    coroutineScope {
        launch {
            try {
                fetchUser(1)
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }
}
```

Cancellation is cooperative: coroutines must check `isActive` or call suspending
functions. Exception handling respects structured concurrency boundaries.

## Testing Coroutines

Use `kotlinx-coroutines-test`. `runTest` runs on virtual time (delays skipped),
waits for child coroutines, rethrows their uncaught failures.

```kotlin
class UserRepository(
    private val api: UserApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO, // inject it
) {
    suspend fun load(id: Int) = withContext(ioDispatcher) { api.fetch(id) }
}

@Test
fun loadsUser() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val repo = UserRepository(FakeApi(), dispatcher)

    val user = repo.load(1)

    assertEquals(1, user.id)
}

// Time-based operators: advance virtual time explicitly
@Test
fun retriesAfterDelay() = runTest {
    val job = launch { retryingSync(delayMs = 1_000) }
    advanceTimeBy(1_001)
    runCurrent()
    // assert on fakes here
    job.cancel()
}
```

- Inject dispatchers. Never hard-code `Dispatchers.IO` (including in `flowOn`)
  where test must control time: work on real dispatcher runs on real time. For
  code using `Dispatchers.Main`, call `Dispatchers.setMain(StandardTestDispatcher())`
  in setup and `resetMain()` in teardown.
- `TestCoroutineDispatcher`, `TestCoroutineScope` and `runBlockingTest` are
  deprecated. Migrate to `runTest` with `StandardTestDispatcher` or
  `UnconfinedTestDispatcher`.
- If test hangs, look for dispatcher not tied to `testScheduler`, or infinite
  collector launched in test scope instead of `backgroundScope`.

## Best Practices

1. **Flow over callbacks** for reactive streams: backpressure, operators,
   structured lifecycle

2. **supervisorScope for independent operations**: one failure does not cancel
   unrelated coroutines

3. **withContext for dispatcher switching**, not new coroutines. Keeps
   structure, cuts overhead.

4. **Handle exceptions explicitly** (try-catch or CoroutineExceptionHandler). No
   silent failures.

5. **Avoid volatile for coroutine synchronization** on multi-threaded
   dispatchers: causes shared mutable state problems. Prefer Atomic types or
   synchronization structures like Mutex.withLock.

## Common Pitfalls

1. **Blocking inside coroutines** (Thread.sleep, blocking I/O): defeats purpose,
   can exhaust thread pools

2. **Catching CancellationException** without rethrowing: breaks cancellation
   propagation in structured concurrency

3. **delay(0) to yield** does not work: `delay` returns immediately for
   non-positive duration without suspending. Call `yield()` instead.

4. **Too many coroutines**: degrades performance. Batch or throttle when
   possible.

5. **Assuming immediate execution** after launch: coroutine may not start until
   dispatcher has capacity

6. **Mixing callbacks and coroutines** without proper bridging: race conditions
   and leaks

## When to Use This Skill

Android apps: network calls, database queries, any I/O-bound work that should
not block main thread.

Server-side Kotlin with Ktor, Spring Boot or Quarkus: concurrent requests without
thread-per-request overhead.

Flow for reactive streams in MVVM, replacing LiveData or RxJava for state and
lifecycle-aware events.

Structured concurrency to coordinate async operations, cancelling when
navigating away from screens or closing connections.

Channels for producer-consumer patterns, pipelines, explicit communication
between concurrent coroutines.

## Resources

- [Kotlin Coroutines Guide](<https://kotlinlang.org/docs/coroutines-guide.html>)
- [Coroutines on Android](<https://developer.android.com/kotlin/coroutines>)
- [Flow Documentation](<https://kotlinlang.org/docs/flow.html>)
- [KotlinConf Coroutines Talks](<https://www.youtube.com/kotlinconf>)
- [Coroutines Best Practices](<https://developer.android.com/kotlin/coroutines/coroutines-best-practices>)
- [Testing coroutines](<https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/>)

Attribution for merged content: see [references/ATTRIBUTION.md](references/ATTRIBUTION.md).
