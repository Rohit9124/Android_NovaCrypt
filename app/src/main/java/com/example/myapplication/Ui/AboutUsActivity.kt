package com.example.myapplication.Ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.myapplication.R

class AboutUsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

        overridePendingTransition(R.anim.premium_slide_in, R.anim.premium_slide_out)

        val toolbar = findViewById<Toolbar>(R.id.toolbarAbout)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "About Us"

        toolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(R.anim.premium_slide_in_reverse, R.anim.premium_slide_out_reverse)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.premium_slide_in_reverse, R.anim.premium_slide_out_reverse)
    }
}
