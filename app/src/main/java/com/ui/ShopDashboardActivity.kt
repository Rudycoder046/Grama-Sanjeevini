package com.example.medicine.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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
        totalStockTxt = findViewById(R.id.totalStockCount)
        expiringTxt = findViewById(R.id.expiringCount)
        
        val totalStockCard = totalStockTxt.parent as android.view.View
        val expiringCard = expiringTxt.parent as android.view.View

        adapter = MedicineAdapter(inventoryList)
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
