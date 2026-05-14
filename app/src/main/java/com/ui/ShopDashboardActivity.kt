package com.example.medicine.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medicine.R
import com.example.medicine.auth.LoginActivity
import com.example.medicine.model.Medicine
import com.example.medicine.util.SampleData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShopDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val inventoryList = mutableListOf<Medicine>()
    private lateinit var adapter: MedicineAdapter
    
    private lateinit var totalStockTxt: TextView
    private lateinit var expiringTxt: TextView
    
    private var isFilterActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val recycler = findViewById<RecyclerView>(R.id.inventoryRecycler)
        val logoutBtn = findViewById<TextView>(R.id.logoutBtn)
        val addBtn = findViewById<Button>(R.id.addNewBtn)
        val shopHeader = findViewById<TextView>(R.id.shopNameHeader)
        val settingsBtn = findViewById<ImageView>(R.id.settingsBtn)
        val profileBtn = findViewById<ImageView>(R.id.profileBtn)
        totalStockTxt = findViewById(R.id.totalStockCount)
        expiringTxt = findViewById(R.id.expiringCount)
        
        val totalStockCard = totalStockTxt.parent as android.view.View
        val expiringCard = expiringTxt.parent as android.view.View

        adapter = MedicineAdapter(inventoryList, 
            onEditClick = { med ->
                val intent = Intent(this, AddMedicineActivity::class.java)
                intent.putExtra("MED_ID", med.id)
                intent.putExtra("MED_NAME", med.name)
                intent.putExtra("MED_SHOP", med.shopName)
                intent.putExtra("MED_PRICE", med.price)
                intent.putExtra("MED_EXPIRY", med.expiryDate)
                intent.putExtra("MED_DIST", med.distance)
                intent.putExtra("MED_QTY", med.quantity)
                intent.putExtra("MED_LIFE", med.lifeSaving)
                intent.putExtra("MED_LAT", med.latitude)
                intent.putExtra("MED_LON", med.longitude)
                intent.putExtra("MED_ADDR", med.shopAddress)
                intent.putExtra("MED_MLINK", med.shopMapsLink)
                startActivity(intent)
            },
            onDeleteClick = { med ->
                showDeleteConfirmation(med)
            }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        val currentUserEmail = auth.currentUser?.email
        shopHeader.text = "Logged in as: $currentUserEmail"

        loadInventory()
        
        // Removed manual button, so we auto-seed if empty
        checkAndSeedData()

        totalStockCard.setOnClickListener {
            if (isFilterActive) {
                isFilterActive = false
                adapter.updateList(inventoryList)
                Toast.makeText(this, "Showing all medicines", Toast.LENGTH_SHORT).show()
                // Reset card styling if needed
            }
        }

        expiringCard.setOnClickListener {
            val expiringList = filterExpiringMedicines()
            if (expiringList.isNotEmpty()) {
                isFilterActive = true
                adapter.updateList(expiringList)
                Toast.makeText(this, "Showing only expiring medicines", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No expiring medicines found", Toast.LENGTH_SHORT).show()
            }
        }

        addBtn.setOnClickListener {
            startActivity(Intent(this, AddMedicineActivity::class.java))
        }

        logoutBtn.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        settingsBtn.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("IS_MEDICAL", true)
            startActivity(intent)
        }

        profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        
        updateWelcomeMessage(shopHeader)
    }

    private fun updateWelcomeMessage(header: TextView) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("profiles").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("name") ?: ""
                    if (name.isNotEmpty()) {
                        header.text = "Welcome, $name"
                    }
                }
            }
    }

    private fun loadInventory() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        db.collection("medicines")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Failed to load inventory", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    inventoryList.clear()
                    var expiringCount = 0
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val today = Date()

                    for (doc in snapshots) {
                        val med = doc.toObject(Medicine::class.java)
                        inventoryList.add(med)
                        
                        // Count expiring/expired items
                        try {
                            val expDate = sdf.parse(med.expiryDate)
                            if (expDate != null) {
                                val diffDays = (expDate.time - today.time) / (1000 * 60 * 60 * 24)
                                if (diffDays < 90) expiringCount++
                            }
                        } catch (ex: Exception) {}
                    }
                    
                    adapter.notifyDataSetChanged()
                    totalStockTxt.text = inventoryList.size.toString()
                    expiringTxt.text = expiringCount.toString()
                    
                    if (expiringCount > 0) {
                        showExpiryAlert(expiringCount)
                    }
                }
            }
    }

    private fun showExpiryAlert(count: Int) {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Expiry Alert")
            .setMessage("You have $count medicine(s) that are either expired or expiring within 3 months. Please check your inventory.")
            .setPositiveButton("View Inventory") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showDeleteConfirmation(med: Medicine) {
        AlertDialog.Builder(this)
            .setTitle("Delete Medicine")
            .setMessage("Are you sure you want to remove '${med.name}' from your inventory?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("medicines").document(med.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Medicine deleted", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkAndSeedData() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val email = user.email ?: ""
        
        // Only seed data for the developer/test account
        if (email != "medical@gmail.com") {
            return
        }
        
        db.collection("medicines")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { 
                if (it.isEmpty) {
                    // Seed the 150 medicines for this user so they have data
                    val batch = db.batch()
                    val sampleList = SampleData.getMedicines()
                    
                    for (item in sampleList) {
                        val ref = db.collection("medicines").document()
                        item["userId"] = uid // Ensure they belong to this user
                        batch.set(ref, item)
                    }
                    
                    batch.commit().addOnSuccessListener {
                        Toast.makeText(this, "Test data seeded for developer account!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun filterExpiringMedicines(): List<Medicine> {
        val expiringList = mutableListOf<Medicine>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Date()

        for (med in inventoryList) {
            try {
                val expDate = sdf.parse(med.expiryDate)
                if (expDate != null) {
                    val diffDays = (expDate.time - today.time) / (1000 * 60 * 60 * 24)
                    if (diffDays < 90) {
                        expiringList.add(med)
                    }
                }
            } catch (e: Exception) {}
        }
        return expiringList
    }
}
