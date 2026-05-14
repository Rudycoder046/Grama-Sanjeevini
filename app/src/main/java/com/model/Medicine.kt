package com.example.medicine.model

import com.google.firebase.firestore.DocumentId

data class Medicine(
    @DocumentId var id: String = "",
    var name: String = "",
    var shopName: String = "",
    var shopAddress: String = "",
    var shopMapsLink: String = "",
    var quantity: Int = 0,
    var expiryDate: String = "",
    var distance: Double = 0.0,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var price: Double = 0.0,
    var lifeSaving: Boolean = false,
    var userId: String = ""
)
