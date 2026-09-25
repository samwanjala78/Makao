package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Property
import com.example.ui.theme.KenyaForestGreen
import com.example.ui.theme.KenyaGoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTourDialog(
    property: Property,
    onDismiss: () -> Unit,
    onBookTour: (clientName: String, clientPhone: String, date: String, timeSlot: String, tourType: String, notes: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedDate by remember { mutableStateOf("Tomorrow") }
    var selectedTimeSlot by remember { mutableStateOf("11:00 AM") }
    var selectedTourType by remember { mutableStateOf("In-Person Guided Tour") }
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

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
        ) {
            Text(
                text = "Schedule a Private Viewing",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${property.title} • ${property.formattedPrice}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = KenyaForestGreen
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tour Type
            Text("Tour Format", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("In-Person Guided Tour", "Live Video Walkthrough").forEach { format ->
                    FilterChip(
                        selected = selectedTourType == format,
                        onClick = { selectedTourType = format },
                        label = { Text(format, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KenyaForestGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Date Selection
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = KenyaForestGreen)
                Spacer(modifier = Modifier.height(4.dp))
                Text(" Select Date", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Today", "Tomorrow", "Saturday", "Sunday").forEach { d ->
                    FilterChip(
                        selected = selectedDate == d,
                        onClick = { selectedDate = d },
                        label = { Text(d, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KenyaForestGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time Slots
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = KenyaForestGreen)
                Spacer(modifier = Modifier.height(4.dp))
                Text(" Preferred Time", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("10:00 AM", "12:00 PM", "2:30 PM", "4:30 PM").forEach { slot ->
                    FilterChip(
                        selected = selectedTimeSlot == slot,
                        onClick = { selectedTimeSlot = slot },
                        label = { Text(slot, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KenyaGoldAccent,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = clientName,
                onValueChange = { clientName = it },
                label = { Text("Your Full Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tour_client_name"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = clientPhone,
                onValueChange = { clientPhone = it },
                label = { Text("Phone Number (+254...)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tour_client_phone"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val name = if (clientName.isNotBlank()) clientName else "Prospective Buyer"
                    val phone = if (clientPhone.isNotBlank()) clientPhone else "+254700000000"
                    onBookTour(name, phone, selectedDate, selectedTimeSlot, selectedTourType, notes)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_tour_booking_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KenyaForestGreen)
            ) {
                Text(
                    text = "Request Viewing with ${property.agentName}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
