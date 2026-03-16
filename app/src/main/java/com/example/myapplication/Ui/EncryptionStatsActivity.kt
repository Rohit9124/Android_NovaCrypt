package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class EncryptionStatsActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var tvSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encryption_stats)

        // 🔹 Enter animation when opening this activity
        overridePendingTransition(R.anim.premium_slide_in, R.anim.premium_slide_out)

        // 🔹 Setup Toolbar with back button
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Encryption Statistics"

        // 🔹 Initialize views
        pieChart = findViewById(R.id.pieChart)
        tvSummary = findViewById(R.id.tvSummary)

        updateStats() // Initial chart setup
    }

    override fun onResume() {
        super.onResume()
        updateStats() // Refresh chart whenever activity becomes visible
    }

    // 🔹 Handle toolbar back button with exit animation
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            overridePendingTransition(R.anim.premium_slide_in_reverse, R.anim.premium_slide_out_reverse)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // 🔹 Handle physical back button with exit animation
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.premium_slide_in_reverse, R.anim.premium_slide_out_reverse)
    }

    /** Updates chart and summary text from SharedPreferences */
    private fun updateStats() {
        val textCount = getEncryptedTextCount()
        val imageCount = getEncryptedImageCount()
        val fileCount = getEncryptedFileCount()

        // Update summary text
        tvSummary.text =
            "Encrypted Texts: $textCount\nEncrypted Images: $imageCount\nEncrypted Files: $fileCount"

        // Prepare PieChart entries
        val entries = ArrayList<PieEntry>()
        if (textCount > 0) entries.add(PieEntry(textCount.toFloat(), "Text"))
        if (imageCount > 0) entries.add(PieEntry(imageCount.toFloat(), "Image"))
        if (fileCount > 0) entries.add(PieEntry(fileCount.toFloat(), "File"))

        val dataSet = PieDataSet(entries, "Encryption Stats")
        dataSet.colors = listOf(
            Color.parseColor("#4CAF50"), // Green for Text
            Color.parseColor("#2196F3"), // Blue for Image
            Color.parseColor("#FF9800")  // Orange for File
        )
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 16f

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = "Encrypted Items"
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.animateY(1000)
        pieChart.invalidate() // Refresh chart
    }

    // Retrieve counts from SharedPreferences
    private fun getEncryptedTextCount(): Int {
        val prefs = getSharedPreferences("encryption_stats", MODE_PRIVATE)
        return prefs.getInt("text_count", 0)
    }

    private fun getEncryptedImageCount(): Int {
        val prefs = getSharedPreferences("encryption_stats", MODE_PRIVATE)
        return prefs.getInt("image_count", 0)
    }

    private fun getEncryptedFileCount(): Int {
        val prefs = getSharedPreferences("encryption_stats", MODE_PRIVATE)
        return prefs.getInt("file_count", 0)
    }

    /** Call this method whenever an encryption is performed to increment count */
    fun incrementEncryptedCount(type: String) {
        val prefs = getSharedPreferences("encryption_stats", MODE_PRIVATE)
        val editor = prefs.edit()
        when (type) {
            "text" -> editor.putInt("text_count", prefs.getInt("text_count", 0) + 1)
            "image" -> editor.putInt("image_count", prefs.getInt("image_count", 0) + 1)
            "file" -> editor.putInt("file_count", prefs.getInt("file_count", 0) + 1)
        }
        editor.apply()
        updateStats() // Optional: refresh chart immediately
    }
}
