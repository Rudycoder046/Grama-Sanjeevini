
package com.example.medicine.model

data class Medicine(
    var name: String = "",
    var shopName: String = "",
    var quantity: Int = 0,
    var expiryDate: String = "",
    var distance: Double = 0.0,
    var price: Double = 0.0,
    var lifeSaving: Boolean = false,
    var userId: String = "" // To track which shop owner added this
)
