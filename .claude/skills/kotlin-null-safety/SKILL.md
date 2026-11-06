---
name: kotlin-null-safety
user-invocable: false
description: Apply Kotlin null safety - nullable types, safe calls, Elvis, smart casts, platform types from Java interop, and null-safe API design. Use when fixing NPE risks, removing `!!`, or designing Kotlin APIs that cross Java boundaries.
allowed-tools: []
---

# Kotlin Null Safety

## Nullable Types

`?` suffix makes type nullable (can hold null). Non-nullable type is guaranteed
non-null at compile time.

```kotlin
// Non-nullable types
var name: String = "Alice"
// name = null // Compilation error

// Nullable types
var nullableName: String? = "Bob"
nullableName = null // OK

// Function parameters
fun greet(name: String) {
    println("Hello, $name")
}

fun greetNullable(name: String?) {
    if (name != null) {
        println("Hello, $name")
    } else {
        println("Hello, guest")
    }
}

// greet(null) // Compilation error
greetNullable(null) // OK

// Nullable return types
fun findUser(id: Int): User? {
    return if (id > 0) User(id, "Alice") else null
}

data class User(val id: Int, val name: String)

// Nullable properties
class Person(
    val name: String,
    val email: String?,
    var phoneNumber: String?
)

// Collections with nullable elements
val nullableList: List<String?> = listOf("A", null, "B")
val listOfNullable: List<String>? = null

// Platform types from Java (String!) have unknown nullability and are NOT checked:
// the compiler lets you use them as non-null, so an unexpected null throws an NPE
// at the use site. Declare the Kotlin type explicitly (String?) or annotate the
// Java side with @Nullable/@NonNull.

// Nullable this
class Service {
    fun process() {
        val self: Service? = this
        self?.validate()
    }

    fun validate() {}
}
```

## Safe Call Operator

Safe call `?.` accesses members of nullable reference. Returns null instead of
throwing NPE when receiver is null.

```kotlin
// Basic safe calls
val name: String? = "Alice"
val length: Int? = name?.length

val nullName: String? = null
val nullLength: Int? = nullName?.length // Returns null

// Chaining safe calls
data class Address(val street: String?, val city: String?)
data class Company(val address: Address?)
data class Employee(val company: Company?)

val employee: Employee? = Employee(Company(Address("Main St", "NYC")))

val city: String? = employee?.company?.address?.city
println(city) // "NYC"

val nullEmployee: Employee? = null
val nullCity: String? = nullEmployee?.company?.address?.city
println(nullCity) // null

// Safe calls with methods
fun processUser(user: User?) {
    user?.let { u ->
        println("Processing ${u.name}")
    }
}

// Safe calls with extension functions
fun String?.orDefault(default: String): String {
    return this ?: default
}

val result = nullName.orDefault("Unknown") // plain call: the extension handles null itself

// Safe calls in expressions
class Profile(val bio: String?)

fun displayBio(profile: Profile?) {
    val bioLength = profile?.bio?.length ?: 0
    println("Bio length: $bioLength")
}

// Safe calls with mutable properties
class Container {
    var value: String? = null

    fun updateValue() {
        value?.let { current ->
            value = current.uppercase()
        }
    }
}
```

Chain short-circuits at first null.

## Elvis Operator and Null Coalescing

Elvis `?:` gives default for null expression.

```kotlin
// Basic Elvis operator
val name: String? = null
val displayName = name ?: "Guest"
println(displayName) // "Guest"

// Elvis with safe calls
fun getUserCity(employee: Employee?): String {
    return employee?.company?.address?.city ?: "Unknown"
}

// Elvis with expressions
fun calculateTotal(subtotal: Double?, taxRate: Double?): Double {
    val sub = subtotal ?: 0.0
    val tax = taxRate ?: 0.15
    return sub * (1 + tax)
}

// Elvis with return/throw
fun requireName(name: String?): String {
    return name ?: throw IllegalArgumentException("Name required")
}

fun processUser(user: User?) {
    val u = user ?: return
    println("Processing ${u.name}")
}

// Chaining Elvis operators
fun findValidValue(
    primary: String?,
    secondary: String?,
    tertiary: String?
): String {
    return primary ?: secondary ?: tertiary ?: "default"
}

// Elvis with nullable properties
class Config {
    var timeout: Int? = null

    fun getTimeout(): Int {
        return timeout ?: 30000
    }
}

// Elvis in constructors
class Service(name: String?) {
    val serviceName: String = name ?: "DefaultService"
}

// Elvis with function calls
fun fetchFromCache(): String? = null
fun fetchFromNetwork(): String? = "data"

fun getData(): String {
    return fetchFromCache() ?: fetchFromNetwork() ?: "fallback"
}
```

Right side evaluates only if left side is null, so defaults are lazy.

## Smart Casts

After null check, compiler casts nullable type to non-nullable. No redundant
casts.

```kotlin
// Smart cast after null check
fun printLength(text: String?) {
    if (text != null) {
        println(text.length) // text is smart-cast to String
    }
}

// Smart cast in expressions
fun processName(name: String?): Int {
    return if (name != null) {
        name.length // Smart-cast
    } else {
        0
    }
}

// Smart cast with return
fun requireUser(user: User?): User {
    if (user == null) {
        throw IllegalStateException("User required")
    }
    return user // Smart-cast to User
}

// Smart cast with Elvis
fun getLength(text: String?): Int {
    val nonNull = text ?: return 0
    return nonNull.length // Smart-cast
}

// Smart cast with when expressions
fun describe(obj: Any?): String {
    return when {
        obj == null -> "null"
        obj is String -> "String of length ${obj.length}"
        obj is Int -> "Int: $obj"
        else -> "Unknown type"
    }
}

// Smart cast with let
fun processNullable(value: String?) {
    value?.let { nonNull ->
        println(nonNull.uppercase()) // nonNull is String
    }
}

// Smart cast limitations
class Container(var value: String?) {
    fun process() {
        // Cannot smart-cast var properties
        if (value != null) {
            // println(value.length) // Compilation error
        }

        // Use local variable for smart cast
        val localValue = value
        if (localValue != null) {
            println(localValue.length) // OK
        }
    }
}

// Smart cast with contracts
fun requireNotNull(value: String?) {
    require(value != null)
    println(value.length) // Smart-cast after require
}
```

Works on immutable variables and val properties, not var properties: var
could change between check and use.

## Not-Null Assertion and Platform Types

Not-null assertion `!!` throws NPE if value is null. Use when null is
impossible but compiler cannot prove it.

```kotlin
// Not-null assertion operator
fun processName(name: String?) {
    val length = name!!.length // Throws NPE if name is null
    println("Length: $length")
}

// Use cases for !!
fun initializeFromConfig(config: Map<String, String>) {
    // We know these keys exist
    val apiKey = config["api_key"]!!
    val endpoint = config["endpoint"]!!

    println("Configured endpoint $endpoint") // never log apiKey
}

// Avoid chaining !!
val city = employee!!.company!!.address!!.city!! // Bad

// Better alternatives
val city2 = employee?.company?.address?.city
    ?: throw IllegalStateException("City required")

// Platform types from Java
class JavaInterop {
    fun useJavaApi() {
        val javaString = JavaClass.getString() // String! (platform type)

        // Treat as nullable for safety
        val length: Int? = javaString?.length

        // Or assert non-null
        val length2: Int = javaString!!.length

        // Or add explicit type
        val explicitString: String = JavaClass.getString()
    }
}

// lateinit for non-null initialization
class Service {
    lateinit var apiClient: ApiClient

    fun initialize(client: ApiClient) {
        apiClient = client
    }

    fun makeRequest() {
        if (::apiClient.isInitialized) {
            apiClient.request()
        }
    }
}

class ApiClient {
    fun request() {}
}

// Use !! sparingly and document assumptions
fun parseJson(json: String?): Data {
    // !! OK here because caller ensures non-null
    return Json.parse(json!!)
}

object Json {
    fun parse(json: String): Data = Data()
}

data class Data(val value: String = "")

class JavaClass {
    companion object {
        @JvmStatic
        fun getString(): String = ""
    }
}
```

## Nullability in Collections and Generics

```kotlin
// Nullable elements vs nullable collections
val listWithNulls: List<String?> = listOf("A", null, "B")
val nullableList: List<String>? = null

// Processing nullable elements
fun filterNulls(items: List<String?>): List<String> {
    return items.filterNotNull()
}

val filtered = filterNulls(listOf("A", null, "B"))
println(filtered) // [A, B]

// Nullable map values
val userIds: Map<String, Int?> = mapOf(
    "alice" to 1,
    "bob" to null,
    "charlie" to 3
)

fun getUserId(name: String): Int? {
    return userIds[name]
}

// Generic functions with nullable types
fun <T> firstOrNull(list: List<T>): T? {
    return list.firstOrNull()
}

// Generic constraints with nullability
fun <T : Any> nonNullable(value: T) {
    // T cannot be null
    println(value.toString())
}

// Nullable generic types
class Container<T>(val value: T?)

val stringContainer = Container<String>(null)
val intContainer = Container<Int>(42)

// Extension functions on nullable types
fun <T> List<T?>.filterNotNullAndMap(transform: (T) -> String): List<String> {
    return this.filterNotNull().map(transform)
}

val result = listOf(1, null, 3).filterNotNullAndMap { it.toString() }

// Nullable receivers: define your own like this (the stdlib already ships
// isNullOrEmpty/isNullOrBlank with contracts, so prefer those for strings)
fun String?.isNullOrShort(min: Int = 3): Boolean {
    return this == null || this.length < min
}

val empty: String? = null
println(empty.isNullOrShort()) // true
println(empty.isNullOrEmpty()) // true (stdlib)

// Sequences with nullable elements
fun processSequence(items: Sequence<String?>) {
    items
        .filterNotNull()
        .map { it.uppercase() }
        .forEach { println(it) }
}

// Generic variance and nullability
interface Producer<out T> {
    fun produce(): T?
}

interface Consumer<in T> {
    fun consume(item: T?)
}
```

`List<String?>` is list of nullable strings. `List<String>?` is nullable list.

## Designing Null-Safe APIs

```kotlin
// Prefer non-nullable parameters
class UserRepository {
    fun save(user: User) {
        // user is guaranteed non-null
        println("Saving ${user.name}")
    }
}

// Use nullable returns for optional values
class UserService {
    fun findById(id: Int): User? {
        return if (id > 0) User(id, "Alice") else null
    }

    fun getAllUsers(): List<User> {
        // Return empty list, not null
        return emptyList()
    }
}

// Builder pattern with nullable accumulation
// (trusted identifiers only; bind user values as query parameters, never interpolate)
class QueryBuilder {
    private var table: String? = null
    private var where: String? = null
    private var orderBy: String? = null

    fun from(table: String) = apply { this.table = table }
    fun where(condition: String) = apply { this.where = condition }
    fun orderBy(column: String) = apply { this.orderBy = column }

    fun build(): String {
        val t = table ?: throw IllegalStateException("Table required")
        val w = where?.let { "WHERE $it" } ?: ""
        val o = orderBy?.let { "ORDER BY $it" } ?: ""

        return "SELECT * FROM $t $w $o".trim()
    }
}

// Nullable configuration with defaults
data class Config(
    val timeout: Int? = null,
    val retries: Int? = null,
    val debug: Boolean? = null
) {
    fun getTimeout() = timeout ?: 30000
    fun getRetries() = retries ?: 3
    fun isDebug() = debug ?: false
}

// Sealed classes for optional values
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object NotFound : Result<Nothing>()
}

fun findUser(id: Int): Result<User> {
    return when {
        id < 0 -> Result.Error("Invalid ID")
        id == 0 -> Result.NotFound
        else -> Result.Success(User(id, "Alice"))
    }
}

// Validation with nullable return
class Validator {
    fun validateEmail(email: String): String? {
        return if (email.contains("@")) {
            null // No error
        } else {
            "Invalid email format"
        }
    }
}

// Nullable callback parameters
class AsyncLoader {
    fun load(
        onSuccess: (User) -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        try {
            val user = User(1, "Alice")
            onSuccess(user)
        } catch (e: Exception) {
            onError?.invoke(e.message ?: "Unknown error")
        }
    }
}
```

## Best Practices

1. **Prefer non-nullable types by default**: more compile-time safety, fewer
   null checks

2. **Use safe call operator `?.` for chaining** instead of multiple null checks

3. **Provide defaults with Elvis operator `?:`** instead of if-else chains for
   simple fallbacks

4. **Return empty collections instead of null** so clients skip null checks

5. **Minimize use of `!!` operator** and document assumptions when used.
   Reintroduces crash risk

6. **Design APIs with explicit nullability** to state intent and prevent misuse

7. **Use lateinit for non-null deferred initialization** instead of nullable
   properties with manual null checks

8. **Apply nullable extensions** like `isNullOrEmpty()` for common null handling

9. **Prefer sealed classes over nullable types** to represent success, error,
   and absent states

## Common Pitfalls

1. **Chaining `!!` operators** hides crash points. Use safe calls or explicit
   validation instead

2. **Ignoring platform types from Java** can cause NPEs. Treat Java returns as
   nullable unless documented

3. **Not considering nullability in equals/hashCode** can break collections
   and comparisons

4. **Forgetting that safe calls return nullable types** lets unexpected nulls
   propagate

5. **Using nullable primary constructor parameters** without defaults
   complicates object creation

6. **Creating deeply nested nullable structures** gets unwieldy. Flatten or use
   sealed classes

7. **Not documenting null semantics** in complex APIs leaves callers guessing
   when nulls are valid or what they mean

## When to Use This Skill

Use nullable types and safe calls for external data (network APIs, databases,
user input) where values may be absent.

## Resources

- [Kotlin Null Safety Documentation](<https://kotlinlang.org/docs/null-safety.html>)
- [Idioms: Null Safety](<https://kotlinlang.org/docs/idioms.html#nullable-values>)
- [Kotlin Coding Conventions](<https://kotlinlang.org/docs/coding-conventions.html>)
- [Effective Kotlin](<https://kt.academy/book/effectivekotlin>)
- [Kotlin for Java Developers](<https://www.coursera.org/learn/kotlin-for-java-developers>)
