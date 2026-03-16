package com.example.myapplication.Authentication

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.myapplication.R
import com.example.myapplication.Ui.Home
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var goBack: TextView
    private lateinit var emailEt: TextInputEditText
    private lateinit var passEt: TextInputEditText
    private lateinit var signInButton: AppCompatButton
    private lateinit var biometricSignInTv: TextView
    private lateinit var forgotPasswordTv: TextView // Forgot Password TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.sign_in)

        // Initialize views
        goBack = findViewById(R.id.textView)
        emailEt = findViewById(R.id.emailEt)
        passEt = findViewById(R.id.passET)
        signInButton = findViewById(R.id.button)
        biometricSignInTv = findViewById(R.id.tvBiometricSignIn)
        forgotPasswordTv = findViewById(R.id.tvForgotPassword) // make sure XML has this

        firebaseAuth = FirebaseAuth.getInstance()

        // Go back to Sign Up screen
        goBack.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // Smooth animation for Sign In button
        signInButton.setOnClickListener { button ->
            button.animate().scaleX(0.95f).scaleY(0.95f).setDuration(150).withEndAction {
                button.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                performSignIn()
            }.start()
        }

        // Biometric sign-in click
        biometricSignInTv.setOnClickListener {
            if (BiometricAuth.canAuthenticateWithBiometric(this)) {
                BiometricAuth.showBiometricPrompt(
                    this,
                    onSuccess = { performInstantSignIn() },
                    onFailure = { error ->
                        Toast.makeText(this, "Biometric auth failed: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Toast.makeText(this, "Biometric authentication not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Forgot Password logic
        forgotPasswordTv.setOnClickListener {
            val email = emailEt.text.toString().trim()

            if (email.isNotEmpty()) {
                sendResetEmail(email)
            } else {
                // Show dialog if email field is empty
                val resetMailEt = EditText(this)
                resetMailEt.hint = "Enter your email"

                AlertDialog.Builder(this)
                    .setTitle("Reset Password")
                    .setMessage("Enter your registered email to receive reset link")
                    .setView(resetMailEt)
                    .setPositiveButton("Submit") { _, _ ->
                        val enteredEmail = resetMailEt.text.toString().trim()
                        if (enteredEmail.isNotEmpty()) {
                            sendResetEmail(enteredEmail)
                        } else {
                            Toast.makeText(this, "Email cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show()
            }
        }
    }

    // Helper function for sending reset email
    private fun sendResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Error sending reset email",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun performSignIn() {
        val email = emailEt.text.toString()
        val pass = passEt.text.toString()

        if (email.isNotEmpty() && pass.isNotEmpty()) {
            firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    startHome()
                } else {
                    Toast.makeText(this, task.exception?.message ?: "Sign in failed", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Empty Fields Are Not Allowed!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performInstantSignIn() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            startHome()
        } else {
            Toast.makeText(this, "No previous sign-in available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startHome() {
        startActivity(Intent(this, Home::class.java))
        overridePendingTransition(R.anim.premium_slide_in, R.anim.premium_slide_out)
        finish()
    }
}
