package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Product(
    val id: String,
    val name: String,
    val category: String = "Plumbing Tools",
    val categories: List<String> = listOf(category),
    val price: Double = 0.0, // Sale Price
    val purchasePrice: Double = 0.0, // Purchase Price
    val mrp: Double = 0.0, // MRP
    val discountPercent: Double = 0.0,
    val stock: Int = 0,
    val unit: String = "Pcs",
    val barcode: String = "",
    val imageBase64: String? = null
)

@JsonClass(generateAdapter = true)
data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val weightKg: Double? = null,
    val weightDescription: String? = null
) {
    val total: Double get() = if (weightKg != null && weightKg > 0) {
        product.price * weightKg
    } else {
        product.price * quantity
    }
}

@JsonClass(generateAdapter = true)
data class InvoiceItem(
    val name: String,
    val price: Double,
    val quantity: Int,
    val weightDescription: String? = null,
    val itemTotal: Double? = null
) {
    val total: Double get() = itemTotal ?: (price * quantity)
}

@JsonClass(generateAdapter = true)
data class Invoice(
    val id: String,
    val timestamp: Long,
    val items: List<InvoiceItem>,
    val totalAmount: Double
)
