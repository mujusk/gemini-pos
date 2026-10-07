package com.example.repository

import android.content.Context
import com.example.model.Invoice
import com.example.model.Product
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class PosRepository(context: Context) {
    private val prefs = context.getSharedPreferences("gemini_pos_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val productListType = Types.newParameterizedType(List::class.java, Product::class.java)
    private val invoiceListType = Types.newParameterizedType(List::class.java, Invoice::class.java)

    private val productAdapter = moshi.adapter<List<Product>>(productListType)
    private val invoiceAdapter = moshi.adapter<List<Invoice>>(invoiceListType)

    init {
        if (!prefs.contains("products")) {
            seedInitialProducts()
        }
    }

    private fun seedInitialProducts() {
        val initialProducts = listOf(
            Product("1", "CPVC Coupler 3/4\"", "2#-CPVC 3/4", listOf("2#-CPVC 3/4"), 12.0, 8.5, 15.0, 0.0, 150, "Pcs", "GEM-1001", null),
            Product("2", "CPVC Elbow 3/4\"", "2#-CPVC 3/4", listOf("2#-CPVC 3/4"), 12.0, 8.5, 15.0, 0.0, 120, "Pcs", "GEM-1002", null),
            Product("3", "CPVC Elbow 1\"", "#1-CPVC 1", listOf("#1-CPVC 1"), 22.0, 16.0, 26.0, 0.0, 80, "Pcs", "GEM-1003", null),
            Product("4", "CPVC End Cap 3/4\"", "2#-CPVC 3/4", listOf("2#-CPVC 3/4"), 15.0, 10.0, 18.0, 0.0, 90, "Pcs", "GEM-1004", null),
            Product("5", "Brass Elbow 3/4\"", "Reducer-all", listOf("Reducer-all", "Plumbing Tools"), 60.0, 44.0, 75.0, 0.0, 45, "Pcs", "GEM-1005", null),
            Product("6", "Brass Ball Valve 3/4\"", "Plumbing Tools", listOf("Plumbing Tools"), 130.0, 98.0, 160.0, 0.0, 30, "Pcs", "GEM-1006", null),
            Product("7", "PVC Solution 118ml", "Plumbing Tools", listOf("Plumbing Tools"), 170.0, 130.0, 200.0, 0.0, 25, "Box", "GEM-1007", null),
            Product("8", "Teflon Tape", "Plumbing Tools", listOf("Plumbing Tools"), 20.0, 14.0, 25.0, 0.0, 200, "Pcs", "GEM-1008", null),
            Product("9", "Galvanized Wire (Binding)", "Hardware", listOf("Hardware"), 140.0, 110.0, 160.0, 0.0, 100, "Kg", "GEM-1009", null),
            Product("10", "White Cement (Adhesive)", "Plumbing Tools", listOf("Plumbing Tools"), 45.0, 32.0, 50.0, 0.0, 200, "Kg", "GEM-1010", null),
            Product("11", "Iron Nails 2.5 inch", "Hardware", listOf("Hardware"), 95.0, 75.0, 110.0, 0.0, 80, "Kg", "GEM-1011", null)
        )
        saveProducts(initialProducts)
    }

    fun getProducts(): List<Product> {
        val json = prefs.getString("products", null) ?: return emptyList()
        return try {
            productAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveProducts(products: List<Product>) {
        val json = productAdapter.toJson(products)
        prefs.edit().putString("products", json).apply()
    }

    fun addProduct(product: Product) {
        val current = getProducts().toMutableList()
        current.add(product)
        saveProducts(current)
    }

    fun updateProduct(updated: Product) {
        val current = getProducts().map { existing ->
            if (existing.id == updated.id) {
                // Permanent barcode lock: ensure assigned barcode is preserved
                val lockedBarcode = if (existing.barcode.isNotBlank()) existing.barcode else updated.barcode
                updated.copy(barcode = lockedBarcode)
            } else {
                existing
            }
        }
        saveProducts(current)
    }

    fun deleteProduct(productId: String) {
        val current = getProducts().filter { it.id != productId }
        saveProducts(current)
    }

    fun getInvoices(): List<Invoice> {
        val json = prefs.getString("invoices", null) ?: return emptyList()
        return try {
            invoiceAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveInvoices(invoices: List<Invoice>) {
        val json = invoiceAdapter.toJson(invoices)
        prefs.edit().putString("invoices", json).apply()
    }

    fun addInvoice(invoice: Invoice) {
        val current = getInvoices().toMutableList()
        current.add(0, invoice) // Add to the top (most recent first)
        saveInvoices(current)

        // Also deduct stock of products sold
        val products = getProducts().toMutableList()
        for (invoiceItem in invoice.items) {
            val index = products.indexOfFirst { it.name == invoiceItem.name }
            if (index != -1) {
                val prod = products[index]
                val newStock = (prod.stock - invoiceItem.quantity).coerceAtLeast(0)
                products[index] = prod.copy(stock = newStock)
            }
        }
        saveProducts(products)
    }

    fun deleteInvoice(invoiceId: String) {
        val current = getInvoices().filter { it.id != invoiceId }
        saveInvoices(current)
    }
}
