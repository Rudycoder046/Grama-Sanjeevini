package com.example.medicine.auth

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.medicine.R
import com.example.medicine.ui.MainActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val registerBtn = findViewById<Button>(R.id.registerBtn)
        val goToMedicalBtn = findViewById<Button>(R.id.goToMedicalBtn)

        // Apply slide-up animation to the root layout
        val rootLayout = findViewById<LinearLayout>(R.id.loginRoot)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        rootLayout.startAnimation(slideUp)

        goToMedicalBtn.setOnClickListener {
            startActivity(Intent(this, MedicalLoginActivity::class.java))
        }

        // LOGIN
        loginBtn.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "Error: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
        // REGISTER
        registerBtn.setOnClickListener {
            auth.createUserWithEmailAndPassword(
                email.text.toString(),
                password.text.toString()
            ).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}