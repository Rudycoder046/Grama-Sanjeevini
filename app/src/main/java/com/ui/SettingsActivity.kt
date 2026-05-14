package com.example.medicine.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.medicine.R
import com.example.medicine.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val resetCard = findViewById<View>(R.id.resetCard)
        val resetBtn = findViewById<Button>(R.id.masterResetBtn)
        val refreshBtn = findViewById<Button>(R.id.refreshBtn)

        // Only show Master Reset if a medical user is logged in
        // We check this by seeing if they came from the Dashboard
        val isMedical = intent.getBooleanExtra("IS_MEDICAL", false)
        if (isMedical) {
            resetCard.visibility = View.VISIBLE
        }

        resetBtn.setOnClickListener {
            showResetConfirmation()
        }

        refreshBtn.setOnClickListener {
            Toast.makeText(this, "Refreshing Application...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showResetConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Master Reset")
            .setMessage("Are you absolutely sure? This will PERMANENTLY delete all your uploaded medicines and your profile information. This cannot be undone.")
            .setPositiveButton("YES, DELETE EVERYTHING") { _, _ ->
                performMasterReset()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun performMasterReset() {
        val uid = auth.currentUser?.uid ?: return
        
        // 1. Delete Medicines
        db.collection("medicines")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot) {
                    batch.delete(doc.reference)
                }
                
                // 2. Delete Profile
                batch.delete(db.collection("profiles").document(uid))
                
                batch.commit().addOnSuccessListener {
                    Toast.makeText(this, "Master Reset Complete. Account is now fresh.", Toast.LENGTH_LONG).show()
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Reset Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}