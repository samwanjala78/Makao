package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.location.UserLocation
import com.example.data.model.PresetLocation
import com.example.data.model.Property

/**
 * Interactive scrollable, draggable, pinch-to-zoom real-estate map
 * showing all available properties across Kenya with Zillow-style price pills.
 */
@Composable
fun ZillowRadarMapView(
    userLocation: UserLocation,
    properties: List<Property>,
    selectedProperty: Property?,
    onSelectProperty: (Property) -> Unit,
    onOpenPropertyDetail: (Property) -> Unit,
    onSelectPresetLocation: (PresetLocation) -> Unit,
    onRequestGps: () -> Unit,
    modifier: Modifier = Modifier
) {
    InteractivePropertyMapView(
        userLocation = userLocation,
        properties = properties,
        selectedProperty = selectedProperty,
        onSelectProperty = onSelectProperty,
        onOpenPropertyDetail = onOpenPropertyDetail,
        onSelectPresetLocation = onSelectPresetLocation,
        onRequestGps = onRequestGps,
        modifier = modifier
    )
}
