package com.example.myapplication.Encryption

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Html
import android.text.InputType
import android.view.MenuItem
import android.view.View
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
import java.io.File
import androidx.appcompat.app.AlertDialog
import com.google.android.material.color.MaterialColors


class FileEncryptionActivity : AppCompatActivity() {

    // Layouts
    private lateinit var mainMenuLayout: ConstraintLayout
    private lateinit var encryptLayout: ConstraintLayout
    private lateinit var decryptLayout: ConstraintLayout

    // Encryption UI
    private lateinit var uploadButton: Button
    private lateinit var encryptButton: Button
    private lateinit var keyField: EditText
    private lateinit var copyKeyButton: ImageButton
    private lateinit var downloadButton: ImageButton
    private lateinit var shareButton: ImageButton
    private lateinit var uploadFileName: TextView
    private lateinit var toggleKeyBtn: ImageButton
    private lateinit var encryptStatusMessage: TextView

    // Decryption UI
    private lateinit var uploadEncryptedButton: Button
    private lateinit var keyInputField: EditText
    private lateinit var uploadedTick: ImageView
    private lateinit var decryptButton: Button
    private lateinit var decryptedFileName: TextView
    private lateinit var openFileButton: Button
    private lateinit var btnAddToPrivate: Button
    private lateinit var toggleKeyInputBtn: ImageButton
    private lateinit var decryptStatusMessage: TextView

    // State
    private var inputFile: File? = null
    private var encryptedFile: File? = null
    private var decryptedFile: File? = null
    private var uploadedEncryptedFileName: String? = null
    private var biometricChecked = false

    // File pickers
    private val filePickerForEncrypt =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                inputFile = copyUriToTempFile(it, "to_encrypt")
                uploadFileName.text = getFileName(it)
            }
        }

    private val filePickerForDecrypt =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                encryptedFile = copyUriToTempFile(it, "to_decrypt")
                uploadedTick.visibility = View.VISIBLE
                uploadedEncryptedFileName = getFileName(it)
                Toast.makeText(this, "Encrypted file uploaded", Toast.LENGTH_SHORT).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_encryption)

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "File Encryption"

        val tvGuide: TextView = findViewById(R.id.tvGuideFile)
        val guideText = getString(R.string.quick_guide_file)
        tvGuide.text = Html.fromHtml(guideText, Html.FROM_HTML_MODE_LEGACY)

        initViews()
        setupMenu()
        setupEncryption()
        setupDecryption()
        setupPasswordToggle()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (mainMenuLayout.visibility != ConstraintLayout.VISIBLE) {
                switchLayoutAnimated(mainMenuLayout)
            } else {
                finish()
                overridePendingTransition(
                    R.anim.premium_fade_in_reverse,
                    R.anim.premium_fade_out_reverse
                )
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.premium_fade_in_reverse, R.anim.premium_fade_out_reverse)
    }

    private fun initViews() {
        mainMenuLayout = findViewById(R.id.layout_main_menu)
        encryptLayout = findViewById(R.id.layout_encrypt)
        decryptLayout = findViewById(R.id.layout_decrypt)

        uploadButton = findViewById(R.id.upload_button)
        encryptButton = findViewById(R.id.encrypt_button)
        keyField = findViewById(R.id.key_field)
        copyKeyButton = findViewById(R.id.copy_icon)
        downloadButton = findViewById(R.id.download_icon)
        shareButton = findViewById(R.id.shareButton)
        uploadFileName = findViewById(R.id.upload_file_name)
        toggleKeyBtn = findViewById(R.id.btn_toggle_key_visibility)
        encryptStatusMessage = findViewById(R.id.encrypt_status_message)
        decryptStatusMessage = findViewById(R.id.decrypt_status_message)

        uploadEncryptedButton = findViewById(R.id.upload_encrypted_button)
        keyInputField = findViewById(R.id.key_input)
        uploadedTick = findViewById(R.id.upload_tick)
        decryptButton = findViewById(R.id.decrypt_button)
        openFileButton = findViewById(R.id.open_file_button)
        btnAddToPrivate = findViewById(R.id.btn_add_to_private)
        btnAddToPrivate.visibility = View.GONE
        toggleKeyInputBtn = findViewById(R.id.btn_toggle_key_input_visibility)
    }

    private fun setupMenu() {
        findViewById<Button>(R.id.btn_encrypt_menu).setOnClickListener {
            switchLayoutAnimated(
                encryptLayout
            )
        }
        findViewById<Button>(R.id.btn_decrypt_menu).setOnClickListener {
            switchLayoutAnimated(
                decryptLayout
            )
        }
    }

    private fun setupEncryption() {
        uploadButton.setOnClickListener { filePickerForEncrypt.launch("*/*") }

        encryptButton.setOnClickListener {
            val password = keyField.text.toString()
            if (inputFile == null || password.isBlank()) {
                Toast.makeText(this, "Upload file and enter password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val outFile = File(cacheDir, "encrypted_${inputFile!!.name}.bin")
            EncryptionUtils.encryptFile(inputFile!!, outFile, password)
            encryptedFile = outFile

            // ✅ Increment file count
            incrementEncryptedCount("file")

            showStatusMessage(encryptStatusMessage, "File encrypted successfully!")
            Toast.makeText(this, "File encrypted!", Toast.LENGTH_SHORT).show()
        }


        copyKeyButton.setOnClickListener {
            val text = keyField.text.toString()
            if (text.isNotBlank()) {
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Password", text))
                Toast.makeText(this, "Password copied", Toast.LENGTH_SHORT).show()
            }
        }

        downloadButton.setOnClickListener {
            encryptedFile?.let { saveEncryptedFileToDownloads(it, "encrypted_file.bin") }
        }

        shareButton.setOnClickListener {
            encryptedFile?.let { file ->
                val fileUri = FileProvider.getUriForFile(this, "$packageName.provider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Encrypted File"))
            }
        }
    }

    @Suppress("UNRESOLVED_REFERENCE")
    private fun setupDecryption() {
        uploadedTick.visibility = View.INVISIBLE
        openFileButton.visibility = View.GONE
        btnAddToPrivate.visibility = View.GONE

        uploadEncryptedButton.setOnClickListener { filePickerForDecrypt.launch("*/*") }

        decryptButton.setOnClickListener {
            val password = keyInputField.text.toString()
            if (encryptedFile == null || password.isBlank()) {
                Toast.makeText(this, "Upload file and enter password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val useBiometric = prefs.getBoolean("pref_use_biometric", false)

            val doDecrypt = {
                try {
                    val outFile = File(
                        cacheDir,
                        "decrypted_${encryptedFile!!.name.removeSuffix(".bin")}"
                    )
                    EncryptionUtils.decryptFile(encryptedFile!!, outFile, password)
                    decryptedFile = outFile

                    // Show "Open File" button instantly
                    openFileButton.visibility = View.VISIBLE


                    btnAddToPrivate.apply {
                        visibility = View.VISIBLE
                        alpha = 0f
                        animate().alpha(1f).setDuration(800).start()
                    }

                    showStatusMessage(decryptStatusMessage, "File decrypted successfully!")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this,
                        "Decryption failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }


            if (useBiometric && !biometricChecked) {
                biometricChecked = true
                if (BiometricAuth.canAuthenticateWithBiometric(this)) {
                    BiometricAuth.showBiometricPrompt(
                        this,
                        onSuccess = { doDecrypt() },
                        onFailure = { err ->
                            Toast.makeText(
                                this,
                                "Authentication failed: $err",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
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

        openFileButton.setOnClickListener {
            decryptedFile?.let { file ->
                val fileUri = FileProvider.getUriForFile(this, "$packageName.provider", file)
                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(openIntent, "Open Decrypted File"))
            }
        }

        btnAddToPrivate.setOnClickListener {
            decryptedFile?.let { file ->
                // Wrap EditText in a LinearLayout for padding
                val container = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(50, 20, 50, 0) // left, top, right, bottom
                }

                val editText = EditText(this).apply {
                    hint = "Enter file name"
                    setText(file.nameWithoutExtension) // Suggest original name without extension
                }

                container.addView(editText)

                // Create AlertDialog
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Save As:")
                    .setView(container)
                    .setPositiveButton("Save", null) // override later
                    .setNegativeButton("Cancel", null)
                    .create()

                dialog.setOnShowListener {
                    val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                    // Apply Material3 theme color to buttons
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
                                val bytes = file.readBytes()

                                // Preserve original extension
                                val extension = file.extension
                                val finalName = if (extension.isNotEmpty()) {
                                    "$customName.$extension"
                                } else {
                                    customName
                                }

                                storage.saveBytes(finalName, bytes, "application/octet-stream")
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
                                    "Failed to save to Private Folder",
                                    Toast.LENGTH_SHORT
                                ).show()
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
            } ?: run {
                Toast.makeText(this, "No file to save", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun setupPasswordToggle() {
        var isPasswordVisibleEncrypt = false
        toggleKeyBtn.setOnClickListener {
            keyField.inputType = if (isPasswordVisibleEncrypt)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

            toggleKeyBtn.setImageResource(R.drawable.baseline_visibility_off_24)
            keyField.setSelection(keyField.text.length)
            isPasswordVisibleEncrypt = !isPasswordVisibleEncrypt
        }

        var isPasswordVisibleDecrypt = false
        toggleKeyInputBtn.setOnClickListener {
            keyInputField.inputType = if (isPasswordVisibleDecrypt)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

            toggleKeyInputBtn.setImageResource(R.drawable.baseline_visibility_off_24)
            keyInputField.setSelection(keyInputField.text.length)
            isPasswordVisibleDecrypt = !isPasswordVisibleDecrypt
        }
    }

    /** Show animated status message */
    private fun showStatusMessage(textView: TextView, message: String) {
        textView.text = message
        textView.visibility = View.VISIBLE
    }


    private fun saveEncryptedFileToDownloads(encryptedFile: File, fileName: String) {
        try {
            val resolver = contentResolver
            val timeStamp = System.currentTimeMillis()
            val safeName = fileName.replace("/", "_").replace("\\", "_")
            val finalName = "${safeName.removeSuffix(".bin")}_$timeStamp.bin"

            val contentValues = ContentValues().apply {
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, finalName)
                put(MediaStore.Files.FileColumns.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, "Download/MyApp")
            }

            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val uri = resolver.insert(collectionUri, contentValues)
            if (uri == null) {
                Toast.makeText(this, "Failed to create file entry", Toast.LENGTH_SHORT).show()
                return
            }

            resolver.openOutputStream(uri)?.use { output ->
                encryptedFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            Toast.makeText(
                this,
                "Encrypted file saved to Downloads/MyApp/$finalName",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
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
                    }
                    .start()
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

    private fun copyUriToTempFile(uri: Uri, prefix: String): File {
        val inputStream = contentResolver.openInputStream(uri)!!
        val tempFile = File.createTempFile(prefix, ".bin", cacheDir)
        tempFile.outputStream().use { inputStream.copyTo(it) }
        return tempFile
    }

    private fun getFileName(uri: Uri): String {
        var name = "Unknown"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) name = it.getString(index)
            }
        }
        return name
    }

    private fun incrementEncryptedCount(type: String) {
        val prefs = getSharedPreferences("encryption_stats", MODE_PRIVATE)
        val editor = prefs.edit()
        when (type) {
            "file" -> editor.putInt("file_count", prefs.getInt("file_count", 0) + 1)
            "folder" -> editor.putInt("folder_count", prefs.getInt("folder_count", 0) + 1)
            // add more types if needed
        }
        editor.apply()
    }
}
