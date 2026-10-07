package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CartItem
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.Product
import com.example.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class PosViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PosRepository(application)

    // UI States
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
    val invoices = _invoices.asStateFlow()

    private val _currentCart = MutableStateFlow<List<CartItem>>(emptyList())
    val currentCart = _currentCart.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _currentTab = MutableStateFlow(0) // 0 = Home, 1 = Catalog, 2 = Items
    val currentTab = _currentTab.asStateFlow()

    // Calculator States
    private val _calculatorMoney = MutableStateFlow("0")
    val calculatorMoney = _calculatorMoney.asStateFlow()

    private val _calculatorQuantity = MutableStateFlow("1")
    val calculatorQuantity = _calculatorQuantity.asStateFlow()

    private val _isCalculatorMoneyFocused = MutableStateFlow(true) // true = Money, false = Quantity
    val isCalculatorMoneyFocused = _isCalculatorMoneyFocused.asStateFlow()

    // Modals & Subscreens
    private val _activeInvoiceReceipt = MutableStateFlow<Invoice?>(null)
    val activeInvoiceReceipt = _activeInvoiceReceipt.asStateFlow()

    private val _isCalculatorModalOpen = MutableStateFlow(false)
    val isCalculatorModalOpen = _isCalculatorModalOpen.asStateFlow()

    private val _isAddProductModalOpen = MutableStateFlow(false)
    val isAddProductModalOpen = _isAddProductModalOpen.asStateFlow()

    private val _isCartDrawerOpen = MutableStateFlow(false)
    val isCartDrawerOpen = _isCartDrawerOpen.asStateFlow()

    // Item Details & Edit/Create Screen States
    private val _selectedProductForDetails = MutableStateFlow<Product?>(null)
    val selectedProductForDetails = _selectedProductForDetails.asStateFlow()

    private val _isEditProductScreenOpen = MutableStateFlow(false)
    val isEditProductScreenOpen = _isEditProductScreenOpen.asStateFlow()

    private val _editingProduct = MutableStateFlow<Product?>(null)
    val editingProduct = _editingProduct.asStateFlow()

    private val _isAdjustStockDialogOpen = MutableStateFlow(false)
    val isAdjustStockDialogOpen = _isAdjustStockDialogOpen.asStateFlow()

    // Barcode Scanner Modal State
    private val _isBarcodeScannerOpen = MutableStateFlow(false)
    val isBarcodeScannerOpen = _isBarcodeScannerOpen.asStateFlow()

    private val _lastScannedProduct = MutableStateFlow<Product?>(null)
    val lastScannedProduct = _lastScannedProduct.asStateFlow()

    // Weight Popup State
    private val _weightPopupProduct = MutableStateFlow<Product?>(null)
    val weightPopupProduct = _weightPopupProduct.asStateFlow()

    // 58mm Thermal Sticker Modal State
    private val _thermalStickerProduct = MutableStateFlow<Product?>(null)
    val thermalStickerProduct = _thermalStickerProduct.asStateFlow()

    // Category list includes static required ones plus any other categories from dynamic products
    val categories: StateFlow<List<String>> = MutableStateFlow(
        listOf("All", "#1-CPVC 1", "2#-CPVC 3/4", "3#-CPVC 1/2", "Reducer-all", "Plumbing Tools")
    ).asStateFlow()

    // Filtered Products for Catalog & Stock
    val filteredProducts = combine(_products, _searchQuery, _selectedCategory) { products, query, category ->
        products.filter { product ->
            val matchesQuery = query.isBlank() ||
                               product.name.contains(query, ignoreCase = true) || 
                               product.category.contains(query, ignoreCase = true) ||
                               product.barcode.contains(query, ignoreCase = true) ||
                               product.categories.any { it.contains(query, ignoreCase = true) }
            val matchesCategory = category == "All" || 
                                  product.category == category || 
                                  product.categories.contains(category)
            matchesQuery && matchesCategory
        }
    }

    // Dashboard metrics calculated dynamically
    val todaySales = _invoices.map { invoicesList ->
        invoicesList.sumOf { it.totalAmount }
    }

    val totalInvoices = _invoices.map { invoicesList ->
        invoicesList.size
    }

    val totalItemsSold = _invoices.map { invoicesList ->
        invoicesList.sumOf { invoice -> invoice.items.sumOf { it.quantity } }
    }

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _products.value = repository.getProducts()
            _invoices.value = repository.getInvoices()
        }
    }

    // Navigation
    fun setTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    // Cart Operations
    fun addToCart(product: Product) {
        val current = _currentCart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id && it.weightKg == null }
        if (index != -1) {
            val item = current[index]
            // Check stock limit
            if (item.quantity < product.stock || product.id.startsWith("custom_")) {
                current[index] = item.copy(quantity = item.quantity + 1)
            }
        } else {
            if (product.stock > 0 || product.id.startsWith("custom_")) {
                current.add(CartItem(product, 1))
            }
        }
        _currentCart.value = current
    }

    fun addWeightItemToCart(product: Product, weightKg: Double, weightDescription: String) {
        val current = _currentCart.value.toMutableList()
        current.add(
            CartItem(
                product = product,
                quantity = 1,
                weightKg = weightKg,
                weightDescription = weightDescription
            )
        )
        _currentCart.value = current
    }

    fun decreaseCartQuantity(product: Product) {
        val current = _currentCart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            val item = current[index]
            if (item.quantity > 1) {
                current[index] = item.copy(quantity = item.quantity - 1)
            } else {
                current.removeAt(index)
            }
        }
        _currentCart.value = current
    }

    fun increaseCartQuantity(product: Product) {
        val current = _currentCart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            val item = current[index]
            if (item.quantity < product.stock || product.id.startsWith("custom_")) {
                current[index] = item.copy(quantity = item.quantity + 1)
            }
        }
        _currentCart.value = current
    }

    fun removeFromCart(product: Product) {
        _currentCart.value = _currentCart.value.filter { it.product.id != product.id }
    }

    fun clearCart() {
        _currentCart.value = emptyList()
    }

    // Modal & Scanner Controllers
    fun setBarcodeScannerOpen(open: Boolean) {
        _isBarcodeScannerOpen.value = open
        if (!open) {
            _lastScannedProduct.value = null
        }
    }

    fun scanBarcode(scannedCode: String): Product? {
        val clean = scannedCode.trim()
        val product = _products.value.firstOrNull { it.barcode.equals(clean, ignoreCase = true) }
        if (product != null) {
            if (product.unit.equals("Kg", ignoreCase = true)) {
                _weightPopupProduct.value = product
            } else {
                addToCart(product)
            }
            _lastScannedProduct.value = product
        }
        return product
    }

    fun openWeightPopup(product: Product?) {
        _weightPopupProduct.value = product
    }

    fun openThermalStickerModal(product: Product?) {
        _thermalStickerProduct.value = product
    }

    // Invoice generation
    fun checkout() {
        if (_currentCart.value.isEmpty()) return

        val cartItems = _currentCart.value
        val invoiceId = generateInvoiceId()
        val items = cartItems.map {
            InvoiceItem(
                name = if (!it.weightDescription.isNullOrBlank()) "${it.product.name} (${it.weightDescription})" else it.product.name,
                price = it.product.price,
                quantity = it.quantity,
                weightDescription = it.weightDescription,
                itemTotal = it.total
            )
        }
        val total = cartItems.sumOf { it.total }

        val invoice = Invoice(
            id = invoiceId,
            timestamp = System.currentTimeMillis(),
            items = items,
            totalAmount = total
        )

        repository.addInvoice(invoice)
        _activeInvoiceReceipt.value = invoice
        clearCart()
        loadData() // Refresh stock counts and invoice list
    }

    private fun generateInvoiceId(): String {
        val invoicesList = _invoices.value
        val baseId = 1001
        val nextNum = baseId + invoicesList.size
        return "#INV-$nextNum"
    }

    fun deleteInvoice(invoiceId: String) {
        repository.deleteInvoice(invoiceId)
        loadData()
        if (_activeInvoiceReceipt.value?.id == invoiceId) {
            _activeInvoiceReceipt.value = null
        }
    }

    // Product Management & Details / Edit screens
    fun openProductDetails(product: Product) {
        _selectedProductForDetails.value = product
    }

    fun closeProductDetails() {
        _selectedProductForDetails.value = null
    }

    fun openCreateProduct() {
        _editingProduct.value = null
        _isEditProductScreenOpen.value = true
    }

    fun openEditProduct(product: Product) {
        _editingProduct.value = product
        _isEditProductScreenOpen.value = true
    }

    fun closeEditProduct() {
        _isEditProductScreenOpen.value = false
        _editingProduct.value = null
    }

    fun openAdjustStockDialog() {
        _isAdjustStockDialogOpen.value = true
    }

    fun closeAdjustStockDialog() {
        _isAdjustStockDialogOpen.value = false
    }

    fun saveProduct(product: Product) {
        val exists = _products.value.any { it.id == product.id }
        if (exists) {
            repository.updateProduct(product)
            if (_selectedProductForDetails.value?.id == product.id) {
                _selectedProductForDetails.value = product
            }
        } else {
            repository.addProduct(product)
        }
        loadData()
        closeEditProduct()
    }

    fun generateUniqueBarcode(): String {
        val existingBarcodes = _products.value.map { it.barcode }.toSet()
        var code: String
        do {
            val randomNum = (100000..999999).random()
            code = "GEM-$randomNum"
        } while (existingBarcodes.contains(code))
        return code
    }

    fun addProduct(name: String, category: String, price: Double, stock: Int) {
        val newProduct = Product(
            id = UUID.randomUUID().toString(),
            name = name,
            category = category,
            categories = listOf(category),
            price = price,
            stock = stock,
            barcode = generateUniqueBarcode()
        )
        repository.addProduct(newProduct)
        loadData()
    }

    fun updateStock(productId: String, newStock: Int) {
        val prod = _products.value.firstOrNull { it.id == productId } ?: return
        val updated = prod.copy(stock = newStock)
        repository.updateProduct(updated)
        loadData()
        if (_selectedProductForDetails.value?.id == productId) {
            _selectedProductForDetails.value = updated
        }
    }

    fun deleteProduct(productId: String) {
        repository.deleteProduct(productId)
        loadData()
        if (_selectedProductForDetails.value?.id == productId) {
            _selectedProductForDetails.value = null
        }
        closeEditProduct()
    }

    // Search and Filters
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    // Calculator Actions
    fun handleCalculatorPress(key: String) {
        val isMoney = _isCalculatorMoneyFocused.value
        val currentStr = if (isMoney) _calculatorMoney.value else _calculatorQuantity.value

        when (key) {
            "DEL" -> {
                val nextStr = if (currentStr.length <= 1) {
                    if (isMoney) "0" else "1"
                } else {
                    currentStr.dropLast(1)
                }
                if (isMoney) {
                    _calculatorMoney.value = nextStr
                } else {
                    _calculatorQuantity.value = nextStr
                }
            }
            "ADD" -> {
                if (isMoney) {
                    // Step 1: Shift focus from Money to Quantity
                    _isCalculatorMoneyFocused.value = false
                } else {
                    // Step 2: Compute Money x Quantity, append custom item, clear & reset
                    val price = _calculatorMoney.value.toDoubleOrNull() ?: 0.0
                    val qty = _calculatorQuantity.value.toIntOrNull() ?: 1
                    if (price > 0 && qty > 0) {
                        val customProduct = Product(
                            id = "custom_${UUID.randomUUID()}",
                            name = "Custom Item: ₹${String.format("%.2f", price)} x $qty",
                            category = "Custom",
                            price = price,
                            stock = qty // Stock is set to quantity so it's valid
                        )
                        val current = _currentCart.value.toMutableList()
                        current.add(CartItem(customProduct, qty))
                        _currentCart.value = current
                    }
                    // Reset
                    _calculatorMoney.value = "0"
                    _calculatorQuantity.value = "1"
                    _isCalculatorMoneyFocused.value = true
                    _isCalculatorModalOpen.value = false // Auto-close calculator upon ADD success
                }
            }
            "." -> {
                if (!currentStr.contains(".")) {
                    val nextStr = currentStr + "."
                    if (isMoney) {
                        _calculatorMoney.value = nextStr
                    } else {
                        _calculatorQuantity.value = nextStr
                    }
                }
            }
            else -> { // Numeric key 0-9
                val nextStr = if (currentStr == "0") {
                    key
                } else {
                    currentStr + key
                }
                if (isMoney) {
                    _calculatorMoney.value = nextStr
                } else {
                    _calculatorQuantity.value = nextStr
                }
            }
        }
    }

    fun setMoneyFocus(isMoney: Boolean) {
        _isCalculatorMoneyFocused.value = isMoney
    }

    // Modal Control
    fun showInvoiceReceipt(invoice: Invoice?) {
        _activeInvoiceReceipt.value = invoice
    }

    fun setCalculatorModalOpen(isOpen: Boolean) {
        _isCalculatorModalOpen.value = isOpen
        if (isOpen) {
            // Reset focus to Money Box and clear inputs
            _calculatorMoney.value = "0"
            _calculatorQuantity.value = "1"
            _isCalculatorMoneyFocused.value = true
        }
    }

    fun setAddProductModalOpen(isOpen: Boolean) {
        _isAddProductModalOpen.value = isOpen
    }

    fun setCartDrawerOpen(isOpen: Boolean) {
        _isCartDrawerOpen.value = isOpen
    }
}
