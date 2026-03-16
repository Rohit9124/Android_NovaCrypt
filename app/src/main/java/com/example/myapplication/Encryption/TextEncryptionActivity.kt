package com.example.myapplication.Encryption

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.InputType
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.PreferenceManager
import com.example.myapplication.R
import com.example.myapplication.Authentication.BiometricAuth
import com.example.myapplication.Storage.PrivateStorage
import androidx.appcompat.app.AlertDialog
import com.google.android.material.color.MaterialColors

class TextEncryptionActivity : AppCompatActivity() {

    // Layouts
    private lateinit var mainMenuLayout: ConstraintLayout
    private lateinit var encryptLayout: ConstraintLayout
    private lateinit var decryptLayout: ConstraintLayout

    // Encrypt views
    private lateinit var plainInput: EditText
    private lateinit var keyField: EditText
    private lateinit var cipherOutput: TextView
    private lateinit var toggleKeyBtn: ImageButton

    // Decrypt views
    private lateinit var cipherInput: EditText
     private lateinit var keyInputText: EditText
    private lateinit var plainOutput: TextView
    private lateinit var btnAddPrivateText: Button
    private lateinit var toggleKeyInputBtn: ImageButton

    private lateinit var privateStorage: PrivateStorage
    private var biometricVerified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_encryption)

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Text Encryption"

        val tvGuide: TextView = findViewById(R.id.tvGuide)
        val guideText = getString(R.string.quick_guide_text)
        tvGuide.text = Html.fromHtml(guideText, Html.FROM_HTML_MODE_LEGACY)

        privateStorage = PrivateStorage(this)

        bindViews()
        setMenuHandlers()
        setEncryptHandlers()
        setDecryptHandlers()
        setPrivateHandlers()
        setPasswordToggleHandlers()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (mainMenuLayout.visibility != View.VISIBLE) {
                switchLayoutAnimated(mainMenuLayout)
            } else {
                finish()
                overridePendingTransition(R.anim.premium_fade_in_reverse, R.anim.premium_fade_out_reverse)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        biometricVerified = false
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.premium_fade_in_reverse, R.anim.premium_fade_out_reverse)
    }

    private fun bindViews() {
        mainMenuLayout = findViewById(R.id.layout_main_menu_text)
        encryptLayout = findViewById(R.id.layout_encrypt_text)
        decryptLayout = findViewById(R.id.layout_decrypt_text)

        plainInput = findViewById(R.id.plain_input)
        keyField = findViewById(R.id.key_field)
        cipherOutput = findViewById(R.id.cipher_output)
        toggleKeyBtn = findViewById(R.id.btn_toggle_key_visibility)

        cipherInput = findViewById(R.id.cipher_input)
        keyInputText = findViewById(R.id.key_input_text)
        plainOutput = findViewById(R.id.plain_output)
        btnAddPrivateText = findViewById(R.id.btn_add_private_text)
        btnAddPrivateText.visibility = View.GONE
        toggleKeyInputBtn = findViewById(R.id.btn_toggle_key_input_visibility)
    }

    private fun setMenuHandlers() {
        findViewById<Button>(R.id.btn_encrypt_menu_text).setOnClickListener {
            switchLayoutAnimated(encryptLayout)
        }
        findViewById<Button>(R.id.btn_decrypt_menu_text).setOnClickListener {
            switchLayoutAnimated(decryptLayout)
        }
    }

    private fun setEncryptHandlers() {
        findViewById<Button>(R.id.encrypt_text_button).setOnClickListener {
            val plain = plainInput.text.toString()
            val password = keyField.text.toString()

            if (plain.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Enter text and key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val cipherB64 = EncryptionUtils.encryptText(plain, password)
                cipherOutput.text = cipherB64
                incrementEncryptedCount("text")
            } catch (e: Exception) {
                Toast.makeText(this, "Encrypt failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        findViewById<Button>(R.id.copy_cipher_button).setOnClickListener {
            val text = cipherOutput.text?.toString().orEmpty()
            if (text.isNotEmpty()) {
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("cipher", text))
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.share_cipher_button).setOnClickListener {
            val text = cipherOutput.text?.toString().orEmpty()
            if (text.isNotEmpty()) {
                val i = Intent(Intent.ACTION_SEND)
                i.type = "text/plain"
                i.putExtra(Intent.EXTRA_TEXT, text)
                startActivity(Intent.createChooser(i, "Share cipher"))
            }
        }
    }

    private fun setDecryptHandlers() {
        findViewById<Button>(R.id.decrypt_text_button).setOnClickListener {
            val cipherB64 = cipherInput.text.toString()
            val password = keyInputText.text.toString()

            if (cipherB64.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Paste cipher and enter key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val biometricEnabled = prefs.getBoolean("pref_use_biometric", false)

            if (biometricEnabled && !biometricVerified) {
                BiometricAuth.showBiometricPrompt(
                    this,
                    onSuccess = {
                        biometricVerified = true
                        decryptText(cipherB64, password)
                    },
                    onFailure = { err ->
                        Toast.makeText(this, "Auth failed: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                decryptText(cipherB64, password)
            }
        }
    }

    private fun decryptText(cipherB64: String, password: String) {
        try {
            val plain = EncryptionUtils.decryptText(cipherB64, password)
            plainOutput.text = plain

            if (btnAddPrivateText.visibility != View.VISIBLE) {
                btnAddPrivateText.apply {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate()
                        .alpha(1f)
                        .setDuration(800)
                        .start()
                }
            }

        } catch (e: Exception) {
            // GCM tag mismatch or wrong key triggers this
            Toast.makeText(this, "Decrypt failed: ${e.message ?: "Invalid cipher or key"}", Toast.LENGTH_LONG).show()
            btnAddPrivateText.visibility = View.GONE
        }
    }

    private fun setPrivateHandlers() {
        btnAddPrivateText.setOnClickListener {
            val plain = plainOutput.text?.toString().orEmpty()
            if (plain.isBlank()) {
                Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 20, 50, 0)
            }

            val editText = EditText(this).apply {
                hint = "Enter file name"
                setText("decrypted_note")
            }

            container.addView(editText)

            val dialog = AlertDialog.Builder(this)
                .setTitle("Save Text As :")
                .setView(container)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create()

            dialog.setOnShowListener {
                val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                val colorPrimary = MaterialColors.getColor(
                    this,
                    com.google.android.material.R.attr.colorOnSurface,
                    android.graphics.Color.BLACK
                )
                saveButton.setTextColor(colorPrimary)
                cancelButton.setTextColor(colorPrimary)

                saveButton.setOnClickListener {
                    val customName = editText.text.toString().trim()
                    if (customName.isNotEmpty()) {
                        try {
                            val finalName =
                                if (customName.endsWith(".txt")) customName else "$customName.txt"
                            privateStorage.saveText(finalName, plain)
                            Toast.makeText(
                                this,
                                "Saved as $finalName in Private Folder",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                this,
                                "Save failed: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(this, "Filename cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            dialog.show()
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
        }
    }

    private fun setPasswordToggleHandlers() {
        toggleKeyBtn.setOnClickListener {
            togglePasswordVisibility(keyField, toggleKeyBtn)
        }
        toggleKeyInputBtn.setOnClickListener {
            togglePasswordVisibility(keyInputText, toggleKeyInputBtn)
        }
    }

    private fun togglePasswordVisibility(editText: EditText, button: ImageButton) {
        if (editText.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            button.setImageResource(R.drawable.baseline_visibility_off_24)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            button.setImageResource(R.drawable.baseline_visibility_off_24)
        }
        editText.setSelection(editText.text.length)
    }

    private fun switchLayoutAnimated(target: View) {
        val layouts = listOf(mainMenuLayout, encryptLayout, decryptLayout)

        layouts.forEach { layout ->
            if (layout != target && layout.visibility == View.VISIBLE) {
                layout.animate()
                    .translationX(-50f)
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction {
                        layout.visibility = View.GONE
                        layout.translationX = 0f
                    }.start()
            }
        }

        target.alpha = 0f
        target.translationX = 50f
        target.visibility = View.VISIBLE
        target.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    private fun incrementEncryptedCount(type: String) {
        val prefs = getSharedPreferences("encryption_stats", MODE_PRIVATE)
        val editor = prefs.edit()
        when (type) {
            "text" -> editor.putInt("text_count", prefs.getInt("text_count", 0) + 1)
        }
        editor.apply()
    }
}
