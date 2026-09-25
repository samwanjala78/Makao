package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.UserLocation
import com.example.data.model.ListingType
import com.example.data.model.PropertyType
import com.example.ui.theme.KenyaForestGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPropertyDialog(
    userLocation: UserLocation,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
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
        agentAgency: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var listingType by remember { mutableStateOf(ListingType.FOR_SALE) }
    var propertyType by remember { mutableStateOf(PropertyType.APARTMENT) }
    var bedrooms by remember { mutableIntStateOf(3) }
    var bathrooms by remember { mutableIntStateOf(3) }
    var areaSqMText by remember { mutableStateOf("180") }
    var hasDsq by remember { mutableStateOf(true) }
    var neighborhood by remember { mutableStateOf("Kilimani") }
    var cityOrCounty by remember { mutableStateOf("Nairobi") }
    var description by remember { mutableStateOf("") }
    var agentName by remember { mutableStateOf("John Kamau") }
    var agentPhone by remember { mutableStateOf("+254712000111") }
    var agentAgency by remember { mutableStateOf("Kamau Premier Properties") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "List Property on Makao Firebase",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Real-time sync to all buyers across Kenya",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Listing Type
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = listingType == ListingType.FOR_SALE,
                    onClick = { listingType = ListingType.FOR_SALE },
                    label = { Text("For Sale") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KenyaForestGreen,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = listingType == ListingType.FOR_RENT,
                    onClick = { listingType = ListingType.FOR_RENT },
                    label = { Text("For Rent") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KenyaForestGreen,
                        selectedLabelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Listing Title (e.g. Royal Palms Executive Villa)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_prop_title"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = tagline,
                onValueChange = { tagline = it },
                label = { Text("Tagline / Headline (e.g. Luxury 4-Bed with DSQ & Pool)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text(if (listingType == ListingType.FOR_SALE) "Price in KES (e.g. 45000000)" else "Monthly Rent in KES (e.g. 120000)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_prop_price"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Property Type
            Text("Property Type", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(PropertyType.APARTMENT, PropertyType.VILLA, PropertyType.TOWNHOUSE, PropertyType.PENTHOUSE).forEach { type ->
                    FilterChip(
                        selected = propertyType == type,
                        onClick = { propertyType = type },
                        label = { Text(type.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Beds & Baths Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = bedrooms.toString(),
                    onValueChange = { bedrooms = it.toIntOrNull() ?: 1 },
                    label = { Text("Bedrooms") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = bathrooms.toString(),
                    onValueChange = { bathrooms = it.toIntOrNull() ?: 1 },
                    label = { Text("Bathrooms") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = areaSqMText,
                    onValueChange = { areaSqMText = it },
                    label = { Text("Area (m²)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Has DSQ Checkbox
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasDsq, onCheckedChange = { hasDsq = it })
                Spacer(modifier = Modifier.width(6.dp))
                Text("Includes Domestic Staff Quarters (DSQ)", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Location
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = neighborhood,
                    onValueChange = { neighborhood = it },
                    label = { Text("Neighborhood (e.g. Karen)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = cityOrCounty,
                    onValueChange = { cityOrCounty = it },
                    label = { Text("City / County") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Property Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Agent Info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = agentName,
                    onValueChange = { agentName = it },
                    label = { Text("Agent Name") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = agentPhone,
                    onValueChange = { agentPhone = it },
                    label = { Text("WhatsApp Phone") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val priceVal = priceText.toDoubleOrNull() ?: 20_000_000.0
                    val periodVal = if (listingType == ListingType.FOR_RENT) "/month" else ""
                    val areaVal = areaSqMText.toIntOrNull() ?: 180
                    onSubmit(
                        if (title.isNotBlank()) title else "Modern Executive Residence",
                        if (tagline.isNotBlank()) tagline else "Spacious living in premier neighborhood",
                        priceVal,
                        periodVal,
                        listingType,
                        propertyType,
                        bedrooms,
                        bathrooms,
                        areaVal,
                        hasDsq,
                        neighborhood,
                        cityOrCounty,
                        userLocation.latitude,
                        userLocation.longitude,
                        if (description.isNotBlank()) description else "Magnificent property with high-end finishes, 24/7 security, backup water and power.",
                        listOf("Borehole Water", "Solar Backup", "24/7 Guards", "Ample Parking"),
                        "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=1200&q=80",
                        agentName,
                        agentPhone,
                        agentAgency
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_property_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KenyaForestGreen),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.height(24.dp))
                } else {
                    Text(
                        text = "Publish to Live Firebase",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
