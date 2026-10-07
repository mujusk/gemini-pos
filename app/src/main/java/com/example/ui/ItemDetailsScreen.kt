package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Invoice
import com.example.model.Product
import com.example.ui.theme.*
import com.example.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsScreen(
    product: Product,
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    val invoices by viewModel.invoices.collectAsState()
    val isAdjustStockOpen by viewModel.isAdjustStockDialogOpen.collectAsState()

    // Handle Android system back gesture/button
    BackHandler {
        viewModel.closeProductDetails()
    }

    // Filter transactions: find all invoices that contain this product
    data class TransactionItem(
        val invoice: Invoice,
        val timestamp: Long,
        val quantity: Int,
        val unitPrice: Double,
        val total: Double
    )

    val itemTransactions = remember(invoices, product.name) {
        val list = mutableListOf<TransactionItem>()
        for (inv in invoices) {
            val matchingItems = inv.items.filter { it.name == product.name }
            for (m in matchingItems) {
                list.add(
                    TransactionItem(
                        invoice = inv,
                        timestamp = inv.timestamp,
                        quantity = m.quantity,
                        unitPrice = m.price,
                        total = m.price * m.quantity
                    )
                )
            }
        }
        list.sortedByDescending { it.timestamp }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = product.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (product.barcode.isNotBlank()) "SKU: ${product.barcode} • ${product.category}" else product.category,
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeProductDetails() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Excel Export Action
                    IconButton(
                        onClick = {
                            Toast.makeText(
                                context,
                                "Exporting '${product.name}' transactions to Excel (.xlsx)...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = "Export Excel",
                            tint = EmeraldAccent
                        )
                    }
                    // Edit Product Action
                    IconButton(
                        onClick = {
                            viewModel.openEditProduct(product)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Item",
                            tint = BlueAccentLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureWhite,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                color = PureWhite,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { viewModel.openAdjustStockDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Adjust Stock",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Product photo or Canvas banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val bitmap = remember(product.imageBase64) {
                            product.imageBase64?.let { decodeBase64ToBitmap(it) }
                        }
                        if (bitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            HardwareIconCanvas(
                                name = product.name,
                                category = product.category,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Summary 4-Metrics Cards Grid (2x2)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Sale Price
                        SummaryMetricCard(
                            label = "Sale Price",
                            value = "₹${String.format("%,.2f", product.price)}",
                            subtext = "MRP: ₹${String.format("%,.2f", if (product.mrp > 0) product.mrp else product.price)}",
                            accentColor = EmeraldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        // Purchase Price
                        SummaryMetricCard(
                            label = "Purchase Price",
                            value = "₹${String.format("%,.2f", product.purchasePrice)}",
                            subtext = "Unit: ${product.unit}",
                            accentColor = BlueAccentLight,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // In Stock Count
                        SummaryMetricCard(
                            label = "In Stock",
                            value = "${product.stock} ${product.unit}",
                            subtext = if (product.stock <= 10) "Low Stock Alert" else "Stock Healthy",
                            accentColor = if (product.stock <= 10) RoseAccent else Slate200,
                            modifier = Modifier.weight(1f)
                        )
                        // Stock Value (Stock x Price)
                        val totalStockValue = product.stock * product.price
                        SummaryMetricCard(
                            label = "Stock Value",
                            value = "₹${String.format("%,.2f", totalStockValue)}",
                            subtext = "At selling price",
                            accentColor = AmberAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section Header: Stock Transactions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stock Transactions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${itemTransactions.size} Sales",
                        fontSize = 13.sp,
                        color = Slate400
                    )
                }
            }

            // Transaction History List
            if (itemTransactions.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No sales transactions recorded yet",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Sold quantities will automatically appear here",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            } else {
                items(itemTransactions) { tx ->
                    val formattedDate = remember(tx.timestamp) {
                        val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                        sdf.format(Date(tx.timestamp))
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.showInvoiceReceipt(tx.invoice)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tx.invoice.id,
                                        color = BlueAccent,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(EmeraldAccent.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "SALE",
                                            color = EmeraldAccent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formattedDate,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${String.format("%,.2f", tx.total)}",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${tx.quantity} ${product.unit} @ ₹${String.format("%.2f", tx.unitPrice)}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Quick Adjust Stock Dialog
    if (isAdjustStockOpen) {
        AdjustStockDialog(
            product = product,
            onDismiss = { viewModel.closeAdjustStockDialog() },
            onSave = { newStock ->
                viewModel.updateStock(product.id, newStock)
                viewModel.closeAdjustStockDialog()
            }
        )
    }
}

@Composable
fun SummaryMetricCard(
    label: String,
    value: String,
    subtext: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.border(1.dp, BorderLight, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = accentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun AdjustStockDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var stockValue by remember { mutableStateOf(product.stock.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Adjust Stock Quantity",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.name,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Stepper + / - controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = {
                            val cur = stockValue.toIntOrNull() ?: 0
                            val next = (cur - 1).coerceAtLeast(0)
                            stockValue = next.toString()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(SurfaceContainerLight, CircleShape)
                            .border(1.dp, BorderLight, CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TextPrimary)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    OutlinedTextField(
                        value = stockValue,
                        onValueChange = { stockValue = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BlueAccent,
                            unfocusedBorderColor = BorderLight,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = SurfaceContainerLight
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = {
                            val cur = stockValue.toIntOrNull() ?: 0
                            stockValue = (cur + 1).toString()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(BlueAccent, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val parsed = stockValue.toIntOrNull() ?: 0
                            onSave(parsed)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent)
                    ) {
                        Text("Save Stock", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
