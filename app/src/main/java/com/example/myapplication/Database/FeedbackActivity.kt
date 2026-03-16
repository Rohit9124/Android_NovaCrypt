package com.example.myapplication.Database

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.myapplication.Database.FirestoreManager
import com.example.myapplication.R

class FeedbackActivity : AppCompatActivity() {

    private lateinit var ratingBar: RatingBar
    private lateinit var etComment: EditText
    private lateinit var btnSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        // Toolbar setup
        val toolbar = findViewById<Toolbar>(R.id.toolbarFeedback)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Feedback"

        toolbar.setNavigationOnClickListener { onBackPressed() }

        // Initialize views
        ratingBar = findViewById(R.id.ratingBar)
        etComment = findViewById(R.id.etComment)
        btnSubmit = findViewById(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val comment = etComment.text.toString().trim()

            if (comment.isNotEmpty()) {
                FirestoreManager.submitFeedback(this, rating, comment) {
                    // Callback after successful submission
                    etComment.text.clear()
                    ratingBar.rating = 0f
                }
            } else {
                etComment.error = "Please enter a comment"
            }
        }
    }
}
