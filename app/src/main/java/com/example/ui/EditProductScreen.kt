package com.example.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.QrCode
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Product
import com.example.ui.theme.*
import com.example.viewmodel.PosViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    editingProduct: Product?,
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    val isEditing = editingProduct != null

    // System Back gesture
    BackHandler {
        viewModel.closeEditProduct()
    }

    // Available categories
    val defaultCategories = listOf(
        "#1-CPVC 1",
        "2#-CPVC 3/4",
        "3#-CPVC 1/2",
        "Reducer-all",
        "Plumbing Tools",
        "Hardware",
        "Electrical"
    )

    val units = listOf("Pcs", "Mtr", "Box", "Kg", "Set", "Pkt")

    // Form fields
    var name by remember(editingProduct) { mutableStateOf(editingProduct?.name ?: "") }
    var selectedUnit by remember(editingProduct) { mutableStateOf(editingProduct?.unit ?: "Pcs") }
    var barcode by remember(editingProduct) {
        mutableStateOf(
            if (editingProduct != null && editingProduct.barcode.isNotBlank()) {
                editingProduct.barcode
            } else {
                viewModel.generateUniqueBarcode()
            }
        )
    }

    var selectedCategories by remember(editingProduct) {
        val initial = editingProduct?.categories?.toMutableSet()
            ?: mutableSetOf(editingProduct?.category ?: "Plumbing Tools")
        if (initial.isEmpty()) initial.add("Plumbing Tools")
        mutableStateOf(initial.toList())
    }

    var mrpStr by remember(editingProduct) {
        mutableStateOf(if ((editingProduct?.mrp ?: 0.0) > 0) editingProduct!!.mrp.toString() else "")
    }
    var salePriceStr by remember(editingProduct) {
        mutableStateOf(if ((editingProduct?.price ?: 0.0) > 0) editingProduct!!.price.toString() else "")
    }
    var purchasePriceStr by remember(editingProduct) {
        mutableStateOf(if ((editingProduct?.purchasePrice ?: 0.0) > 0) editingProduct!!.purchasePrice.toString() else "")
    }
    var discountStr by remember(editingProduct) {
        mutableStateOf(if ((editingProduct?.discountPercent ?: 0.0) > 0) editingProduct!!.discountPercent.toString() else "")
    }
    var stockStr by remember(editingProduct) {
        mutableStateOf(if (editingProduct != null) editingProduct.stock.toString() else "50")
    }

    var imageBase64 by remember(editingProduct) { mutableStateOf(editingProduct?.imageBase64) }

    // Photo picker launcher (Android Photo Picker - no dangerous permissions needed)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = uriToBase64(context, uri)
            if (encoded != null) {
                imageBase64 = encoded
                Toast.makeText(context, "Product image attached successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto calculate discount percentage when MRP and Sale Price change
    fun recalculateDiscount(mrpVal: Double, saleVal: Double) {
        if (mrpVal > 0 && saleVal > 0 && mrpVal >= saleVal) {
            val discount = ((mrpVal - saleVal) / mrpVal) * 100.0
            discountStr = String.format(java.util.Locale.US, "%.1f", discount)
        }
    }

    // Unit dropdown expansion state
    var isUnitDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Item" else "Add New Item",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeEditProduct() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isEditing) {
                        OutlinedButton(
                            onClick = {
                                editingProduct?.let { viewModel.deleteProduct(it.id) }
                                Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseAccent),
                            border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = RoseAccent)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Prominent Save Button
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, "Please enter an item name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val salePrice = salePriceStr.toDoubleOrNull() ?: 0.0
                            if (salePrice <= 0.0) {
                                Toast.makeText(context, "Please enter a valid sale price", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val mrp = mrpStr.toDoubleOrNull() ?: salePrice
                            val purchasePrice = purchasePriceStr.toDoubleOrNull() ?: (salePrice * 0.75)
                            val discountPercent = discountStr.toDoubleOrNull() ?: 0.0
                            val stock = stockStr.toIntOrNull() ?: 0

                            val primaryCat = selectedCategories.firstOrNull() ?: "Plumbing Tools"

                            val productToSave = Product(
                                id = editingProduct?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                category = primaryCat,
                                categories = if (selectedCategories.isEmpty()) listOf(primaryCat) else selectedCategories,
                                price = salePrice,
                                purchasePrice = purchasePrice,
                                mrp = mrp,
                                discountPercent = discountPercent,
                                stock = stock,
                                unit = selectedUnit,
                                barcode = barcode.ifBlank { viewModel.generateUniqueBarcode() },
                                imageBase64 = imageBase64
                            )

                            viewModel.saveProduct(productToSave)
                            Toast.makeText(
                                context,
                                if (isEditing) "Item updated successfully!" else "Item registered in catalog!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(if (isEditing) 1.5f else 1f)
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEditing) "Save / Edit" else "Save Item",
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Photo Upload Box
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val bitmap = remember(imageBase64) {
                            imageBase64?.let { decodeBase64ToBitmap(it) }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Item Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Change photo overlay pill
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .background(PureWhite.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                                    .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.CameraAlt,
                                        contentDescription = null,
                                        tint = TextPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Change Photo",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(BlueAccent.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CameraAlt,
                                        contentDescription = null,
                                        tint = BlueAccent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Tap to upload product photo",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "PNG, JPG up to 10MB (Saved in database)",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Name & Unit Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Item Identity",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueAccentLight
                    )

                    // Item Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Item Name *") },
                        placeholder = { Text("e.g. CPVC Coupler 3/4\"") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )

                    // Unit Selection Dropdown & Opening Stock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Unit Selector
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = selectedUnit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit") },
                                trailingIcon = {
                                    IconButton(onClick = { isUnitDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Slate400)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isUnitDropdownExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = outlinedFieldColors()
                            )

                            DropdownMenu(
                                expanded = isUnitDropdownExpanded,
                                onDismissRequest = { isUnitDropdownExpanded = false },
                                modifier = Modifier.background(PureWhite)
                            ) {
                                units.forEach { unitOption ->
                                    DropdownMenuItem(
                                        text = { Text(unitOption, color = TextPrimary) },
                                        onClick = {
                                            selectedUnit = unitOption
                                            isUnitDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Stock Count
                        OutlinedTextField(
                            value = stockStr,
                            onValueChange = { stockStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("In Stock") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedFieldColors(),
                            singleLine = true
                        )
                    }
                }
            }

            // Section 3: Barcode / SKU & 58mm Thermal Sticker Printing
            item {
                val isBarcodeLocked = editingProduct != null && editingProduct.barcode.isNotBlank()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Barcode / SKU",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlueAccentLight
                            )
                            if (isBarcodeLocked) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = EmeraldAccent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = EmeraldAccent,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            "Permanently Locked",
                                            color = EmeraldAccent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = {
                                if (!isBarcodeLocked) {
                                    barcode = it.uppercase()
                                }
                            },
                            readOnly = isBarcodeLocked,
                            placeholder = { Text("GEM-XXXXXX") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isBarcodeLocked) Icons.Default.Lock else Icons.Outlined.QrCode,
                                    contentDescription = null,
                                    tint = if (isBarcodeLocked) EmeraldAccent else Slate400
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedFieldColors(),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                            singleLine = true
                        )

                        if (!isBarcodeLocked) {
                            Button(
                                onClick = {
                                    barcode = viewModel.generateUniqueBarcode()
                                    Toast.makeText(context, "New unique code assigned: $barcode", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Text("Assign Code", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }

                    // 58mm Thermal Sticker Printing Button
                    Button(
                        onClick = {
                            val effectiveProduct = Product(
                                id = editingProduct?.id ?: UUID.randomUUID().toString(),
                                name = name.ifBlank { "Sample Product" },
                                category = selectedCategories.firstOrNull() ?: "General",
                                categories = selectedCategories,
                                price = salePriceStr.toDoubleOrNull() ?: 0.0,
                                purchasePrice = purchasePriceStr.toDoubleOrNull() ?: 0.0,
                                mrp = mrpStr.toDoubleOrNull() ?: 0.0,
                                discountPercent = discountStr.toDoubleOrNull() ?: 0.0,
                                stock = stockStr.toIntOrNull() ?: 0,
                                unit = selectedUnit,
                                barcode = barcode,
                                imageBase64 = imageBase64
                            )
                            viewModel.openThermalStickerModal(effectiveProduct)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(1.5.dp, BlueAccent, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Print Barcode (58mm QR Sticker)",
                            color = BlueAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Section 4: Multi-Category Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Categories (Select one or more)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueAccent
                        )
                        Text(
                            text = "${selectedCategories.size} selected",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(defaultCategories) { cat ->
                            val isSelected = selectedCategories.contains(cat)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val current = selectedCategories.toMutableList()
                                    if (isSelected) {
                                        if (current.size > 1) { // keep at least 1 category
                                            current.remove(cat)
                                        }
                                    } else {
                                        current.add(cat)
                                    }
                                    selectedCategories = current
                                },
                                label = { Text(cat, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BlueAccent,
                                    selectedLabelColor = Color.White,
                                    containerColor = PureWhite,
                                    labelColor = TextPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = BorderLight,
                                    selectedBorderColor = BlueAccent
                                )
                            )
                        }
                    }
                }
            }

            // Section 5: Pricing Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Pricing & Margins",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueAccentLight
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // MRP
                        OutlinedTextField(
                            value = mrpStr,
                            onValueChange = {
                                mrpStr = it
                                val mrpVal = it.toDoubleOrNull() ?: 0.0
                                val saleVal = salePriceStr.toDoubleOrNull() ?: 0.0
                                recalculateDiscount(mrpVal, saleVal)
                            },
                            label = { Text("MRP (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedFieldColors(),
                            singleLine = true
                        )

                        // Sale Price
                        OutlinedTextField(
                            value = salePriceStr,
                            onValueChange = {
                                salePriceStr = it
                                val mrpVal = mrpStr.toDoubleOrNull() ?: 0.0
                                val saleVal = it.toDoubleOrNull() ?: 0.0
                                recalculateDiscount(mrpVal, saleVal)
                            },
                            label = { Text("Sale Price (₹) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedFieldColors(),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Purchase Price
                        OutlinedTextField(
                            value = purchasePriceStr,
                            onValueChange = { purchasePriceStr = it },
                            label = { Text("Purchase Price (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedFieldColors(),
                            singleLine = true
                        )

                        // Discount (%)
                        OutlinedTextField(
                            value = discountStr,
                            onValueChange = { discountStr = it },
                            label = { Text("Discount (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = outlinedFieldColors(),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun outlinedFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedBorderColor = BlueAccent,
        unfocusedBorderColor = BorderLight,
        focusedContainerColor = PureWhite,
        unfocusedContainerColor = SurfaceContainerLight,
        focusedLabelColor = BlueAccent,
        unfocusedLabelColor = TextSecondary
    )
}
