package com.gusak.lab.radarnebo.server

import java.lang.ref.Reference
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class SingleInstanceTest {
    private val dir = Files.createTempDirectory("single-instance").toFile()
    private val acquired = mutableListOf<SingleInstance>()

    /** Virtual time for a wait that can only time out: each pause moves it on, so no test waits out a timeout. */
    private val time = TestTimeSource()
    private val virtualWait = SingleInstance.LockWait(time) { time += it }

    /** Acquires in [dir] and remembers the instance, so the test can't leave the lock held. */
    private fun acquire(
        timeout: Duration = 15.seconds,
        wait: SingleInstance.LockWait = SingleInstance.LockWait(),
        onShutdownRequest: () -> Unit = {},
    ): SingleInstance = SingleInstance.acquire(dir, timeout, wait, onShutdownRequest).also { acquired += it }

    @AfterTest
    fun cleanUp() {
        acquired.forEach { it.close() }
        dir.deleteRecursively()
    }

    @Test
    fun aNewerInstanceTakesOverFromTheRunningOne() {
        val asked = CountDownLatch(1)
        val first = CompletableFuture<SingleInstance>()
        first.complete(
            acquire {
                asked.countDown()
                first.join().close()
            },
        )
        acquire()
        assertTrue(asked.await(1, TimeUnit.SECONDS), "the running instance was asked to shut down")
    }

    @Test
    fun anInstanceThatWontGoBlocksTheNewOne() {
        val asked = CountDownLatch(1)
        val requests = AtomicInteger()
        acquire {
            requests.incrementAndGet()
            asked.countDown()
        }
        assertFailsWith<IllegalStateException> { acquire(timeout = 500.milliseconds, wait = virtualWait) }
        assertTrue(asked.await(1, TimeUnit.SECONDS), "the running instance was asked to shut down")
        assertEquals(1, requests.get())
    }

    /**
     * Best-effort: System.gc() is only a hint, and the instance stays reachable by design, so nothing can assert that a
     * collection ran; a regression shows up only when one happens to.
     */
    @Test
    fun theLockOutlivesADroppedInstance() {
        val asked = CountDownLatch(1)
        val requests = AtomicInteger()
        // Dropped, as main() does: only a weak reference is kept, so the test can close it at the end.
        val dropped = WeakReference(
            SingleInstance.acquire(dir) {
                requests.incrementAndGet()
                asked.countDown()
            },
        )
        repeat(5) { collectGarbage() }
        assertFailsWith<IllegalStateException> { acquire(timeout = 500.milliseconds, wait = virtualWait) }
        assertTrue(asked.await(1, TimeUnit.SECONDS), "the running instance was asked to shut down")
        assertEquals(1, requests.get(), "still running, so it was asked to go")
        assertNotNull(dropped.get()).close()
    }

    @Test
    fun aRequestWithoutTheTokenIsIgnored() {
        val asked = CountDownLatch(1)
        val running = acquire { asked.countDown() }
        send("guess\n")
        awaitHandled()
        assertEquals(1L, asked.count, "a wrong token doesn't stop the server")
        running.close()
        acquire().close()
    }

    @Test
    fun aStaleOrGarbledControlFileOnlyCostsTheWaitForTheLock() {
        val holder = acquire()
        // As a crashed instance leaves it (its port dead), then unreadable: asking fails quietly and the wait times
        // out.
        val deadPort = ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { it.localPort }
        for (contents in listOf("$deadPort stale-token", "garbage")) {
            dir.resolve("server.control").writeText(contents)
            assertFailsWith<IllegalStateException> { acquire(timeout = 300.milliseconds, wait = virtualWait) }
        }
        holder.close()
        acquire().close()
    }

    @Test
    fun aSilentPeerOnTheControlPortDoesNotBlockATakeover() {
        val asked = CountDownLatch(1)
        val running = CompletableFuture<SingleInstance>()
        running.complete(
            acquire {
                asked.countDown()
                running.join().close()
            },
        )
        Socket(InetAddress.getLoopbackAddress(), controlPort()).use {
            // Connected and sending nothing, while a newer instance asks the running one to go.
            acquire(timeout = 5.seconds)
            assertTrue(asked.await(1, TimeUnit.SECONDS), "the running instance was still asked to shut down")
        }
    }

    private fun controlPort(): Int = dir.resolve("server.control").readText().substringBefore(" ").toInt()

    /** Sends [message] to the running instance's control port and hangs up. */
    private fun send(message: String) {
        Socket(InetAddress.getLoopbackAddress(), controlPort()).use { socket ->
            socket.getOutputStream().write(message.toByteArray())
        }
    }

    /**
     * Returns once the running instance has handled every request sent before: it handles them one at a time, in
     * order, so by the time it has read and closed this probe, the earlier ones have had their answer.
     */
    private fun awaitHandled() {
        Socket(InetAddress.getLoopbackAddress(), controlPort()).use { probe ->
            probe.soTimeout = 5_000
            probe.shutdownOutput()
            assertEquals(-1, probe.getInputStream().read(), "the running instance closed the probe")
        }
    }

    /** Asks for a collection and waits until one has cleared a weakly held sentinel, for 50 ms at most. */
    private fun collectGarbage() {
        val collected = ReferenceQueue<Any>()
        val sentinel = WeakReference(Any(), collected)
        System.gc()
        collected.remove(50)
        Reference.reachabilityFence(sentinel)
    }
}
