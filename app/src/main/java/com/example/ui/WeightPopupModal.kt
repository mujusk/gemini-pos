package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Product
import com.example.ui.theme.*
import com.example.viewmodel.PosViewModel

@Composable
fun WeightPopupModal(
    product: Product,
    viewModel: PosViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Kilograms and Grams inputs
    var kgInput by remember { mutableStateOf("0") }
    var gramsInput by remember { mutableStateOf("250") }

    // Calculate total weight in kg
    val kgVal = kgInput.toDoubleOrNull() ?: 0.0
    val gramsVal = gramsInput.toDoubleOrNull() ?: 0.0
    val totalWeightKg = kgVal + (gramsVal / 1000.0)

    // Calculate exact proportional price
    val calculatedPrice = product.price * totalWeightKg

    // Human readable weight notation
    val weightNotation = remember(totalWeightKg, gramsVal, kgVal) {
        if (kgVal > 0 && gramsVal > 0) {
            "${kgVal.toInt()}kg ${gramsVal.toInt()}g"
        } else if (kgVal > 0) {
            "${if (kgVal % 1.0 == 0.0) kgVal.toInt().toString() else kgVal}kg"
        } else {
            "${gramsVal.toInt()}g"
        }
    }

    val quickPresets = listOf(
        "100g" to (0 to 100),
        "250g" to (0 to 250),
        "500g" to (0 to 500),
        "750g" to (0 to 750),
        "1 kg" to (1 to 0),
        "1.5 kg" to (1 to 500),
        "2 kg" to (2 to 0),
        "5 kg" to (5 to 0)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(BlueAccent.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Scale,
                                    contentDescription = "Scale",
                                    tint = BlueAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Enter Weight (Kg / Grams)",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Weight-Based Item Billing",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Product Details Banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLight),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Rate: ₹${String.format("%.2f", product.price)} per ${product.unit}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Surface(
                                color = EmeraldAccent.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "₹${String.format("%.2f", calculatedPrice)}",
                                    color = EmeraldAccent,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dual Weight Inputs: Kg & Grams
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Kilograms Input
                        OutlinedTextField(
                            value = kgInput,
                            onValueChange = {
                                if (it.all { ch -> ch.isDigit() || ch == '.' }) {
                                    kgInput = it
                                }
                            },
                            label = { Text("Kilograms (Kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BlueAccent,
                                unfocusedBorderColor = BorderLight,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = BlueAccent,
                                unfocusedLabelColor = TextSecondary,
                                focusedContainerColor = PureWhite,
                                unfocusedContainerColor = SurfaceContainerLight
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        // Grams Input
                        OutlinedTextField(
                            value = gramsInput,
                            onValueChange = {
                                if (it.all { ch -> ch.isDigit() }) {
                                    gramsInput = it
                                }
                            },
                            label = { Text("Grams (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BlueAccent,
                                unfocusedBorderColor = BorderLight,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = BlueAccent,
                                unfocusedLabelColor = TextSecondary,
                                focusedContainerColor = PureWhite,
                                unfocusedContainerColor = SurfaceContainerLight
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Chips Selection
                    Text(
                        text = "QUICK WEIGHT SELECTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2 rows of 4 chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickPresets.take(4).forEach { (label, values) ->
                                val (k, g) = values
                                val isSelected = kgInput == k.toString() && gramsInput == g.toString()
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BlueAccent else SurfaceContainerLight)
                                        .border(1.dp, if (isSelected) BlueAccent else BorderLight, RoundedCornerShape(8.dp))
                                        .clickable {
                                            kgInput = k.toString()
                                            gramsInput = g.toString()
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickPresets.drop(4).forEach { (label, values) ->
                                val (k, g) = values
                                val isSelected = kgInput == k.toString() && gramsInput == g.toString()
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BlueAccent else SurfaceContainerLight)
                                        .border(1.dp, if (isSelected) BlueAccent else BorderLight, RoundedCornerShape(8.dp))
                                        .clickable {
                                            kgInput = k.toString()
                                            gramsInput = g.toString()
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Calculation Summary Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLight),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Selected Weight", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    text = "$weightNotation (${String.format("%.3f", totalWeightKg)} kg)",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Rate Formula", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    text = "₹${product.price}/kg × ${String.format("%.3f", totalWeightKg)} kg",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Divider(color = BorderLight, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Calculated Total", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "₹${String.format("%.2f", calculatedPrice)}",
                                    color = EmeraldAccent,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, BorderLight),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (totalWeightKg <= 0.0) {
                                    Toast.makeText(context, "Please enter a valid weight", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val notation = "${product.name} - $weightNotation @ ₹${String.format("%.0f", product.price)}/kg"
                                viewModel.addWeightItemToCart(
                                    product = product,
                                    weightKg = totalWeightKg,
                                    weightDescription = notation
                                )
                                Toast.makeText(context, "Added $weightNotation to bill", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add to Bill (₹${String.format("%.2f", calculatedPrice)})", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
