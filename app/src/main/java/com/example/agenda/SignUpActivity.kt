package com.example.agenda

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.agenda.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Handle sign-up button click
        binding.signupButton.setOnClickListener {
            val email = binding.emailEdit.text.toString().trim()
            val password = binding.passwordEdit.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEdit.text.toString().trim()

            // Validate input
            if (!validateInput(email, password, confirmPassword)) return@setOnClickListener

            // Show progress bar
            binding.progressBar.visibility = View.VISIBLE

            // Create user with Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    // Hide progress bar
                    binding.progressBar.visibility = View.GONE

                    if (task.isSuccessful) {
                        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        // Navigate to login activity
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, task.exception?.localizedMessage ?: "Sign-up failed", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Handle redirect to Login page
        binding.loginRedirectButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    // Validate user input
    private fun validateInput(email: String, password: String, confirmPassword: String): Boolean {
        if (email.isEmpty()) {
            binding.emailEdit.error = "Email is required"
            binding.emailEdit.requestFocus()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEdit.error = "Enter a valid email"
            binding.emailEdit.requestFocus()
            return false
        }
        if (password.isEmpty()) {
            binding.passwordEdit.error = "Password is required"
            binding.passwordEdit.requestFocus()
            return false
        }
        if (password.length < 6) {
            binding.passwordEdit.error = "Password must be at least 6 characters"
            binding.passwordEdit.requestFocus()
            return false
        }
        if (password != confirmPassword) {
            binding.confirmPasswordEdit.error = "Passwords do not match"
            binding.confirmPasswordEdit.requestFocus()
            return false
        }
        return true
    }
}
