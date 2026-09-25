package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KenyaForestGreen
import com.example.ui.theme.KenyaGoldAccent
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

@Composable
fun MortgageCalculatorCard(
    propertyPrice: Double,
    modifier: Modifier = Modifier
) {
    var downPaymentPercent by remember { mutableFloatStateOf(20f) }
    var interestRatePercent by remember { mutableFloatStateOf(13.0f) } // Kenyan standard commercial mortgage rate
    var loanTermYears by remember { mutableIntStateOf(20) }

    val downPaymentAmount = propertyPrice * (downPaymentPercent / 100.0)
    val principalLoanAmount = propertyPrice - downPaymentAmount

    // Monthly payment formula
    val monthlyRate = (interestRatePercent / 100.0) / 12.0
    val totalMonths = loanTermYears * 12
    val monthlyPayment = if (monthlyRate > 0) {
        val factor = (1.0 + monthlyRate).pow(totalMonths.toDouble())
        principalLoanAmount * (monthlyRate * factor) / (factor - 1.0)
    } else {
        principalLoanAmount / totalMonths
    }

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kenya Mortgage Estimator",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = KenyaForestGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "CBK Benchmark ~13%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KenyaForestGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Highlighted Monthly Payment Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = KenyaForestGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ESTIMATED MONTHLY REPAYMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = KenyaGoldAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "KES ${numberFormat.format(monthlyPayment.toLong())} / mo",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    Text(
                        text = "Principal & Interest over $loanTermYears years",
                        fontSize = 11.sp,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Down Payment Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Down Payment (${downPaymentPercent.toInt()}%)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "KES ${numberFormat.format(downPaymentAmount.toLong())}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = downPaymentPercent,
                onValueChange = { downPaymentPercent = it },
                valueRange = 10f..50f,
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = KenyaForestGreen,
                    activeTrackColor = KenyaForestGreen
                )
            )

            // Interest Rate Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Bank Interest Rate",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${String.format(Locale.US, "%.1f", interestRatePercent)}% p.a.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = interestRatePercent,
                onValueChange = { interestRatePercent = it },
                valueRange = 9.0f..18.0f,
                steps = 17,
                colors = SliderDefaults.colors(
                    thumbColor = KenyaGoldAccent,
                    activeTrackColor = KenyaGoldAccent
                )
            )

            // Loan Term Selection
            Text(
                text = "Loan Term (Years)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10, 15, 20, 25).forEach { years ->
                    FilterChip(
                        selected = loanTermYears == years,
                        onClick = { loanTermYears = years },
                        label = { Text("$years yrs", fontSize = 12.sp) }
                    )
                }
            }
        }
    }
}
