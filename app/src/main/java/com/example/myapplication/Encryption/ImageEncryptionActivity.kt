package com.example.myapplication.Encryption

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.content.Intent.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.text.Html
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.example.myapplication.R
import com.example.myapplication.Authentication.BiometricAuth
import com.example.myapplication.Storage.PrivateStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import javax.crypto.SecretKey
import androidx.appcompat.app.AlertDialog
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class ImageEncryptionActivity : AppCompatActivity() {

    // Encryption vars
    private var secretKey: SecretKey? = null
    private var encryptedImageBytes: ByteArray? = null
    private var selectedImageBitmap: Bitmap? = null

    // Decryption vars
    private var encryptedFileBytes: ByteArray? = null
    private var lastDecryptedBitmap: Bitmap? = null

    // Layout containers
    private lateinit var mainMenuLayout: ConstraintLayout
    private lateinit var encryptLayout: ConstraintLayout
    private lateinit var decryptLayout: ConstraintLayout
    private lateinit var secureViewLayout: ConstraintLayout

    // Encryption UI
    private lateinit var uploadImageView: ImageView
    private lateinit var encryptedImageView: ImageView
    private lateinit var keyField: EditText
    private lateinit var uploadButton: Button
    private lateinit var encryptButton: Button
    private lateinit var copyKeyButton: ImageButton
    private lateinit var downloadButton: ImageButton
    private lateinit var shareButton: ImageButton

    // Decryption UI
    private lateinit var uploadEncryptedButton: Button
    private lateinit var keyInputField: EditText
    private lateinit var uploadedTick: ImageView
    private lateinit var decryptButton: Button
    private lateinit var btnAddToPrivate: Button

    // Secure view UI
    private lateinit var secureImageView: ImageView
    private lateinit var countdownTextView: TextView
    private val viewingTimeMillis: Long = 30_000

    private var isBiometricAuthenticated = false

    // Image picker
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val selectedImage: Uri? = result.data?.data
                selectedImage?.let {
                    val inputStream: InputStream? = contentResolver.openInputStream(it)
                    selectedImageBitmap = BitmapFactory.decodeStream(inputStream)
                    uploadImageView.setImageBitmap(selectedImageBitmap)
                }
            }
        }

    // File picker
    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val selectedFileUri: Uri? = result.data?.data
                selectedFileUri?.let {
                    val inputStream: InputStream? = contentResolver.openInputStream(it)
                    encryptedFileBytes = inputStream?.readBytes()
                    uploadedTick.visibility = if (encryptedFileBytes != null) View.VISIBLE else View.INVISIBLE
                    Toast.makeText(this, if (encryptedFileBytes != null) "Encrypted file uploaded" else "Upload failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_encryption)

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Image Encryption"

        val tvGuideImage: TextView = findViewById(R.id.tvGuideImage)
        val guideText = getString(R.string.quick_guide)
        tvGuideImage.text = Html.fromHtml(guideText, Html.FROM_HTML_MODE_LEGACY)

        initViews()
        setupMenu()
        setupEncryption()
        setupDecryption()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (mainMenuLayout.visibility != ConstraintLayout.VISIBLE) {
                switchLayoutAnimated(mainMenuLayout)
            } else {
                finish()
                // 👁 add reverse animation on toolbar back
                overridePendingTransition(R.anim.premium_fade_in_reverse, R.anim.premium_fade_out_reverse)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onBackPressed() {
        super.onBackPressed()
        // 👁 add reverse animation on device back
        overridePendingTransition(R.anim.premium_fade_in_reverse, R.anim.premium_fade_out_reverse)
    }

    private fun initViews() {
        mainMenuLayout = findViewById(R.id.layout_main_menu)
        encryptLayout = findViewById(R.id.layout_encrypt)
        decryptLayout = findViewById(R.id.layout_decrypt)
        secureViewLayout = findViewById(R.id.layout_secure_view)

        // Encryption
        uploadImageView = findViewById(R.id.upload_image_view)
        encryptedImageView = findViewById(R.id.ecrypted_image)
        keyField = findViewById(R.id.key_field)
        uploadButton = findViewById(R.id.upload_button)
        encryptButton = findViewById(R.id.encrypt_button)
        copyKeyButton = findViewById(R.id.copy_icon)
        downloadButton = findViewById(R.id.download_icon)
        shareButton = findViewById(R.id.shareButton)

        // Decryption
        uploadEncryptedButton = findViewById(R.id.upload_encrypted_button)
        keyInputField = findViewById(R.id.key_input)
        uploadedTick = findViewById(R.id.upload_tick)
        decryptButton = findViewById(R.id.decrypt_button)
        btnAddToPrivate = findViewById(R.id.btn_add_to_private)
        btnAddToPrivate.visibility = View.GONE

        // Secure viewing
        secureImageView = findViewById(R.id.decrypted_imageView)
        countdownTextView = findViewById(R.id.countdownTextView)
    }

    private fun setupMenu() {
        findViewById<Button>(R.id.btn_encrypt_menu).setOnClickListener {
            switchLayoutAnimated(encryptLayout)
        }
        findViewById<Button>(R.id.btn_decrypt_menu).setOnClickListener {
            switchLayoutAnimated(decryptLayout)
        }
    }

    private fun setupEncryption() {
        uploadButton.setOnClickListener {
            val intent = Intent(ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            imagePickerLauncher.launch(intent)
        }

        encryptButton.setOnClickListener {
            if (selectedImageBitmap == null) {
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent double clicks
            encryptButton.isEnabled = false
            Toast.makeText(this, "Encrypting, please wait...", Toast.LENGTH_SHORT).show()

            // Run encryption in background safely
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    secretKey = EncryptionUtils.generateKey()
                    val result = EncryptionUtils.encryptImage(this@ImageEncryptionActivity, selectedImageBitmap!!, secretKey!!)

                    withContext(Dispatchers.Main) {
                        encryptButton.isEnabled = true
                        if (result != null) {
                            encryptedImageBytes = result
                            encryptedImageView.setImageResource(R.drawable.baseline_lock_24)
                            keyField.setText(EncryptionUtils.keyToString(secretKey!!))
                            Toast.makeText(this@ImageEncryptionActivity, "Image encrypted successfully!", Toast.LENGTH_SHORT).show()
                            incrementEncryptedCount("image")
                        } else {
                            Toast.makeText(this@ImageEncryptionActivity, "Encryption failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        encryptButton.isEnabled = true
                        Toast.makeText(this@ImageEncryptionActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        downloadButton.setOnClickListener {
            encryptedImageBytes?.let { saveEncryptedImageToFile(it, "encrypted_image.bin") }
        }

        copyKeyButton.setOnClickListener {
            val keyString = keyField.text.toString()
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Encryption Key", keyString))
            Toast.makeText(this, "Key copied", Toast.LENGTH_SHORT).show()
        }

        shareButton.setOnClickListener {
            if (encryptedImageBytes == null) {
                Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val tempFile = File.createTempFile("encrypted_image", ".bin", cacheDir)
            tempFile.outputStream().use { it.write(encryptedImageBytes) }
            val fileUri = FileProvider.getUriForFile(this, "$packageName.provider", tempFile)
            val keyString = keyField.text.toString()
            val shareIntent = Intent(ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(EXTRA_STREAM, fileUri)
                putExtra(EXTRA_TEXT, "Encryption key:\n$keyString")
                addFlags(FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(createChooser(shareIntent, "Share Encrypted Image"))
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Suppress("UNRESOLVED_REFERENCE")
    private fun setupDecryption() {
        uploadedTick.visibility = View.INVISIBLE

        uploadEncryptedButton.setOnClickListener {
            val intent = Intent(ACTION_GET_CONTENT)
            intent.type = "*/*"
            filePickerLauncher.launch(intent)
        }

        decryptButton.setOnClickListener {
            val keyString = keyInputField.text.toString().trim()

            if (encryptedFileBytes == null) {
                Toast.makeText(this, "Please upload an encrypted file", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (keyString.isBlank()) {
                Toast.makeText(this, "Please enter a key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val useBiometric = prefs.getBoolean("pref_use_biometric", false)

            // Define decryption logic
            val doDecrypt: () -> Unit = {
                Toast.makeText(this, "Decrypting, please wait...", Toast.LENGTH_SHORT).show()

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val secretKey = EncryptionUtils.stringToKey(keyString)
                        val decryptedBitmap = EncryptionUtils.decryptImage(encryptedFileBytes!!, secretKey)

                        withContext(Dispatchers.Main) {
                            lastDecryptedBitmap = decryptedBitmap

                            if (decryptedBitmap != null) {
                                showSecureView(decryptedBitmap)

                                btnAddToPrivate.apply {
                                    visibility = View.VISIBLE
                                    alpha = 0f
                                    animate().alpha(1f).setDuration(800).start()
                                }

                                Toast.makeText(
                                    this@ImageEncryptionActivity,
                                    "Image decrypted successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@ImageEncryptionActivity,
                                    "Decryption failed or invalid key",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ImageEncryptionActivity,
                                "Error: ${e.localizedMessage ?: "Unknown error"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }


            if (useBiometric && !isBiometricAuthenticated) {
                if (BiometricAuth.canAuthenticateWithBiometric(this)) {
                    BiometricAuth.showBiometricPrompt(
                        this,
                        onSuccess = {
                            isBiometricAuthenticated = true
                            doDecrypt()
                        },
                        onFailure = { err ->
                            Toast.makeText(this, "Authentication failed: $err", Toast.LENGTH_SHORT)
                                .show()
                        })
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.biometric_not_enrolled),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                doDecrypt()
            }
        }

        btnAddToPrivate.setOnClickListener {
            lastDecryptedBitmap?.let { bitmap ->
                // Wrap EditText in a LinearLayout for proper padding
                val container = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(50, 20, 50, 0) // left, top, right, bottom padding
                }

                val editText = EditText(this).apply {
                    hint = "Enter image name"
                    setText("DecryptedImage") // Default suggestion
                }

                container.addView(editText)

                // Create AlertDialog
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Save Image As:")
                    .setView(container)
                    .setPositiveButton("Save", null) // override later
                    .setNegativeButton("Cancel", null)
                    .create()

                dialog.setOnShowListener {
                    val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                    // Get primary color using Material3 utility
                    val colorPrimary = MaterialColors.getColor(
                        this,
                        com.google.android.material.R.attr.colorOnSurface,
                        android.graphics.Color.BLACK
                    )

                    saveButton.setTextColor(colorPrimary)
                    cancelButton.setTextColor(colorPrimary)

                    // Override Save click
                    saveButton.setOnClickListener {
                        val customName = editText.text.toString().trim()
                        if (customName.isNotEmpty()) {
                            try {
                                val storage = PrivateStorage(this)
                                val baos = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                                val bytes = baos.toByteArray()

                                val finalName =
                                    if (customName.endsWith(".png")) customName else "$customName.png"

                                storage.saveBytes(finalName, bytes, "image/png")

                                Toast.makeText(
                                    this,
                                    "Image saved as $finalName in Private Folder",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dialog.dismiss()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } else {
                            Toast.makeText(this, "Filename cannot be empty", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }

                dialog.show()
                // Rounded corners background
                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
            } ?: Toast.makeText(this, "No decrypted image available", Toast.LENGTH_SHORT).show()
        }
    }

            @RequiresApi(Build.VERSION_CODES.R)
    private fun showSecureView(bitmap: Bitmap?) {
        switchLayoutAnimated(secureViewLayout)
        secureImageView.setImageBitmap(bitmap)
        startCountdownTimer()
        enableImmersiveMode()
    }

    private fun saveEncryptedImageToFile(encryptedBytes: ByteArray, fileName: String) {
        try {
            val resolver = contentResolver
            val timeStamp = System.currentTimeMillis()
            val safeName = fileName.replace("/", "_").replace("\\", "_")
            val finalName = "${safeName.removeSuffix(".bin")}_$timeStamp.bin"

            val contentValues = ContentValues().apply {
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, finalName)
                put(MediaStore.Files.FileColumns.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, "Download/MyApp") // custom folder in Downloads
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            if (uri == null) {
                Toast.makeText(this, "Failed to create file entry", Toast.LENGTH_SHORT).show()
                return
            }

            resolver.openOutputStream(uri)?.use { output ->
                output.write(encryptedBytes)
                output.flush()
            }

            Toast.makeText(this, "Encrypted image saved to Downloads/MyApp/$finalName", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    // ✅ Animated layout switch
    private fun switchLayoutAnimated(target: View) {
        val layouts = listOf(mainMenuLayout, encryptLayout, decryptLayout, secureViewLayout)

        // Hide all other layouts with fade out + slide left
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

        // Show target layout with fade in + slide from right
        target.alpha = 0f
        target.translationX = 50f
        target.visibility = View.VISIBLE
        target.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(300)
            .start()
    }


    private fun startCountdownTimer() {
        object : CountDownTimer(viewingTimeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdownTextView.text = "Time left: ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                switchLayoutAnimated(decryptLayout)
            }
        }.start()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun enableImmersiveMode() {
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun incrementEncryptedCount(type: String) {
        val prefs = getSharedPreferences("encryption_stats", MODE_PRIVATE)
        val editor = prefs.edit()
        when (type) {
            "image" -> editor.putInt("image_count", prefs.getInt("image_count", 0) + 1)
        }
        editor.apply()
    }
}
