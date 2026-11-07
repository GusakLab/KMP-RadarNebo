package com.gusak.lab.radarnebo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A liquid glass panel floating over the map, with a dimmed scrim around it: tapping the scrim or the close
 * button dismisses it. [content] fills the panel below its header and scrolls itself.
 */
@Composable
internal fun GlassDialog(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    /** Space kept free at the bottom, e.g. for the floating navigation bar. */
    bottomInset: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = radarColors
    // Not [modifier]: that insets the panel, while the scrim must reach under the system bars and cutouts.
    AnimatedVisibility(visible, Modifier.fillMaxSize(), enter = fadeIn(tween(GlassScrimFadeMillis)), exit = fadeOut(tween(GlassScrimFadeMillis))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(glassScrim(colors.isDark))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
    }
    AnimatedVisibility(
        visible,
        modifier.fillMaxSize(),
        enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.94f),
        exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.96f),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = bottomInset + 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlassPanel(title, subtitle, onDismiss, backdrop, content)
        }
    }
}

/** The dialog's glass panel: a header with the close button over [content]. */
@Composable
private fun GlassPanel(
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit,
    backdrop: GlassBackdrop,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    val tint = MaterialTheme.colorScheme.surface.copy(alpha = GlassDialogTint)
    Column(
        Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .liquidGlass(backdrop, shape, blurRadius = 28.dp, magnification = 1.02f, tint = tint)
            .clip(shape)
            // Taps inside the panel must not reach the scrim.
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = {}),
    ) {
        // The panel is no Surface, so text in it would otherwise default to black, unreadable in dark themes.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, top = 20.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f).padding(top = 4.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    if (subtitle != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                CloseButton(onDismiss)
            }
            HorizontalDivider(color = radarColors.glassRimShade)
            Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.Top, content = content)
        }
    }
}

/**
 * Opacity of the surface colour over the dialog's glass: with the glass tint it keeps text at WCAG AA contrast
 * whatever map, imagery or alert is behind the dialog.
 */
internal const val GlassDialogTint = 0.7f

/** The dim an open dialog lays over everything behind it, and how long it takes to fade. */
internal fun glassScrim(isDark: Boolean) = Color.Black.copy(alpha = if (isDark) 0.35f else 0.2f)
internal const val GlassScrimFadeMillis = 200

@Composable
private fun CloseButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(radarColors.glassSelection)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Close" },
        contentAlignment = Alignment.Center,
    ) {
        Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
