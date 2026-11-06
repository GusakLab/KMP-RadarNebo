package com.gusak.lab.radarnebo.server

import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Keeps one RadarNebo server per machine, whether started headless (`:mission-control:server:run`) or in the
 * console. The newest one wins: [acquire] asks a running instance to shut down, waits for it to go, and then holds
 * the lock until [close] or exit. The OS releases the lock when a process dies, so a crashed instance never blocks
 * the next one.
 *
 * A shutdown request goes over a loopback socket and must carry the token from [CONTROL_FILE], which only this
 * user can read.
 */
class SingleInstance private constructor(
    private val channel: FileChannel,
    private val lock: FileLock,
    private val control: ServerSocket,
    private val controlFile: File,
    private val token: String,
) : AutoCloseable {

    override fun close() {
        synchronized(Companion) { if (held === this) held = null }
        runCatching { control.close() }
        if (ownsControlFile()) controlFile.delete()
        runCatching { lock.release() }
        runCatching { channel.close() }
        releaseLock.withLock { released.signalAll() }
    }

    /** False once a newer instance has written its own control file, or the file is gone. */
    private fun ownsControlFile(): Boolean = try {
        controlFile.readText().endsWith(token)
    } catch (_: IOException) {
        false
    }

    /**
     * How [acquire] times its wait for a running instance to go, and pauses between attempts on the lock; tests
     * pass virtual time.
     */
    internal class LockWait(
        val timeSource: TimeSource = TimeSource.Monotonic,
        val pause: (Duration) -> Unit = { awaitRelease(it) },
    )

    companion object {
        private val log = LoggerFactory.getLogger(SingleInstance::class.java)
        private const val LOCK_FILE = "server.lock"
        private const val CONTROL_FILE = "server.control"
        private const val CONTROL_READ_TIMEOUT_MS = 1_000

        /** How long to wait before trying the lock again, unless an instance in this JVM releases it sooner. */
        private val RETRY_INTERVAL = 100.milliseconds

        val defaultDir: File get() = File(System.getProperty("user.home"), ".radarnebo")

        // Kept reachable here: callers may drop the instance, and a collected FileChannel closes and frees the lock.
        private var held: SingleInstance? = null

        // Signalled when an instance in this JVM releases the lock, so one waiting for it takes over at once. A
        // release by another process can't be observed, so the wait for it polls every RETRY_INTERVAL.
        private val releaseLock = ReentrantLock()
        private val released: Condition = releaseLock.newCondition()

        /**
         * Becomes the only server instance, first asking a running one to shut down. [onShutdownRequest] runs
         * (once, on a background thread) when a newer instance asks this one to go: it should stop the server and
         * exit, or [close] this. Throws if the running instance hasn't gone within [timeout].
         */
        fun acquire(
            dir: File = defaultDir,
            timeout: Duration = 15.seconds,
            onShutdownRequest: () -> Unit,
        ): SingleInstance = acquire(dir, timeout, LockWait(), onShutdownRequest)

        /** [acquire], waiting for the running instance as [wait] says. */
        internal fun acquire(
            dir: File,
            timeout: Duration,
            wait: LockWait,
            onShutdownRequest: () -> Unit,
        ): SingleInstance {
            dir.mkdirs()
            val channel = FileChannel.open(
                dir.resolve(LOCK_FILE).toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            )
            // Whatever goes wrong from here closes the channel, which also releases a lock taken before the failure.
            var acquired = false
            try {
                return takeOver(channel, dir.resolve(CONTROL_FILE), timeout, wait, onShutdownRequest)
                    .also { acquired = true }
            } finally {
                if (!acquired) runCatching { channel.close() }
            }
        }

        /** Takes the lock on [channel], asking the instance that holds it to shut down and waiting for it to go. */
        private fun takeOver(
            channel: FileChannel,
            controlFile: File,
            timeout: Duration,
            wait: LockWait,
            onShutdownRequest: () -> Unit,
        ): SingleInstance {
            val started = wait.timeSource.markNow()
            var asked = false
            while (true) {
                val lock = tryLock(channel)
                if (lock != null) return hold(channel, lock, controlFile, onShutdownRequest)
                if (!asked) {
                    log.info("Another RadarNebo server is running; asking it to shut down")
                    askToShutDown(controlFile)
                    asked = true
                }
                if (started.elapsedNow() > timeout) {
                    error("Another RadarNebo server is running and didn't shut down within $timeout")
                }
                wait.pause(RETRY_INTERVAL)
            }
        }

        private fun tryLock(channel: FileChannel): FileLock? = try {
            channel.tryLock()
        } catch (_: OverlappingFileLockException) {
            null // held by this JVM, e.g. an earlier instance in the same process
        }

        /** Waits until an instance in this JVM releases the lock, or for [timeout] at most. */
        private fun awaitRelease(timeout: Duration) {
            releaseLock.withLock { released.await(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS) }
        }

        private fun hold(
            channel: FileChannel,
            lock: FileLock,
            controlFile: File,
            onShutdownRequest: () -> Unit,
        ): SingleInstance {
            val token = UUID.randomUUID().toString()
            val control = ServerSocket(0, 4, InetAddress.getLoopbackAddress())
            try {
                writePrivately(controlFile, "${control.localPort} $token")
            } catch (e: IOException) {
                runCatching { control.close() }
                throw e
            }
            listen(control, token, onShutdownRequest)
            return SingleInstance(channel, lock, control, controlFile, token)
                .also { synchronized(Companion) { held = it } }
        }

        /**
         * Runs [onShutdownRequest] when a request with [token] arrives on [control], until it closes. A raw thread
         * bridges the blocking accept loop; a daemon, so it never keeps the process alive on its own.
         */
        private fun listen(control: ServerSocket, token: String, onShutdownRequest: () -> Unit) {
            val requested = AtomicBoolean(false)
            // Exactly what askToShutDown sends.
            val expected = "$token\n".encodeToByteArray()
            thread(isDaemon = true, name = "single-instance") {
                while (!control.isClosed) {
                    val accepted = accept(control) ?: continue
                    // A local peer must not block the listener: the read is bounded in size (the expected message)
                    // and each read in time, whatever the peer sends or doesn't.
                    val received = accepted.use { socket -> readRequest(socket, expected.size) }
                    // Constant-time, so a local peer can't learn the token from how long a guess takes to reject.
                    if (received != null && MessageDigest.isEqual(received, expected) &&
                        requested.compareAndSet(false, true)
                    ) {
                        log.info("A newer RadarNebo server asked this one to shut down")
                        onShutdownRequest()
                    }
                }
            }
        }

        /** The next connection on [control]; null when accepting failed, e.g. because [control] closed. */
        private fun accept(control: ServerSocket): Socket? = try {
            control.accept()
        } catch (_: IOException) {
            null
        }

        private fun readRequest(socket: Socket, size: Int): ByteArray? = try {
            socket.soTimeout = CONTROL_READ_TIMEOUT_MS
            socket.getInputStream().readNBytes(size)
        } catch (_: IOException) {
            null
        }

        private fun askToShutDown(controlFile: File) {
            val (port, token) = readControlFile(controlFile)?.takeIf { it.size == 2 } ?: return
            try {
                Socket(InetAddress.getLoopbackAddress(), port.toInt()).use { socket ->
                    socket.getOutputStream().write("$token\n".encodeToByteArray())
                }
            } catch (e: IOException) {
                warnUnreachable(e)
            } catch (e: IllegalArgumentException) {
                // A garbled port: not a number, or out of range.
                warnUnreachable(e)
            }
        }

        /** The control file's port and token, as written; null when it can't be read. */
        private fun readControlFile(controlFile: File): List<String>? = try {
            controlFile.readText().trim().split(" ", limit = 2)
        } catch (_: IOException) {
            null
        }

        private fun warnUnreachable(e: Exception) {
            log.warn("Couldn't reach the running server to ask it to shut down: {}", e.message)
        }

        private fun writePrivately(file: File, text: String) {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.delete()
            // Owner-only from the start, so the token is never readable by others, even briefly.
            createOwnerOnly(tmp)
            tmp.writeText(text)
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }

        /** Creates [file] readable and writable by its owner only, where the file system has POSIX permissions. */
        private fun createOwnerOnly(file: File) {
            val ownerOnly = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))
            try {
                Files.createFile(file.toPath(), ownerOnly)
            } catch (_: UnsupportedOperationException) {
                // No POSIX permissions here (Windows): writing the text creates the file as the platform does.
            } catch (_: IOException) {
                // Writing the text creates the file, or fails with the reason.
            }
        }
    }
}
