package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirestoreService
import com.example.data.location.LocationService
import com.example.data.location.UserLocation
import com.example.data.model.ListingType
import com.example.data.model.PresetLocation
import com.example.data.model.Property
import com.example.data.model.PropertyType
import com.example.data.model.TourBooking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder(val label: String) {
    NEAREST("Nearest to Me"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    NEWEST("Newest Listings")
}

data class FilterState(
    val searchQuery: String = "",
    val listingType: ListingType? = null,
    val propertyType: PropertyType = PropertyType.ALL,
    val bedrooms: Int = 0, // 0 = any
    val maxRadiusKm: Float = 0f, // 0 = any distance, or 5, 10, 25, 50, 100
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val sortOrder: SortOrder = SortOrder.NEAREST
)

class MakaoViewModel(application: Application) : AndroidViewModel(application) {

    private val firestoreService = FirestoreService()
    private val locationService = LocationService(application.applicationContext)

    private val _userLocation = MutableStateFlow(LocationService.DEFAULT_KENYA_LOCATION)
    val userLocation: StateFlow<UserLocation> = _userLocation.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(
        setOf("prop_karen_villa_01", "prop_diani_villa_03")
    )
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _selectedProperty = MutableStateFlow<Property?>(null)
    val selectedProperty: StateFlow<Property?> = _selectedProperty.asStateFlow()

    private val _selectedMapProperty = MutableStateFlow<Property?>(null)
    val selectedMapProperty: StateFlow<Property?> = _selectedMapProperty.asStateFlow()

    private val _isMapView = MutableStateFlow(false)
    val isMapView: StateFlow<Boolean> = _isMapView.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    // Real-time properties from Firestore, annotated with live distance to user
    private val rawPropertiesFlow = firestoreService.getPropertiesFlow()

    val propertiesWithDistance: StateFlow<List<Property>> = combine(
        rawPropertiesFlow,
        _userLocation
    ) { properties, userLoc ->
        properties.map { prop ->
            val dist = if (prop.latitude != 0.0 && prop.longitude != 0.0) {
                LocationService.calculateDistanceKm(
                    userLoc.latitude,
                    userLoc.longitude,
                    prop.latitude,
                    prop.longitude
                )
            } else null
            prop.copy(distanceKm = dist)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val filteredProperties: StateFlow<List<Property>> = combine(
        propertiesWithDistance,
        _filterState
    ) { list, filters ->
        var result = list

        // Search query
        if (filters.searchQuery.isNotBlank()) {
            val q = filters.searchQuery.trim().lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                it.neighborhood.lowercase().contains(q) ||
                it.cityOrCounty.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.tagline.lowercase().contains(q)
            }
        }

        // Listing Type (Sale vs Rent)
        if (filters.listingType != null) {
            result = result.filter { it.listingType == filters.listingType }
        }

        // Property Type
        if (filters.propertyType != PropertyType.ALL) {
            result = result.filter { it.propertyType == filters.propertyType }
        }

        // Bedrooms
        if (filters.bedrooms > 0) {
            result = result.filter { it.bedrooms >= filters.bedrooms }
        }

        // Geolocation Radius Filter
        if (filters.maxRadiusKm > 0f) {
            result = result.filter {
                val d = it.distanceKm
                d != null && d <= filters.maxRadiusKm
            }
        }

        // Min / Max Price
        if (filters.minPrice != null) {
            result = result.filter { it.price >= filters.minPrice }
        }
        if (filters.maxPrice != null) {
            result = result.filter { it.price <= filters.maxPrice }
        }

        // Sorting
        result = when (filters.sortOrder) {
            SortOrder.NEAREST -> result.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
            SortOrder.PRICE_LOW_HIGH -> result.sortedBy { it.price }
            SortOrder.PRICE_HIGH_LOW -> result.sortedByDescending { it.price }
            SortOrder.NEWEST -> result.sortedByDescending { it.createdAt }
        }

        result
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun hasLocationPermission(): Boolean = locationService.hasLocationPermission()

    fun requestGpsLocation() {
        viewModelScope.launch {
            val loc = locationService.getCurrentLocation()
            if (loc != null) {
                _userLocation.value = loc
                _snackbarMessage.value = "GPS Location Updated: Nearby homes recalculated!"
            } else {
                _snackbarMessage.value = "Unable to fetch live GPS. Using current Kenya location."
            }
        }
    }

    fun selectPresetLocation(preset: PresetLocation) {
        _userLocation.value = UserLocation(
            latitude = preset.latitude,
            longitude = preset.longitude,
            displayName = preset.name,
            isGpsActive = false
        )
        _snackbarMessage.value = "Location set to ${preset.name}. Finding nearby homes!"
    }

    fun toggleMapView() {
        _isMapView.value = !_isMapView.value
    }

    fun selectProperty(property: Property?) {
        _selectedProperty.value = property
    }

    fun selectMapProperty(property: Property?) {
        _selectedMapProperty.value = property
    }

    fun toggleFavorite(propertyId: String) {
        val current = _favoriteIds.value.toMutableSet()
        if (current.contains(propertyId)) {
            current.remove(propertyId)
            _snackbarMessage.value = "Removed from Saved Homes"
        } else {
            current.add(propertyId)
            _snackbarMessage.value = "Saved to My Homes!"
        }
        _favoriteIds.value = current
    }

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun updateListingType(type: ListingType?) {
        _filterState.value = _filterState.value.copy(listingType = type)
    }

    fun updatePropertyType(type: PropertyType) {
        _filterState.value = _filterState.value.copy(propertyType = type)
    }

    fun updateBedrooms(bedrooms: Int) {
        _filterState.value = _filterState.value.copy(bedrooms = bedrooms)
    }

    fun updateRadius(radiusKm: Float) {
        _filterState.value = _filterState.value.copy(maxRadiusKm = radiusKm)
    }

    fun updateSortOrder(order: SortOrder) {
        _filterState.value = _filterState.value.copy(sortOrder = order)
    }

    fun resetFilters() {
        _filterState.value = FilterState()
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun postNewListing(
        title: String,
        tagline: String,
        price: Double,
        pricePeriod: String,
        listingType: ListingType,
        propertyType: PropertyType,
        bedrooms: Int,
        bathrooms: Int,
        areaSqM: Int,
        hasDsq: Boolean,
        neighborhood: String,
        cityOrCounty: String,
        latitude: Double,
        longitude: Double,
        description: String,
        amenities: List<String>,
        imageUrl: String,
        agentName: String,
        agentPhone: String,
        agentAgency: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val newProperty = Property(
                id = "prop_${System.currentTimeMillis()}",
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
                imageUrl = if (imageUrl.isNotBlank()) imageUrl else "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=1200&q=80",
                localDrawableName = "nairobi_villa_1790242690991",
                agentName = agentName,
                agentPhone = agentPhone,
                agentAgency = agentAgency,
                verifiedTitleDeed = true,
                featured = false
            )

            firestoreService.addProperty(newProperty)
            _isSubmitting.value = false
            _snackbarMessage.value = "🎉 Property listed live on Makao Firebase!"
            onSuccess()
        }
    }

    fun bookTour(
        property: Property,
        clientName: String,
        clientPhone: String,
        date: String,
        timeSlot: String,
        tourType: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val booking = TourBooking(
                propertyId = property.id,
                propertyTitle = property.title,
                propertyPrice = property.formattedPrice,
                propertyLocation = "${property.neighborhood}, ${property.cityOrCounty}",
                clientName = clientName,
                clientPhone = clientPhone,
                date = date,
                timeSlot = timeSlot,
                tourType = tourType,
                notes = notes
            )
            firestoreService.bookTour(booking)
            _snackbarMessage.value = "Viewing scheduled with agent ${property.agentName}!"
            onSuccess()
        }
    }
}
