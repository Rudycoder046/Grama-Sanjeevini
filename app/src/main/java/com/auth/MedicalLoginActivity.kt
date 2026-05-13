package com.example.medicine.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.medicine.R
import com.example.medicine.ui.ShopDashboardActivity
import com.example.medicine.ui.AddMedicineActivity
import com.google.firebase.auth.FirebaseAuth

class MedicalLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medical_login)

        auth = FirebaseAuth.getInstance()

        val email = findViewById<EditText>(R.id.medEmail)
        val password = findViewById<EditText>(R.id.medPassword)
        val loginBtn = findViewById<Button>(R.id.medLoginBtn)
        val registerBtn = findViewById<Button>(R.id.medRegisterBtn)

        loginBtn.setOnClickListener {
            val mail = email.text.toString().trim()
            val pass = password.text.toString().trim()

            if (mail.isNotEmpty() && pass.isNotEmpty()) {
                auth.signInWithEmailAndPassword(mail, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(this, "Welcome Medical User", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, ShopDashboardActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Login Failed: ${it.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show()
            }
        }

        registerBtn.setOnClickListener {
            val mail = email.text.toString().trim()
            val pass = password.text.toString().trim()

            if (mail.isNotEmpty() && pass.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(mail, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(this, "Medical Account Created", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}