package com.example.medicine.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.*
import com.example.medicine.R
import com.example.medicine.model.Medicine
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private val list = mutableListOf<Medicine>()
    private lateinit var adapter: MedicineAdapter
    private lateinit var loader: ProgressBar
    private lateinit var recycler: RecyclerView
    private lateinit var searchContainer: View
    private lateinit var emptyState: View
    private lateinit var logoBackground: View
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLat = 0.0
    private var userLon = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        requestLocationPermission()

        val searchBox = findViewById<AutoCompleteTextView>(R.id.searchBox)
        val btn = findViewById<Button>(R.id.searchBtn)
        recycler = findViewById(R.id.recyclerView)
        loader = findViewById(R.id.searchLoader)
        searchContainer = findViewById(R.id.searchContainer)
        emptyState = findViewById(R.id.emptyState)
        logoBackground = findViewById(R.id.logoBackground)
        val settingsBtn = findViewById<ImageView>(R.id.settingsBtn)

        adapter = MedicineAdapter(list)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        btn.setOnClickListener {
            val query = searchBox.text.toString().trim()
            if (query.isNotEmpty()) {
                search(query)
            } else {
                Toast.makeText(this, "Enter medicine name", Toast.LENGTH_SHORT).show()
            }
        }

        settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        setupAutoComplete(searchBox)
    }

    private fun setupAutoComplete(searchBox: AutoCompleteTextView) {
        db.collection("medicines")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val allMedNames = querySnapshot.documents
                    .map { it.getString("name") ?: "" }
                    .filter { it.isNotEmpty() }
                    .distinct()
                
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, allMedNames)
                searchBox.setAdapter(adapter)
            }
    }

    private fun showLifeSavingAlert(medicineName: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("🚨 Life-Saving Medicine Found!")
            .setMessage("'$medicineName' is marked as a life-saving medicine. Please contact the pharmacy immediately for availability.")
            .setPositiveButton("OK", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
        } else {
            getUserLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation()
            } else {
                Toast.makeText(this, "Location permission denied. Distances cannot be calculated.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                if (location != null) {
                    userLat = location.latitude
                    userLon = location.longitude
                    Log.d("LOCATION_DEBUG", "Last location: $userLat, $userLon")
                } else {
                    // Try to get fresh location if lastLocation is null
                    requestFreshLocation()
                }
            }
        } catch (e: SecurityException) {
            Log.e("LOCATION_ERROR", "SecurityException: ${e.message}")
        }
    }

    private fun requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 1000
        ).setMaxUpdates(1).build()

        fusedLocationClient.requestLocationUpdates(locationRequest, object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    userLat = location.latitude
                    userLon = location.longitude
                    Log.d("LOCATION_DEBUG", "Fresh location: $userLat, $userLon")
                }
            }
        }, mainLooper)
    }

    private fun search(name: String) {
        if (userLat == 0.0) {
            Toast.makeText(this, "Acquiring location... please wait a moment", Toast.LENGTH_SHORT).show()
            getUserLocation()
        }

        loader.visibility = View.VISIBLE
        
        db.collection("medicines")
            .get()
            .addOnSuccessListener { querySnapshot ->
                loader.visibility = View.GONE
                list.clear()
                
                Log.d("SEARCH_DEBUG", "Found ${querySnapshot.size()} documents in collection")
                for (doc in querySnapshot) {
                    val med = doc.toObject(Medicine::class.java)
                    val medName = med.name
                    
                    var finalDistance = med.distance // Fallback to manual distance
                    
                    // If medicine has real GPS data, calculate real distance
                    if (med.latitude != 0.0 && userLat != 0.0) {
                        val results = FloatArray(1)
                        Location.distanceBetween(userLat, userLon, med.latitude, med.longitude, results)
                        finalDistance = (results[0] / 1000.0).toDouble() // Convert meters to km
                    }
                    
                    Log.d("SEARCH_DEBUG", "Checking: $medName, Dist: $finalDistance against Query: $name")
                    
                    if (medName.contains(name, ignoreCase = true) && finalDistance <= 50.0) {
                        med.distance = finalDistance // Update object for UI display
                        list.add(med)
                        Log.d("SEARCH_DEBUG", "Match found and added: ${med.name}")
                        
                        // Alert for life saving medicine
                        if (med.lifeSaving) {
                            showLifeSavingAlert(med.name)
                        }
                    }
                }
                
                // Sort by price ascending
                list.sortBy { it.price }
                
                adapter.notifyDataSetChanged()
                
                if (list.isEmpty()) {
                    Toast.makeText(this, "No medicines found", Toast.LENGTH_LONG).show()
                    recycler.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                    logoBackground.visibility = View.VISIBLE
                } else {
                    recycler.visibility = View.VISIBLE
                    emptyState.visibility = View.GONE
                    logoBackground.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                loader.visibility = View.GONE
                Log.e("SEARCH_ERROR", "Search failed", e)
                if (e.message?.contains("disabled") == true) {
                    Toast.makeText(this, "ERROR: Cloud Firestore API is disabled. Please enable it in Firebase Console.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}