package com.example.data.firebase

import android.util.Log
import com.example.data.model.ListingType
import com.example.data.model.Property
import com.example.data.model.PropertyType
import com.example.data.model.TourBooking
import com.example.data.seed.KenyaPropertySeed
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreService {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore not initialized or google-services missing, using offline local sync: ${e.message}")
            null
        }
    }

    // In-memory cache for seamless real-time fallback
    private val localFallbackProperties = mutableListOf<Property>().apply {
        addAll(KenyaPropertySeed.getInitialProperties())
    }

    private val localBookings = mutableListOf<TourBooking>()

    fun getPropertiesFlow(): Flow<List<Property>> = callbackFlow {
        val db = firestore
        if (db == null) {
            // Emits local fallback and remains active
            trySend(localFallbackProperties.toList())
            awaitClose { }
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            val collection = db.collection(COLLECTION_PROPERTIES)
            
            // First check if collection is empty, if so seed initial Kenya listings
            collection.limit(1).get().addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    seedInitialData(db)
                }
            }

            registration = collection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore snapshot listener error: ${error.message}")
                    // On error, emit local fallback so UI remains fully functional
                    trySend(localFallbackProperties.toList())
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val properties = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            val listingTypeStr = data["listingType"] as? String ?: ListingType.FOR_SALE.name
                            val propertyTypeStr = data["propertyType"] as? String ?: PropertyType.APARTMENT.name
                            
                            val listingType = try {
                                ListingType.valueOf(listingTypeStr)
                            } catch (e: Exception) {
                                ListingType.FOR_SALE
                            }

                            val propertyType = try {
                                PropertyType.valueOf(propertyTypeStr)
                            } catch (e: Exception) {
                                PropertyType.APARTMENT
                            }

                            @Suppress("UNCHECKED_CAST")
                            val amenities = (data["amenities"] as? List<String>) ?: emptyList()

                            Property(
                                id = doc.id,
                                title = data["title"] as? String ?: "",
                                tagline = data["tagline"] as? String ?: "",
                                price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                                pricePeriod = data["pricePeriod"] as? String ?: "",
                                listingType = listingType,
                                propertyType = propertyType,
                                bedrooms = (data["bedrooms"] as? Number)?.toInt() ?: 0,
                                bathrooms = (data["bathrooms"] as? Number)?.toInt() ?: 0,
                                areaSqM = (data["areaSqM"] as? Number)?.toInt() ?: 0,
                                parkingSpots = (data["parkingSpots"] as? Number)?.toInt() ?: 1,
                                hasDsq = data["hasDsq"] as? Boolean ?: false,
                                neighborhood = data["neighborhood"] as? String ?: "",
                                cityOrCounty = data["cityOrCounty"] as? String ?: "Nairobi",
                                latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                                longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                                description = data["description"] as? String ?: "",
                                amenities = amenities,
                                imageUrl = data["imageUrl"] as? String ?: "",
                                localDrawableName = data["localDrawableName"] as? String ?: "",
                                agentName = data["agentName"] as? String ?: "Makao Kenya Agent",
                                agentPhone = data["agentPhone"] as? String ?: "+254700000000",
                                agentAgency = data["agentAgency"] as? String ?: "Makao Properties",
                                verifiedTitleDeed = data["verifiedTitleDeed"] as? Boolean ?: true,
                                featured = data["featured"] as? Boolean ?: false,
                                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing property doc: ${doc.id}", e)
                            null
                        }
                    }
                    // Keep fallback in sync
                    localFallbackProperties.clear()
                    localFallbackProperties.addAll(properties)
                    trySend(properties)
                } else {
                    // Empty or still seeding
                    trySend(localFallbackProperties.toList())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach snapshot listener", e)
            trySend(localFallbackProperties.toList())
        }

        awaitClose {
            registration?.remove()
        }
    }

    suspend fun addProperty(property: Property): Result<Unit> {
        return try {
            val db = firestore
            val propertyId = if (property.id.isNotBlank()) property.id else "prop_${System.currentTimeMillis()}"
            val propertyToSave = property.copy(id = propertyId)

            // Update in local fallback immediately
            val existingIndex = localFallbackProperties.indexOfFirst { it.id == propertyId }
            if (existingIndex >= 0) {
                localFallbackProperties[existingIndex] = propertyToSave
            } else {
                localFallbackProperties.add(0, propertyToSave)
            }

            if (db != null) {
                val data = hashMapOf(
                    "id" to propertyToSave.id,
                    "title" to propertyToSave.title,
                    "tagline" to propertyToSave.tagline,
                    "price" to propertyToSave.price,
                    "pricePeriod" to propertyToSave.pricePeriod,
                    "listingType" to propertyToSave.listingType.name,
                    "propertyType" to propertyToSave.propertyType.name,
                    "bedrooms" to propertyToSave.bedrooms,
                    "bathrooms" to propertyToSave.bathrooms,
                    "areaSqM" to propertyToSave.areaSqM,
                    "parkingSpots" to propertyToSave.parkingSpots,
                    "hasDsq" to propertyToSave.hasDsq,
                    "neighborhood" to propertyToSave.neighborhood,
                    "cityOrCounty" to propertyToSave.cityOrCounty,
                    "latitude" to propertyToSave.latitude,
                    "longitude" to propertyToSave.longitude,
                    "description" to propertyToSave.description,
                    "amenities" to propertyToSave.amenities,
                    "imageUrl" to propertyToSave.imageUrl,
                    "localDrawableName" to propertyToSave.localDrawableName,
                    "agentName" to propertyToSave.agentName,
                    "agentPhone" to propertyToSave.agentPhone,
                    "agentAgency" to propertyToSave.agentAgency,
                    "verifiedTitleDeed" to propertyToSave.verifiedTitleDeed,
                    "featured" to propertyToSave.featured,
                    "createdAt" to propertyToSave.createdAt
                )
                db.collection(COLLECTION_PROPERTIES).document(propertyId).set(data, SetOptions.merge()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding property to Firestore: ${e.message}")
            // Even if remote network fails, local add succeeded
            Result.success(Unit)
        }
    }

    suspend fun bookTour(booking: TourBooking): Result<Unit> {
        return try {
            localBookings.add(0, booking)
            val db = firestore
            if (db != null) {
                val data = hashMapOf(
                    "id" to booking.id,
                    "propertyId" to booking.propertyId,
                    "propertyTitle" to booking.propertyTitle,
                    "propertyPrice" to booking.propertyPrice,
                    "propertyLocation" to booking.propertyLocation,
                    "clientName" to booking.clientName,
                    "clientPhone" to booking.clientPhone,
                    "date" to booking.date,
                    "timeSlot" to booking.timeSlot,
                    "tourType" to booking.tourType,
                    "notes" to booking.notes,
                    "timestamp" to booking.timestamp
                )
                db.collection(COLLECTION_TOURS).document(booking.id).set(data).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error booking tour: ${e.message}")
            Result.success(Unit)
        }
    }

    fun getLocalBookings(): List<TourBooking> = localBookings.toList()

    private fun seedInitialData(db: FirebaseFirestore) {
        val initialList = KenyaPropertySeed.getInitialProperties()
        val batch = db.batch()
        for (prop in initialList) {
            val docRef = db.collection(COLLECTION_PROPERTIES).document(prop.id)
            val data = hashMapOf(
                "id" to prop.id,
                "title" to prop.title,
                "tagline" to prop.tagline,
                "price" to prop.price,
                "pricePeriod" to prop.pricePeriod,
                "listingType" to prop.listingType.name,
                "propertyType" to prop.propertyType.name,
                "bedrooms" to prop.bedrooms,
                "bathrooms" to prop.bathrooms,
                "areaSqM" to prop.areaSqM,
                "parkingSpots" to prop.parkingSpots,
                "hasDsq" to prop.hasDsq,
                "neighborhood" to prop.neighborhood,
                "cityOrCounty" to prop.cityOrCounty,
                "latitude" to prop.latitude,
                "longitude" to prop.longitude,
                "description" to prop.description,
                "amenities" to prop.amenities,
                "imageUrl" to prop.imageUrl,
                "localDrawableName" to prop.localDrawableName,
                "agentName" to prop.agentName,
                "agentPhone" to prop.agentPhone,
                "agentAgency" to prop.agentAgency,
                "verifiedTitleDeed" to prop.verifiedTitleDeed,
                "featured" to prop.featured,
                "createdAt" to prop.createdAt
            )
            batch.set(docRef, data)
        }
        batch.commit().addOnSuccessListener {
            Log.d(TAG, "Successfully seeded initial Kenya properties to Firestore")
        }.addOnFailureListener { e ->
            Log.w(TAG, "Failed to seed properties: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "FirestoreService"
        private const val COLLECTION_PROPERTIES = "makao_properties"
        private const val COLLECTION_TOURS = "makao_tour_bookings"
    }
}
