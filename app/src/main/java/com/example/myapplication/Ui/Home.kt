package com.example.myapplication.Ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.Authentication.SignInActivity
import com.example.myapplication.Database.FirestoreManager
import com.example.myapplication.Encryption.FileEncryptionActivity
import com.example.myapplication.Encryption.ImageEncryptionActivity
import com.example.myapplication.Encryption.TextEncryptionActivity
import com.example.myapplication.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.util.Locale
import android.util.TypedValue
import com.example.myapplication.Storage.PrivateFolderActivity
import android.content.pm.PackageManager


// 🔹 Data class for future feature list
data class Feature(val title: String, val description: String)

class Home : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout

    // 🔹 Locale hook for pre-Android 13 devices
    override fun attachBaseContext(newBase: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(newBase)
        val languageCode = prefs.getString("language", "en") ?: "en"

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        val wrappedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(wrappedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val textViewLogo = findViewById<TextView>(R.id.textView5)
        applyNeonStrokeAnimation(textViewLogo, this)


        // 🔹 Setup Drawer + Toolbar
        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.open_nav, R.string.close_nav
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            private val interpolator = DecelerateInterpolator()
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                val content = findViewById<View>(R.id.fragment_container)
                val smoothOffset = interpolator.getInterpolation(slideOffset)
                val scale = 1 - 0.08f * smoothOffset
                content.scaleX = scale
                content.scaleY = scale
                val alpha = 1 - 0.25f * smoothOffset
                content.alpha = alpha
                val translationX = drawerView.width * 0.1f * smoothOffset
                content.translationX = translationX
            }
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {
                val content = findViewById<View>(R.id.fragment_container)
                content.scaleX = 1f
                content.scaleY = 1f
                content.alpha = 1f
                content.translationX = 0f
            }
            override fun onDrawerStateChanged(newState: Int) {}
        })

        // 🔹 Show Email in Navigation Drawer Header
        val headerView = navigationView.getHeaderView(0)
        val tvEmail = headerView.findViewById<TextView>(R.id.tvUsername)
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val email = firebaseUser?.email ?: "Guest"
        tvEmail.text = email

        // ---- Dashboard Cards ----
        val encryptTextCard: MaterialCardView = findViewById(R.id.card_encrypt_text)
        val encryptImageCard: MaterialCardView = findViewById(R.id.card_encrypt_image)
        val encryptFileCard: MaterialCardView = findViewById(R.id.card_encrypt_file)

        encryptTextCard.setOnClickListener {
            val intent = Intent(this, TextEncryptionActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.premium_fade_in, R.anim.premium_fade_out)
            startActivity(intent, options.toBundle())
        }

        encryptImageCard.setOnClickListener {
            val intent = Intent(this, ImageEncryptionActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.premium_fade_in, R.anim.premium_fade_out)
            startActivity(intent, options.toBundle())
        }

        encryptFileCard.setOnClickListener {
            val intent = Intent(this, FileEncryptionActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.premium_fade_in, R.anim.premium_fade_out)
            startActivity(intent, options.toBundle())
        }
    }
    // Function for neon glow + white stroke
    fun applyNeonStrokeAnimation(textView: TextView, context: Context) {
        // Enable software layer for shadow/stroke
        textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        // Detect dark mode
        val isDarkMode = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // Base glow color for current mode
        val baseGlowColor = if (isDarkMode) Color.parseColor("#584B71") else Color.parseColor("#8692F7")

        // Pulsing glow animation
        val glowAnimator = ValueAnimator.ofObject(
            ArgbEvaluator(),
            baseGlowColor,
            Color.WHITE
        ).apply {
            duration = 1500L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                val glowColor = animator.animatedValue as Int
                textView.setShadowLayer(16f, 0f, 0f, glowColor)
            }
            start()
        }

        // Stroke effect (always white)
        textView.paint.apply {
            style = android.graphics.Paint.Style.FILL_AND_STROKE
            strokeWidth = 8f
            color = Color.WHITE
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
    }




    // 🔹 Handle Action Bar Menu Clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_rate -> {
                showRatingDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 🔹 Show Rating Dialog
    private fun showRatingDialog() {
        // Inflate your custom dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_feedback, null)

        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBarDialog)
        val etComment = dialogView.findViewById<EditText>(R.id.etCommentDialog)

        // Create AlertDialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Rate NovaCrypt")
            .setView(dialogView)
            .setPositiveButton("Submit", null) // will override later
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val submitButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            // Get primary color using Material3 utility (safe)
            val colorPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)

            // Apply color to buttons
            submitButton.setTextColor(colorPrimary)
            cancelButton.setTextColor(colorPrimary)

            // Handle submit click
            submitButton.setOnClickListener {
                val rating = ratingBar.rating.toInt()
                val comment = etComment.text.toString().trim()

                if (comment.isNotEmpty()) {
                    FirestoreManager.submitFeedback(this, rating, comment) {
                        Toast.makeText(this, "Thanks for your feedback!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                } else {
                    Toast.makeText(this, "Please enter feedback", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
    }

    // 🔹 Handle Navigation Drawer clicks
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show()
            R.id.nav_rate -> showRatingDialog()
            R.id.nav_share -> {
                try {
                    // Your Google Drive folder link
                    val driveLink = "https://drive.google.com/drive/folders/14NdU81Uqix5CeFSAn4xtreU5i6qCV0ln"

                    // Intent to share the link
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Download NovaCrypt APK")
                        putExtra(Intent.EXTRA_TEXT, "Hey! You can download the NovaCrypt APK from this link:\n$driveLink")
                    }

                    // Show chooser to let user pick an app
                    startActivity(Intent.createChooser(shareIntent, "Share APK via"))

                } catch (e: Exception) {
                    Toast.makeText(this, "Error sharing link: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }


            R.id.nav_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                val options = ActivityOptions.makeCustomAnimation(
                    this,
                    R.anim.premium_fade_in,
                    R.anim.premium_fade_out
                )
                startActivity(intent, options.toBundle())
            }
            R.id.nav_private_folder -> {
                val intent = Intent(this, PrivateFolderActivity::class.java)
                val options = ActivityOptions.makeCustomAnimation(
                    this,
                    R.anim.premium_fade_in,
                    R.anim.premium_fade_out
                )
                startActivity(intent, options.toBundle())
            }


            R.id.nav_logout -> {
                val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                sharedPref.edit().clear().apply()

                val intent = Intent(this, SignInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                val options = ActivityOptions.makeCustomAnimation(
                    this,
                    R.anim.premium_fade_in,
                    R.anim.premium_fade_out
                )
                startActivity(intent, options.toBundle())
                finish()
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}

// 🔹 Adapter for future feature carousel (not used yet)
class FeatureAdapter(private val features: List<Feature>) :
    RecyclerView.Adapter<FeatureAdapter.FeatureViewHolder>() {

    class FeatureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvTitle)
        val description: TextView = itemView.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feature_card, parent, false)
        return FeatureViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        val feature = features[position]
        holder.title.text = feature.title
        holder.description.text = feature.description
    }

    override fun getItemCount(): Int = features.size
}
