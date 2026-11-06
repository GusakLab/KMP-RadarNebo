package com.gusak.lab.radarnebo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusak.lab.radarnebo.data.FavoriteLocation
import com.gusak.lab.radarnebo.data.ThemeMode
import com.gusak.lab.radarnebo.domain.ProximityHit
import com.gusak.lab.radarnebo.geo.City
import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.geo.Regions
import com.gusak.lab.radarnebo.ui.preview.PreviewData

/**
 * Favorite places: search to add one, and each saved place with its current threat status. Shown in a
 * [GlassDialog].
 */
@Composable
internal fun FavoritesScreen(
    state: UiState,
    onAdd: (String, GeoPoint) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showCustom by remember { mutableStateOf(false) }
    val hits = remember(state.proximity) { state.proximity.associateBy { it.favorite.id } }
    val savedNames = state.favorites.map { it.name }.toSet()
    val suggestions = Regions.cities.filter {
        it.name !in savedNames && query.isNotBlank() && it.name.contains(query.trim(), ignoreCase = true)
    }

    LazyColumn(
        modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search city") },
                placeholder = { Text("e.g. Kyiv, Lviv, Odesa") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        items(suggestions, key = { "s:" + it.name }) { city ->
            CitySuggestion(city) {
                onAdd(city.name, city.point)
                query = ""
            }
        }
        item { CoordinatesTip(onAddByCoordinates = { showCustom = true }) }
        savedPlaces(state.favorites, hits, onAdd, onRemove)
    }

    if (showCustom) {
        CoordinatesDialog(
            onDismiss = { showCustom = false },
            onConfirm = { name, point ->
                onAdd(name, point)
                showCustom = false
            },
        )
    }
}

/** The saved places with their status, or a card for adding the first ones. */
private fun LazyListScope.savedPlaces(
    favorites: List<FavoriteLocation>,
    hits: Map<String, ProximityHit>,
    onAdd: (String, GeoPoint) -> Unit,
    onRemove: (String) -> Unit,
) {
    item {
        Text(
            "SAVED PLACES",
            Modifier.padding(start = 4.dp, top = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
        )
    }
    if (favorites.isEmpty()) {
        item { NoFavoritesCard(onAdd) }
    }
    items(favorites, key = { it.id }) { favorite ->
        FavoriteRow(favorite, hits[favorite.id], onRemove = { onRemove(favorite.id) })
    }
}

/** A city matching the search, added with a tap. */
@Composable
private fun CitySuggestion(city: City, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("+", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(city.name, style = MaterialTheme.typography.bodyMedium)
            Regions.oblast(city.oblastUid)?.nameEn?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CoordinatesTip(onAddByCoordinates: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Tip: long-press the map to add any point.",
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onAddByCoordinates) { Text("Add by coordinates") }
    }
}

/** What to do with no favorites yet, with the first few cities to add in a tap. */
@Composable
private fun NoFavoritesCard(onAdd: (String, GeoPoint) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f))
            .padding(16.dp),
    ) {
        Text("No favorites yet", style = MaterialTheme.typography.titleSmall)
        Text(
            "Add your home, family or work locations to be warned when a threat comes near.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text("Quick add", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Regions.cities.take(4).forEach { city ->
                TextButton(onClick = { onAdd(city.name, city.point) }) { Text(city.name) }
            }
        }
    }
}

/** A saved place, red while a threat is near ([hit]), with a button to remove it. */
@Composable
private fun FavoriteRow(favorite: FavoriteLocation, hit: ProximityHit?, onRemove: () -> Unit) {
    val statusColor = if (hit == null) radarColors.okText else radarColors.dangerText
    val idle = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (hit != null) radarColors.danger.copy(alpha = STATUS_TINT) else idle)
            .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(statusColor))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(favorite.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "${formatCoord(favorite.lat)}, ${formatCoord(favorite.lon)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (hit == null) {
                    "Clear"
                } else {
                    "${hit.threat.type.label} · " + if (hit.insideRegion) {
                        "region-wide in ${hit.threat.oblastName}"
                    } else {
                        "${hit.distanceKm.toInt()} km – ${hit.threat.title}"
                    }
                },
                color = statusColor,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}

@Composable
private fun CoordinatesDialog(onDismiss: () -> Unit, onConfirm: (String, GeoPoint) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var lat by rememberSaveable { mutableStateOf("") }
    var lon by rememberSaveable { mutableStateOf("") }
    val point = lat.replace(',', '.').toDoubleOrNull()?.let { la ->
        lon.replace(',', '.').toDoubleOrNull()?.let { lo -> GeoPoint(la, lo) }
    }?.takeIf { it.lat in 44.0..53.0 && it.lon in 22.0..41.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add by coordinates") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(lat, { lat = it }, label = { Text("Latitude (44–53)") }, singleLine = true)
                OutlinedTextField(lon, { lon = it }, label = { Text("Longitude (22–41)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { point?.let { onConfirm(name, it) } }, enabled = point != null) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Preview(name = "Favorites · dark", widthDp = 420, heightDp = 860)
@Composable
private fun FavoritesScreenDarkPreview() {
    RadarTheme(ThemeMode.Dark) { Surface { FavoritesScreen(PreviewData.state(), onAdd = { _, _ -> }, onRemove = {}) } }
}

@Preview(name = "Favorites · light", widthDp = 420, heightDp = 860)
@Composable
private fun FavoritesScreenLightPreview() {
    RadarTheme(ThemeMode.Light) {
        Surface { FavoritesScreen(PreviewData.state(ThemeMode.Light), onAdd = { _, _ -> }, onRemove = {}) }
    }
}

@Preview(name = "Favorites · empty", widthDp = 420, heightDp = 860)
@Composable
private fun FavoritesScreenEmptyPreview() {
    RadarTheme(ThemeMode.Light) { Surface { FavoritesScreen(UiState(), onAdd = { _, _ -> }, onRemove = {}) } }
}
