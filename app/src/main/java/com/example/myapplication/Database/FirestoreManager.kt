package com.example.myapplication.Database

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreManager {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun submitFeedback(
        context: Context,
        rating: Int,
        comment: String,
        onComplete: (() -> Unit)? = null // optional callback
    ) {
        // Use email instead of UID
        val userEmail = auth.currentUser?.email ?: "anonymous"

        val feedback = hashMapOf(
            "userEmail" to userEmail,
            "rating" to rating,
            "comment" to comment,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("feedback")
            .add(feedback)
            .addOnSuccessListener {
                Toast.makeText(context, "Feedback submitted successfully", Toast.LENGTH_SHORT).show()
                onComplete?.invoke() // optional callback
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error submitting feedback: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
