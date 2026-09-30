package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.LocationService
import com.example.data.location.UserLocation
import com.example.data.model.ListingType
import com.example.data.model.PresetLocation
import com.example.data.model.Property
import com.example.ui.theme.KenyaForestGreen
import com.example.ui.theme.KenyaGoldAccent
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class GoogleMapTypeMode(val label: String, val mapType: MapType) {
    NORMAL("Normal", MapType.NORMAL),
    SATELLITE("Satellite", MapType.SATELLITE),
    TERRAIN("Terrain", MapType.TERRAIN)
}

/**
 * Real Estate Interactive Map powered by Google Compose Maps library.
 * Features:
 * - GoogleMap with CameraPositionState
 * - Custom Compose Zillow-style price pills (MarkerComposable)
 * - Map layer toggles (Normal, Satellite, Terrain)
 * - Kenya preset location shortcuts (Karen, Kilimani, Westlands, Mombasa, Kisumu, etc.)
 * - Floating controls (Zoom in, Zoom out, GPS My Location)
 * - Dynamic "Search this area" floating button
 * - Bottom synchronized Property Preview Carousel & Detail Card
 */
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

    // Initialize Google Maps camera state centered on userLocation or Kenya default
    val defaultCenter = remember(userLocation.latitude, userLocation.longitude) {
        val lat = if (userLocation.latitude != 0.0) userLocation.latitude else -1.2921
        val lng = if (userLocation.longitude != 0.0) userLocation.longitude else 36.8219
        LatLng(lat, lng)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 12.5f)
    }

    var currentMapTypeMode by remember { mutableStateOf(GoogleMapTypeMode.NORMAL) }
    var lastSearchedCenter by remember { mutableStateOf(defaultCenter) }
    var hasMovedCameraAway by remember { mutableStateOf(false) }

    val carouselState = rememberLazyListState()

    // Smoothly fly camera to userLocation when GPS becomes active or initially updates
    LaunchedEffect(userLocation.isGpsActive, userLocation.latitude, userLocation.longitude) {
        if (userLocation.isGpsActive && userLocation.latitude != 0.0 && userLocation.longitude != 0.0) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(userLocation.latitude, userLocation.longitude),
                    13.5f
                ),
                800
            )
            lastSearchedCenter = LatLng(userLocation.latitude, userLocation.longitude)
            hasMovedCameraAway = false
        }
    }

    // When camera moves far enough from the last searched center, display "Search this area"
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val target = cameraPositionState.position.target
            val latDiff = abs(target.latitude - lastSearchedCenter.latitude)
            val lngDiff = abs(target.longitude - lastSearchedCenter.longitude)
            if (latDiff > 0.02 || lngDiff > 0.02) {
                hasMovedCameraAway = true
            }
        }
    }

    // Scroll carousel to selected property if changed
    LaunchedEffect(selectedProperty?.id) {
        if (selectedProperty != null) {
            val idx = properties.indexOfFirst { it.id == selectedProperty.id }
            if (idx >= 0) {
                carouselState.animateScrollToItem(idx)
            }
        }
    }

    // Map UI Settings
    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = true,
            rotationGesturesEnabled = true,
            scrollGesturesEnabled = true,
            tiltGesturesEnabled = true,
            zoomGesturesEnabled = true,
            mapToolbarEnabled = false
        )
    }

    val mapProperties = remember(currentMapTypeMode) {
        MapProperties(
            mapType = currentMapTypeMode.mapType,
            isMyLocationEnabled = false
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("google_compose_map_view")
    ) {
        // 1. GOOGLE COMPOSE MAP
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            contentPadding = PaddingValues(top = 110.dp, bottom = if (selectedProperty != null) 210.dp else 140.dp),
            onMapClick = {
                // Tapping on empty map dismisses focused card
                // User can re-select by tapping any pin or carousel item
            }
        ) {
            // User Location Marker
            if (userLocation.latitude != 0.0 && userLocation.longitude != 0.0) {
                MarkerComposable(
                    keys = arrayOf("user_location", userLocation.latitude, userLocation.longitude, userLocation.isGpsActive),
                    state = rememberMarkerState(
                        key = "user_gps_location",
                        position = LatLng(userLocation.latitude, userLocation.longitude)
                    ),
                    title = "Your Location",
                    snippet = userLocation.displayName,
                    zIndex = 50f
                ) {
                    UserLocationPulseDot(isGpsActive = userLocation.isGpsActive)
                }
            }

            // Real Estate Properties Markers
            properties.forEach { property ->
                val isSelected = selectedProperty?.id == property.id
                val position = remember(property.latitude, property.longitude) {
                    LatLng(property.latitude, property.longitude)
                }

                MarkerComposable(
                    keys = arrayOf(property.id, isSelected, property.price, property.listingType),
                    state = rememberMarkerState(
                        key = property.id,
                        position = position
                    ),
                    title = property.title,
                    snippet = "${property.formattedPrice} • ${property.neighborhood}",
                    zIndex = if (isSelected) 100f else 10f,
                    onClick = {
                        onSelectProperty(property)
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLng(position),
                                500
                            )
                        }
                        true
                    }
                ) {
                    ZillowPropertyPricePill(
                        property = property,
                        isSelected = isSelected
                    )
                }
            }
        }

        // 2. TOP OVERLAY: LOCATION BAR & PRESET REGION CHIPS
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Header Card with current location, home count, and layer switcher
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${properties.size} Makao homes in Kenya • Tap pin for details",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Map Layer Selector (Normal, Satellite, Terrain)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier
                            .clickable {
                                currentMapTypeMode = when (currentMapTypeMode) {
                                    GoogleMapTypeMode.NORMAL -> GoogleMapTypeMode.SATELLITE
                                    GoogleMapTypeMode.SATELLITE -> GoogleMapTypeMode.TERRAIN
                                    GoogleMapTypeMode.TERRAIN -> GoogleMapTypeMode.NORMAL
                                }
                            }
                            .testTag("map_layer_toggle_button")
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
                                text = currentMapTypeMode.label,
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
                    val targetPos = cameraPositionState.position.target
                    val isNear = abs(targetPos.latitude - preset.latitude) < 0.05 &&
                            abs(targetPos.longitude - preset.longitude) < 0.05

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isNear) KenyaForestGreen else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .clickable {
                                onSelectPresetLocation(preset)
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(preset.latitude, preset.longitude),
                                            13.5f
                                        ),
                                        700
                                    )
                                    lastSearchedCenter = LatLng(preset.latitude, preset.longitude)
                                    hasMovedCameraAway = false
                                }
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

            // Floating "Search This Area" pill when user has panned away
            AnimatedVisibility(
                visible = hasMovedCameraAway,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = KenyaForestGreen,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .clickable {
                            lastSearchedCenter = cameraPositionState.position.target
                            hasMovedCameraAway = false
                        }
                        .testTag("search_this_area_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
                            text = "Search this area (${properties.size} homes)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 3. FLOATING ACTION BUTTONS: Zoom In (+), Zoom Out (-), My Location (GPS)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Zoom In Button
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomIn(), 300)
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_zoom_in_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom in")
            }

            // Zoom Out Button
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomOut(), 300)
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_zoom_out_button")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom out")
            }

            // Center on GPS User Location
            FloatingActionButton(
                onClick = {
                    onRequestGps()
                    if (userLocation.latitude != 0.0 && userLocation.longitude != 0.0) {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(userLocation.latitude, userLocation.longitude),
                                    14f
                                ),
                                700
                            )
                        }
                    }
                },
                shape = CircleShape,
                containerColor = if (userLocation.isGpsActive) Color(0xFF2563EB) else MaterialTheme.colorScheme.surface,
                contentColor = if (userLocation.isGpsActive) Color.White else KenyaForestGreen,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_recenter_gps_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter on my location"
                )
            }
        }

        // 4. BOTTOM PREVIEW CAROUSEL / SELECTED PROPERTY CARD (Zillow style)
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
                                onClick = {
                                    // Deselect by triggering select with another or closing
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close preview",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(width = 96.dp, height = 72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                PropertyImage(
                                    localDrawableName = selectedProperty.localDrawableName,
                                    imageUrl = selectedProperty.imageUrl,
                                    contentDescription = selectedProperty.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedProperty.fullFormattedPrice,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = KenyaForestGreen
                                )

                                Text(
                                    text = selectedProperty.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = KenyaGoldAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${selectedProperty.neighborhood}, ${selectedProperty.cityOrCounty}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Specs & Action Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedProperty.bedrooms > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Bed, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("${selectedProperty.bedrooms} bd", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                                if (selectedProperty.bathrooms > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Bathtub, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("${selectedProperty.bathrooms} ba", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                                if (selectedProperty.areaSqM > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SquareFoot, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("${selectedProperty.areaSqM} m²", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            Button(
                                onClick = { onOpenPropertyDetail(selectedProperty) },
                                colors = ButtonDefaults.buttonColors(containerColor = KenyaForestGreen),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("map_view_details_button")
                            ) {
                                Text("View Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            } else {
                // Horizontal Property Carousel at bottom
                LazyRow(
                    state = carouselState,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    items(properties, key = { it.id }) { prop ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier
                                .width(240.dp)
                                .clickable {
                                    onSelectProperty(prop)
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLng(
                                                LatLng(prop.latitude, prop.longitude)
                                            ),
                                            500
                                        )
                                    }
                                }
                                .testTag("map_carousel_card_${prop.id}")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    PropertyImage(
                                        localDrawableName = prop.localDrawableName,
                                        imageUrl = prop.imageUrl,
                                        contentDescription = prop.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (prop.listingType == ListingType.FOR_SALE) KenyaForestGreen else Color(0xFF1D4ED8),
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            text = prop.listingType.label,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = prop.formattedPrice,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KenyaForestGreen
                                )

                                Text(
                                    text = prop.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${prop.neighborhood} • ${prop.bedrooms} beds",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Zillow-style custom price pill rendered inside Google Maps MarkerComposable.
 */
@Composable
fun ZillowPropertyPricePill(
    property: Property,
    isSelected: Boolean
) {
    val isForSale = property.listingType == ListingType.FOR_SALE
    val baseBgColor = when {
        isSelected -> KenyaGoldAccent
        isForSale -> KenyaForestGreen
        else -> Color(0xFF0288D1)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = baseBgColor,
            border = BorderStroke(
                width = if (isSelected) 2.5.dp else 1.5.dp,
                color = Color.White
            ),
            shadowElevation = if (isSelected) 10.dp else 4.dp
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (isSelected) 10.dp else 8.dp,
                    vertical = if (isSelected) 6.dp else 4.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    text = property.formattedPrice,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isSelected) 12.sp else 11.sp
                )
                if (property.bedrooms > 0) {
                    Text(
                        text = " • ${property.bedrooms}bd",
                        color = Color.White.copy(alpha = 0.92f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Downward pointer arrow indicator
        Canvas(modifier = Modifier.size(width = 10.dp, height = 6.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(path, color = baseBgColor)
            drawPath(path, color = Color.White, style = Stroke(width = 1.5f))
        }
    }
}

/**
 * Animated pulsating GPS marker dot for user location.
 */
@Composable
fun UserLocationPulseDot(isGpsActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_size"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    val dotColor = if (isGpsActive) Color(0xFF2563EB) else KenyaForestGreen

    Box(
        modifier = Modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(pulseSize.dp)) {
            drawCircle(
                color = dotColor.copy(alpha = pulseAlpha)
            )
        }
        Surface(
            shape = CircleShape,
            color = dotColor,
            border = BorderStroke(2.dp, Color.White),
            shadowElevation = 5.dp,
            modifier = Modifier.size(16.dp)
        ) {}
    }
}
