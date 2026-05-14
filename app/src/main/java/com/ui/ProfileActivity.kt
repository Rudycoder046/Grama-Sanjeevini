package com.example.medicine.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.medicine.R
import com.example.medicine.model.UserProfile
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private lateinit var profileImg: ImageView
    private lateinit var licenseImg: ImageView
    private lateinit var nameEdt: EditText
    private lateinit var emailEdt: EditText
    private lateinit var qualSpinner: Spinner
    private lateinit var phoneEdt: EditText
    private lateinit var shopEdt: EditText
    private lateinit var locEdt: EditText
    private lateinit var mapsLinkEdt: EditText
    private lateinit var loader: ProgressBar
    private lateinit var gpsStatus: TextView
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private val qualOptions = listOf("D PHARM", "B Pharm")
    
    private var profileBase64 = ""
    private var licenseBase64 = ""
    private var currentLat = 0.0
    private var currentLon = 0.0
    
    private var isPickingProfile = true

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val base64 = encodeImage(bitmap)
                if (isPickingProfile) {
                    profileImg.setImageBitmap(bitmap)
                    profileBase64 = base64
                } else {
                    licenseImg.setImageBitmap(bitmap)
                    licenseBase64 = base64
                }
            } catch (e: Exception) {
                Log.e("PROFILE_ERROR", "Image pick failed", e)
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        profileImg = findViewById(R.id.profileImage)
        licenseImg = findViewById(R.id.licenseImage)
        nameEdt = findViewById(R.id.profName)
        emailEdt = findViewById(R.id.profEmail)
        qualSpinner = findViewById(R.id.profQualSpinner)
        phoneEdt = findViewById(R.id.profPhone)
        shopEdt = findViewById(R.id.profShopName)
        locEdt = findViewById(R.id.profLocation)
        mapsLinkEdt = findViewById(R.id.profMapsLink)
        loader = findViewById(R.id.profileLoader)
        gpsStatus = findViewById(R.id.gpsStatus)
        val saveBtn = findViewById<Button>(R.id.saveProfileBtn)
        val pinBtn = findViewById<Button>(R.id.pinLocationBtn)
        
        setupQualSpinner()

        emailEdt.setText(auth.currentUser?.email)

        loadProfile()

        profileImg.setOnClickListener {
            isPickingProfile = true
            pickImage.launch("image/*")
        }

        licenseImg.setOnClickListener {
            isPickingProfile = false
            pickImage.launch("image/*")
        }
        
        pinBtn.setOnClickListener {
            requestLocation()
        }

        saveBtn.setOnClickListener {
            saveProfile()
        }
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }
        
        loader.visibility = View.VISIBLE
        fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
            loader.visibility = View.GONE
            if (location != null) {
                currentLat = location.latitude
                currentLon = location.longitude
                gpsStatus.text = String.format(java.util.Locale.getDefault(), "📍 GPS Pinned: %.4f, %.4f", currentLat, currentLon)
                Toast.makeText(this, "Location Pinned!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Could not get location. Is GPS on?", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupQualSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, qualOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        qualSpinner.adapter = adapter
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        loader.visibility = View.VISIBLE
        
        db.collection("profiles").document(uid).get()
            .addOnSuccessListener { doc ->
                loader.visibility = View.GONE
                if (doc.exists()) {
                    val profile = doc.toObject(UserProfile::class.java)
                    profile?.let {
                        nameEdt.setText(it.name)
                        
                        // Set spinner selection
                        val qualIndex = qualOptions.indexOf(it.qualification)
                        if (qualIndex >= 0) qualSpinner.setSelection(qualIndex)
                        
                        phoneEdt.setText(it.phone.replace("+91 ", ""))
                        shopEdt.setText(it.shopName)
                        locEdt.setText(it.location)
                        mapsLinkEdt.setText(it.googleMapsLink)
                        
                        if (it.latitude != 0.0) {
                            currentLat = it.latitude
                            currentLon = it.longitude
                            gpsStatus.text = "📍 GPS Pinned: ${String.format("%.4f", currentLat)}, ${String.format("%.4f", currentLon)}"
                        }
                        
                        if (it.profilePhotoBase64.isNotEmpty()) {
                            profileBase64 = it.profilePhotoBase64
                            profileImg.setImageBitmap(decodeImage(it.profilePhotoBase64))
                        }
                        if (it.licensePhotoBase64.isNotEmpty()) {
                            licenseBase64 = it.licensePhotoBase64
                            licenseImg.setImageBitmap(decodeImage(it.licensePhotoBase64))
                        }
                    }
                }
            }
            .addOnFailureListener {
                loader.visibility = View.GONE
            }
    }

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return
        val name = nameEdt.text.toString().trim()
        val email = emailEdt.text.toString().trim()
        val qual = qualSpinner.selectedItem.toString()
        var phone = phoneEdt.text.toString().trim()
        val shop = shopEdt.text.toString().trim()
        val loc = locEdt.text.toString().trim()
        val mLink = mapsLinkEdt.text.toString().trim()

        if (name.isEmpty() || qual.isEmpty() || phone.isEmpty() || shop.isEmpty() || loc.isEmpty()) {
            Toast.makeText(this, "Please fill all professional fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (phone.length != 10) {
            Toast.makeText(this, "Phone number must be exactly 10 digits", Toast.LENGTH_SHORT).show()
            return
        }
        
        phone = "+91 $phone"

        val profile = UserProfile(uid, name, email, qual, phone, shop, loc, mLink, currentLat, currentLon, profileBase64, licenseBase64)
        
        loader.visibility = View.VISIBLE
        db.collection("profiles").document(uid).set(profile)
            .addOnSuccessListener {
                updateMedicineLocations(uid, profile)
            }
            .addOnFailureListener {
                loader.visibility = View.GONE
                Toast.makeText(this, "Sync Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateMedicineLocations(uid: String, profile: UserProfile) {
        db.collection("medicines")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    loader.visibility = View.GONE
                    Toast.makeText(this, "Profile Synchronized Successfully", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                for (doc in snapshot) {
                    batch.update(doc.reference, mapOf(
                        "shopName" to profile.shopName,
                        "shopAddress" to profile.location,
                        "shopMapsLink" to profile.googleMapsLink,
                        "latitude" to profile.latitude,
                        "longitude" to profile.longitude
                    ))
                }

                batch.commit()
                    .addOnSuccessListener {
                        loader.visibility = View.GONE
                        Toast.makeText(this, "Profile and Medicines Updated Successfully", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        loader.visibility = View.GONE
                        Log.e("SYNC_ERROR", "Failed to update medicines", e)
                        Toast.makeText(this, "Profile saved, but medicines update failed", Toast.LENGTH_LONG).show()
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                loader.visibility = View.GONE
                Log.e("SYNC_ERROR", "Failed to query medicines", e)
                Toast.makeText(this, "Profile saved, but could not sync medicines", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun encodeImage(bm: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    private fun decodeImage(base64: String): Bitmap {
        val imageBytes = Base64.decode(base64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}