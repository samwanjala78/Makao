package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.LocationService
import com.example.data.location.UserLocation
import com.example.data.model.ListingType
import com.example.data.model.PresetLocation
import com.example.data.model.Property
import com.example.ui.components.InteractivePropertyMapView
import com.example.ui.components.PropertyCard
import com.example.ui.components.ZillowRadarMapView
import com.example.ui.theme.KenyaForestGreen
import com.example.ui.theme.KenyaGoldAccent
import com.example.ui.viewmodel.FilterState
import com.example.ui.viewmodel.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    properties: List<Property>,
    allProperties: List<Property>,
    favoriteIds: Set<String>,
    userLocation: UserLocation,
    filterState: FilterState,
    isMapView: Boolean,
    selectedMapProperty: Property?,
    onToggleMapView: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onListingTypeChange: (ListingType?) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onSelectPresetLocation: (PresetLocation) -> Unit,
    onRequestGps: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectProperty: (Property) -> Unit,
    onSelectMapProperty: (Property) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenAddProperty: () -> Unit,
    modifier: Modifier = Modifier
) {
    var locationMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP HEADER: Zillow Search Bar & Quick Switchers
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Location Chip & View Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Location Picker Pill
                        Box {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { locationMenuExpanded = true }
                                    .testTag("location_picker_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = KenyaForestGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = userLocation.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 180.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = locationMenuExpanded,
                                onDismissRequest = { locationMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.MyLocation,
                                                contentDescription = null,
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Use Current GPS Location", fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        onRequestGps()
                                        locationMenuExpanded = false
                                    }
                                )
                                androidx.compose.material3.HorizontalDivider()
                                LocationService.getPresetLocations().forEach { preset ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(preset.name, fontWeight = FontWeight.SemiBold)
                                                Text(preset.description, fontSize = 11.sp, color = Color.Gray)
                                            }
                                        },
                                        onClick = {
                                            onSelectPresetLocation(preset)
                                            locationMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Right actions: Map/List Toggle & Filter Icon
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Map vs List Toggle
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = KenyaForestGreen,
                                modifier = Modifier
                                    .clickable(onClick = onToggleMapView)
                                    .testTag("toggle_map_view_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isMapView) Icons.Default.ViewList else Icons.Default.Map,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isMapView) "List View" else "Map View",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Filter Button
                            IconButton(
                                onClick = onOpenFilters,
                                modifier = Modifier.testTag("open_filters_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Filters",
                                    tint = KenyaForestGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Search Input
                    OutlinedTextField(
                        value = filterState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search Karen, Kilimani, Nyali, Kisumu...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                        },
                        trailingIcon = {
                            if (filterState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KenyaForestGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("search_homes_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Filter Chips (All / For Sale / For Rent / Radius)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = filterState.listingType == null,
                                onClick = { onListingTypeChange(null) },
                                label = { Text("All Purpose") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KenyaForestGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterState.listingType == ListingType.FOR_SALE,
                                onClick = { onListingTypeChange(ListingType.FOR_SALE) },
                                label = { Text("For Sale") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KenyaForestGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterState.listingType == ListingType.FOR_RENT,
                                onClick = { onListingTypeChange(ListingType.FOR_RENT) },
                                label = { Text("For Rent") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KenyaForestGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterState.maxRadiusKm == 15f,
                                onClick = { onRadiusChange(if (filterState.maxRadiusKm == 15f) 0f else 15f) },
                                label = { Text("Within 15km") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KenyaGoldAccent,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterState.maxRadiusKm == 30f,
                                onClick = { onRadiusChange(if (filterState.maxRadiusKm == 30f) 0f else 30f) },
                                label = { Text("Within 30km") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KenyaGoldAccent,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // BODY: Toggle between List View and Interactive Scrollable Map View
            if (isMapView) {
                val mapProperties = if (properties.isNotEmpty()) properties else allProperties
                InteractivePropertyMapView(
                    userLocation = userLocation,
                    properties = mapProperties,
                    selectedProperty = selectedMapProperty,
                    onSelectProperty = onSelectMapProperty,
                    onOpenPropertyDetail = onSelectProperty,
                    onSelectPresetLocation = onSelectPresetLocation,
                    onRequestGps = onRequestGps,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // List View
                if (properties.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = KenyaForestGreen.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Homes Found Nearby",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No properties match your current distance or criteria. Try expanding the radius or clearing filters.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onOpenFilters,
                                colors = ButtonDefaults.buttonColors(containerColor = KenyaForestGreen)
                            ) {
                                Text("Adjust Geolocation Filters")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section Header: Result count and Geolocation label
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${properties.size} Properties Available",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Sorted by ${filterState.sortOrder.label.lowercase()}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (userLocation.isGpsActive) "🟢 GPS Active" else "📍 Kenya Hub",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KenyaForestGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Items
                        items(properties, key = { it.id }) { property ->
                            PropertyCard(
                                property = property,
                                isFavorite = favoriteIds.contains(property.id),
                                onToggleFavorite = onToggleFavorite,
                                onClick = { onSelectProperty(property) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button: Post Listing in real-time
        ExtendedFloatingActionButton(
            onClick = onOpenAddProperty,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("List Property", fontWeight = FontWeight.Bold) },
            containerColor = KenyaForestGreen,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("fab_add_property")
        )
    }
}
