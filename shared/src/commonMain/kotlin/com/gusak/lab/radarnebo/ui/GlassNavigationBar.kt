package com.gusak.lab.radarnebo.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusak.lab.radarnebo.data.ThemeMode
import kotlin.math.abs

internal data class GlassNavItem(val label: String, val glyph: String, val badge: Int = 0)

internal object GlassNavBarDefaults {
    val ItemWidth = 84.dp
    val Height = 64.dp
    val Margin = 12.dp

    /** Space content should leave below itself so it can scroll clear of the floating bar. */
    val ContentInset = Height + Margin * 2
}

/** Floating pill-shaped navigation bar made of liquid glass, with a lens that glides between items. */
@Composable
internal fun GlassNavigationBar(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)
    val lens = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex) {
        lens.animateTo(selectedIndex.toFloat(), spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow))
    }

    Box(
        modifier
            .height(GlassNavBarDefaults.Height)
            .liquidGlass(backdrop, shape)
            .padding(6.dp),
    ) {
        // The lens stretches while travelling, like a droplet, and settles back to round.
        val stretch = 1f + (abs(lens.targetValue - lens.value).coerceAtMost(1f)) * 0.35f
        GlassLens(position = lens.value, stretch = stretch, shape = shape)
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.Start) {
            items.forEachIndexed { index, item ->
                GlassNavTab(item, selected = index == selectedIndex, onClick = { onSelect(index) })
            }
        }
    }
}

/** The lens over the selected tab, [position] tabs from the start, [stretch] times as wide while travelling. */
@Composable
private fun GlassLens(position: Float, stretch: Float, shape: Shape) {
    val colors = radarColors
    val itemWidth = GlassNavBarDefaults.ItemWidth
    Box(
        Modifier
            .offset(x = itemWidth * position)
            .width(itemWidth)
            .height(GlassNavBarDefaults.Height - 12.dp)
            .graphicsLayer {
                scaleX = stretch
                scaleY = 1f / (1f + (stretch - 1f) * 0.4f)
            }
            .background(colors.glassSelection, shape)
            .border(1.dp, Brush.verticalGradient(listOf(colors.glassRimLight, colors.glassRimShade)), shape),
    )
}

@Composable
private fun GlassNavTab(item: GlassNavItem, selected: Boolean, onClick: () -> Unit) {
    // The lens marks the selected tab; its glyph takes the accent, while labels stay in the text
    // colour, which the glass tint keeps readable over any backdrop.
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    )
    Column(
        Modifier
            .width(GlassNavBarDefaults.ItemWidth)
            .height(GlassNavBarDefaults.Height - 12.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Text(item.glyph, color = tint, fontSize = 20.sp)
            if (item.badge > 0) {
                CountBadge(
                    item.badge.coerceAtMost(9),
                    radarColors.danger,
                    Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-4).dp),
                )
            }
        }
        Text(
            item.label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun GlassBarPreviewContent(theme: ThemeMode) {
    RadarTheme(theme) {
        val backdrop = rememberGlassBackdrop()
        Box(Modifier.size(width = 420.dp, height = 160.dp)) {
            // Something colourful behind the glass so the blur is visible.
            Box(
                Modifier
                    .fillMaxSize()
                    .glassBackdropSource(backdrop)
                    .background(
                        Brush.horizontalGradient(listOf(radarColors.danger, radarColors.favorite, radarColors.warning)),
                    ),
            )
            GlassNavigationBar(
                items = listOf(
                    GlassNavItem("Map", "◎"),
                    GlassNavItem("Favorites", "★", badge = 2),
                    GlassNavItem("Settings", "⚙"),
                ),
                selectedIndex = 0,
                onSelect = {},
                backdrop = backdrop,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Preview(name = "Glass navigation bar · dark")
@Composable
private fun GlassNavigationBarDarkPreview() = GlassBarPreviewContent(ThemeMode.Dark)

@Preview(name = "Glass navigation bar · light")
@Composable
private fun GlassNavigationBarLightPreview() = GlassBarPreviewContent(ThemeMode.Light)
