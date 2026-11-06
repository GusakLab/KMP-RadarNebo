package com.gusak.lab.radarnebo.desktop

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gusak.lab.radarnebo.data.InMemoryStorage
import com.gusak.lab.radarnebo.data.RadarNeboApi
import com.gusak.lab.radarnebo.di.appModules
import com.gusak.lab.radarnebo.di.inMemoryStorageModule
import com.gusak.lab.radarnebo.ui.AppViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** A server started after the app is picked up by the 5 s reconnect timer, or at once by Reconnect. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReconnectTest {
    // The view model's Main dispatcher, the mock server and the view model's clock share one virtual-time
    // scheduler, so the tests step over the 5 s timer instead of waiting for it.
    private val scheduler = TestCoroutineScheduler()
    private var serverUp = false

    /** Refuses connections until [serverUp]; then serves no alerts (tracks and health are optional). */
    private val server = MockEngine(
        MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(scheduler)
            addHandler { request ->
                if (!serverUp) throw IOException("Failed to connect to localhost:8080")
                when (request.url.encodedPath) {
                    RadarNeboApi.ACTIVE_ALERTS_PATH ->
                        respond("""{"alerts":[]}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
                    else -> respond("", HttpStatusCode.NotFound)
                }
            }
        },
    )

    private val koin: KoinApplication = koinApplication {
        allowOverride(true)
        modules(
            appModules(inMemoryStorageModule(InMemoryStorage())) +
                module {
                    single { RadarNeboApi(RadarNeboApi.client(server)) }
                    viewModel {
                        AppViewModel(
                            get(), get(), get(), get(), get(), get(), get(),
                            clock = { START_MS + scheduler.currentTime },
                        )
                    }
                },
        )
    }

    private val viewModels = ViewModelStore()

    @BeforeTest
    fun useVirtualTime() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
    }

    @AfterTest
    fun close() {
        viewModels.clear()
        koin.close()
        Dispatchers.resetMain()
    }

    @Test
    fun retriesAnUnreachableServerAfterFiveSeconds() = withViewModel { vm ->
        runCurrent()
        val failed = vm.state.value
        assertNotNull(failed.error, "the first poll fails")
        val wait = assertNotNull(failed.nextRetryAt) - Instant.fromEpochMilliseconds(START_MS + currentTime)
        assertTrue(wait > 3.seconds && wait <= 5.seconds, "next try in about 5 s, was $wait")

        serverUp = true
        advanceTimeBy(wait)
        runCurrent()
        val connected = vm.state.value
        assertNotNull(connected.lastUpdated, "the retry reached the server")
        assertNull(connected.error)
        assertNull(connected.nextRetryAt)
    }

    @Test
    fun reconnectTriesAtOnce() = withViewModel { vm ->
        runCurrent()
        assertNotNull(vm.state.value.error, "the first poll fails")

        serverUp = true
        vm.reconnect()
        runCurrent()
        assertNotNull(vm.state.value.lastUpdated, "reconnected without waiting for the timer")
    }

    /**
     * Runs [test] with a view model from a [ViewModelStore] and clears the store before returning. The view model's
     * poll loop never ends, so without that runTest would keep running it on the shared scheduler.
     */
    private fun withViewModel(test: suspend TestScope.(AppViewModel) -> Unit): TestResult = runTest(scheduler) {
        val factory = viewModelFactory { initializer { koin.koin.get<AppViewModel>() } }
        val vm = ViewModelProvider.create(viewModels, factory)[AppViewModel::class]
        try {
            test(vm)
        } finally {
            viewModels.clear()
        }
    }

    private companion object {
        /** 2026-03-14T15:00:00Z, the instant the previews use. */
        const val START_MS: Long = 1_773_500_400_000
    }
}
