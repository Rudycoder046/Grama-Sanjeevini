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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val name = findViewById<EditText>(R.id.inputName)
        val shop = findViewById<EditText>(R.id.inputShopName)
        val price = findViewById<EditText>(R.id.inputPrice)
        val expiry = findViewById<EditText>(R.id.inputExpiry)
        val distance = findViewById<EditText>(R.id.inputDistance)
        val quantity = findViewById<EditText>(R.id.inputQuantity)
        saveBtn = findViewById(R.id.saveBtn)
        loader = findViewById(R.id.saveLoader)

        saveBtn.setOnClickListener {
            val medName = name.text.toString().trim()
            val medShop = shop.text.toString().trim()
            val medPriceStr = price.text.toString().trim()
            val medExpiry = expiry.text.toString().trim()
            val medDistStr = distance.text.toString().trim()
            val medQtyStr = quantity.text.toString().trim()

            if (medName.isEmpty() || medShop.isEmpty() || medExpiry.isEmpty() || medPriceStr.isEmpty() || medDistStr.isEmpty() || medQtyStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val medPrice = medPriceStr.toDoubleOrNull() ?: 0.0
            val medDist = medDistStr.toDoubleOrNull() ?: 0.0
            val medQty = medQtyStr.toIntOrNull() ?: 0

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Session Expired. Please login again.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val medicineData = hashMapOf(
                "name" to medName,
                "shopName" to medShop,
                "price" to medPrice,
                "expiryDate" to medExpiry,
                "distance" to medDist,
                "quantity" to medQty,
                "lifeSaving" to false,
                "userId" to currentUser.uid
            )

            saveBtn.isEnabled = false
            loader.visibility = View.VISIBLE
            
            db.collection("medicines")
                .add(medicineData)
                .addOnSuccessListener {
                    loader.visibility = View.GONE
                    saveBtn.isEnabled = true
                    Toast.makeText(this, "Successfully Saved to Database!", Toast.LENGTH_LONG).show()
                    clearFields(name, shop, price, expiry, distance, quantity)
                    finish() // Close activity after success so it updates dashboard
                }
                .addOnFailureListener { e ->
                    loader.visibility = View.GONE
                    saveBtn.isEnabled = true
                    Log.e("UPLOAD_ERROR", "Failed to upload", e)
                    
                    if (e.message?.contains("disabled") == true) {
                        Toast.makeText(this, "CRITICAL: Cloud Firestore API is disabled in Firebase Console. Please enable it.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun clearFields(vararg edits: EditText) {
        for (edit in edits) {
            edit.text.clear()
        }
    }
}