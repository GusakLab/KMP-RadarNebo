let meta = #{
    name: "server-instrumentation",
    description: "Add server metrics, tracing, logging, Koin, Arrow, Resilience4j, HikariCP, Kotest and MockK",
    phases: [
        #{ title: "Implement", detail: "one agent edits the server on the current branch" },
        #{ title: "Verify", detail: "run the server tests" },
        #{ title: "Fix", detail: "one repair pass if the tests failed" },
    ],
};

let verify_schema = #{
    "type": "object",
    "required": ["passed", "summary"],
    "properties": #{
        "passed": #{ "type": "boolean" },
        "summary": #{ "type": "string" },
    },
};

phase("Implement");
let spec = "You are editing /home/worxbend/Projects/Github/KMP-RadarNebo on branch chore/server-instrumentation. ";
spec += "Read CODE_STYLE.md sections Modules and Server layers, and CONTRIBUTING.md Dependencies, before editing. ";
spec += "Read the files you change. Do not commit, do not push, do not edit the three untracked skill README.md files. ";
spec += "Do not change /v1 response bodies. Do not edit golden digests. allWarningsAsErrors is on: no @Suppress. ";
spec += "Versions go only in gradle/libs.versions.toml. ";
spec += "Goal: a small observability seam so metrics, tracing, logging and a circuit breaker can be added without a second architecture. ";
spec += "Keep ServerComponents.create as the composition root the console and tests already call. ";
spec += "Use a local koinApplication owned by each ServerComponents, closed in close(). Never call GlobalContext.startKoin. ";
spec += "Use the classic Koin DSL single { }, not the compiler-plugin single<T>() DSL. ";
spec += "Pin koin-core and koin-logger-slf4j at the existing koin version 4.2.2. Do not add koin-ktor. ";
spec += "Add ktor-server-metrics-micrometer at the existing Ktor version 3.6.1. ";
spec += "Add micrometer-registry-prometheus at the Micrometer version that artifact already pulls. Read it from the resolved classpath. Do not upgrade Ktor. ";
spec += "Add opentelemetry instrumentation io.opentelemetry.instrumentation:opentelemetry-ktor-3.0 at the version named on https://ktor.io/docs/server-opentelemetry.html. ";
spec += "Install KtorServerTelemetry only when OTEL_EXPORTER_OTLP_ENDPOINT is non-blank. Tests leave it unset, so golden responses gain no trace headers. ";
spec += "Do not capture Authorization or any query string. ";
spec += "Add com.zaxxer:HikariCP that runs on Java 17. Wire ArchiveDatabase through a HikariDataSource. ";
spec += "SQLite stays one writer: when the JDBC URL starts with jdbc:sqlite, maximumPoolSize is 1 even if HIKARI_MAX_POOL is higher. ";
spec += "connectionInitSql sets PRAGMA journal_mode=WAL for SQLite. Close the pool when the archive closes. In-memory archives stay InMemory and close as a no-op. ";
spec += "Add resilience4j-circuitbreaker and resilience4j-micrometer at one 2.3.x version. Do not add resilience4j-kotlin or resilience4j-retry. Do not add HTTP retries. ";
spec += "The poller already backs off. A second retry would break the alerts.in.ua budget of at most 8 requests a minute. ";
spec += "Put an UpstreamGuard port on SituationPoller with a default that always allows, so existing poller tests stay valid. ";
spec += "The Resilience4j circuit breaker is the infrastructure implementation. When it is open, pollOnce does not call source.fetch and waits the normal poll interval. ";
spec += "Record success and failure on the guard. Register the breaker on the same Prometheus registry. ";
spec += "Add io.arrow-kt:arrow-core at the newest 2.x line that compiles on Kotlin 2.4.21. ";
spec += "Use Either only for ObservabilityConfig.fromEnv. Invalid HIKARI_MAX_POOL (not an int, below 1, or above 16) is Left. ";
spec += "Blank means the default. ServerConfig stays as it is. Startup throws IllegalArgumentException with the Left message, same as invalid ServerConfig. ";
spec += "Add GET /metrics beside /health: same client rate limit, no client key. Body is Prometheus text. ";
spec += "Micrometer timers must not keep query strings. A MeterFilter strips ? and everything after it from every tag value. ";
spec += "Never add a tag named token, authorization or key. A request GET /metrics?token=secret must not put that secret in the scrape body. ";
spec += "Logging: extend mission-control/server/src/main/resources/logback.xml so the pattern includes MDC trace and span ids. ";
spec += "Install Ktor CallLogging at INFO only when the response status is 500 or above. Log method, path without query, and status. Never log Authorization. ";
spec += "Keep the io.ktor logger at WARN. ";
spec += "Add Kotest (runner junit5 and assertions) and MockK. Existing tests use kotlin.test. ";
spec += "If you enable the JUnit Platform, add junit-vintage-engine so the current kotlin.test classes still run. ";
spec += "Prove it by running the full :mission-control:server:test task. ";
spec += "New Kotest spec covers: Either rejects HIKARI_MAX_POOL=0; /metrics is 200 and does not contain a token passed as a query parameter; ";
spec += "after the breaker opens, MockK shows source.fetch is not called. ";
spec += "Packages: config type in server.config, guard port next to the poller in server.application, ";
spec += "Resilience4j, Micrometer and tracing in server.infrastructure.observability, Koin module in server.di. ";
spec += "Domain must not import Ktor, Exposed, JDBC, Micrometer, Koin, Arrow or Resilience4j. ";
spec += "Application must not import Ktor routing or Exposed. One primary type per file. ";
spec += "Document /metrics, OTEL_EXPORTER_OTLP_ENDPOINT, HIKARI_MAX_POOL and the SQLite pool of 1 in docs/deployment.md. ";
spec += "If a library does not resolve or does not compile on Kotlin 2.4.21, stop and say which coordinate failed. Do not suppress the error. ";
spec += "Do not refactor tracking, simulation or the /v1 contract.";
let built = agent(spec, #{ label: "implement", capability_mode: "all" });
if built == () || built.success != true {
    complete(#{ summary: "Implementation agent failed.", passed: false });
}

phase("Verify");
let check_prompt = "In /home/worxbend/Projects/Github/KMP-RadarNebo run ./gradlew --continue :mission-control:server:test. ";
check_prompt += "Read the test report. Set passed true only if the task exits 0 and existing classes such as RadarApiTest still ran. ";
check_prompt += "summary is the first failing test or the success line, at most 500 characters. Do not edit files.";
let check = agent(check_prompt, #{
    label: "verify",
    capability_mode: "execute",
    output_schema: verify_schema,
});
let ok = check != () && check.success && check.output != () && check.output.passed == true;
if ok {
    complete(#{ summary: "Server tests passed.", passed: true });
}

phase("Fix");
let failure = "unknown failure";
if check != () && check.success && check.output != () && check.output.summary != () {
    failure = check.output.summary;
}
let repair = "The server tests failed. Failure: " + failure + ". ";
repair += "Fix only that failure in /home/worxbend/Projects/Github/KMP-RadarNebo on branch chore/server-instrumentation. ";
repair += "Do not commit. Do not edit golden digests to force a pass. Do not add HTTP retries. ";
repair += "Re-run ./gradlew --continue :mission-control:server:test and stop when it exits 0.";
let repaired = agent(repair, #{ label: "fix", capability_mode: "all" });
if repaired == () || repaired.success != true {
    complete(#{ summary: "Repair agent failed.", passed: false });
}

let again_prompt = "Re-run ./gradlew --continue :mission-control:server:test in /home/worxbend/Projects/Github/KMP-RadarNebo. ";
again_prompt += "Set passed true only if the task exits 0. summary is the result line. Do not edit files.";
let again = agent(again_prompt, #{
    label: "verify-again",
    capability_mode: "execute",
    output_schema: verify_schema,
});
let passed_again = again != () && again.success && again.output != () && again.output.passed == true;
complete(#{ summary: if passed_again { "Server tests passed after one fix." } else { "Server tests still fail." }, passed: passed_again });
