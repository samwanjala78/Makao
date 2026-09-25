package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Property
import com.example.data.model.TourBooking
import com.example.ui.components.AddPropertyDialog
import com.example.ui.components.FilterBottomSheet
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PropertyDetailScreen
import com.example.ui.screens.TourBookingsScreen
import com.example.ui.theme.KenyaForestGreen
import com.example.ui.theme.KenyaGoldAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MakaoViewModel

enum class MainTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    EXPLORE("Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
    MAP("Nearby Map", Icons.Filled.Map, Icons.Outlined.Map),
    SAVED("Saved", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    TOURS("Tours", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MakaoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MakaoApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MakaoApp(viewModel: MakaoViewModel) {
    val properties by viewModel.filteredProperties.collectAsStateWithLifecycle()
    val allProperties by viewModel.propertiesWithDistance.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val selectedProperty by viewModel.selectedProperty.collectAsStateWithLifecycle()
    val selectedMapProperty by viewModel.selectedMapProperty.collectAsStateWithLifecycle()
    val isMapView by viewModel.isMapView.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainTab.EXPLORE) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showAddPropertySheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val localBookings = remember { mutableStateListOf<TourBooking>() }

    // Geolocation permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.requestGpsLocation()
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.hasLocationPermission()) {
            viewModel.requestGpsLocation()
        } else {
            // Proactively request location to find homes nearby
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Snackbar listener
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    val favoriteProperties = remember(allProperties, favoriteIds) {
        allProperties.filter { favoriteIds.contains(it.id) }
    }

    // If a property is selected, show Detail Screen
    if (selectedProperty != null) {
        val prop = selectedProperty!!
        PropertyDetailScreen(
            property = prop,
            isFavorite = favoriteIds.contains(prop.id),
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onBack = { viewModel.selectProperty(null) },
            onBookTour = { name, phone, date, slot, type, notes ->
                viewModel.bookTour(prop, name, phone, date, slot, type, notes) {
                    val newBooking = TourBooking(
                        propertyId = prop.id,
                        propertyTitle = prop.title,
                        propertyPrice = prop.formattedPrice,
                        propertyLocation = "${prop.neighborhood}, ${prop.cityOrCounty}",
                        clientName = name,
                        clientPhone = phone,
                        date = date,
                        timeSlot = slot,
                        tourType = type,
                        notes = notes
                    )
                    localBookings.add(0, newBooking)
                }
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    MainTab.values().forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                currentTab = tab
                                if (tab == MainTab.MAP && !isMapView) {
                                    viewModel.toggleMapView()
                                } else if (tab == MainTab.EXPLORE && isMapView) {
                                    viewModel.toggleMapView()
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = KenyaForestGreen,
                                selectedTextColor = KenyaForestGreen,
                                indicatorColor = KenyaForestGreen.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_switch"
                ) { tab ->
                    when (tab) {
                        MainTab.EXPLORE, MainTab.MAP -> {
                            HomeScreen(
                                properties = properties,
                                allProperties = allProperties,
                                favoriteIds = favoriteIds,
                                userLocation = userLocation,
                                filterState = filterState,
                                isMapView = (tab == MainTab.MAP) || isMapView,
                                selectedMapProperty = selectedMapProperty,
                                onToggleMapView = {
                                    viewModel.toggleMapView()
                                    if (currentTab == MainTab.MAP) {
                                        currentTab = MainTab.EXPLORE
                                    } else if (isMapView) {
                                        currentTab = MainTab.MAP
                                    }
                                },
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                onListingTypeChange = { viewModel.updateListingType(it) },
                                onRadiusChange = { viewModel.updateRadius(it) },
                                onSortOrderChange = { viewModel.updateSortOrder(it) },
                                onSelectPresetLocation = { viewModel.selectPresetLocation(it) },
                                onRequestGps = {
                                    if (viewModel.hasLocationPermission()) {
                                        viewModel.requestGpsLocation()
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onSelectProperty = { viewModel.selectProperty(it) },
                                onSelectMapProperty = { viewModel.selectMapProperty(it) },
                                onOpenFilters = { showFilterSheet = true },
                                onOpenAddProperty = { showAddPropertySheet = true }
                            )
                        }
                        MainTab.SAVED -> {
                            FavoritesScreen(
                                favoriteProperties = favoriteProperties,
                                favoriteIds = favoriteIds,
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onSelectProperty = { viewModel.selectProperty(it) },
                                onExploreClicked = { currentTab = MainTab.EXPLORE }
                            )
                        }
                        MainTab.TOURS -> {
                            TourBookingsScreen(bookings = localBookings.toList())
                        }
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            filterState = filterState,
            totalMatchingCount = properties.size,
            onListingTypeChange = { viewModel.updateListingType(it) },
            onPropertyTypeChange = { viewModel.updatePropertyType(it) },
            onBedroomsChange = { viewModel.updateBedrooms(it) },
            onRadiusChange = { viewModel.updateRadius(it) },
            onSortOrderChange = { viewModel.updateSortOrder(it) },
            onResetFilters = { viewModel.resetFilters() },
            onDismiss = { showFilterSheet = false }
        )
    }

    // Add Property Bottom Sheet
    if (showAddPropertySheet) {
        AddPropertyDialog(
            userLocation = userLocation,
            isSubmitting = isSubmitting,
            onDismiss = { showAddPropertySheet = false },
            onSubmit = { title, tagline, price, pricePeriod, listingType, propertyType,
                         bedrooms, bathrooms, areaSqM, hasDsq, neighborhood, cityOrCounty,
                         latitude, longitude, description, amenities, imageUrl,
                         agentName, agentPhone, agentAgency ->
                viewModel.postNewListing(
                    title = title,
                    tagline = tagline,
                    price = price,
                    pricePeriod = pricePeriod,
                    listingType = listingType,
                    propertyType = propertyType,
                    bedrooms = bedrooms,
                    bathrooms = bathrooms,
                    areaSqM = areaSqM,
                    hasDsq = hasDsq,
                    neighborhood = neighborhood,
                    cityOrCounty = cityOrCounty,
                    latitude = latitude,
                    longitude = longitude,
                    description = description,
                    amenities = amenities,
                    imageUrl = imageUrl,
                    agentName = agentName,
                    agentPhone = agentPhone,
                    agentAgency = agentAgency,
                    onSuccess = {
                        showAddPropertySheet = false
                    }
                )
            }
        )
    }
}
