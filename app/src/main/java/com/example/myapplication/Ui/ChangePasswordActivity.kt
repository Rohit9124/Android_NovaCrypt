package com.example.myapplication

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var currentPasswordEt: EditText
    private lateinit var newPasswordEt: EditText
    private lateinit var confirmPasswordEt: EditText
    private lateinit var btnChange: Button

    private lateinit var firebaseAuth: FirebaseAuth
    private var currentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        // 🔹 Enter animation
        overridePendingTransition(R.anim.premium_slide_in, R.anim.premium_slide_out)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Change Password"

        currentPasswordEt = findViewById(R.id.etCurrentPassword)
        newPasswordEt = findViewById(R.id.etNewPassword)
        confirmPasswordEt = findViewById(R.id.etConfirmPassword)
        btnChange = findViewById(R.id.btnChangePassword)

        firebaseAuth = FirebaseAuth.getInstance()
        currentUser = firebaseAuth.currentUser

        btnChange.setOnClickListener {
            val currentPassword = currentPasswordEt.text.toString()
            val newPassword = newPasswordEt.text.toString()
            val confirmPassword = confirmPasswordEt.text.toString()

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            changePassword(currentPassword, newPassword)
        }
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val user = currentUser ?: return
        val credential =
            com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, currentPassword)

        user.reauthenticate(credential).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show()
                        finish()
                        // 🔹 Exit animation
                        overridePendingTransition(R.anim.premium_slide_in_reverse, R.anim.premium_slide_out_reverse)
                    } else {
                        Toast.makeText(
                            this,
                            "Failed to change password: ${updateTask.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "Authentication failed: ${authTask.exception?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            overridePendingTransition(R.anim.premium_slide_in_reverse, R.anim.premium_slide_out_reverse)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.premium_slide_in_reverse, R.anim.premium_slide_out_reverse)
    }
}
