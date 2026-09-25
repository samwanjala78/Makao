package com.example.data.model

enum class ListingType(val label: String) {
    FOR_SALE("For Sale"),
    FOR_RENT("For Rent")
}

enum class PropertyType(val label: String) {
    ALL("All Types"),
    APARTMENT("Apartment"),
    VILLA("Villa"),
    TOWNHOUSE("Townhouse"),
    PENTHOUSE("Penthouse"),
    COMMERCIAL("Commercial"),
    LAND("Land")
}

data class Property(
    val id: String = "",
    val title: String = "",
    val tagline: String = "",
    val price: Double = 0.0,
    val pricePeriod: String = "", // e.g. "/month" or ""
    val listingType: ListingType = ListingType.FOR_SALE,
    val propertyType: PropertyType = PropertyType.APARTMENT,
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val areaSqM: Int = 0,
    val parkingSpots: Int = 1,
    val hasDsq: Boolean = false, // Domestic Staff Quarters (prominent in Kenya)
    val neighborhood: String = "", // e.g. "Karen", "Kilimani", "Westlands"
    val cityOrCounty: String = "Nairobi",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val description: String = "",
    val amenities: List<String> = emptyList(),
    val imageUrl: String = "",
    val localDrawableName: String = "",
    val agentName: String = "Makao Premier Realty",
    val agentPhone: String = "+254712345678",
    val agentAgency: String = "Nairobi Prime Real Estate",
    val verifiedTitleDeed: Boolean = true,
    val featured: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    // Transient field calculated dynamically relative to user's geolocation:
    val distanceKm: Double? = null
) {
    val formattedPrice: String
        get() {
            return if (price >= 1_000_000) {
                val millions = price / 1_000_000.0
                if (millions % 1.0 == 0.0) {
                    "KES ${millions.toInt()}M${if (pricePeriod.isNotBlank()) " $pricePeriod" else ""}"
                } else {
                    "KES ${String.format("%.1f", millions)}M${if (pricePeriod.isNotBlank()) " $pricePeriod" else ""}"
                }
            } else if (price >= 1_000) {
                val thousands = price / 1_000.0
                if (thousands % 1.0 == 0.0) {
                    "KES ${thousands.toInt()}K${if (pricePeriod.isNotBlank()) " $pricePeriod" else ""}"
                } else {
                    "KES ${String.format("%.0f", thousands)}K${if (pricePeriod.isNotBlank()) " $pricePeriod" else ""}"
                }
            } else {
                "KES ${price.toInt()}"
            }
        }

    val fullFormattedPrice: String
        get() {
            val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(price.toLong())
            return "KES $formatted${if (pricePeriod.isNotBlank()) " $pricePeriod" else ""}"
        }

    val formattedDistance: String
        get() {
            val dist = distanceKm ?: return ""
            return if (dist < 1.0) {
                "${(dist * 1000).toInt()}m away"
            } else {
                "${String.format("%.1f", dist)} km away"
            }
        }
}

data class PresetLocation(
    val id: String,
    val name: String,
    val county: String,
    val latitude: Double,
    val longitude: Double,
    val description: String
)

data class TourBooking(
    val id: String = java.util.UUID.randomUUID().toString(),
    val propertyId: String,
    val propertyTitle: String,
    val propertyPrice: String,
    val propertyLocation: String,
    val clientName: String,
    val clientPhone: String,
    val date: String,
    val timeSlot: String,
    val tourType: String = "In-Person Guided Tour",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
