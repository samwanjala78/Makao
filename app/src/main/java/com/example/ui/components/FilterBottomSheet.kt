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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ListingType
import com.example.data.model.PropertyType
import com.example.ui.theme.KenyaForestGreen
import com.example.ui.theme.KenyaGoldAccent
import com.example.ui.viewmodel.FilterState
import com.example.ui.viewmodel.SortOrder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    filterState: FilterState,
    totalMatchingCount: Int,
    onListingTypeChange: (ListingType?) -> Unit,
    onPropertyTypeChange: (PropertyType) -> Unit,
    onBedroomsChange: (Int) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Homes",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedButton(
                    onClick = onResetFilters,
                    modifier = Modifier.testTag("reset_filters_button")
                ) {
                    Text("Reset", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Listing Type (All, For Sale, For Rent)
            Text(
                text = "Listing Purpose",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterState.listingType == null,
                    onClick = { onListingTypeChange(null) },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KenyaForestGreen,
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White
                    )
                )
                FilterChip(
                    selected = filterState.listingType == ListingType.FOR_SALE,
                    onClick = { onListingTypeChange(ListingType.FOR_SALE) },
                    label = { Text("For Sale") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KenyaForestGreen,
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White
                    )
                )
                FilterChip(
                    selected = filterState.listingType == ListingType.FOR_RENT,
                    onClick = { onListingTypeChange(ListingType.FOR_RENT) },
                    label = { Text("For Rent") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KenyaForestGreen,
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Geolocation Nearby Radius
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Nearby Geolocation Radius",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (filterState.maxRadiusKm == 0f) "Any Distance" else "Within ${filterState.maxRadiusKm.toInt()} km",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = KenyaForestGreen
                )
            }
            Slider(
                value = filterState.maxRadiusKm,
                onValueChange = onRadiusChange,
                valueRange = 0f..100f,
                steps = 9, // 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100
                colors = SliderDefaults.colors(
                    thumbColor = KenyaForestGreen,
                    activeTrackColor = KenyaForestGreen
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Property Type Chips
            Text(
                text = "Property Type",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PropertyType.values().forEach { type ->
                    FilterChip(
                        selected = filterState.propertyType == type,
                        onClick = { onPropertyTypeChange(type) },
                        label = { Text(type.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KenyaForestGreen,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Bedrooms
            Text(
                text = "Bedrooms",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to "Any", 1 to "1+", 2 to "2+", 3 to "3+", 4 to "4+", 5 to "5+").forEach { (count, label) ->
                    FilterChip(
                        selected = filterState.bedrooms == count,
                        onClick = { onBedroomsChange(count) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KenyaGoldAccent,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Sort By
            Text(
                text = "Sort Results By",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortOrder.values().forEach { order ->
                    FilterChip(
                        selected = filterState.sortOrder == order,
                        onClick = { onSortOrderChange(order) },
                        label = { Text(order.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KenyaForestGreen,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Apply Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("apply_filters_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KenyaForestGreen)
            ) {
                Text(
                    text = "Show $totalMatchingCount Homes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
