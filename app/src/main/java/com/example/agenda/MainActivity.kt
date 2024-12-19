package com.example.agenda

import android.content.Intent
import android.util.Patterns
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.agenda.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
//firebase auth
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Handle login button click
        binding.loginButton.setOnClickListener {
            val email = binding.emailEdit.text.toString().trim()
            val password = binding.passwordEdit.text.toString().trim()

            // Validate input
            if (!validateInput(email, password)) return@setOnClickListener

            // Show progress bar
            binding.progressBar.visibility = View.VISIBLE

            // Sign in with Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    // Hide progress bar
                    binding.progressBar.visibility = View.GONE

                    if (task.isSuccessful) {
                        // Navigate to AgendaActivity
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, AgendaActivity::class.java))
                        finish()
                    } else {
                        // Show error message
                        Toast.makeText(this, task.exception?.localizedMessage ?: "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Handle redirect to Sign Up page
        binding.signupRedirectButton.setOnClickListener {
            // Navigate to SignUpActivity
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // Handle reset password click
        binding.resetPasswordTextView.setOnClickListener {
            showResetPasswordDialog()
        }
    }

    // Show dialog to reset password
    private fun showResetPasswordDialog() {
        val emailInput = EditText(this).apply {
            hint = "Enter your email"
        }

        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setView(emailInput)
            .setPositiveButton("Send Reset Email") { _, _ ->
                val email = emailInput.text.toString().trim()
                if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    sendPasswordResetEmail(email)
                } else {
                    Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    // Send reset email via Firebase
    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Reset email sent! Check your inbox.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to send reset email: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Validate user input
    private fun validateInput(email: String, password: String): Boolean {
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
        return true
    }
}
