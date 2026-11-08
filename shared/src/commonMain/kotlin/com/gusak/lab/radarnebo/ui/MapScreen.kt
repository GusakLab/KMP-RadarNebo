package com.gusak.lab.radarnebo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusak.lab.radarnebo.data.AppSettings
import com.gusak.lab.radarnebo.data.MapMode
import com.gusak.lab.radarnebo.data.TargetFilter
import com.gusak.lab.radarnebo.data.ThemeMode
import com.gusak.lab.radarnebo.geo.GeoPoint
import com.gusak.lab.radarnebo.tracking.DroneTrack
import com.gusak.lab.radarnebo.tracking.Place
import com.gusak.lab.radarnebo.tracking.TargetClassifier
import com.gusak.lab.radarnebo.tracking.TrackInterpolator
import com.gusak.lab.radarnebo.tracking.TrackPredictor
import com.gusak.lab.radarnebo.ui.map.CinematicShot
import com.gusak.lab.radarnebo.ui.map.LocalMapTiles
import com.gusak.lab.radarnebo.ui.map.MapCamera
import com.gusak.lab.radarnebo.ui.map.MapProjection
import com.gusak.lab.radarnebo.ui.map.MapSelection
import com.gusak.lab.radarnebo.ui.map.ShotPlanner
import com.gusak.lab.radarnebo.ui.map.TileSource
import com.gusak.lab.radarnebo.ui.map.UkraineMap
import com.gusak.lab.radarnebo.ui.map.groundUnder
import com.gusak.lab.radarnebo.ui.map.rememberCinematicDirector
import com.gusak.lab.radarnebo.ui.preview.PreviewData
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun MapScreen(
    state: UiState,
    onModeChange: (MapMode) -> Unit,
    onAddFavorite: (String, GeoPoint) -> Unit,
    /** Saves a new warning radius (km) around favorites. */
    onRadiusChange: (Int) -> Unit = {},
    /** Saves which targets the Targets list and the map show. */
    onTargetFilterChange: (TargetFilter) -> Unit = {},
    /** Opens the request inspector from the server card; null hides that button. */
    onOpenRequests: (() -> Unit)? = null,
    /** Tries the server again now, from the status strip's Reconnect button while it can't be reached. */
    onReconnect: () -> Unit = {},
    /** Space taken by floating UI at the bottom (the glass navigation bar); the map still draws beneath it. */
    bottomInset: Dp = 0.dp,
    start: MapScreenStart = MapScreenStart(),
    /** Local time in epoch milliseconds; the server's is this plus the state's clock offset. */
    clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    /** A threat to select after the map is showing. Null except for a Linux notification click. */
    focus: MapFocus? = null,
) {
    val screen = rememberMapScreenState(start)
    LaunchedEffect(focus?.token) {
        val current = focus ?: return@LaunchedEffect
        val marker = state.threats.firstOrNull { it.id == current.threatId } ?: return@LaunchedEffect
        screen.select(MapSelection.Threat(marker), state)
    }
    val data = rememberMapScreenData(state, screen, bottomInset, clock)
    val actions =
        MapActions(onModeChange, onAddFavorite, onRadiusChange, onTargetFilterChange, onOpenRequests, onReconnect)
    MapScreenContent(data, screen, actions)
}

@Composable
private fun rememberMapScreenData(
    state: UiState,
    screen: MapScreenState,
    bottomInset: Dp,
    clock: () -> Long,
): MapScreenData {
    val alertedFavoriteIds = remember(state.proximity) { state.proximity.map { it.favorite.id }.toSet() }
    // Favorites first so that, at equal distance, a favorite wins over the city it sits in.
    val predictionPlaces = remember(state.favorites) {
        state.favorites.map { Place(it.name, it.point, isFavorite = true) } + TrackPredictor.cityPlaces
    }
    return MapScreenData(
        state = state,
        alertedFavoriteIds = alertedFavoriteIds,
        predictionPlaces = predictionPlaces,
        // Radius while the slider is being dragged: the rings follow it live, the setting is saved on release.
        radiusKm = screen.radiusDraft ?: state.settings.proximityRadiusKm,
        bottomInset = bottomInset,
        clock = clock,
    )
}

/** The map with everything floating over it, laid out for the screen's size. */
@Composable
private fun MapScreenContent(data: MapScreenData, screen: MapScreenState, actions: MapActions) {
    val state = data.state
    BoxWithConstraints(Modifier.fillMaxSize().background(radarColors.mapBackground)) {
        // The map credit sits below bottom panels and cards, clear of both the panel's content and the map's labels.
        val creditSpace = if (tileSource(state) != null && !data.cinematic) CreditHeight else 0.dp
        val layout = mapLayout(LocalDensity.current, data.bottomInset, creditSpace)
        val panelVisible = screen.targetsOpen && !data.cinematic
        val framer = TargetFramer(LocalDensity.current, layout, state, data.predictionPlaces, data.clock)
        FollowSelection(screen, framer, panelVisible)
        MapLayer(data, screen, panelVisible)
        MapTopBar(data, screen, actions, showProximity = !(panelVisible && !layout.wide))
        TargetsPanelLayer(data, screen, actions, layout, panelVisible)
        SelectionLayer(data, screen, layout, panelVisible)
        RadiusLayer(data, screen, actions, layout)
        // Kept off the cinematic view, which is framed as a clean camera shot.
        tileSource(state)?.takeIf { !data.cinematic }?.let { MapCredit(it, data.bottomInset) }
        AddFavoriteLayer(data, screen, actions, layout)
    }
}

/** What the map screen draws, derived from the state it was given. */
private class MapScreenData(
    val state: UiState,
    val alertedFavoriteIds: Set<String>,
    val predictionPlaces: List<Place>,
    val radiusKm: Int,
    val bottomInset: Dp,
    val clock: () -> Long,
) {
    val cinematic: Boolean get() = state.settings.mapMode == MapMode.Cinematic
}

/** What the map screen tells its caller. */
private class MapActions(
    val onModeChange: (MapMode) -> Unit,
    val onAddFavorite: (String, GeoPoint) -> Unit,
    val onRadiusChange: (Int) -> Unit,
    val onTargetFilterChange: (TargetFilter) -> Unit,
    val onOpenRequests: (() -> Unit)?,
    val onReconnect: () -> Unit,
)

/** The map screen's own UI state: what is selected, open and pending, and the static map's camera. */
@Stable
private class MapScreenState(
    selection: MutableState<MapSelection?>,
    pendingFavorite: MutableState<GeoPoint?>,
    targetsOpen: MutableState<Boolean>,
    radiusOpen: MutableState<Boolean>,
    radiusDraft: MutableState<Int?>,
    selectedTrackId: MutableState<String?>,
    selectedThreatId: MutableState<String?>,
    staticCamera: MutableState<MapCamera>,
    serverInfoOpen: MutableState<Boolean>,
    hoveredTrackId: MutableState<String?>,
    cameraBeforeHover: MutableState<MapCamera?>,
    followingSelection: MutableState<Boolean>,
) {
    var selection by selection
    var pendingFavorite by pendingFavorite
    val lastPendingFavorite = LastSpot()
    var targetsOpen by targetsOpen
    var radiusOpen by radiusOpen
    var radiusDraft by radiusDraft
    var selectedTrackId by selectedTrackId

    // A threat picked on the map that no track follows; the targets panel shows it like a target.
    var selectedThreatId by selectedThreatId
    var staticCamera by staticCamera
    var serverInfoOpen by serverInfoOpen

    // Hovering a row in the targets list previews that target; the map returns when the pointer leaves.
    var hoveredTrackId by hoveredTrackId
    var cameraBeforeHover by cameraBeforeHover

    // The camera keeps the selected target in view as it moves, until the user pans the map.
    var followingSelection by followingSelection

    fun closeRadius() {
        radiusOpen = false
        radiusDraft = null
    }

    fun select(picked: MapSelection?, state: UiState) {
        // Threats open in the targets panel too: on their track when one follows them, on their own otherwise.
        val trackId = when (picked) {
            is MapSelection.Track -> picked.track.id
            is MapSelection.Threat -> state.allTracks.firstOrNull { picked.marker.id in it.markerIds }?.id
            else -> null
        }
        if (picked is MapSelection.Track || picked is MapSelection.Threat) {
            selectedTrackId = trackId
            selectedThreatId = if (trackId == null) (picked as MapSelection.Threat).marker.id else null
            targetsOpen = true
            selection = null
        } else {
            selection = picked
            if (picked != null) {
                targetsOpen = false
                closeRadius()
            }
        }
    }

    /** Flies the static map to [target]; with [cut], jumps there instead. */
    suspend fun flyTo(target: MapCamera, cut: Boolean) {
        // Previews render only the first frames: cut straight to the framing instead of flying there.
        if (cut) {
            staticCamera = target
            return
        }
        val from = staticCamera
        animate(0f, 1f, animationSpec = tween(700, easing = FastOutSlowInEasing)) { f, _ ->
            staticCamera = MapCamera(lerp(from.center, target.center, f), from.zoom + (target.zoom - from.zoom) * f)
        }
    }
}

@Composable
private fun rememberMapScreenState(start: MapScreenStart): MapScreenState {
    val selection = remember { mutableStateOf(start.selection) }
    val pendingFavorite = remember { mutableStateOf(start.pendingFavorite) }
    val targetsOpen = rememberSaveable { mutableStateOf(start.targetsOpen) }
    val radiusOpen = rememberSaveable { mutableStateOf(start.radiusOpen) }
    val radiusDraft = remember { mutableStateOf<Int?>(null) }
    val selectedTrackId = rememberSaveable { mutableStateOf(start.selectedTrackId) }
    val selectedThreatId = rememberSaveable { mutableStateOf(start.selectedThreatId) }
    val staticCamera = remember { mutableStateOf(start.camera) }
    val serverInfoOpen = rememberSaveable { mutableStateOf(false) }
    val hoveredTrackId = remember { mutableStateOf<String?>(null) }
    val cameraBeforeHover = remember { mutableStateOf<MapCamera?>(null) }
    val followingSelection = remember { mutableStateOf(false) }
    return remember {
        MapScreenState(
            selection, pendingFavorite, targetsOpen, radiusOpen, radiusDraft, selectedTrackId, selectedThreatId,
            staticCamera, serverInfoOpen, hoveredTrackId, cameraBeforeHover, followingSelection,
        )
    }
}

/** Where things go on a map screen of the given size. */
private class MapLayout(
    val wide: Boolean,
    val panelHeight: Dp,
    /** Room kept under bottom panels and cards for the map credit, when there is one. */
    val creditSpace: Dp,
    val viewport: Size,
    /** Part of the map the targets panel leaves visible, where a selected target should be framed. */
    val visibleArea: Rect,
)

private fun BoxWithConstraintsScope.mapLayout(density: Density, bottomInset: Dp, creditSpace: Dp): MapLayout {
    val wide = maxWidth >= 720.dp
    val panelHeight = minOf(maxHeight * 0.5f, 460.dp)
    val visibleArea = with(density) {
        if (wide) {
            Rect(0f, 120.dp.toPx(), (maxWidth - PanelWidth - 24.dp).toPx(), (maxHeight - bottomInset).toPx())
        } else {
            val bottom = maxHeight - bottomInset - creditSpace - panelHeight - 12.dp
            Rect(0f, 120.dp.toPx(), maxWidth.toPx(), bottom.toPx())
        }
    }
    val viewport = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
    return MapLayout(wide, panelHeight, creditSpace, viewport, visibleArea)
}

/** Cameras that frame targets in the part of the map [layout] leaves visible, as of [state]. */
private class TargetFramer(
    private val density: Density,
    private val layout: MapLayout,
    private val state: UiState,
    private val predictionPlaces: List<Place>,
    private val clock: () -> Long,
) {
    // Targets fly above the map and the camera's perspective pushes them out from the centre of the view:
    // [visible] moved to where the target's ground spot must be for the target itself to show in its middle.
    private fun airFrame(track: DroneTrack, viewport: Size, visible: Rect): Rect {
        val altitude = TargetClassifier.classify(track).kind.typicalAltitudeM
        return visible.translate(with(density) { groundUnder(visible.center, altitude, viewport) } - visible.center)
    }

    // Frames a target's whole path and predicted destination (or a lone threat) in the part of the map left visible.
    fun framing(trackId: String?, threatId: String? = null): MapCamera? {
        val viewport = layout.viewport
        if (viewport.width <= 0f) return null
        val track = state.tracks.firstOrNull { it.id == trackId }
        if (track == null) {
            val threat = state.threats.firstOrNull { it.id == threatId } ?: return null
            return MapCamera.framing(listOf(threat.point), viewport, layout.visibleArea)
        }
        val now = clock() + state.clockOffsetMs
        val position = TrackInterpolator.positionAt(track, now)?.point
        val points = track.samples.map { it.point } + listOfNotNull(position) +
            listOfNotNull(TrackPredictor.predict(track, now, predictionPlaces)?.place?.point)
        return MapCamera.framing(points, viewport, airFrame(track, viewport, layout.visibleArea), focus = position)
    }

    // Camera at [zoom] with the selected target, where it is now, in the middle of the visible part of the map.
    fun following(trackId: String?, zoom: Float): MapCamera? {
        val track = state.tracks.firstOrNull { it.id == trackId } ?: return null
        val now = clock() + state.clockOffsetMs
        val position = TrackInterpolator.positionAt(track, now) ?: return null
        val frame = airFrame(track, layout.viewport, layout.visibleArea)
        return MapCamera.centeredOn(MapProjection.toWorld(position.point), zoom, layout.viewport, frame)
    }
}

/**
 * Flies the static map to the selected target, then keeps it in the middle of the visible part of the map as it
 * moves; previews the hovered one. [framer] frames as of the moment each starts; following uses the latest.
 */
@Composable
private fun FollowSelection(screen: MapScreenState, framer: TargetFramer, panelVisible: Boolean) {
    val latest by rememberUpdatedState(framer)
    val inspectionMode = LocalInspectionMode.current

    // Fly the static map to the selected target, then keep it in the middle of the visible part of
    // the map as it moves, until the user pans away. Hover previews take the camera meanwhile.
    LaunchedEffect(screen.selectedTrackId, screen.selectedThreatId, panelVisible) {
        screen.followingSelection = false
        if (!panelVisible) return@LaunchedEffect
        framer.framing(screen.selectedTrackId, screen.selectedThreatId)?.let { screen.flyTo(it, cut = inspectionMode) }
        if (screen.selectedTrackId == null || inspectionMode) return@LaunchedEffect
        screen.followingSelection = true
        while (screen.followingSelection) {
            withFrameMillis { }
            if (screen.hoveredTrackId != null || screen.cameraBeforeHover != null) continue
            latest.following(screen.selectedTrackId, screen.staticCamera.zoom)?.let { screen.staticCamera = it }
        }
    }

    // Preview the hovered target, and fly back once the pointer leaves the list. The short wait keeps
    // the map still while the pointer merely crosses rows.
    LaunchedEffect(screen.hoveredTrackId, panelVisible) {
        val hovered = screen.hoveredTrackId.takeIf { panelVisible }
        delay(if (hovered != null) 200 else 300)
        val target = framer.framing(hovered)
        if (target != null) {
            if (screen.cameraBeforeHover == null) screen.cameraBeforeHover = screen.staticCamera
            screen.flyTo(target, cut = inspectionMode)
        } else {
            screen.cameraBeforeHover?.let { before ->
                // A followed target has moved on meanwhile: return to where it is now.
                val now = if (screen.followingSelection) latest.following(screen.selectedTrackId, before.zoom) else null
                screen.flyTo(now ?: before, cut = inspectionMode)
                screen.cameraBeforeHover = null
            }
        }
    }
}

/** The static map, or the cinematic one, fading between them. */
@Composable
private fun MapLayer(data: MapScreenData, screen: MapScreenState, panelVisible: Boolean) {
    val state = data.state
    val onSelect: (MapSelection?) -> Unit = { screen.select(it, state) }
    Crossfade(state.settings.mapMode) { mode ->
        when (mode) {
            MapMode.Static -> StaticMap(data, screen, panelVisible, onSelect)
            MapMode.Cinematic -> CinematicMap(data, screen.pendingFavorite, onSelect) {
                screen.selection = null
                screen.pendingFavorite = it
            }
        }
    }
}

@Composable
private fun StaticMap(
    data: MapScreenData,
    screen: MapScreenState,
    panelVisible: Boolean,
    onSelect: (MapSelection?) -> Unit,
) {
    val state = data.state
    UkraineMap(
        tileSource = tileSource(state),
        camera = screen.staticCamera,
        threats = state.mapThreats,
        alertedOblasts = state.alertedOblasts,
        favorites = state.favorites,
        alertedFavoriteIds = data.alertedFavoriteIds,
        proximityRadiusKm = data.radiusKm,
        modifier = Modifier.fillMaxSize(),
        selectedTrackId = (screen.selectedTrackId ?: screen.hoveredTrackId).takeIf { panelVisible },
        highlightedThreatId = screen.selectedThreatId.takeIf { panelVisible },
        predictionPlaces = data.predictionPlaces,
        tracks = state.tracks,
        clockOffsetMs = state.clockOffsetMs,
        onCameraChange = {
            // Panning lets go of the followed target; zooming keeps it in the middle.
            if (it.zoom == screen.staticCamera.zoom) screen.followingSelection = false
            screen.staticCamera = it
        },
        onSelect = onSelect,
        pendingFavorite = screen.pendingFavorite,
        onLongPress = {
            screen.selection = null
            screen.closeRadius()
            screen.pendingFavorite = it
        },
        clock = data.clock,
    )
}

/** The status strip, which expands into server details, on the left; the map's buttons on the right. */
@Composable
private fun MapTopBar(data: MapScreenData, screen: MapScreenState, actions: MapActions, showProximity: Boolean) {
    val state = data.state
    // Safe drawing rather than the status bar alone: in landscape the notch and system bars are at the sides.
    val safeTopAndSides = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    Row(
        Modifier.windowInsetsPadding(safeTopAndSides).padding(12.dp).fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            StatusStrip(
                state,
                expanded = screen.serverInfoOpen,
                onClick = { screen.serverInfoOpen = !screen.serverInfoOpen },
                actions = actions,
                clock = data.clock,
            )
            if (!data.cinematic && state.proximity.isNotEmpty() && showProximity) {
                Spacer(Modifier.height(8.dp))
                ProximityBanner(state)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MapButtonColumn(data, screen, actions)
        }
    }
}

@Composable
private fun ColumnScope.MapButtonColumn(data: MapScreenData, screen: MapScreenState, actions: MapActions) {
    val cinematic = data.cinematic
    CinematicToggle(
        cinematic = cinematic,
        onToggle = { on -> actions.onModeChange(if (on) MapMode.Cinematic else MapMode.Static) },
    )
    AnimatedVisibility(visible = !cinematic, enter = fadeIn(), exit = fadeOut()) {
        TargetsButton(
            open = screen.targetsOpen,
            activeCount = data.state.tracks.count { it.active },
            onClick = {
                screen.targetsOpen = !screen.targetsOpen
                if (screen.targetsOpen) {
                    screen.selection = null
                    screen.closeRadius()
                }
            },
        )
    }
    AnimatedVisibility(visible = !cinematic, enter = fadeIn(), exit = fadeOut()) {
        RadiusButton(
            open = screen.radiusOpen,
            radiusKm = data.radiusKm,
            onClick = {
                if (screen.radiusOpen) {
                    screen.closeRadius()
                } else {
                    screen.radiusOpen = true
                    screen.selection = null
                    screen.pendingFavorite = null
                    screen.targetsOpen = false
                }
            },
        )
    }
    val onOpenRequests = actions.onOpenRequests
    if (onOpenRequests != null) {
        AnimatedVisibility(visible = !cinematic, enter = fadeIn(), exit = fadeOut()) {
            // Below the radius button's hanging "km" label.
            RequestsButton(onClick = onOpenRequests, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun BoxScope.TargetsPanelLayer(
    data: MapScreenData,
    screen: MapScreenState,
    actions: MapActions,
    layout: MapLayout,
    panelVisible: Boolean,
) {
    val state = data.state
    AnimatedVisibility(
        visible = panelVisible,
        enter = fadeIn() + slideInVertically { it / 8 },
        exit = fadeOut() + slideOutVertically { it / 8 },
        modifier = panelPlacement(layout, data.bottomInset),
    ) {
        TargetsPanel(
            tracks = state.allTracks,
            clockOffsetMs = state.clockOffsetMs,
            places = data.predictionPlaces,
            selectedTrackId = screen.selectedTrackId,
            onSelect = {
                // Choosing a target keeps the map on it instead of returning from the hover preview.
                screen.selectedTrackId = it
                screen.selectedThreatId = null
                screen.hoveredTrackId = null
                screen.cameraBeforeHover = null
            },
            onClose = { screen.targetsOpen = false },
            clock = data.clock,
            modifier = Modifier.fillMaxSize(),
            filter = state.settings.targetFilter,
            onFilterChange = actions.onTargetFilterChange,
            onHover = { id, hovered ->
                if (hovered) {
                    screen.hoveredTrackId = id
                } else if (screen.hoveredTrackId == id) {
                    screen.hoveredTrackId = null
                }
            },
            selectedThreat = state.threats.firstOrNull { it.id == screen.selectedThreatId },
        )
    }
}

/** Where the targets panel goes: a column at the right on wide screens, a sheet at the bottom on narrow ones. */
private fun BoxScope.panelPlacement(layout: MapLayout, bottomInset: Dp): Modifier = if (layout.wide) {
    Modifier
        .align(Alignment.TopEnd)
        .statusBarsPadding()
        .padding(top = 128.dp, end = 12.dp, bottom = bottomInset + 12.dp)
        .width(PanelWidth)
} else {
    Modifier
        .align(Alignment.BottomCenter)
        .padding(start = 12.dp, end = 12.dp, bottom = bottomInset + layout.creditSpace + 12.dp)
        .fillMaxWidth()
        .height(layout.panelHeight)
}

/** Space a bottom card keeps free below itself: the navigation bar and the map credit. */
private fun bottomCardPadding(data: MapScreenData, layout: MapLayout): Modifier =
    Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp + data.bottomInset + layout.creditSpace)

@Composable
private fun BoxScope.SelectionLayer(
    data: MapScreenData,
    screen: MapScreenState,
    layout: MapLayout,
    panelVisible: Boolean,
) {
    AnimatedVisibility(
        visible = screen.selection != null && !panelVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter).then(bottomCardPadding(data, layout)),
    ) {
        screen.selection?.let { SelectionCard(it, data.state, onDismiss = { screen.selection = null }) }
    }
}

@Composable
private fun BoxScope.RadiusLayer(data: MapScreenData, screen: MapScreenState, actions: MapActions, layout: MapLayout) {
    AnimatedVisibility(
        visible = screen.radiusOpen && !data.cinematic,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = Modifier.align(Alignment.BottomCenter).then(bottomCardPadding(data, layout)).widthIn(max = 480.dp),
    ) {
        RadiusCard(
            radiusKm = data.radiusKm,
            favoriteCount = data.state.favorites.size,
            onPreview = { screen.radiusDraft = it },
            onCommit = { screen.radiusDraft?.let(actions.onRadiusChange) },
            onClose = screen::closeRadius,
        )
    }
}

/** The tile set's credit, which its terms require, in the bottom corner. */
@Composable
private fun BoxScope.MapCredit(source: TileSource, bottomInset: Dp) {
    Text(
        source.attribution,
        Modifier.align(Alignment.BottomStart)
            .padding(start = 8.dp, bottom = bottomInset + 4.dp)
            .background(radarColors.overlay, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
        color = radarColors.onOverlayMuted,
        fontSize = 9.sp,
    )
}

@Composable
private fun BoxScope.AddFavoriteLayer(
    data: MapScreenData,
    screen: MapScreenState,
    actions: MapActions,
    layout: MapLayout,
) {
    // Long-press asks to add the spot as a favorite. A card rather than a dialog keeps the map
    // and the pin on the pressed spot in view. It keeps its last spot while sliding away.
    screen.pendingFavorite?.let { screen.lastPendingFavorite.point = it }
    AnimatedVisibility(
        visible = screen.pendingFavorite != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = Modifier.align(Alignment.BottomCenter).imePadding().then(bottomCardPadding(data, layout))
            .widthIn(max = 480.dp),
    ) {
        screen.lastPendingFavorite.point?.let { point ->
            AddFavoriteCard(
                point = point,
                proximityRadiusKm = data.radiusKm,
                onConfirm = { name ->
                    actions.onAddFavorite(name, point)
                    screen.pendingFavorite = null
                },
                onDismiss = { screen.pendingFavorite = null },
            )
        }
    }
}

private val PanelWidth = 380.dp

/** Room kept under a bottom panel for the map credit. */
private val CreditHeight = 18.dp

/** The tiles the map draws under the oblasts, when the platform provides them and the settings ask for a map. */
@Composable
private fun tileSource(state: UiState): TileSource? =
    if (LocalMapTiles.current == null) null else TileSource.of(state.settings.mapBackground, radarColors.isDark)

@Composable
private fun CinematicMap(
    data: MapScreenData,
    pendingFavorite: GeoPoint?,
    onSelect: (MapSelection?) -> Unit,
    onLongPress: (GeoPoint) -> Unit,
) {
    val state = data.state
    val shots = remember(state.proximity, state.favorites, state.mapThreats, state.settings.proximityRadiusKm) {
        ShotPlanner.plan(state.proximity, state.favorites, state.mapThreats, state.settings.proximityRadiusKm)
    }
    val director = rememberCinematicDirector(shots)
    val shot = director.currentShot
    val danger = shot?.kind == CinematicShot.Kind.ThreatNearFavorite
    val letterbox by animateDpAsState(if (danger) 64.dp else 44.dp)

    Box(Modifier.fillMaxSize()) {
        UkraineMap(
            tileSource = tileSource(state),
            camera = director.camera,
            threats = state.mapThreats,
            alertedOblasts = state.alertedOblasts,
            favorites = state.favorites,
            alertedFavoriteIds = data.alertedFavoriteIds,
            proximityRadiusKm = state.settings.proximityRadiusKm,
            modifier = Modifier.fillMaxSize(),
            highlightedThreatId = shot?.threatId,
            predictionPlaces = data.predictionPlaces,
            tracks = state.tracks,
            clockOffsetMs = state.clockOffsetMs,
            pendingFavorite = pendingFavorite,
            onSelect = onSelect,
            onLongPress = onLongPress,
            clock = data.clock,
        )
        Letterbox(letterbox, data.bottomInset, danger)
        if (shot != null) {
            val caption = ShotCaption(shot, danger, "SHOT ${director.shotIndex + 1}/${director.shotCount}", state)
            ShotCaptionCard(caption, Modifier.align(Alignment.BottomStart), bottom = letterbox + data.bottomInset)
        }
    }
}

/** Letterbox bars [height] tall, over [bottomInset] at the bottom, and a red frame while in [danger]. */
@Composable
private fun BoxScope.Letterbox(height: Dp, bottomInset: Dp, danger: Boolean) {
    // Letterbox bars and a vignette frame sell the "camera" feel; they stay black in both themes.
    // The bottom bar extends under the floating navigation bar.
    Box(
        Modifier.align(Alignment.TopCenter).fillMaxWidth().height(height + 40.dp)
            .background(Color.Black.copy(alpha = 0.55f)),
    )
    Box(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(height + bottomInset)
            .background(Color.Black.copy(alpha = 0.85f)),
    )
    if (danger) {
        Box(Modifier.fillMaxSize().border(3.dp, radarColors.danger.copy(alpha = 0.8f)))
    }
}

/** The caption under a cinematic shot: its title and subtitle, which shot it is and how many alerts there are. */
private class ShotCaption(val shot: CinematicShot, val danger: Boolean, val counter: String, state: UiState) {
    val status: String = if (state.proximity.isEmpty()) "PATROL" else "${state.proximity.size} POI ALERT(S)"
}

@Composable
private fun ShotCaptionCard(caption: ShotCaption, modifier: Modifier, bottom: Dp) {
    Column(
        modifier
            .padding(start = 16.dp, end = 16.dp, bottom = bottom + 12.dp)
            .background(radarColors.overlay, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            caption.shot.title,
            color = if (caption.danger) radarColors.dangerText else radarColors.okText,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Text(caption.shot.subtitle, color = radarColors.onOverlay, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        Text(
            "${caption.counter} · ${caption.status}",
            color = radarColors.onOverlayMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
    }
}

/**
 * The data summary and the server's state; tapping it shows more about the server in place. Raw errors and the
 * request inspector are for debug builds only (those with [MapActions.onOpenRequests]): network errors name the
 * server's address.
 */
@Composable
private fun StatusStrip(
    state: UiState,
    expanded: Boolean,
    onClick: () -> Unit,
    actions: MapActions,
    clock: () -> Long,
) {
    val server = ServerState.of(state)
    val debug = actions.onOpenRequests != null
    val text = buildString {
        if (state.simulated) append("SIMULATED DATA · not the real situation · ")
        append("${state.threats.size} ${if (state.settings.dronesOnly) "drone threats" else "threats"}")
        if (state.threats.size > state.mapThreats.size) append(" (showing ${state.mapThreats.size})")
        state.lastUpdated?.let { append(" · updated ${formatTime(it)}") }
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(radarColors.overlay)
            .clickable(onClickLabel = if (expanded) "Hide server details" else "Show server details", onClick = onClick)
            .animateContentSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        // Level with the first line of text, which the dot stays beside while the strip expands.
        Box(Modifier.padding(top = 4.dp).size(8.dp).background(server.color(), CircleShape))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(text, color = radarColors.onOverlay, fontSize = 12.sp)
            Text(server.label, color = server.color(), fontSize = 11.sp)
            if (debug) state.error?.let { Text(it, color = radarColors.dangerText, fontSize = 12.sp) }
            if (server == ServerState.Offline) ReconnectRow(state, actions.onReconnect, clock)
            if (expanded) ServerDetails(state, server, actions.onOpenRequests, clock)
        }
    }
}

/** What the expanded strip adds: where the data comes from, how fresh it is and how often it is refreshed. */
@Composable
private fun ServerDetails(state: UiState, server: ServerState, onOpenRequests: (() -> Unit)?, clock: () -> Long) {
    Column(Modifier.padding(top = 6.dp).widthIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        state.serverHealth?.let { health ->
            StatusLine(
                "Source",
                if (health.simulated) "SIMULATED" else "LIVE",
                if (health.simulated) radarColors.warningText else radarColors.okText,
            )
        }
        StatusLine("Updated", state.lastUpdated?.let { rememberAgo(it, clock) } ?: "not yet")
        StatusLine("Refresh", "every ${state.settings.pollSeconds} s, while the app is open")
        if (server == ServerState.Offline) {
            Text(
                if (onOpenRequests != null) {
                    "Is the server running? Start it with ./gradlew :mission-control:server:run, or change its " +
                        "address in Settings."
                } else {
                    "Check your internet connection. The app keeps trying."
                },
                color = radarColors.onOverlayMuted,
                fontSize = 11.sp,
            )
        }
        if (onOpenRequests != null) {
            StripButton("Show requests", Modifier.padding(top = 2.dp), onClick = onOpenRequests)
        }
    }
}

/** Counts down to the next attempt at an unreachable server, with a button to try right away. */
@Composable
private fun ReconnectRow(state: UiState, onReconnect: () -> Unit, clock: () -> Long) {
    val retryAt = state.nextRetryAt
    var now by remember { mutableStateOf(Instant.fromEpochMilliseconds(clock())) }
    LaunchedEffect(retryAt) {
        while (true) {
            now = Instant.fromEpochMilliseconds(clock())
            delay(250)
        }
    }
    val secondsLeft = retryAt?.let { ((it - now).inWholeMilliseconds + 999) / 1000 }?.coerceAtLeast(0)
    val trying = state.loading || secondsLeft == 0L
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            when {
                trying -> "Reconnecting…"
                secondsLeft != null -> "Retrying in ${secondsLeft}s"
                else -> "Not connected"
            },
            color = radarColors.onOverlayMuted,
            fontSize = 11.sp,
        )
        Spacer(Modifier.width(10.dp))
        StripButton("Reconnect", enabled = !trying, onClick = onReconnect)
    }
}

/** A small outlined button sized for the status strip's text. */
@Composable
private fun StripButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.4f)
    Text(
        label,
        modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

/** The app's view of its server, shown by the status strip's dot and label. */
private enum class ServerState(val label: String) {
    Connecting("connecting…"),
    Live("server live"),
    /** The server answers but cannot refresh its own data; it keeps serving the last data. */
    Degraded("server up, data may be stale"),
    Offline("no data from the server");

    @Composable
    fun color() = when (this) {
        Connecting -> radarColors.onOverlayMuted
        Live -> radarColors.okText
        Degraded -> radarColors.warningText
        Offline -> radarColors.dangerText
    }

    companion object {
        fun of(state: UiState) = when {
            state.error != null -> Offline
            state.serverHealth?.isOk == false -> Degraded
            state.lastUpdated == null -> Connecting
            else -> Live
        }
    }
}

/** How long ago [instant] was, to the second ("4 s ago", "2 min 5 s ago"), ticking while shown. */
@Composable
private fun rememberAgo(instant: Instant, clock: () -> Long): String {
    var now by remember { mutableStateOf(Instant.fromEpochMilliseconds(clock())) }
    LaunchedEffect(instant) {
        while (true) {
            now = Instant.fromEpochMilliseconds(clock())
            delay(1_000)
        }
    }
    val seconds = (now - instant).inWholeSeconds.coerceAtLeast(0)
    return when {
        seconds < 60 -> "$seconds s ago"
        seconds < 3_600 -> "${seconds / 60} min ${seconds % 60} s ago"
        else -> "${seconds / 3_600} h ${seconds % 3_600 / 60} min ago"
    }
}

/** A labelled value in the expanded status strip. */
@Composable
private fun StatusLine(label: String, value: String, valueColor: Color = radarColors.onOverlay) {
    Row {
        Text(label, Modifier.width(64.dp), color = radarColors.onOverlayMuted, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 11.sp)
    }
}

@Composable
private fun ProximityBanner(state: UiState) {
    // Darker than the alert red, so white text on it stays readable in every palette.
    Card(colors = CardDefaults.cardColors(containerColor = lerp(radarColors.danger, Color.Black, 0.35f))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Threats near your locations", fontWeight = FontWeight.Bold, color = Color.White)
            state.proximity.take(3).forEach { hit ->
                Text(
                    "${hit.favorite.name}: ${hit.threat.type.label} – " +
                        if (hit.insideRegion) "region-wide alert" else "${hit.distanceKm.toInt()} km",
                    color = Color.White,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun SelectionCard(selection: MapSelection, state: UiState, onDismiss: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(14.dp)) {
            when (selection) {
                // Targets and threats open in the targets panel instead of this card.
                is MapSelection.Track, is MapSelection.Threat -> Unit
                is MapSelection.Favorite -> {
                    val f = selection.favorite
                    val hit = state.proximity.firstOrNull { it.favorite.id == f.id }
                    Text(f.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (hit == null) {
                            "Clear within ${state.settings.proximityRadiusKm} km"
                        } else {
                            "${hit.threat.type.label} – " +
                                if (hit.insideRegion) "region-wide" else "${hit.distanceKm.toInt()} km away"
                        },
                        color = if (hit == null) radarColors.okText else radarColors.dangerText,
                    )
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
        }
    }
}

private val RadiusPresetsKm = listOf(10, 25, 50, 100)

/**
 * Sets how close a threat must come to a favorite to raise a warning. [onPreview] follows the slider
 * so the rings on the map resize live; [onCommit] saves once the drag ends or a preset is picked.
 */
@Composable
private fun RadiusCard(
    radiusKm: Int,
    favoriteCount: Int,
    onPreview: (Int) -> Unit,
    onCommit: () -> Unit,
    onClose: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 6.dp, end = 8.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Warning radius", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "$radiusKm km",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = onClose) { Text("Done") }
            }
            Column(Modifier.padding(end = 8.dp)) {
                Text(
                    when (favoriteCount) {
                        0 -> "Threats this close to a favorite place raise a warning. Long-press the map to add one."
                        1 -> "You're warned when a threat comes this close to your favorite place."
                        else -> "You're warned when a threat comes this close to any of your $favoriteCount " +
                            "favorite places."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RadiusSlider(radiusKm, onPreview, onCommit)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadiusPresetsKm.forEach { km ->
                        FilterChip(
                            selected = km == radiusKm,
                            onClick = {
                                onPreview(km)
                                onCommit()
                            },
                            label = { Text("$km km") },
                        )
                    }
                }
            }
        }
    }
}

/** The radius on a slider that reports every step ([onPreview]) and the release ([onCommit]). */
@Composable
private fun RadiusSlider(radiusKm: Int, onPreview: (Int) -> Unit, onCommit: () -> Unit) {
    val range = AppSettings.RADIUS_RANGE.first.toFloat()..AppSettings.RADIUS_RANGE.last.toFloat()
    val slider = remember(range) { SliderState(radiusKm.toFloat(), steps = 0, range) }
    // The caller owns the value: the slider shows it and reports moves instead of keeping them.
    slider.value = radiusKm.toFloat()
    Slider(slider, onValueChange = { onPreview(it.roundToInt()) }, onValueChangeFinished = onCommit)
}

/** Plain holder (not snapshot state) for the spot a closing card keeps showing. */
private class LastSpot(var point: GeoPoint? = null)

/** Asks whether to add a long-pressed spot to favorites, suggesting a name from the nearest city. */
@Composable
private fun AddFavoriteCard(
    point: GeoPoint,
    proximityRadiusKm: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val place = remember(point) { describePlace(point) }
    // The suggestion starts selected, so typing replaces it and Enter accepts it.
    var name by rememberSaveable(point, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(place.suggestedName, TextRange(0, place.suggestedName.length)))
    }
    val canAdd = name.text.isNotBlank()
    val confirm = { if (canAdd) onConfirm(name.text) }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PlaceDescription(point, place)
            Spacer(Modifier.height(8.dp))
            FavoriteNameField(point, name, onNameChange = { name = it }, onConfirm = confirm, onDismiss = onDismiss)
            Text(
                "You'll be warned when a threat comes within $proximityRadiusKm km.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.align(Alignment.End)) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = confirm, enabled = canAdd) { Text("Add to favorites") }
            }
        }
    }
}

/** What and where the long-pressed spot is, and a warning when it is outside Ukraine. */
@Composable
private fun PlaceDescription(point: GeoPoint, place: PlaceSummary) {
    Text("Add this place to favorites?", style = MaterialTheme.typography.titleMedium)
    Text(place.where, style = MaterialTheme.typography.bodyMedium)
    Text(
        "${formatCoord(point.lat)}, ${formatCoord(point.lon)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (!place.inUkraine) {
        Text(
            "Outside Ukraine: no alerts are tracked here.",
            style = MaterialTheme.typography.bodySmall,
            color = LocalRadarColors.current.warning,
        )
    }
}

/** The new favorite's name, focused as the card opens; Enter adds it and Escape cancels. */
@Composable
private fun FavoriteNameField(
    point: GeoPoint,
    name: TextFieldValue,
    onNameChange: (TextFieldValue) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(point) { focus.requestFocus() }
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Name") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onConfirm() }),
        modifier = Modifier.fillMaxWidth().focusRequester(focus).onPreviewKeyEvent { event ->
            // Desktop: Enter adds, like the Done key on a phone keyboard; Escape cancels.
            val down = event.type == KeyEventType.KeyDown
            when (event.key) {
                Key.Enter, Key.NumPadEnter -> {
                    if (down) onConfirm()
                    true
                }
                Key.Escape -> {
                    if (down) onDismiss()
                    true
                }
                else -> false
            }
        },
    )
}

@Composable
private fun MapScreenPreview(state: UiState, theme: ThemeMode = ThemeMode.Dark) {
    RadarTheme(theme) {
        MapScreen(state, onModeChange = {}, onAddFavorite = { _, _ -> }, clock = PreviewData::nowMs)
    }
}

@Preview(name = "Map · static · dark", widthDp = 420, heightDp = 860)
@Composable
private fun MapScreenStaticDarkPreview() = MapScreenPreview(PreviewData.state())

@Preview(name = "Map · static · light", widthDp = 420, heightDp = 860)
@Composable
private fun MapScreenStaticLightPreview() = MapScreenPreview(PreviewData.state(ThemeMode.Light), ThemeMode.Light)

@Preview(name = "Map · cinematic", widthDp = 420, heightDp = 860)
@Composable
private fun MapScreenCinematicPreview() = MapScreenPreview(PreviewData.state(mapMode = MapMode.Cinematic))

@Preview(name = "Map · all threat types", widthDp = 420, heightDp = 860)
@Composable
private fun MapScreenAllTypesPreview() = MapScreenPreview(PreviewData.state(dronesOnly = false))

@Preview(name = "Map · tablet", widthDp = 1100, heightDp = 760)
@Composable
private fun MapScreenTabletPreview() = MapScreenPreview(PreviewData.state())

@Preview(name = "Radius card · dark", widthDp = 420)
@Composable
private fun RadiusCardDarkPreview() {
    RadarTheme(ThemeMode.Dark) {
        RadiusCard(radiusKm = 50, favoriteCount = 2, onPreview = {}, onCommit = {}, onClose = {})
    }
}

@Preview(name = "Radius card · light, no favorites", widthDp = 420)
@Composable
private fun RadiusCardLightPreview() {
    RadarTheme(ThemeMode.Light) {
        RadiusCard(radiusKm = 120, favoriteCount = 0, onPreview = {}, onCommit = {}, onClose = {})
    }
}

@Preview(name = "Add favorite card", widthDp = 420)
@Composable
private fun AddFavoriteCardPreview() = PreviewAddFavoriteCard(ThemeMode.Light, GeoPoint(48.86, 30.32))

@Preview(name = "Add favorite card · dark, in a city", widthDp = 420)
@Composable
private fun AddFavoriteCardInCityPreview() = PreviewAddFavoriteCard(ThemeMode.Dark, GeoPoint(50.45, 30.52))

@Preview(name = "Add favorite card · outside Ukraine", widthDp = 420)
@Composable
private fun AddFavoriteCardOutsidePreview() = PreviewAddFavoriteCard(ThemeMode.Dark, GeoPoint(45.2, 31.6))

@Composable
private fun PreviewAddFavoriteCard(theme: ThemeMode, point: GeoPoint) {
    RadarTheme(theme) { AddFavoriteCard(point, proximityRadiusKm = 20, onConfirm = {}, onDismiss = {}) }
}
