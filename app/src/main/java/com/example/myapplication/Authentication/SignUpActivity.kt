package com.example.myapplication.Authentication

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.myapplication.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {

    private lateinit var signUpButton: AppCompatButton
    private lateinit var goToSignIn: TextView
    private lateinit var emailEt: TextInputEditText
    private lateinit var passEt: TextInputEditText
    private lateinit var confirmPassEt: TextInputEditText
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.sing_up)

        // Initialize views
        signUpButton = findViewById(R.id.button)
        goToSignIn = findViewById(R.id.textView)
        emailEt = findViewById(R.id.emailEt)
        passEt = findViewById(R.id.passET)
        confirmPassEt = findViewById(R.id.confirmPassEt)

        firebaseAuth = FirebaseAuth.getInstance()

        // Navigate to Sign In
        goToSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        // Smooth premium click animation for Sign Up button
        signUpButton.setOnClickListener { button ->
            // Animate button press
            button.animate().scaleX(0.95f).scaleY(0.95f).setDuration(150).withEndAction {
                button.animate().scaleX(1f).scaleY(1f).setDuration(150).start()

                // After animation completes, perform sign up
                performSignUp()
            }.start()
        }
    }

    private fun performSignUp() {
        val email = emailEt.text.toString()
        val pass = passEt.text.toString()
        val confirmPass = confirmPassEt.text.toString()

        if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
            if (pass == confirmPass) {
                firebaseAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, SignInActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, task.exception?.message ?: "Sign up failed", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Empty Fields Are Not Allowed!", Toast.LENGTH_SHORT).show()
        }
    }
}
