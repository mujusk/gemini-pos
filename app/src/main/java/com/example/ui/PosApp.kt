package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CartItem
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.Product
import com.example.ui.theme.*
import com.example.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosApp(viewModel: PosViewModel) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val currentTab by viewModel.currentTab.collectAsState()
    val cart by viewModel.currentCart.collectAsState()
    val isCartOpen by viewModel.isCartDrawerOpen.collectAsState()
    val activeInvoice by viewModel.activeInvoiceReceipt.collectAsState()
    val isCalcOpen by viewModel.isCalculatorModalOpen.collectAsState()
    val isAddProductOpen by viewModel.isAddProductModalOpen.collectAsState()

    Scaffold(
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
                NavigationBar(
                    containerColor = PureWhite,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .height(64.dp)
                        .padding(bottom = 4.dp)
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { viewModel.setTab(0) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("HOME", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BlueAccent,
                            selectedTextColor = BlueAccent,
                            indicatorColor = BlueAccent.copy(alpha = 0.12f),
                            unselectedIconColor = Slate500,
                            unselectedTextColor = Slate500
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { viewModel.setTab(1) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 1) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                                contentDescription = "Catalog",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("CATALOG", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BlueAccent,
                            selectedTextColor = BlueAccent,
                            indicatorColor = BlueAccent.copy(alpha = 0.12f),
                            unselectedIconColor = Slate500,
                            unselectedTextColor = Slate500
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { viewModel.setTab(2) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 2) Icons.Filled.ListAlt else Icons.Outlined.ListAlt,
                                contentDescription = "Items",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("ITEMS", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BlueAccent,
                            selectedTextColor = BlueAccent,
                            indicatorColor = BlueAccent.copy(alpha = 0.12f),
                            unselectedIconColor = Slate500,
                            unselectedTextColor = Slate500
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (isLandscape) {
                LandscapePosLayout(viewModel = viewModel)
            } else {
                // Main content based on current tab
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "MainTabsTransitions"
                ) { tab ->
                    when (tab) {
                        0 -> HomeScreen(viewModel)
                        1 -> CatalogScreen(viewModel)
                        2 -> ItemsScreen(viewModel)
                    }
                }

                // Floating bottom cart drawer / bar
                if (currentTab == 1 && cart.isNotEmpty()) {
                    val itemCount = cart.sumOf { it.quantity }
                    val subtotal = cart.sumOf { it.total }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Card(
                            onClick = { viewModel.setCartDrawerOpen(true) },
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .border(1.5.dp, BlueAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Badge(
                                        containerColor = BlueAccent,
                                        contentColor = Color.White,
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = itemCount.toString(),
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Active Bill",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "₹${String.format("%,.2f", subtotal)}",
                                            color = TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "View Bill",
                                        color = BlueAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = BlueAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: 13-Button Calculator
    if (isCalcOpen && !isLandscape) {
        CalculatorModal(viewModel)
    }

    // Drawer: View & Edit Active Bill
    if (isCartOpen && !isLandscape) {
        CartDrawer(viewModel)
    }

    // Item Details Screen (Screen 1)
    val selectedProductForDetails by viewModel.selectedProductForDetails.collectAsState()
    selectedProductForDetails?.let { product: Product ->
        ItemDetailsScreen(product = product, viewModel = viewModel)
    }

    // Edit & Create Full Form Screen (Screen 2)
    val isEditProductOpen by viewModel.isEditProductScreenOpen.collectAsState()
    val editingProduct by viewModel.editingProduct.collectAsState()
    if (isEditProductOpen) {
        EditProductScreen(editingProduct = editingProduct, viewModel = viewModel)
    }

    // Modal: Invoice Receipt Slip
    activeInvoice?.let { invoice ->
        ReceiptModal(invoice = invoice, viewModel = viewModel)
    }

    // Modal: Barcode Scanner
    val isBarcodeScannerOpen by viewModel.isBarcodeScannerOpen.collectAsState()
    if (isBarcodeScannerOpen) {
        BarcodeScannerModal(
            viewModel = viewModel,
            onDismiss = { viewModel.setBarcodeScannerOpen(false) }
        )
    }

    // Modal: Weight-Based Item Popup
    val weightPopupProduct by viewModel.weightPopupProduct.collectAsState()
    weightPopupProduct?.let { product ->
        WeightPopupModal(
            product = product,
            viewModel = viewModel,
            onDismiss = { viewModel.openWeightPopup(null) }
        )
    }

    // Modal: 58mm Thermal Sticker Printing
    val thermalStickerProduct by viewModel.thermalStickerProduct.collectAsState()
    thermalStickerProduct?.let { product ->
        ThermalStickerModal(
            product = product,
            onDismiss = { viewModel.openThermalStickerModal(null) }
        )
    }
}

// ==========================================
// IMAGE & BASE64 PERSISTENCE HELPERS
// ==========================================
fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        if (bitmap == null) return null
        val maxDim = 400
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else bitmap
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val byteArray = outputStream.toByteArray()
        Base64.encodeToString(byteArray, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}

// ==========================================
// BROWSER INTENT & AUTO-LANDSCAPE PC POS
// ==========================================
fun openInBrowser(context: Context) {
    try {
        val appUrl = "https://mujusk.github.io/gemini-pos/"
        
        // Copy live URL to clipboard for user convenience
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Gemini POS Web App", appUrl)
        clipboard?.setPrimaryClip(clip)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.android.chrome")
        }
        try {
            context.startActivity(intent)
            Toast.makeText(context, "Opening Gemini POS in Chrome...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(appUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
            Toast.makeText(context, "Opening Gemini POS...", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Opening web app: $e", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun LandscapePosLayout(viewModel: PosViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val cart by viewModel.currentCart.collectAsState()
    val subtotal = cart.sumOf { it.total }
    var isCalculatorOpenInLandscape by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Landscape Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureWhite)
                .border(BorderStroke(1.dp, BorderLight))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(BlueAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hardware,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gemini POS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setBarcodeScannerOpen(true) }) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Barcode Scanner",
                        tint = BlueAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { openInBrowser(context) }) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = "Open in browser",
                        tint = TextSecondary
                    )
                }
            }
        }

        // Side-by-side desktop layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // LEFT SIDE: Catalog Grid & Search (or Dashboard/Items)
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .background(PureWhite, RoundedCornerShape(14.dp))
                    .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
            ) {
                when (currentTab) {
                    0 -> HomeScreen(viewModel)
                    1 -> CatalogScreen(viewModel, isLandscape = true)
                    2 -> ItemsScreen(viewModel)
                }
            }

            // RIGHT SIDE: The active bill/cart and the collapsible 13-button calculator
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
                    .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    // Active Bill Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = BlueAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active Bill",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = BlueAccent) {
                                Text(
                                    text = cart.sumOf { it.quantity }.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Calculator toggle pill in header
                            Surface(
                                onClick = { isCalculatorOpenInLandscape = !isCalculatorOpenInLandscape },
                                color = if (isCalculatorOpenInLandscape) BlueAccent.copy(alpha = 0.15f) else SurfaceContainerLight,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isCalculatorOpenInLandscape) BlueAccent else BorderLight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = "Calculator",
                                        tint = if (isCalculatorOpenInLandscape) BlueAccent else TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isCalculatorOpenInLandscape) "Close Calc" else "Calculator",
                                        color = if (isCalculatorOpenInLandscape) BlueAccent else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (cart.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Clear",
                                    color = RoseAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { viewModel.clearCart() }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Scrollable Cart Items (full height when calculator is collapsed)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(SurfaceContainerLight, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderLight, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        if (cart.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Outlined.ShoppingCart,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Cart is empty. Tap items on the left to add.",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(cart) { item ->
                                    LandscapeCartItemRow(
                                        item = item,
                                        onAdd = { viewModel.increaseCartQuantity(item.product) },
                                        onMinus = { viewModel.decreaseCartQuantity(item.product) },
                                        onRemove = { viewModel.removeFromCart(item.product) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Total & Checkout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total", color = TextSecondary, fontSize = 10.sp)
                            Text(
                                text = "₹${String.format("%,.2f", subtotal)}",
                                color = EmeraldAccent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quick Calculator icon button next to Save Bill
                            IconButton(
                                onClick = { isCalculatorOpenInLandscape = !isCalculatorOpenInLandscape },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        if (isCalculatorOpenInLandscape) BlueAccent else SurfaceContainerLight,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, BorderLight, RoundedCornerShape(8.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "Calculator",
                                    tint = if (isCalculatorOpenInLandscape) Color.White else BlueAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { viewModel.checkout() },
                                enabled = cart.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Bill", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Collapsible Docked 13-Button Calculator (hidden/collapsed by default)
                    AnimatedVisibility(
                        visible = isCalculatorOpenInLandscape,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Divider(color = BorderLight, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                            Docked13ButtonCalculator(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LandscapeCartItemRow(
    item: CartItem,
    onAdd: () -> Unit,
    onMinus: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = item.product.name,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "₹${String.format("%.2f", item.product.price)} each",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            // Quantity controls
            Row(
                modifier = Modifier.weight(1.1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMinus,
                    modifier = Modifier
                        .size(22.dp)
                        .background(SurfaceContainerLight, CircleShape)
                        .border(1.dp, BorderLight, CircleShape)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Minus", tint = TextPrimary, modifier = Modifier.size(12.dp))
                }

                Text(
                    text = item.quantity.toString(),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .size(22.dp)
                        .background(BlueAccent, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Plus", tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }

            // Price and remove
            Column(
                modifier = Modifier.weight(0.9f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "₹${String.format("%.2f", item.total)}",
                    color = EmeraldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Remove",
                    color = RoseAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onRemove() }
                )
            }
        }
    }
}

@Composable
fun Docked13ButtonCalculator(viewModel: PosViewModel) {
    val money by viewModel.calculatorMoney.collectAsState()
    val qty by viewModel.calculatorQuantity.collectAsState()
    val isMoneyFocused by viewModel.isCalculatorMoneyFocused.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLight, RoundedCornerShape(12.dp))
            .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        // Display Boxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Money Box
            Card(
                onClick = { viewModel.setMoneyFocus(true) },
                colors = CardDefaults.cardColors(
                    containerColor = PureWhite
                ),
                modifier = Modifier
                    .weight(1.2f)
                    .height(44.dp)
                    .border(
                        width = 1.5.dp,
                        color = if (isMoneyFocused) BlueAccent else BorderLight,
                        shape = RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("₹", fontSize = 11.sp, color = if (isMoneyFocused) BlueAccent else TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = "₹$money",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quantity Box
            Card(
                onClick = { viewModel.setMoneyFocus(false) },
                colors = CardDefaults.cardColors(
                    containerColor = PureWhite
                ),
                modifier = Modifier
                    .weight(0.8f)
                    .height(44.dp)
                    .border(
                        width = 1.5.dp,
                        color = if (!isMoneyFocused) BlueAccent else BorderLight,
                        shape = RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Qty", fontSize = 11.sp, color = if (!isMoneyFocused) BlueAccent else TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = qty,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 13 Keys Keypad
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Left Grid of 12 Keys: 0-9, Decimal, DEL
            Column(
                modifier = Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val keyRows = listOf(
                    listOf("7", "8", "9"),
                    listOf("4", "5", "6"),
                    listOf("1", "2", "3"),
                    listOf("0", ".", "DEL")
                )

                for (row in keyRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        for (key in row) {
                            val isDel = key == "DEL"
                            Card(
                                onClick = { viewModel.handleCalculatorPress(key) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDel) RoseAccent.copy(alpha = 0.12f) else PureWhite
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .border(1.dp, BorderLight, RoundedCornerShape(8.dp))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDel) RoseAccent else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right Side ADD Button (Spans full height of the 4 rows!)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(151.dp)
            ) {
                Button(
                    onClick = { viewModel.handleCalculatorPress("ADD") },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ADD",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCENE 1: HOME SCREEN (Dashboard & Invoices)
// ==========================================
@Composable
fun HomeScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val query by viewModel.searchQuery.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val salesTotal by viewModel.todaySales.collectAsState(initial = 0.0)
    val totalInvs by viewModel.totalInvoices.collectAsState(initial = 0)
    val totalItems by viewModel.totalItemsSold.collectAsState(initial = 0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header (Compact & Sleek)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Logo Canvas
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(BlueAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hardware,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Fast Barcode Scanner Header Action
                    IconButton(onClick = { viewModel.setBarcodeScannerOpen(true) }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Barcode Scanner",
                            tint = BlueAccentLight,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = { openInBrowser(context) }) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = "Open in browser",
                            tint = Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = {}) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = RoseAccent) {
                                    Text("2", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Slate400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Analytics Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Today's Sales Hero Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Graph Sparkline Decorative Background
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            val path = Path()
                            path.moveTo(0f, size.height * 0.8f)
                            path.quadraticTo(
                                size.width * 0.25f, size.height * 0.6f,
                                size.width * 0.5f, size.height * 0.4f
                            )
                            path.quadraticTo(
                                size.width * 0.75f, size.height * 0.7f,
                                size.width, size.height * 0.2f
                            )
                            drawPath(
                                path = path,
                                color = BlueAccent.copy(alpha = 0.15f),
                                style = Stroke(width = 6f)
                            )
                        }

                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Today's Total Sales",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(BlueAccent.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = BlueAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "₹${String.format("%,.2f", salesTotal)}",
                                color = TextPrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Two smaller metric cards side-by-side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Invoices", color = TextSecondary, fontSize = 12.sp)
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = totalInvs.toString(),
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Items Sold", color = TextSecondary, fontSize = 12.sp)
                                Icon(
                                    imageVector = Icons.Default.Inventory,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = totalItems.toString(),
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Recent Invoices Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Invoices",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = {}) {
                    Text("Show all", color = BlueAccentLight)
                }
            }
        }

        // Recent Invoices List
        if (invoices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No invoices generated yet",
                            color = Slate400,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(invoices) { invoice ->
                InvoiceCard(invoice = invoice) {
                    viewModel.showInvoiceReceipt(invoice)
                }
            }
        }
    }
}

@Composable
fun InvoiceCard(invoice: Invoice, onClick: () -> Unit) {
    val formattedTime = remember(invoice.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        sdf.format(Date(invoice.timestamp))
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceContainerLight, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = BlueAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = invoice.id,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$formattedTime • ${invoice.items.sumOf { it.quantity }} items",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            Text(
                text = "₹${String.format("%,.2f", invoice.totalAmount)}",
                color = EmeraldAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==========================================
// SCENE 2: CATALOG SCREEN (Fast-Billing & Grid)
// ==========================================
@Composable
fun CatalogScreen(viewModel: PosViewModel, isLandscape: Boolean = false) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val filteredList by viewModel.filteredProducts.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = if (isLandscape) 4.dp else 10.dp)
    ) {
        if (!isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(BlueAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hardware,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Barcode Scanner button
                    IconButton(onClick = { viewModel.setBarcodeScannerOpen(true) }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = BlueAccentLight,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = { openInBrowser(context) }) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = "Open in browser",
                            tint = Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Search & Fast Calculator row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search items or code...", color = Slate400, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = BlueAccent,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite
                ),
                singleLine = true
            )

            // 13-Button Manual Fast Calculator trigger button
            IconButton(
                onClick = { viewModel.setCalculatorModalOpen(true) },
                modifier = Modifier
                    .size(52.dp)
                    .background(BlueAccent, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Fast Calculator",
                    tint = Color.White
                )
            }
        }

        // Horizontal Category Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) BlueAccent else PureWhite)
                        .border(
                            1.dp,
                            if (isSelected) BlueAccent else BorderLight,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.setCategory(category) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Product Grid
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Slate600,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No products found",
                        color = Slate400,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = if (isLandscape) 16.dp else 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredList) { product ->
                    ProductCard(product = product) {
                        if (product.unit.equals("Kg", ignoreCase = true)) {
                            viewModel.openWeightPopup(product)
                        } else {
                            viewModel.addToCart(product)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product, onAddClick: () -> Unit) {
    val isOutOfStock = product.stock <= 0
    val sizeBadge = remember(product.name) {
        // Extract size e.g. 3/4" or 1"
        val regex = "(\\d+/\\d+\"|\\d+\")".toRegex()
        regex.find(product.name)?.value ?: ""
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isOutOfStock) RoseAccent.copy(alpha = 0.3f) else BorderLight,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Visual Product Icon Canvas placeholder with size badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(SurfaceContainerLight, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val photoBmp = remember(product.imageBase64) {
                    product.imageBase64?.let { decodeBase64ToBitmap(it) }
                }
                if (photoBmp != null) {
                    androidx.compose.foundation.Image(
                        bitmap = photoBmp.asImageBitmap(),
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Render beautiful customized vector graphic on canvas
                    HardwareIconCanvas(
                        name = product.name,
                        category = product.category,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Size Badge
                if (sizeBadge.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(BlueAccent, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = sizeBadge,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = product.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(40.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Stock tag
            if (isOutOfStock) {
                Text(
                    text = "Out of Stock",
                    color = RoseAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "Stock: ${product.stock}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Price & Quick Tap Add row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${String.format("%.2f", product.price)}",
                    color = EmeraldAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onAddClick,
                    enabled = !isOutOfStock,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isOutOfStock) SurfaceContainerLight else BlueAccent,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to cart",
                        tint = if (isOutOfStock) Slate400 else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Draw professional stylized representations of hardware items on Canvas
@Composable
fun HardwareIconCanvas(name: String, category: String, modifier: Modifier = Modifier) {
    val isBrass = name.contains("Brass", ignoreCase = true)
    val isTeflon = name.contains("Teflon", ignoreCase = true)
    val isSolution = name.contains("Solution", ignoreCase = true)
    val isElbow = name.contains("Elbow", ignoreCase = true)
    val isCoupler = name.contains("Coupler", ignoreCase = true)
    val isEndCap = name.contains("End Cap", ignoreCase = true)

    val color = when {
        isBrass -> AmberAccent
        isTeflon -> BlueAccentLight
        isSolution -> EmeraldAccent
        else -> Slate200 // Default white/CPVC gray
    }

    Canvas(modifier = modifier.padding(16.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 3f

        when {
            isElbow -> {
                // Draw L-shaped pipe elbow
                val path = Path().apply {
                    moveTo(cx - radius * 0.5f, cy - radius * 0.9f)
                    lineTo(cx + radius * 0.5f, cy - radius * 0.9f)
                    lineTo(cx + radius * 0.5f, cy + radius * 0.1f)
                    lineTo(cx - radius * 0.9f, cy + radius * 0.1f)
                    lineTo(cx - radius * 0.9f, cy - radius * 0.9f)
                }
                // Custom drawn pipe stroke
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.2f)
                )
                drawCircle(color, radius = radius * 0.35f, center = Offset(cx - radius * 0.2f, cy - radius * 0.4f))
            }
            isCoupler -> {
                // Draw Cylinder coupler
                drawRoundRect(
                    color = color.copy(alpha = 0.3f),
                    topLeft = Offset(cx - radius * 0.4f, cy - radius * 0.8f),
                    size = Size(radius * 0.8f, radius * 1.6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
                // Draw details lines
                drawLine(
                    color = color,
                    start = Offset(cx - radius * 0.4f, cy),
                    end = Offset(cx + radius * 0.4f, cy),
                    strokeWidth = 4f
                )
            }
            isEndCap -> {
                // Cylinder cap
                drawRoundRect(
                    color = color.copy(alpha = 0.3f),
                    topLeft = Offset(cx - radius * 0.5f, cy - radius * 0.5f),
                    size = Size(radius * 1.0f, radius * 0.9f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
                drawCircle(color, radius = radius * 0.25f, center = Offset(cx, cy - radius * 0.1f))
            }
            isTeflon -> {
                // Outer ring
                drawCircle(
                    color = color,
                    radius = radius * 0.9f,
                    style = Stroke(width = 8f)
                )
                // Inner spool
                drawCircle(
                    color = color.copy(alpha = 0.4f),
                    radius = radius * 0.5f
                )
            }
            isSolution -> {
                // Solution Can
                drawRoundRect(
                    color = color.copy(alpha = 0.2f),
                    topLeft = Offset(cx - radius * 0.5f, cy - radius * 0.5f),
                    size = Size(radius * 1.0f, radius * 1.2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                // Can neck/lid
                drawRect(
                    color = color,
                    topLeft = Offset(cx - radius * 0.2f, cy - radius * 0.8f),
                    size = Size(radius * 0.4f, radius * 0.3f)
                )
            }
            else -> {
                // Generic hardware shape (gear/bolt representation)
                drawCircle(
                    color = color.copy(alpha = 0.3f),
                    radius = radius * 0.8f
                )
                drawCircle(
                    color = Slate900,
                    radius = radius * 0.3f
                )
            }
        }
    }
}

// ==========================================
// SCENE 3: ITEMS SCREEN (Stock Management)
// ==========================================
@Composable
fun ItemsScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState(initial = emptyList())

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Items Title & Actions Row (Compact)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Items & Stock",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.setBarcodeScannerOpen(true) }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = BlueAccentLight,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = { openInBrowser(context) }) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = "Open in browser",
                            tint = Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search in stock
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search items or category...", color = Slate400, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = BlueAccent,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stock Headings Table Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Name & Category", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                Text("Price", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("Stock Actions", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
            }

            Divider(color = BorderLight, thickness = 1.dp)

            // Products List with stock controls and edit/delete
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No inventory registered", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts) { product ->
                        StockRowItem(
                            product = product,
                            onClick = { viewModel.openProductDetails(product) },
                            onAddStock = { viewModel.updateStock(product.id, product.stock + 1) },
                            onMinusStock = { viewModel.updateStock(product.id, (product.stock - 1).coerceAtLeast(0)) },
                            onDelete = { viewModel.deleteProduct(product.id) }
                        )
                    }
                }
            }
        }

        // Floating Pill-Shaped RED Button: + Add New Item (docked right above bottom navigation bar)
        Button(
            onClick = { viewModel.openCreateProduct() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .height(48.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("+ Add New Item", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun StockRowItem(
    product: Product,
    onClick: () -> Unit,
    onAddStock: () -> Unit,
    onMinusStock: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Details
            Column(modifier = Modifier.weight(2f)) {
                Text(
                    text = product.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.category,
                        color = BlueAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Qty: ${product.stock}",
                        color = if (product.stock <= 10) RoseAccent else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (product.stock <= 10) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Price Column
            Text(
                text = "₹${String.format("%.2f", product.price)}",
                color = EmeraldAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )

            // Dynamic Qty actions (+ / - / delete)
            Row(
                modifier = Modifier.weight(1.5f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMinusStock,
                    modifier = Modifier
                        .size(28.dp)
                        .background(SurfaceContainerLight, CircleShape)
                        .border(1.dp, BorderLight, CircleShape)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Stock", tint = TextPrimary, modifier = Modifier.size(14.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onAddStock,
                    modifier = Modifier
                        .size(28.dp)
                        .background(SurfaceContainerLight, CircleShape)
                        .border(1.dp, BorderLight, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Stock", tint = TextPrimary, modifier = Modifier.size(14.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete product", tint = RoseAccent, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ==========================================
// SCENE 2 FAST CALCULATOR (13-BUTTON POPUP)
// ==========================================
@Composable
fun CalculatorModal(viewModel: PosViewModel) {
    val money by viewModel.calculatorMoney.collectAsState()
    val qty by viewModel.calculatorQuantity.collectAsState()
    val isMoneyFocused by viewModel.isCalculatorMoneyFocused.collectAsState()

    Dialog(
        onDismissRequest = { viewModel.setCalculatorModalOpen(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = BlueAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fast Calculator Billing",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = { viewModel.setCalculatorModalOpen(false) }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display Boxes (Money Box & Quantity Box Stacked side-by-side)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Money Box
                    Card(
                        onClick = { viewModel.setMoneyFocus(true) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMoneyFocused) PureWhite else SurfaceContainerLight
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(68.dp)
                            .border(
                                width = 2.dp,
                                color = if (isMoneyFocused) BlueAccent else BorderLight,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Money Box (₹)", fontSize = 11.sp, color = if (isMoneyFocused) BlueAccent else TextSecondary, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "₹$money",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Quantity Box
                    Card(
                        onClick = { viewModel.setMoneyFocus(false) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isMoneyFocused) PureWhite else SurfaceContainerLight
                        ),
                        modifier = Modifier
                            .weight(0.8f)
                            .height(68.dp)
                            .border(
                                width = 2.dp,
                                color = if (!isMoneyFocused) BlueAccent else BorderLight,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Quantity Box", fontSize = 11.sp, color = if (!isMoneyFocused) BlueAccent else TextSecondary, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = qty,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tactile Keypad (Exactly 13 Keys)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left Grid of 12 Keys: 0-9, Decimal, DEL
                    Column(
                        modifier = Modifier.weight(3f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val keyRows = listOf(
                            listOf("7", "8", "9"),
                            listOf("4", "5", "6"),
                            listOf("1", "2", "3"),
                            listOf("0", ".", "DEL")
                        )

                        for (row in keyRows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (key in row) {
                                    CalculatorButton(
                                        key = key,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        viewModel.handleCalculatorPress(key)
                                    }
                                }
                            }
                        }
                    }

                    // Right Side ADD Button (Spans full height of the keypad!)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(246.dp) // Total height matches 4 button rows perfectly
                    ) {
                        Button(
                            onClick = { viewModel.handleCalculatorPress("ADD") },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, BlueAccent, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "A\nD\nD",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(key: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isDel = key == "DEL"
    val containerColor = if (isDel) RoseAccent.copy(alpha = 0.12f) else PureWhite
    val contentColor = if (isDel) RoseAccent else TextPrimary

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .height(54.dp)
            .border(1.dp, BorderLight, RoundedCornerShape(14.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

// ==========================================
// SCENE 2 DRAWER: CURRENT BILL CART
// ==========================================
@Composable
fun CartDrawer(viewModel: PosViewModel) {
    val cart by viewModel.currentCart.collectAsState()
    val subtotal = cart.sumOf { it.total }

    Dialog(
        onDismissRequest = { viewModel.setCartDrawerOpen(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .border(BorderStroke(1.dp, BorderLight), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header Drawer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = BlueAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Checkout Cart",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = { viewModel.setCartDrawerOpen(false) }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable cart items list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cart) { item ->
                        CartRowItem(
                            item = item,
                            onAdd = { viewModel.increaseCartQuantity(item.product) },
                            onMinus = { viewModel.decreaseCartQuantity(item.product) },
                            onRemove = { viewModel.removeFromCart(item.product) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = BorderLight, thickness = 1.dp)

                Spacer(modifier = Modifier.height(16.dp))

                // Totals panel
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", color = TextSecondary, fontSize = 14.sp)
                        Text("₹${String.format("%,.2f", subtotal)}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tax/Discount", color = TextSecondary, fontSize = 14.sp)
                        Text("₹0.00", color = TextPrimary, fontSize = 14.sp)
                    }

                    Divider(color = BorderLight, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grand Total", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "₹${String.format("%,.2f", subtotal)}",
                            color = EmeraldAccent,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons - strictly elevated above safe navigation handles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearCart()
                            viewModel.setCartDrawerOpen(false)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseAccent),
                        border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text("Clear Bill", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.checkout()
                            viewModel.setCartDrawerOpen(false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Finish Bill", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
}

@Composable
fun CartRowItem(
    item: CartItem,
    onAdd: () -> Unit,
    onMinus: () -> Unit,
    onRemove: () -> Unit
) {
    val isWeightItem = item.weightKg != null && item.weightKg > 0

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
                Text(
                    text = item.product.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (isWeightItem) {
                    Text(
                        text = item.weightDescription ?: "${item.weightKg} kg @ ₹${item.product.price}/kg",
                        color = EmeraldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "₹${String.format("%.2f", item.product.price)} each",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Quantity or Weight badge row
            Row(
                modifier = Modifier.weight(1.2f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isWeightItem) {
                    Surface(
                        color = BlueAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Scale, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "By Weight",
                                color = BlueAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = onMinus,
                        modifier = Modifier
                            .size(28.dp)
                            .background(SurfaceContainerLight, CircleShape)
                            .border(1.dp, BorderLight, CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TextPrimary, modifier = Modifier.size(14.dp))
                    }

                    Text(
                        text = item.quantity.toString(),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )

                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .size(28.dp)
                            .background(SurfaceContainerLight, CircleShape)
                            .border(1.dp, BorderLight, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextPrimary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "₹${String.format("%.2f", item.total)}",
                    color = EmeraldAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Remove",
                    color = RoseAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onRemove() }
                )
            }
        }
    }
}

// ==========================================
// SCENE 3 POPUP: ADD NEW PRODUCT MODAL
// ==========================================
@Composable
fun AddProductModal(viewModel: PosViewModel) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Plumbing Tools") }
    var priceStr by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf("") }

    val categories = listOf("#1-CPVC 1", "2#-CPVC 3/4", "3#-CPVC 1/2", "Reducer-all", "Plumbing Tools")
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { viewModel.setAddProductModalOpen(false) }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add New Item", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    IconButton(onClick = { viewModel.setAddProductModalOpen(false) }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Inputs
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = BorderLight,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            Icon(
                                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.clickable { expanded = !expanded }
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BlueAccent,
                            unfocusedBorderColor = BorderLight,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(PureWhite)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = TextPrimary) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BlueAccent,
                            unfocusedBorderColor = BorderLight,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Stock Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BlueAccent,
                            unfocusedBorderColor = BorderLight,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val price = priceStr.toDoubleOrNull() ?: 0.0
                        val stock = stockStr.toIntOrNull() ?: 0
                        if (name.isNotEmpty() && price > 0) {
                            viewModel.addProduct(name, category, price, stock)
                            viewModel.setAddProductModalOpen(false)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Save Item", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ==========================================
// SCENE 4: ACTIVE RECEIPT SLIP MODAL
// ==========================================
@Composable
fun ReceiptModal(invoice: Invoice, viewModel: PosViewModel) {
    val context = LocalContext.current
    val formattedDate = remember(invoice.timestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sdf.format(Date(invoice.timestamp))
    }

    Dialog(
        onDismissRequest = { viewModel.showInvoiceReceipt(null) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f)
                .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Checkmark success banner
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(EmeraldAccent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = EmeraldAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Bill ${invoice.id}",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Saved Successfully",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Scrollable Invoice slip (looks like hardware receipt)
                Card(
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderLight, RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Gemini",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Hardware Store Counter POS\nTransaction ID: ${invoice.id.replace("#", "")}\nDate: $formattedDate",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Receipt table headers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bill Item", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.8f))
                            Text("Qty", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
                            Text("Price", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }

                        Divider(color = BorderLight, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // Items list
                        invoice.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.name,
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1.8f)
                                )
                                Text(
                                    text = "x${item.quantity}",
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(0.6f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "₹${String.format("%.2f", item.price * item.quantity)}",
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        Divider(color = BorderLight, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))

                        // Grand total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "₹${String.format("%,.2f", invoice.totalAmount)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Thank you for shopping!\nPrinted successfully.",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Print / Share, New Bill, Delete
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Receipt sent to printer!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Print / Share", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteInvoice(invoice.id)
                                viewModel.showInvoiceReceipt(null)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseAccent),
                            border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Delete Bill", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.showInvoiceReceipt(null)
                                viewModel.setTab(1) // Return to catalog billing screen
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(50.dp)
                        ) {
                            Text("New Bill", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// Decorative border color token
val Slate850 = Color(0xFF1E293B).copy(alpha = 0.5f)
