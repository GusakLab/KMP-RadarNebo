package com.gusak.lab.radarnebo.ui.map

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.gusak.lab.radarnebo.geo.GeoPoint
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import org.jetbrains.compose.resources.MissingResourceException
import org.jetbrains.compose.resources.decodeToImageBitmap
import radarnebo.shared.generated.resources.Res
import kotlin.concurrent.Volatile
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sinh
import kotlin.math.tan
import kotlin.time.Clock

private data class TileKey(val source: TileSource, val z: Int, val x: Int, val y: Int)

/**
 * What one drawn frame of the map asked for, at the zoom that frame wanted. [missing] tiles are still to come, whether
 * loading or waiting to retry, with a coarser stand-in or nothing in their place; [absent] ones never will, being
 * outside what's bundled.
 */
data class TileFrame(val source: TileSource, val z: Int, val drawn: Int, val missing: Int, val absent: Int)

/**
 * The tiles bundled for each street style, per zoom: the ranges in files/map/coverage.txt, which
 * tools/download-map-tiles.py writes from the map's visible bounds. Tiles outside it are never looked up.
 */
private class BundledCoverage(private val ranges: Map<Int, IntArray>) {
    operator fun contains(key: TileKey): Boolean {
        val r = ranges[key.z] ?: return false
        return key.x in r[0]..r[1] && key.y in r[2]..r[3]
    }

    companion object {
        fun parse(text: String) = BundledCoverage(
            text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map { line -> line.split(Regex("\\s+")).map(String::toInt) }
                .associate { it[0] to intArrayOf(it[1], it[2], it[3], it[4]) },
        )
    }
}

/** Where [drawMapTiles] gets its tiles from. Public only because [MapTiles] is. */
interface TileLookup {
    /** The tile to draw, or null while it is missing (a lookup may start loading it). */
    fun get(source: TileSource, z: Int, x: Int, y: Int): ImageBitmap?

    /** A tile already in memory, or null; never starts a load. */
    fun cached(source: TileSource, z: Int, x: Int, y: Int): ImageBitmap?

    /** Whether a tile [get] returned null for can still arrive. */
    fun canArrive(source: TileSource, z: Int, x: Int, y: Int): Boolean = false

    /** Told what each drawn frame asked for. */
    fun onFrameDrawn(frame: TileFrame) {}
}

/** A view's tiles read ahead of drawing (see [MapTiles.preload]); it never changes. */
private class PreloadedTiles(private val tiles: Map<TileKey, ImageBitmap>) : TileLookup {
    override fun get(source: TileSource, z: Int, x: Int, y: Int): ImageBitmap? = tiles[TileKey(source, z, x, y)]

    override fun cached(source: TileSource, z: Int, x: Int, y: Int): ImageBitmap? = get(source, z, x, y)
}

/**
 * Loads map tiles on demand, from the app's resources for [TileSource.bundled] sets and over the network for
 * the rest, and keeps the latest [capacity] in memory. [get] never waits: it returns what
 * is cached and starts loading what isn't; [loaded] changes when a tile arrives, so a canvas reading it redraws.
 * Used from the main thread only (drawing happens there), so the cache needs no locking.
 *
 * Previews render only their first frames, so they draw from [preload] instead. Without a [client], other tiles
 * are never downloaded.
 */
class MapTiles(
    private val client: HttpClient?,
    private val capacity: Int = 400,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : TileLookup {
    private val cache = LinkedHashMap<TileKey, ImageBitmap>()
    private val loading = HashSet<TileKey>()
    private val failedAt = HashMap<TileKey, Long>()

    // Tile servers ask clients to keep parallel requests low.
    private val permits = Semaphore(6)

    // Read once, on the first bundled tile asked for; until then bundled tiles are reported missing.
    private var coverage: BundledCoverage? = null
    private var coverageRequested = false

    /** Counts arrivals; reading it in a draw block makes the canvas redraw as tiles come in. */
    internal var loaded: Int by mutableIntStateOf(0)
        private set

    /**
     * The latest frame's tiles. The app never reads this: store screenshot captures wait on it (PlayScreenshots in
     * androidTest), as the UI gives no sign of when the map has finished loading. Written while drawing, so it must
     * stay out of snapshot state, and read from the test's thread.
     */
    @Volatile
    var lastFrame: TileFrame? = null
        private set

    /** True while a tile asked for, or the bundled coverage, is still loading: for tests to wait on. */
    internal val isLoading: Boolean get() = loading.isNotEmpty() || (coverageRequested && coverage == null)

    /** A tile already in memory, or null; never starts a download. */
    override fun cached(source: TileSource, z: Int, x: Int, y: Int): ImageBitmap? {
        val key = TileKey(source, z, x, y)
        return cache.remove(key)?.also { cache[key] = it }
    }

    override fun get(source: TileSource, z: Int, x: Int, y: Int): ImageBitmap? {
        cached(source, z, x, y)?.let { return it }
        val key = TileKey(source, z, x, y)
        val bundled = source.bundled
        if (bundled != null && !isBundled(key)) return null
        if (bundled == null && client == null) return null
        val now = clock()
        if (key in loading || (failedAt[key]?.let { now - it < RETRY_MS } == true)) return null
        loading += key
        scope.launch {
            val image = withContext(Dispatchers.Default) {
                if (bundled != null) readBundled(bundled, key) else download(key)
            }
            loading -= key
            if (image == null) {
                failedAt[key] = clock()
            } else {
                failedAt -= key
                store(key, image)
                loaded++
            }
        }
        return null
    }

    /**
     * Reads the bundled tiles [drawMapTiles] draws for [camera] over a view of [size], all at once: previews
     * render only their first frames, so they draw from this rather than from tiles arriving one by one. Touches
     * none of this cache's state, so it can run on any thread.
     */
    internal suspend fun preload(source: TileSource, camera: MapCamera, size: Size): TileLookup {
        val directory = source.bundled ?: return PreloadedTiles(emptyMap())
        return withContext(Dispatchers.Default) {
            val coverage = BundledCoverage.parse(readCoverage().orEmpty())
            val tiles = TileWindow.covering(source, camera, size).keys(source)
                .filter { it in coverage }
                .mapNotNull { key -> readBundled(directory, key)?.let { key to it } }
                .toMap()
            PreloadedTiles(tiles)
        }
    }

    private fun store(key: TileKey, image: ImageBitmap) {
        cache[key] = image
        while (cache.size > capacity) cache.remove(cache.keys.first())
    }

    private suspend fun download(key: TileKey): ImageBitmap? {
        val http = client ?: return null
        return permits.withPermit {
            try {
                val response = http.get(key.source.url(key.z, key.x, key.y)) {
                    header(HttpHeaders.UserAgent, USER_AGENT)
                }
                if (response.status.isSuccess()) response.bodyAsBytes().decodeToImageBitmap() else null
            } catch (_: IOException) {
                // Offline, unreachable or timed out: the tile counts as failed and is tried again later.
                null
            } catch (_: IllegalArgumentException) {
                // A body that is no image.
                null
            }
        }
    }

    override fun canArrive(source: TileSource, z: Int, x: Int, y: Int): Boolean {
        if (source.bundled == null) return client != null
        return coverage?.contains(TileKey(source, z, x, y)) ?: true
    }

    override fun onFrameDrawn(frame: TileFrame) {
        lastFrame = frame
    }

    private fun isBundled(key: TileKey): Boolean {
        coverage?.let { return key in it }
        if (!coverageRequested) {
            coverageRequested = true
            scope.launch {
                val text = withContext(Dispatchers.Default) { readCoverage() }
                coverage = BundledCoverage.parse(text.orEmpty())
                loaded++
            }
        }
        return false
    }

    private companion object {
        const val RETRY_MS = 30_000L
        const val USER_AGENT = "RadarNebo map client"
    }
}

private suspend fun readBundled(directory: String, key: TileKey): ImageBitmap? = try {
    Res.readBytes("files/map/$directory/${key.z}/${key.x}/${key.y}.${key.source.extension}").decodeToImageBitmap()
} catch (_: MissingResourceException) {
    null
} catch (_: IllegalArgumentException) {
    // A file the platform's image decoder can't read.
    null
}

private suspend fun readCoverage(): String? = try {
    Res.readBytes("files/map/coverage.txt").decodeToString()
} catch (_: MissingResourceException) {
    null
}

/** Map tiles for the map to draw under the oblasts; null (tests, most previews) draws none. */
internal val LocalMapTiles: ProvidableCompositionLocal<MapTiles?> = staticCompositionLocalOf { null }

private const val TILE_PX = 256
private const val MAX_TILES = 80

// Web Mercator, which tiles use: x is linear in longitude, y is not in latitude.
private fun lonToTileX(lon: Double, n: Int) = (lon + 180.0) / 360.0 * n
private fun latToTileY(lat: Double, n: Int) =
    (1.0 - ln(tan(lat * PI / 180.0) + 1.0 / cos(lat * PI / 180.0)) / PI) / 2.0 * n
private fun tileYToLat(y: Double, n: Int) = atan(sinh(PI * (1.0 - 2.0 * y / n))) * 180.0 / PI
private fun tileXToLon(x: Double, n: Int) = x / n * 360.0 - 180.0

/** The tiles covering a view: columns [xs] and rows [ys] at zoom level [z]. */
private class TileWindow(val z: Int, val xs: IntRange, val ys: IntRange) {
    val count: Int get() = (xs.last - xs.first + 1) * (ys.last - ys.first + 1)

    fun keys(source: TileSource): List<TileKey> = xs.flatMap { x -> ys.map { y -> TileKey(source, z, x, y) } }

    companion object {
        /**
         * The window [camera] shows over a view of [size]: the zoom level whose tiles are at least as sharp as the
         * screen, coarsened while that takes more than [MAX_TILES] tiles.
         */
        fun covering(source: TileSource, camera: MapCamera, size: Size): TileWindow {
            val topLeft = MapProjection.toGeo(camera.toWorld(Offset.Zero, size))
            val bottomRight = MapProjection.toGeo(camera.toWorld(Offset(size.width, size.height), size))
            // Screen pixels per degree of longitude decide the zoom level whose tiles are at least that sharp.
            val pxPerDegree = size.width / (bottomRight.lon - topLeft.lon)
            val sharpest = ceil(ln(pxPerDegree * 360.0 / TILE_PX) / ln(2.0)).toInt().coerceIn(3, source.maxZoom)
            var window = at(sharpest, topLeft, bottomRight)
            while (window.count > MAX_TILES && window.z > 3) window = at(window.z - 1, topLeft, bottomRight)
            return window
        }

        private fun at(z: Int, topLeft: GeoPoint, bottomRight: GeoPoint): TileWindow {
            val n = 1 shl z
            val x0 = floor(lonToTileX(topLeft.lon, n)).toInt().coerceAtLeast(0)
            val x1 = floor(lonToTileX(bottomRight.lon, n)).toInt().coerceAtMost(n - 1)
            val y0 = floor(latToTileY(topLeft.lat.coerceIn(-85.0, 85.0), n)).toInt().coerceAtLeast(0)
            val y1 = floor(latToTileY(bottomRight.lat.coerceIn(-85.0, 85.0), n)).toInt().coerceAtMost(n - 1)
            return TileWindow(z, x0..x1, y0..y1)
        }
    }
}

/**
 * Draws the tiles covering the view. The map is equirectangular and tiles are Mercator, so each tile is drawn
 * as horizontal strips, each placed at its own latitude; this keeps coastlines on the oblast borders. A tile
 * still loading is stood in for by the nearest coarser one already cached.
 */
internal fun DrawScope.drawMapTiles(tiles: TileLookup, source: TileSource, camera: MapCamera) {
    val window = TileWindow.covering(source, camera, size)
    val z = window.z
    var drawn = 0
    var missing = 0
    var absent = 0
    for (x in window.xs) for (y in window.ys) {
        val key = TileKey(source, z, x, y)
        val image = tiles.get(source, z, x, y)
        if (image != null) {
            drawn++
            drawTile(image, IntOffset.Zero, TILE_PX, key, camera)
        } else {
            if (tiles.canArrive(source, z, x, y)) missing++ else absent++
            drawCachedAncestor(tiles, key, camera)
        }
    }
    tiles.onFrameDrawn(TileFrame(source, z, drawn, missing, absent))
}

/** A cached ancestor of [key], cropped to this tile's part of it. */
private fun DrawScope.drawCachedAncestor(tiles: TileLookup, key: TileKey, camera: MapCamera) {
    for (up in 1..4) {
        if (key.z - up < 0) break
        val parent = tiles.cached(key.source, key.z - up, key.x shr up, key.y shr up) ?: continue
        val part = TILE_PX shr up
        val src = IntOffset((key.x and ((1 shl up) - 1)) * part, (key.y and ((1 shl up) - 1)) * part)
        drawTile(parent, src, part, key, camera)
        break
    }
}

private const val STRIPS = 8

/** Draws the [srcSize] square of [image] at [src] where the tile [key] belongs on the map. */
private fun DrawScope.drawTile(image: ImageBitmap, src: IntOffset, srcSize: Int, key: TileKey, camera: MapCamera) {
    val n = 1 shl key.z
    val left = camera.toScreen(MapProjection.toWorld(GeoPoint(0.0, tileXToLon(key.x.toDouble(), n))), size).x
    val right = camera.toScreen(MapProjection.toWorld(GeoPoint(0.0, tileXToLon(key.x + 1.0, n))), size).x
    val l = left.roundToInt()
    val w = right.roundToInt() - l
    for (i in 0 until STRIPS) {
        val lat0 = tileYToLat(key.y + i.toDouble() / STRIPS, n)
        val lat1 = tileYToLat(key.y + (i + 1).toDouble() / STRIPS, n)
        val top = camera.toScreen(MapProjection.toWorld(GeoPoint(lat0, 0.0)), size).y.roundToInt()
        val bottom = camera.toScreen(MapProjection.toWorld(GeoPoint(lat1, 0.0)), size).y.roundToInt()
        if (bottom < 0 || top > size.height || w <= 0 || bottom <= top) continue
        val sy0 = src.y + srcSize * i / STRIPS
        val sy1 = src.y + srcSize * (i + 1) / STRIPS
        drawImage(
            image,
            srcOffset = IntOffset(src.x, sy0),
            srcSize = IntSize(srcSize, (sy1 - sy0).coerceAtLeast(1)),
            dstOffset = IntOffset(l, top),
            dstSize = IntSize(w, bottom - top),
            filterQuality = FilterQuality.Medium,
        )
    }
}
