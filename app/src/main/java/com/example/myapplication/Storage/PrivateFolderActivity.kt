package com.example.myapplication.Storage

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.FileProvider
import com.example.myapplication.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AlertDialog
import com.google.android.material.color.MaterialColors
import android.graphics.Color

class PrivateFolderActivity : AppCompatActivity() {

    private lateinit var storage: PrivateStorage
    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var containerText: LinearLayout
    private lateinit var containerImages: LinearLayout
    private lateinit var containerFiles: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var llContent: LinearLayout

    private lateinit var btnText: Button
    private lateinit var btnImages: Button
    private lateinit var btnFiles: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_private_folder)

        // 🔹 Setup enter animation
        overridePendingTransition(R.anim.premium_slide_in, R.anim.premium_slide_out)

        // 🔹 Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Private Folder"

        storage = PrivateStorage(this)

        // 🔹 Get all views
        llContent = findViewById(R.id.llContent) // Wrap all sections in XML inside this
        containerText = findViewById(R.id.containerText)
        containerImages = findViewById(R.id.containerImages)
        containerFiles = findViewById(R.id.containerFiles)
        tvEmpty = findViewById(R.id.tvEmpty)

        btnText = findViewById(R.id.btnText)
        btnImages = findViewById(R.id.btnImages)
        btnFiles = findViewById(R.id.btnFiles)

        // 🔹 Hide content initially
        llContent.visibility = View.GONE
        tvEmpty.visibility = View.GONE

        // 🔹 Button toggles
        btnText.setOnClickListener { toggleSection(containerText, btnText) }
        btnImages.setOnClickListener { toggleSection(containerImages, btnImages) }
        btnFiles.setOnClickListener { toggleSection(containerFiles, btnFiles) }

        // 🔹 Check biometric availability
        val bm = BiometricManager.from(this)
        val can = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (can == BiometricManager.BIOMETRIC_SUCCESS) {
            showBiometricPrompt()
        } else {
            Toast.makeText(this, "No biometric or PIN/pattern set up", Toast.LENGTH_LONG).show()
            showContent()
            updateList()
        }
    }

    // 🔹 Show content layout after auth
    private fun showContent() {
        llContent.visibility = View.VISIBLE
        tvEmpty.visibility = View.VISIBLE
    }

    // 🔹 Inflate search menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_private_storage, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.queryHint = "Search files..."
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterPrivateItems(newText.orEmpty())
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            overridePendingTransition(
                R.anim.premium_slide_in_reverse,
                R.anim.premium_slide_out_reverse
            )
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.premium_slide_in_reverse, R.anim.premium_slide_out_reverse)
    }

    private fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Private Folder")
            .setSubtitle("Authenticate with fingerprint or device credential")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    runOnUiThread {
                        showContent()
                        updateList()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    runOnUiThread {
                        Toast.makeText(
                            this@PrivateFolderActivity,
                            "Authentication error: $errString",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    runOnUiThread {
                        Toast.makeText(
                            this@PrivateFolderActivity,
                            "Authentication failed, try again",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    // 🔹 Update and display items
    private fun updateList() {
        val items = storage.listItems()
        containerText.removeAllViews()
        containerImages.removeAllViews()
        containerFiles.removeAllViews()

        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

        for (item in items) {
            val cardView = layoutInflater.inflate(R.layout.item_private, containerText, false)

            val ivIcon = cardView.findViewById<ImageView>(R.id.ivIcon)
            val tvName = cardView.findViewById<TextView>(R.id.tvName)
            val tvSize = cardView.findViewById<TextView>(R.id.tvSize)

            tvName.text = item.displayName
            cardView.tag = item.displayName

            val sizeKb = (item.sizeBytes / 1024).coerceAtLeast(1)
            tvSize.text = "$sizeKb KB • ${item.mime ?: "unknown"} • ${formatDate(item.savedAt)}"

            when {
                item.mime?.startsWith("image") == true -> ivIcon.setImageResource(android.R.drawable.ic_menu_gallery)
                item.mime?.startsWith("text") == true -> ivIcon.setImageResource(android.R.drawable.ic_menu_edit)
                else -> ivIcon.setImageResource(android.R.drawable.ic_menu_save)
            }

            cardView.setOnClickListener { showItem(item) }
            cardView.setOnLongClickListener {
                confirmDelete(item)
                true
            }

            when {
                item.mime?.startsWith("text") == true -> containerText.addView(cardView)
                item.mime?.startsWith("image") == true -> containerImages.addView(cardView)
                else -> containerFiles.addView(cardView)
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun showItem(item: PrivateItem) {
        try {
            val bytes = storage.readFileBytes(item.filename)

            when {
                // 🔹 Image files
                item.mime?.startsWith("image") == true -> {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    val iv = ImageView(this).apply { setImageBitmap(bitmap) }

                    android.app.AlertDialog.Builder(this)
                        .setTitle(item.displayName)
                        .setView(iv)
                        .setPositiveButton("Close", null)
                        .show()
                }

// 🔹 Text files
                item.mime?.startsWith("text") == true -> {
                    val text = String(bytes, Charsets.UTF_8)

                    val dialog = MaterialAlertDialogBuilder(this)
                        .setTitle(item.displayName)
                        .setMessage(text)
                        .setPositiveButton("Close", null)
                        .create()

                    // Rounded background
                    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)

                    // Style button after dialog is shown
                    dialog.setOnShowListener {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                            MaterialColors.getColor(
                                this,
                                com.google.android.material.R.attr.colorPrimary,
                                android.graphics.Color.BLACK
                            )
                        )
                    }

                    dialog.show()
                }

                // 🔹 Other file types
                else -> {
                    // Save file to cache
                    val tempFile = File.createTempFile("private_", "_${item.displayName}", cacheDir)
                    tempFile.outputStream().use { it.write(bytes) }

                    // Debug log
                    println("✅ Saved file: ${tempFile.absolutePath}, size=${tempFile.length()} bytes")

                    // Build URI
                    val fileUri = FileProvider.getUriForFile(
                        this,
                        "$packageName.provider",
                        tempFile
                    )

                    // Detect MIME type (fallback to */*)
                    val mimeType = item.mime ?: contentResolver.getType(fileUri) ?: "*/*"

                    val openIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(fileUri, "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    try {
                        startActivity(Intent.createChooser(openIntent, "Open with"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.app.AlertDialog.Builder(this)
                            .setTitle(item.displayName)
                            .setMessage(
                                "Cannot open this file type.\n" +
                                        "Size: ${item.sizeBytes} bytes\n" +
                                        "Mime: $mimeType"
                            )
                            .setPositiveButton("Close", null)
                            .show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to open file", Toast.LENGTH_SHORT).show()
        }
    }


    @Suppress("UNRESOLVED_REFERENCE")
    fun confirmDelete(item: PrivateItem) {
        // Create a Material-themed AlertDialog
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete \"${item.displayName}\"?")
            .setPositiveButton("Delete") { _, _ ->
                storage.delete(item.filename)
                updateList()
                Toast.makeText(this, "${item.displayName} deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Apply rounded background (same as your feedback dialog)
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)

        dialog.show()

        // Style buttons with MaterialColors for consistency
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorPrimary,
                Color.BLACK
            )
        )
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorPrimary,
                Color.DKGRAY
            )
        )
    }

    // 🔹 Expand/collapse with smooth animation
    private fun toggleSection(container: LinearLayout, button: Button) {
        if (container.visibility == View.VISIBLE) {
            collapse(container)
            setArrow(button, false)
        } else {
            expand(container)
            setArrow(button, true)
        }
    }

    private fun expand(v: View) {
        v.measure(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val targetHeight = v.measuredHeight
        v.layoutParams.height = 0
        v.visibility = View.VISIBLE
        val anim: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                v.layoutParams.height =
                    if (interpolatedTime == 1f) LinearLayout.LayoutParams.WRAP_CONTENT
                    else (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean = true
        }
        anim.duration = (targetHeight / v.context.resources.displayMetrics.density).toLong()
        v.startAnimation(anim)
    }

    private fun collapse(v: View) {
        val initialHeight = v.measuredHeight
        val anim: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                if (interpolatedTime == 1f) {
                    v.visibility = View.GONE
                } else {
                    v.layoutParams.height =
                        initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean = true
        }
        anim.duration = (initialHeight / v.context.resources.displayMetrics.density).toLong()
        v.startAnimation(anim)
    }

    private fun setArrow(button: Button, expanded: Boolean) {
        val drawable = if (expanded) R.drawable.ic_arrow_drop_up else R.drawable.ic_arrow_drop_down
        button.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0)
    }

    // 🔹 Filter logic for search
    private fun filterPrivateItems(query: String) {
        filterContainer(containerText, query)
        filterContainer(containerImages, query)
        filterContainer(containerFiles, query)

        tvEmpty.visibility =
            if (containerText.childCount + containerImages.childCount + containerFiles.childCount == 0)
                View.VISIBLE else View.GONE
    }

    private fun filterContainer(container: LinearLayout, query: String) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            val tag = child.tag?.toString() ?: ""
            child.visibility = if (tag.contains(query, ignoreCase = true)) View.VISIBLE else View.GONE
        }
    }
}
