package com.gusak.lab.radarnebo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared snapshot of the content behind glass surfaces. The content area records itself into
 * [content] every time it draws ([glassBackdropSource]); glass surfaces redraw it blurred behind them.
 */
@Stable
internal class GlassBackdrop(
    val content: GraphicsLayer,
    val blurred: GraphicsLayer,
    /** False where the backdrop can't be recorded (Android Studio previews): glass falls back to a frosted tint. */
    val enabled: Boolean,
) {
    var sourceOrigin: Offset by mutableStateOf(Offset.Zero)

    /** Bumped on every source draw so glass surfaces, drawn in other nodes, redraw with it. */
    var version: Int by mutableIntStateOf(0)
}

/**
 * Whether recording and blurring the backdrop works in IDE previews on this platform. Android
 * previews render with Layoutlib, which doesn't reliably support recorded graphics layers with a
 * blur render effect; desktop previews render with Skia like the app, so they keep the real glass.
 */
internal expect val glassBackdropWorksInPreview: Boolean

@Composable
internal fun rememberGlassBackdrop(): GlassBackdrop {
    val content = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    val enabled = !LocalInspectionMode.current || glassBackdropWorksInPreview
    return remember(content, blurred, enabled) { GlassBackdrop(content, blurred, enabled) }
}

/** Records everything this node (and its children) draws, for glass surfaces placed above it. */
internal fun Modifier.glassBackdropSource(backdrop: GlassBackdrop): Modifier = this
    .onGloballyPositioned { backdrop.sourceOrigin = it.positionInRoot() }
    .drawWithContent {
        if (!backdrop.enabled) return@drawWithContent drawContent()
        backdrop.content.record { this@drawWithContent.drawContent() }
        drawLayer(backdrop.content)
        backdrop.version++
    }

private val saturate = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.6f) })

/**
 * iOS-style liquid glass: the backdrop is blurred, saturated and slightly magnified (a cheap
 * stand-in for refraction), tinted, lit by a top highlight and outlined by a specular rim.
 */
@Composable
internal fun Modifier.liquidGlass(
    backdrop: GlassBackdrop,
    shape: Shape,
    blurRadius: Dp = 18.dp,
    magnification: Float = 1.08f,
    /** Extra tint over the glass, for panels whose text must stay readable over a busy backdrop. */
    tint: Color = Color.Transparent,
): Modifier {
    val colors = radarColors
    var position by remember { mutableStateOf(Offset.Zero) }
    val shadow = Color.Black.copy(alpha = 0.5f)
    return this
        .shadow(18.dp, shape, clip = false, ambientColor = shadow, spotColor = shadow)
        .onGloballyPositioned { position = it.positionInRoot() }
        .drawBehind {
            backdrop.version // re-draw whenever the content behind changes
            val outline = shape.createOutline(size, layoutDirection, this)
            val clip = Path().apply { addOutline(outline) }
            if (!backdrop.enabled) {
                drawFrostedStandIn(clip, outline, colors, tint)
                return@drawBehind
            }
            drawBlurredBackdrop(backdrop, clip, position - backdrop.sourceOrigin, blurRadius, magnification)
            clipPath(clip) {
                drawRect(colors.glassTint)
                drawRect(tint)
                val highlight = Color.White.copy(alpha = if (colors.isDark) 0.10f else 0.35f)
                drawRect(Brush.verticalGradient(0f to highlight, 0.55f to Color.Transparent))
            }
            drawSpecularRim(outline, colors)
        }
}

/** Frosted stand-in: no backdrop to blur, so an opaque-enough overlay keeps labels readable. */
private fun DrawScope.drawFrostedStandIn(clip: Path, outline: Outline, colors: RadarColors, tint: Color) {
    clipPath(clip) {
        drawRect(colors.overlay)
        drawRect(tint)
    }
    drawOutline(outline, colors.glassRimLight, style = Stroke(1.dp.toPx()))
}

/** The backdrop behind this surface ([offset] from where it was recorded), blurred, saturated and magnified. */
private fun DrawScope.drawBlurredBackdrop(
    backdrop: GlassBackdrop,
    clip: Path,
    offset: Offset,
    blurRadius: Dp,
    magnification: Float,
) {
    val radius = blurRadius.toPx()
    backdrop.blurred.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
    backdrop.blurred.colorFilter = saturate
    val source = backdrop.content
    backdrop.blurred.record(source.size) { drawLayer(source) }
    clipPath(clip) {
        withTransform({
            scale(magnification, magnification, pivot = center)
            translate(-offset.x, -offset.y)
        }) {
            drawLayer(backdrop.blurred)
        }
    }
}

private fun DrawScope.drawSpecularRim(outline: Outline, colors: RadarColors) {
    drawOutline(
        outline,
        Brush.linearGradient(
            0f to colors.glassRimLight,
            0.35f to colors.glassRimShade,
            0.65f to colors.glassRimShade,
            1f to colors.glassRimLight.copy(alpha = colors.glassRimLight.alpha * 0.6f),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
        style = Stroke(1.dp.toPx()),
    )
}
