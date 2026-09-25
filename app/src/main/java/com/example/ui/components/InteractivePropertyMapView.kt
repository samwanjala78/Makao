package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.LocationService
import com.example.data.location.UserLocation
import com.example.data.model.ListingType
import com.example.data.model.PresetLocation
import com.example.data.model.Property
import com.example.ui.theme.KenyaForestGreen
import com.example.ui.theme.KenyaGoldAccent
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class MapThemeMode(val label: String) {
    STREET("Streets"),
    SATELLITE("Satellite"),
    MINIMAL("Light")
}

// Kenya Geographic Landmarks & Highway Corridors
private data class RoadLine(val name: String, val coords: List<Pair<Double, Double>>, val isHighway: Boolean = true)
private data class GeoPolygon(val name: String, val points: List<Pair<Double, Double>>, val color: Color)
private data class GeoLabel(val name: String, val lat: Double, val lon: Double, val minZoom: Float = 9.0f, val isMajor: Boolean = false)

@Composable
fun InteractivePropertyMapView(
    userLocation: UserLocation,
    properties: List<Property>,
    selectedProperty: Property?,
    onSelectProperty: (Property) -> Unit,
    onOpenPropertyDetail: (Property) -> Unit,
    onSelectPresetLocation: (PresetLocation) -> Unit,
    onRequestGps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Interactive camera state: Center latitude, longitude, and zoom level
    var cameraLat by remember { mutableDoubleStateOf(userLocation.latitude) }
    var cameraLon by remember { mutableDoubleStateOf(userLocation.longitude) }
    var zoomLevel by remember { mutableFloatStateOf(12.0f) } // 6.0f (country) to 18.0f (street)

    var currentTheme by remember { mutableStateOf(MapThemeMode.STREET) }
    var hasMovedCamera by remember { mutableStateOf(false) }
    var searchAreaToast by remember { mutableStateOf(false) }

    val carouselState = rememberLazyListState()

    // Smooth camera animations
    val animLat = remember { Animatable(userLocation.latitude.toFloat()) }
    val animLon = remember { Animatable(userLocation.longitude.toFloat()) }
    val animZoom = remember { Animatable(12.0f) }

    // Synchronize camera when user location changes initially
    LaunchedEffect(userLocation) {
        if (!hasMovedCamera) {
            cameraLat = userLocation.latitude
            cameraLon = userLocation.longitude
        }
    }

    // Scroll carousel to selected property if changed externally
    LaunchedEffect(selectedProperty) {
        if (selectedProperty != null) {
            val idx = properties.indexOfFirst { it.id == selectedProperty.id }
            if (idx >= 0) {
                carouselState.animateScrollToItem(idx)
            }
        }
    }

    // Pulse animation for user GPS location
    val infiniteTransition = rememberInfiniteTransition(label = "gps_pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    // Helper function to animate camera smoothly to a location
    fun animateCameraTo(targetLat: Double, targetLon: Double, targetZoom: Float = 13.0f) {
        coroutineScope.launch {
            launch { animLat.animateTo(targetLat.toFloat(), tween(650, easing = FastOutSlowInEasing)) { cameraLat = value.toDouble() } }
            launch { animLon.animateTo(targetLon.toFloat(), tween(650, easing = FastOutSlowInEasing)) { cameraLon = value.toDouble() } }
            launch { animZoom.animateTo(targetZoom, tween(650, easing = FastOutSlowInEasing)) { zoomLevel = value } }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("interactive_property_map_view")
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val center = Offset(widthPx / 2f, heightPx / 2f)

        // Equirectangular / Mercator projection scale
        // At zoom 10: 1 degree latitude is ~4500 px. Zooming doubles pixels per degree.
        val basePixelsPerDegree = 4500f
        val zoomMultiplier = 2f.pow(zoomLevel - 10f)
        val scaleLat = basePixelsPerDegree * zoomMultiplier
        val scaleLon = scaleLat * cos(Math.toRadians(cameraLat)).toFloat().coerceIn(0.6f, 1.2f)

        fun toScreen(lat: Double, lon: Double): Offset {
            val px = center.x + (lon - cameraLon).toFloat() * scaleLon
            val py = center.y - (lat - cameraLat).toFloat() * scaleLat
            return Offset(px, py)
        }

        // Precompute screen positions of property pins
        val pinPositions = remember(properties, cameraLat, cameraLon, zoomLevel, widthPx, heightPx, selectedProperty) {
            properties.mapNotNull { prop ->
                if (prop.latitude == 0.0 || prop.longitude == 0.0) return@mapNotNull null
                val screenPos = toScreen(prop.latitude, prop.longitude)
                // Filter out pins way outside the viewport
                if (screenPos.x < -120f || screenPos.x > widthPx + 120f ||
                    screenPos.y < -120f || screenPos.y > heightPx + 120f) {
                    return@mapNotNull null
                }
                PinData(
                    property = prop,
                    screenPos = screenPos,
                    isSelected = prop.id == selectedProperty?.id
                )
            }
        }

        // Visible properties count inside viewport
        val visiblePropertiesCount = pinPositions.size

        // 1. Fully scrollable, draggable, pinch-to-zoom Compose Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cameraLat, cameraLon, zoomLevel) {
                    // Pan and pinch-to-zoom gestures
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        hasMovedCamera = true
                        // Apply zoom change with limits
                        val newZoom = (zoomLevel * zoomChange).coerceIn(6.0f, 17.5f)
                        zoomLevel = newZoom

                        // Pan: convert pixel delta to lat/lon degrees
                        val dLon = pan.x / scaleLon
                        val dLat = pan.y / scaleLat

                        cameraLon = (cameraLon - dLon).coerceIn(33.0, 42.5) // Bound to Kenya
                        cameraLat = (cameraLat + dLat).coerceIn(-5.5, 5.0)
                    }
                }
                .pointerInput(pinPositions) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            // Double tap zooms in at tap point
                            val targetZoom = (zoomLevel + 1.2f).coerceAtMost(17.5f)
                            animateCameraTo(
                                targetLat = cameraLat - (tapOffset.y - center.y) / scaleLat * 0.4,
                                targetLon = cameraLon + (tapOffset.x - center.x) / scaleLon * 0.4,
                                targetZoom = targetZoom
                            )
                        },
                        onTap = { tapOffset ->
                            // Hit test property pins (48dp touch target)
                            val hitRadius = 45f
                            val hit = pinPositions.lastOrNull { pin ->
                                val dx = tapOffset.x - pin.screenPos.x
                                val dy = tapOffset.y - (pin.screenPos.y - 18f)
                                (dx * dx + dy * dy) <= (hitRadius * hitRadius)
                            }
                            if (hit != null) {
                                onSelectProperty(hit.property)
                                // Smoothly center slightly above bottom preview card
                                animateCameraTo(hit.property.latitude, hit.property.longitude, max(zoomLevel, 13.0f))
                            }
                        }
                    )
                }
        ) {
            // Background base tone depending on theme
            val baseColor = when (currentTheme) {
                MapThemeMode.STREET -> Color(0xFFF3F4F1)
                MapThemeMode.SATELLITE -> Color(0xFF1E2822)
                MapThemeMode.MINIMAL -> Color(0xFFFBFBFA)
            }
            drawRect(color = baseColor)

            // 1. WATER BODIES (Indian Ocean, Lake Victoria, Lake Naivasha)
            drawKenyanWaterBodies(
                theme = currentTheme,
                toScreen = { lat, lon -> toScreen(lat, lon) }
            )

            // 2. PARKS & NATURE RESERVES (Nairobi National Park, Karura Forest, Ngong)
            drawKenyanParksAndGreenery(
                theme = currentTheme,
                toScreen = { lat, lon -> toScreen(lat, lon) }
            )

            // 3. ROAD NETWORK & HIGHWAYS (Nairobi Expressway, Mombasa Rd, Thika Superhighway, Bypasses)
            drawKenyanRoadNetwork(
                theme = currentTheme,
                zoom = zoomLevel,
                toScreen = { lat, lon -> toScreen(lat, lon) }
            )

            // 4. URBAN STREET GRID (Fades in when zoomed in)
            if (zoomLevel >= 11.0f) {
                drawUrbanStreetGrid(
                    theme = currentTheme,
                    zoom = zoomLevel,
                    cameraLat = cameraLat,
                    cameraLon = cameraLon,
                    center = center,
                    scaleLat = scaleLat,
                    scaleLon = scaleLon,
                    size = size
                )
            }

            // 5. GEOGRAPHIC LABELS (Nairobi, Westlands, Karen, Kilimani, Mombasa, Kisumu, Nakuru)
            drawKenyanGeoLabels(
                theme = currentTheme,
                zoom = zoomLevel,
                toScreen = { lat, lon -> toScreen(lat, lon) }
            )

            // 6. USER LOCATION BEACON (Blue GPS dot with pulsating radar ring)
            if (userLocation.latitude != 0.0 && userLocation.longitude != 0.0) {
                val userScreenPos = toScreen(userLocation.latitude, userLocation.longitude)
                if (userScreenPos.x in -50f..size.width + 50f && userScreenPos.y in -50f..size.height + 50f) {
                    // Pulsing blue halo
                    drawCircle(
                        color = Color(0xFF2563EB).copy(alpha = pulseAlpha),
                        radius = pulseRadius,
                        center = userScreenPos
                    )
                    // White border
                    drawCircle(
                        color = Color.White,
                        radius = 11f,
                        center = userScreenPos
                    )
                    // Solid blue center
                    drawCircle(
                        color = Color(0xFF2563EB),
                        radius = 8f,
                        center = userScreenPos
                    )
                }
            }

            // 7. PROPERTY PINS (Zillow-style price pills)
            // Draw unselected pins first, then selected pin on top
            val sortedPins = pinPositions.sortedBy { if (it.isSelected) 1 else 0 }

            for (pin in sortedPins) {
                drawZillowPropertyPin(
                    pin = pin,
                    theme = currentTheme
                )
            }
        }

        // TOP CONTROLS: Location Title, Quick Preset Region Chips, Search This Area
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 10.dp, start = 12.dp, end = 12.dp)
        ) {
            // Location Header & Map Stats Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = KenyaForestGreen.copy(alpha = 0.12f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = KenyaForestGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = userLocation.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "$visiblePropertiesCount homes on map • Drag & pinch to explore",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Map layer theme switcher
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier
                            .clickable {
                                currentTheme = when (currentTheme) {
                                    MapThemeMode.STREET -> MapThemeMode.SATELLITE
                                    MapThemeMode.SATELLITE -> MapThemeMode.MINIMAL
                                    MapThemeMode.MINIMAL -> MapThemeMode.STREET
                                }
                            }
                            .testTag("map_theme_toggle_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Change map layer",
                                tint = KenyaForestGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentTheme.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Kenya Region Selector chips (Karen, Kilimani, Westlands, Mombasa, Kisumu, etc.)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(LocationService.getPresetLocations()) { preset ->
                    val isNear = kotlin.math.abs(cameraLat - preset.latitude) < 0.05 &&
                            kotlin.math.abs(cameraLon - preset.longitude) < 0.05

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isNear) KenyaForestGreen else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .clickable {
                                onSelectPresetLocation(preset)
                                animateCameraTo(preset.latitude, preset.longitude, 13.5f)
                            }
                            .testTag("preset_chip_${preset.id}")
                    ) {
                        Text(
                            text = preset.name,
                            fontSize = 11.sp,
                            fontWeight = if (isNear) FontWeight.Bold else FontWeight.Medium,
                            color = if (isNear) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Floating "Search This Area" pill when user has scrolled away
            AnimatedVisibility(
                visible = hasMovedCamera,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = KenyaForestGreen,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .clickable {
                            hasMovedCamera = false
                            searchAreaToast = true
                        }
                        .testTag("search_this_area_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Search this area ($visiblePropertiesCount homes)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // FLOATING ACTION BUTTONS: Zoom In (+), Zoom Out (-), My Location (GPS)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Zoom In Button
            FloatingActionButton(
                onClick = {
                    val nextZoom = (zoomLevel + 0.8f).coerceAtMost(17.5f)
                    animateCameraTo(cameraLat, cameraLon, nextZoom)
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp).testTag("map_zoom_in_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom in")
            }

            // Zoom Out Button
            FloatingActionButton(
                onClick = {
                    val nextZoom = (zoomLevel - 0.8f).coerceAtLeast(6.0f)
                    animateCameraTo(cameraLat, cameraLon, nextZoom)
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp).testTag("map_zoom_out_button")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom out")
            }

            // Center on GPS User Location
            FloatingActionButton(
                onClick = {
                    onRequestGps()
                    animateCameraTo(userLocation.latitude, userLocation.longitude, 13.5f)
                },
                shape = CircleShape,
                containerColor = if (userLocation.isGpsActive) Color(0xFF2563EB) else MaterialTheme.colorScheme.surface,
                contentColor = if (userLocation.isGpsActive) Color.White else KenyaForestGreen,
                modifier = Modifier.size(44.dp).testTag("map_recenter_gps_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter on my location"
                )
            }
        }

        // BOTTOM PREVIEW CAROUSEL / SELECTED PROPERTY CARD (Zillow style)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            if (selectedProperty != null) {
                // Focused Single Property Detailed Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clickable { onOpenPropertyDetail(selectedProperty) }
                        .testTag("map_selected_property_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedProperty.listingType == ListingType.FOR_SALE) KenyaForestGreen.copy(alpha = 0.15f) else Color(0xFF1D4ED8).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${selectedProperty.propertyType.label.uppercase()} • ${selectedProperty.listingType.label.uppercase()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedProperty.listingType == ListingType.FOR_SALE) KenyaForestGreen else Color(0xFF1D4ED8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = { onSelectProperty(selectedProperty) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close preview",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(84.dp)
                            ) {
                                PropertyImage(
                                    localDrawableName = selectedProperty.localDrawableName,
                                    imageUrl = selectedProperty.imageUrl,
                                    contentDescription = selectedProperty.title,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedProperty.formattedPrice,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = KenyaForestGreen
                                )
                                Text(
                                    text = selectedProperty.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${selectedProperty.bedrooms} bds • ${selectedProperty.bathrooms} ba • ${selectedProperty.areaSqM} m²",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "📍 ${selectedProperty.neighborhood}, ${selectedProperty.cityOrCounty}${if (selectedProperty.distanceKm != null) " • ${selectedProperty.formattedDistance}" else ""}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = KenyaGoldAccent,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onOpenPropertyDetail(selectedProperty) },
                                colors = ButtonDefaults.buttonColors(containerColor = KenyaForestGreen),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("View Home Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            } else if (pinPositions.isNotEmpty()) {
                // Swipeable bottom carousel of visible homes (Zillow style)
                LazyRow(
                    state = carouselState,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pinPositions) { pin ->
                        val prop = pin.property
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .width(270.dp)
                                .clickable {
                                    onSelectProperty(prop)
                                    animateCameraTo(prop.latitude, prop.longitude, max(zoomLevel, 13.0f))
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(68.dp)
                                ) {
                                    PropertyImage(
                                        localDrawableName = prop.localDrawableName,
                                        imageUrl = prop.imageUrl,
                                        contentDescription = prop.title,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prop.formattedPrice,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KenyaForestGreen
                                    )
                                    Text(
                                        text = prop.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${prop.bedrooms} bds • ${prop.neighborhood}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (prop.distanceKm != null) {
                                        Text(
                                            text = prop.formattedDistance,
                                            fontSize = 11.sp,
                                            color = KenyaGoldAccent,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PinData(
    val property: Property,
    val screenPos: Offset,
    val isSelected: Boolean
)

// -------------------------------------------------------------
// CARTOGRAPHIC DRAWING HELPERS
// -------------------------------------------------------------

private fun DrawScope.drawZillowPropertyPin(
    pin: PinData,
    theme: MapThemeMode
) {
    val prop = pin.property
    val offset = pin.screenPos
    val isSelected = pin.isSelected

    // Abbreviated price tag text
    val priceLabel = if (prop.price >= 1_000_000) {
        val m = prop.price / 1_000_000.0
        if (m % 1.0 == 0.0) "KSh ${m.toInt()}M" else "KSh ${String.format("%.1f", m)}M"
    } else {
        val k = prop.price / 1_000.0
        if (k % 1.0 == 0.0) "KSh ${k.toInt()}K" else "KSh ${String.format("%.0f", k)}K"
    }

    val bubbleWidth = if (isSelected) 110f else 96f
    val bubbleHeight = if (isSelected) 40f else 32f
    val bubbleTopLeft = Offset(offset.x - (bubbleWidth / 2f), offset.y - bubbleHeight - 12f)

    val pinColor = when {
        isSelected -> KenyaGoldAccent
        prop.listingType == ListingType.FOR_RENT -> Color(0xFF1D4ED8) // Deep Blue for rent
        else -> KenyaForestGreen // Deep Emerald for sale
    }

    // Selected Pin: pulsating outer ring & pointer
    if (isSelected) {
        drawCircle(
            color = KenyaGoldAccent.copy(alpha = 0.35f),
            radius = 24f,
            center = offset
        )
    }

    // Downward pointer stem/triangle pointing to GPS position
    val path = Path().apply {
        moveTo(offset.x - 7f, offset.y - 12f)
        lineTo(offset.x + 7f, offset.y - 12f)
        lineTo(offset.x, offset.y)
        close()
    }
    drawPath(path = path, color = pinColor)

    // Pill background
    drawRoundRect(
        color = pinColor,
        topLeft = bubbleTopLeft,
        size = Size(bubbleWidth, bubbleHeight),
        cornerRadius = CornerRadius(12f, 12f)
    )

    // Pill border (thick white border when selected)
    drawRoundRect(
        color = Color.White,
        topLeft = bubbleTopLeft,
        size = Size(bubbleWidth, bubbleHeight),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = if (isSelected) 3.5f else 1.5f)
    )

    // Text on price pill using native Android paint
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = if (isSelected) 24f else 20f
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }

    drawContext.canvas.nativeCanvas.drawText(
        priceLabel,
        offset.x,
        offset.y - 12f - (bubbleHeight / 2f) + (textPaint.textSize / 3f),
        textPaint
    )
}

private fun DrawScope.drawKenyanWaterBodies(
    theme: MapThemeMode,
    toScreen: (Double, Double) -> Offset
) {
    val waterColor = when (theme) {
        MapThemeMode.STREET -> Color(0xFFBFDBFE)
        MapThemeMode.SATELLITE -> Color(0xFF0F172A)
        MapThemeMode.MINIMAL -> Color(0xFFE0F2FE)
    }

    // 1. Indian Ocean Coast (East of ~39.5 lon near Mombasa, Diani, Malindi)
    val coastTop = toScreen(-3.0, 40.0)
    val coastBottom = toScreen(-4.8, 39.5)
    if (coastTop.x < size.width + 1000f) {
        val oceanPath = Path().apply {
            val p1 = toScreen(-2.5, 40.2)
            val p2 = toScreen(-3.2, 40.1)
            val p3 = toScreen(-3.6, 39.9)
            val p4 = toScreen(-4.0, 39.7) // Mombasa coast
            val p5 = toScreen(-4.3, 39.6) // Diani
            val p6 = toScreen(-4.8, 39.3)
            val pFarEast1 = Offset(size.width + 1200f, p6.y)
            val pFarEast2 = Offset(size.width + 1200f, p1.y)

            moveTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            lineTo(p4.x, p4.y)
            lineTo(p5.x, p5.y)
            lineTo(p6.x, p6.y)
            lineTo(pFarEast1.x, pFarEast1.y)
            lineTo(pFarEast2.x, pFarEast2.y)
            close()
        }
        drawPath(oceanPath, waterColor)
    }

    // 2. Lake Victoria (West of ~34.8 lon near Kisumu)
    val lakeVicCenter = toScreen(-0.30, 34.4)
    drawCircle(
        color = waterColor,
        radius = 280f,
        center = lakeVicCenter
    )

    // 3. Lake Naivasha & Lake Nakuru (Great Rift Valley)
    val naivashaCenter = toScreen(-0.76, 36.35)
    drawOval(
        color = waterColor,
        topLeft = Offset(naivashaCenter.x - 45f, naivashaCenter.y - 30f),
        size = Size(90f, 60f)
    )

    val nakuruCenter = toScreen(-0.37, 36.08)
    drawOval(
        color = waterColor,
        topLeft = Offset(nakuruCenter.x - 35f, nakuruCenter.y - 25f),
        size = Size(70f, 50f)
    )

    // Nairobi Dam
    val nairobiDamCenter = toScreen(-1.317, 36.804)
    drawCircle(
        color = waterColor,
        radius = 18f,
        center = nairobiDamCenter
    )
}

private fun DrawScope.drawKenyanParksAndGreenery(
    theme: MapThemeMode,
    toScreen: (Double, Double) -> Offset
) {
    val parkColor = when (theme) {
        MapThemeMode.STREET -> Color(0xFFDCFCE7)
        MapThemeMode.SATELLITE -> Color(0xFF064E3B)
        MapThemeMode.MINIMAL -> Color(0xFFF0FDF4)
    }

    // Nairobi National Park (vast safari reserve south of Nairobi city)
    val nnpP1 = toScreen(-1.32, 36.75)
    val nnpP2 = toScreen(-1.32, 36.95)
    val nnpP3 = toScreen(-1.44, 36.98)
    val nnpP4 = toScreen(-1.44, 36.78)

    val nnpPath = Path().apply {
        moveTo(nnpP1.x, nnpP1.y)
        lineTo(nnpP2.x, nnpP2.y)
        lineTo(nnpP3.x, nnpP3.y)
        lineTo(nnpP4.x, nnpP4.y)
        close()
    }
    drawPath(nnpPath, parkColor)

    // Karura Forest (North Nairobi green belt)
    val karuraP1 = toScreen(-1.23, 36.81)
    val karuraP2 = toScreen(-1.23, 36.85)
    val karuraP3 = toScreen(-1.26, 36.84)
    val karuraP4 = toScreen(-1.25, 36.81)
    val karuraPath = Path().apply {
        moveTo(karuraP1.x, karuraP1.y)
        lineTo(karuraP2.x, karuraP2.y)
        lineTo(karuraP3.x, karuraP3.y)
        lineTo(karuraP4.x, karuraP4.y)
        close()
    }
    drawPath(karuraPath, parkColor)

    // Ngong Forest / Sanctuary (near Karen & Langata)
    val ngongCenter = toScreen(-1.31, 36.72)
    drawOval(
        color = parkColor,
        topLeft = Offset(ngongCenter.x - 70f, ngongCenter.y - 45f),
        size = Size(140f, 90f)
    )
}

private fun DrawScope.drawKenyanRoadNetwork(
    theme: MapThemeMode,
    zoom: Float,
    toScreen: (Double, Double) -> Offset
) {
    val highwayCasing = when (theme) {
        MapThemeMode.STREET -> Color(0xFFFBBF24) // Gold Highway Corridor
        MapThemeMode.SATELLITE -> Color(0xFF38BDF8)
        MapThemeMode.MINIMAL -> Color(0xFFCBD5E1)
    }
    val majorRoadColor = when (theme) {
        MapThemeMode.STREET -> Color(0xFFFFFFFF)
        MapThemeMode.SATELLITE -> Color(0xFF475569)
        MapThemeMode.MINIMAL -> Color(0xFFE2E8F0)
    }

    val highwayWidth = if (zoom >= 12f) 8f else 5f
    val majorWidth = if (zoom >= 12f) 5f else 3f

    // 1. Nairobi Expressway / A104 Corridor (Waiyaki Way -> Uhuru Highway -> Mombasa Rd -> Athi River)
    val expresswayCoords = listOf(
        Pair(-1.220, 36.680), // Kikuyu / Sigona
        Pair(-1.255, 36.760), // Kangemi
        Pair(-1.265, 36.800), // Westlands / Sarit
        Pair(-1.285, 36.818), // Nairobi CBD / Uhuru Highway
        Pair(-1.315, 36.845), // Nyayo Stadium / South B
        Pair(-1.335, 36.885), // JKIA Interchange
        Pair(-1.365, 36.935), // Syokimau
        Pair(-1.420, 37.010)  // Athi River
    )
    drawRoadSegment(expresswayCoords, highwayCasing, highwayWidth, toScreen)

    // 2. Thika Superhighway A2 (Nairobi CBD -> Pangani -> Kasarani -> Roysambu -> Ruiru -> Thika)
    val thikaCoords = listOf(
        Pair(-1.285, 36.818), // CBD
        Pair(-1.268, 36.835), // Pangani
        Pair(-1.240, 36.865), // Muthaiga
        Pair(-1.215, 36.895), // Kasarani
        Pair(-1.185, 36.930), // Roysambu
        Pair(-1.145, 36.965), // Ruiru
        Pair(-1.035, 37.075)  // Thika
    )
    drawRoadSegment(thikaCoords, highwayCasing, highwayWidth, toScreen)

    // 3. Nairobi Southern Bypass (Mombasa Rd -> Langata -> Karen -> Kikuyu A104)
    val southernBypassCoords = listOf(
        Pair(-1.350, 36.890), // Mombasa Rd junction
        Pair(-1.340, 36.820), // Wilson Airport / Langata
        Pair(-1.325, 36.720), // Karen / Dagoretti
        Pair(-1.260, 36.670)  // Kikuyu junction
    )
    drawRoadSegment(southernBypassCoords, majorRoadColor, majorWidth, toScreen)

    // 4. Ngong Road (CBD -> Upperhill -> Kilimani -> Junction Mall -> Karen)
    val ngongRoadCoords = listOf(
        Pair(-1.290, 36.815), // Upperhill
        Pair(-1.295, 36.785), // Kilimani / Yaya
        Pair(-1.300, 36.755), // Junction Mall
        Pair(-1.315, 36.705)  // Karen Roundabout
    )
    drawRoadSegment(ngongRoadCoords, majorRoadColor, majorWidth, toScreen)

    // 5. Mombasa Coastal Highway B8 (Mombasa -> Nyali -> Bamburi -> Kilifi -> Malindi)
    val coastHighwayCoords = listOf(
        Pair(-4.300, 39.580), // Diani
        Pair(-4.080, 39.660), // Likoni
        Pair(-4.045, 39.680), // Mombasa CBD
        Pair(-4.030, 39.710), // Nyali
        Pair(-3.980, 39.730), // Bamburi
        Pair(-3.630, 39.850)  // Kilifi
    )
    drawRoadSegment(coastHighwayCoords, highwayCasing, highwayWidth, toScreen)
}

private fun DrawScope.drawRoadSegment(
    coords: List<Pair<Double, Double>>,
    color: Color,
    width: Float,
    toScreen: (Double, Double) -> Offset
) {
    if (coords.size < 2) return
    val path = Path()
    val first = toScreen(coords[0].first, coords[0].second)
    path.moveTo(first.x, first.y)
    for (i in 1 until coords.size) {
        val next = toScreen(coords[i].first, coords[i].second)
        path.lineTo(next.x, next.y)
    }
    drawPath(path, color, style = Stroke(width = width))
}

private fun DrawScope.drawUrbanStreetGrid(
    theme: MapThemeMode,
    zoom: Float,
    cameraLat: Double,
    cameraLon: Double,
    center: Offset,
    scaleLat: Float,
    scaleLon: Float,
    size: Size
) {
    val streetColor = when (theme) {
        MapThemeMode.STREET -> Color(0xFFE5E7EB)
        MapThemeMode.SATELLITE -> Color(0xFF334155)
        MapThemeMode.MINIMAL -> Color(0xFFF1F5F9)
    }

    // Dynamic grid representing neighborhood street layout
    val gridStep = if (zoom >= 13.5f) 0.005 else 0.015
    val minLat = cameraLat - (size.height / 2f / scaleLat)
    val maxLat = cameraLat + (size.height / 2f / scaleLat)
    val minLon = cameraLon - (size.width / 2f / scaleLon)
    val maxLon = cameraLon + (size.width / 2f / scaleLon)

    var curLat = minLat - (minLat % gridStep)
    while (curLat <= maxLat) {
        val y = center.y - (curLat - cameraLat).toFloat() * scaleLat
        drawLine(
            color = streetColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.5f
        )
        curLat += gridStep
    }

    var curLon = minLon - (minLon % gridStep)
    while (curLon <= maxLon) {
        val x = center.x + (curLon - cameraLon).toFloat() * scaleLon
        drawLine(
            color = streetColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.5f
        )
        curLon += gridStep
    }
}

private fun DrawScope.drawKenyanGeoLabels(
    theme: MapThemeMode,
    zoom: Float,
    toScreen: (Double, Double) -> Offset
) {
    val textColor = when (theme) {
        MapThemeMode.STREET -> android.graphics.Color.rgb(55, 65, 81)
        MapThemeMode.SATELLITE -> android.graphics.Color.rgb(243, 244, 246)
        MapThemeMode.MINIMAL -> android.graphics.Color.rgb(75, 85, 99)
    }

    val labels = listOf(
        GeoLabel("NAIROBI", -1.286389, 36.817223, minZoom = 7.0f, isMajor = true),
        GeoLabel("Westlands", -1.2683, 36.8070, minZoom = 11.0f),
        GeoLabel("Karen", -1.3197, 36.7067, minZoom = 10.5f),
        GeoLabel("Kilimani", -1.2921, 36.7865, minZoom = 11.5f),
        GeoLabel("Kileleshwa", -1.2815, 36.7845, minZoom = 12.0f),
        GeoLabel("Runda", -1.2185, 36.8122, minZoom = 11.0f),
        GeoLabel("Gigiri (UN)", -1.2330, 36.8180, minZoom = 12.0f),
        GeoLabel("Muthaiga", -1.2550, 36.8320, minZoom = 12.0f),
        GeoLabel("Lavington", -1.2820, 36.7680, minZoom = 11.5f),
        GeoLabel("Langata", -1.3400, 36.7600, minZoom = 11.5f),
        GeoLabel("JKIA Airport ✈️", -1.3280, 36.9250, minZoom = 11.0f),
        GeoLabel("Nairobi National Park 🦁", -1.3700, 36.8500, minZoom = 10.0f),
        GeoLabel("MOMBASA", -4.0435, 39.6682, minZoom = 7.0f, isMajor = true),
        GeoLabel("Nyali", -4.0384, 39.7027, minZoom = 11.0f),
        GeoLabel("Diani Beach", -4.2797, 39.5947, minZoom = 10.0f),
        GeoLabel("KISUMU", -0.0917, 34.7680, minZoom = 7.0f, isMajor = true),
        GeoLabel("Milimani Kisumu", -0.1050, 34.7550, minZoom = 11.5f),
        GeoLabel("NAKURU", -0.3031, 36.0800, minZoom = 7.0f, isMajor = true),
        GeoLabel("Lake Naivasha", -0.7600, 36.3500, minZoom = 9.0f)
    )

    for (lbl in labels) {
        if (zoom >= lbl.minZoom) {
            val pt = toScreen(lbl.lat, lbl.lon)
            if (pt.x in 0f..size.width && pt.y in 0f..size.height) {
                val paint = android.graphics.Paint().apply {
                    color = textColor
                    textSize = if (lbl.isMajor) 28f else 22f
                    isFakeBoldText = lbl.isMajor
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    if (lbl.isMajor) {
                        setShadowLayer(4f, 0f, 1f, android.graphics.Color.WHITE)
                    }
                }
                drawContext.canvas.nativeCanvas.drawText(lbl.name, pt.x, pt.y, paint)
            }
        }
    }
}
