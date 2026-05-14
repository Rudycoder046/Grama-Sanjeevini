package com.example.medicine.model

data class UserProfile(
    var userId: String = "",
    var name: String = "",
    var email: String = "",
    var qualification: String = "",
    var phone: String = "",
    var shopName: String = "",
    var location: String = "", // Detailed address text
    var googleMapsLink: String = "", // Pasted link from G Maps
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var profilePhotoBase64: String = "",
    var licensePhotoBase64: String = ""
)
