package com.example.medicine.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import com.example.medicine.R
import com.example.medicine.model.Medicine
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private val list = mutableListOf<Medicine>()
    private lateinit var adapter: MedicineAdapter
    private lateinit var loader: ProgressBar
    private lateinit var recycler: RecyclerView
    private lateinit var searchContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()

        val searchBox = findViewById<EditText>(R.id.searchBox)
        val btn = findViewById<Button>(R.id.searchBtn)
        recycler = findViewById(R.id.recyclerView)
        loader = findViewById(R.id.searchLoader)
        searchContainer = findViewById(R.id.searchContainer)

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
    }

    private fun search(name: String) {
        loader.visibility = View.VISIBLE
        
        db.collection("medicines")
            .get()
            .addOnSuccessListener { querySnapshot ->
                loader.visibility = View.GONE
                list.clear()
                
                Log.d("SEARCH_DEBUG", "Found ${querySnapshot.size()} documents in collection")
                for (doc in querySnapshot) {
                    val data = doc.data
                    val nameInDb = data["name"] as? String ?: ""
                    val distInDb = (data["distance"] as? Number)?.toDouble() ?: 0.0
                    
                    Log.d("SEARCH_DEBUG", "Checking: $nameInDb, Dist: $distInDb against Query: $name")
                    
                    if (nameInDb.contains(name, ignoreCase = true) && distInDb <= 50.0) {
                        val med = doc.toObject(Medicine::class.java)
                        list.add(med)
                        Log.d("SEARCH_DEBUG", "Match found and added: ${med.name}")
                    }
                }
                
                adapter.notifyDataSetChanged()
                
                if (list.isEmpty()) {
                    Toast.makeText(this, "No medicines found", Toast.LENGTH_LONG).show()
                    recycler.visibility = View.GONE
                    // Move search back to center if no results
                    val params = searchContainer.layoutParams as RelativeLayout.LayoutParams
                    params.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
                    params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
                    searchContainer.layoutParams = params
                } else {
                    recycler.visibility = View.VISIBLE
                    // Move search to top to show results
                    val params = searchContainer.layoutParams as RelativeLayout.LayoutParams
                    params.removeRule(RelativeLayout.CENTER_IN_PARENT)
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                    searchContainer.layoutParams = params
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