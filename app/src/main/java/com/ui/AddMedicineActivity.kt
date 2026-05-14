package com.example.medicine.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.medicine.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddMedicineActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var loader: ProgressBar
    private lateinit var saveBtn: Button
    
    private var medicineId: String? = null
    
    private var shopLat = 0.0
    private var shopLon = 0.0
    private var shopAddress = ""
    private var shopMapsLink = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val name = findViewById<EditText>(R.id.inputName)
        val shop = findViewById<EditText>(R.id.inputShopName)
        val price = findViewById<EditText>(R.id.inputPrice)
        val expiry = findViewById<EditText>(R.id.inputExpiry)
        val quantity = findViewById<EditText>(R.id.inputQuantity)
        val lifeSavingCheck = findViewById<CheckBox>(R.id.checkLifeSaving)
        saveBtn = findViewById(R.id.saveBtn)
        loader = findViewById(R.id.saveLoader)
        
        // Auto-fill shop details from profile if available
        if (intent.getStringExtra("MED_ID") == null) {
            autoFillShopDetails(shop)
        }
        
        // Check for edit mode
        medicineId = intent.getStringExtra("MED_ID")
        if (medicineId != null) {
            val title = findViewById<TextView>(R.id.pageTitle)
            title.text = "Edit Medicine"
            saveBtn.text = "Update Medicine"
            
            name.setText(intent.getStringExtra("MED_NAME"))
            shop.setText(intent.getStringExtra("MED_SHOP"))
            price.setText(intent.getDoubleExtra("MED_PRICE", 0.0).toString())
            expiry.setText(intent.getStringExtra("MED_EXPIRY"))
            quantity.setText(intent.getIntExtra("MED_QTY", 0).toString())
            lifeSavingCheck.isChecked = intent.getBooleanExtra("MED_LIFE", false)
            
            // For edits, we preserve existing details
            shopLat = intent.getDoubleExtra("MED_LAT", 0.0)
            shopLon = intent.getDoubleExtra("MED_LON", 0.0)
            shopAddress = intent.getStringExtra("MED_ADDR") ?: ""
            shopMapsLink = intent.getStringExtra("MED_MLINK") ?: ""
        }

        saveBtn.setOnClickListener {
            val medName = name.text.toString().trim()
            val medShop = shop.text.toString().trim()
            val medPriceStr = price.text.toString().trim()
            val medExpiry = expiry.text.toString().trim()
            val medQtyStr = quantity.text.toString().trim()

            if (medName.isEmpty() || medShop.isEmpty() || medExpiry.isEmpty() || medPriceStr.isEmpty() || medQtyStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val medPrice = medPriceStr.toDoubleOrNull() ?: 0.0
            val medQty = medQtyStr.toIntOrNull() ?: 0
            val isLifeSaving = lifeSavingCheck.isChecked

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Session Expired. Please login again.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val medicineData = hashMapOf(
                "name" to medName,
                "shopName" to medShop,
                "shopAddress" to shopAddress,
                "shopMapsLink" to shopMapsLink,
                "price" to medPrice,
                "expiryDate" to medExpiry,
                "latitude" to shopLat,
                "longitude" to shopLon,
                "quantity" to medQty,
                "lifeSaving" to isLifeSaving,
                "userId" to currentUser.uid
            )

            Log.d("UPLOAD_DEBUG", "Medicine Data: $medicineData")

            saveBtn.isEnabled = false
            loader.visibility = View.VISIBLE
            
            val task = if (medicineId != null) {
                db.collection("medicines").document(medicineId!!).set(medicineData)
            } else {
                db.collection("medicines").add(medicineData)
            }
            
            task.addOnSuccessListener {
                loader.visibility = View.GONE
                saveBtn.isEnabled = true
                val msg = if (medicineId != null) "Successfully Updated!" else "Successfully Saved!"
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                loader.visibility = View.GONE
                saveBtn.isEnabled = true
                Log.e("UPLOAD_ERROR", "Failed to upload", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun autoFillShopDetails(shopEdt: EditText) {
        val uid = auth.currentUser?.uid ?: return
        loader.visibility = View.VISIBLE
        db.collection("profiles").document(uid).get()
            .addOnSuccessListener { doc ->
                loader.visibility = View.GONE
                if (doc.exists()) {
                    val shopName = doc.getString("shopName") ?: ""
                    val addr = doc.getString("location") ?: ""
                    val mLink = doc.getString("googleMapsLink") ?: ""
                    val lat = doc.getDouble("latitude") ?: 0.0
                    val lon = doc.getDouble("longitude") ?: 0.0
                    
                    Log.d("AUTOFILL_DEBUG", "Profile data: $shopName, $addr, $mLink, $lat, $lon")

                    if (shopName.isNotEmpty()) shopEdt.setText(shopName)
                    
                    shopLat = lat
                    shopLon = lon
                    shopAddress = addr
                    shopMapsLink = mLink
                } else {
                    Log.d("AUTOFILL_DEBUG", "No profile found for UID: $uid")
                    Toast.makeText(this, "Profile not found. Please set up your profile first.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                loader.visibility = View.GONE
                Log.e("AUTOFILL_ERROR", "Failed to fetch profile", e)
            }
    }
}