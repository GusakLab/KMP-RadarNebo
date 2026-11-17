package com.gusak.lab.radarnebo.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import java.io.File
import javax.swing.SwingUtilities
import kotlin.test.assertNotNull

internal object PreviewRenderer {
    private val previewPattern = Regex(
        """@Preview\(([^)]*)\)\s*@Composable\s*(?:(?:private|internal)\s+)?fun\s+(\w+)\s*\(""",
    )

    /**
     * Every `@Preview` in the shared UI. The annotation has binary retention, so reflection can't
     * see it; the sources are the reliable list (and give each preview's size).
     */
    fun discover(sources: File = File(System.getProperty("preview.sources"))): List<PreviewFunction> =
        sources.walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            val text = file.readText()
            val packageLine = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE).find(text)
            val pkg = assertNotNull(packageLine, "no package line in $file").groupValues[1]
            val className = "$pkg.${file.nameWithoutExtension}Kt"
            previewPattern.findAll(text).map { match ->
                val args = match.groupValues[1]
                PreviewFunction(
                    name = Regex("""name\s*=\s*"([^"]*)"""").find(args)?.groupValues?.get(1) ?: match.groupValues[2],
                    className = className,
                    functionName = match.groupValues[2],
                    widthDp = Regex("""widthDp\s*=\s*(\d+)""").find(args)?.groupValues?.get(1)?.toInt(),
                    heightDp = Regex("""heightDp\s*=\s*(\d+)""").find(args)?.groupValues?.get(1)?.toInt(),
                )
            }
        }.toList()

    /** How many `@Preview` annotations the sources hold, to check [discover] missed none. */
    fun annotationCount(sources: File = File(System.getProperty("preview.sources"))): Int = sources.walkTopDown()
        .filter { it.extension == "kt" }
        .sumOf { Regex("""@Preview\(""").findAll(it.readText()).count() }

    /**
     * Renders [preview] the way an IDE does: at its declared size, with [LocalInspectionMode] on.
     * A few frames run first so effects (camera framing, the first data poll) have landed.
     *
     * The scene lives on the Swing event thread, where the main dispatcher runs. Lifecycle effects add observers
     * both while the scene composes and from main-dispatcher work, and `LifecycleRegistry` breaks when two threads
     * touch it ("no event up from RESUMED").
     */
    fun render(preview: PreviewFunction, density: Float = 2f): Image {
        val method = preview.method()
        val widthDp = preview.widthDp ?: 440
        val heightDp = preview.heightDp ?: 520
        val scene = onEventThread {
            ImageComposeScene((widthDp * density).toInt(), (heightDp * density).toInt(), Density(density)) {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    method.invoke(null, currentComposer, 0)
                }
            }
        }
        try {
            var image = onEventThread { scene.render(0) }
            repeat(12) {
                // Each frame is its own event, so work dispatched to the main dispatcher in between (the app
                // preview's first poll) lands before it.
                image = onEventThread { scene.render((it + 1) * 16_000_000L) }
            }
            return image
        } finally {
            onEventThread { scene.close() }
        }
    }

    /** Runs [block] on the Swing event thread and returns its result, or throws what it threw. */
    private fun <T> onEventThread(block: () -> T): T {
        var result: Result<T>? = null
        SwingUtilities.invokeAndWait { result = runCatching(block) }
        return checkNotNull(result).getOrThrow()
    }

    /** True when the image is more than a single flat colour. */
    fun hasContent(image: Image): Boolean {
        val bitmap = Bitmap.makeFromImage(image)
        val colors = HashSet<Int>()
        for (y in 0 until image.height step 7) for (x in 0 until image.width step 7) {
            colors += bitmap.getColor(x, y)
            if (colors.size > 3) return true
        }
        return false
    }
}
