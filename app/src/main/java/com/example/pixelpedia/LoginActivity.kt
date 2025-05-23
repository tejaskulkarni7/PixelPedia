package com.example.pixelpedia


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.Executor


class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var emailEditText: EditText;
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var resetPasswordButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        //Initialize Firebase
        auth = FirebaseAuth.getInstance()
        auth.signOut()

        //Initialize our views here
        emailEditText = findViewById(R.id.resetPasswordText)
        passwordEditText = findViewById(R.id.loginPasswordText)
        loginButton = findViewById(R.id.resetPasswordSubmit)
        registerButton = findViewById(R.id.loginRegisterButton)
        resetPasswordButton =  findViewById(R.id.resetPasswordButton)


        //Handling Login logic
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { loginTask ->
                        if (loginTask.isSuccessful) {
                            val user = FirebaseAuth.getInstance().currentUser
                            if (user?.isEmailVerified == true) {
                                val sharedPreferences = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                                sharedPreferences.edit().putString("user_email", user.email).apply()

                                val db = FirebaseFirestore.getInstance()
                                user.uid.let { userId ->
                                    db.collection("users").document(userId).get()
                                        .addOnSuccessListener { document ->
                                            val biometricEnabled = document.getBoolean("biometric_enabled") ?: false
                                            if (biometricEnabled) {
                                                Log.d("TAG", "BIOMETRIC ENABLED")
                                                promptBiometricLogin()
                                            } else {
                                                Log.d("TAG", "BIOMETRIC NOT ENABLED")
                                                startActivity(Intent(this, MainActivity::class.java))
                                                finish()
                                            }
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this, "Failed to fetch biometric setting.", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this, MainActivity::class.java))
                                            finish()
                                        }
                                }
                            } else {
                                Toast.makeText(
                                    this,
                                    "Please verify your email before logging in.",
                                    Toast.LENGTH_LONG
                                ).show()
                                auth.signOut()
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "Login Failed: ${loginTask.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter your email and password", Toast.LENGTH_SHORT).show()
            }
        }


        //Set logic for signing up here
        registerButton.setOnClickListener {
            //Open the register activity(screen)
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        resetPasswordButton.setOnClickListener{
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)
        }


    }

    override fun onStart() {
        super.onStart()
        // Check if a user is already logged in
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // User is already logged in, navigate to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            // If user is not logged in, show biometric prompt or login page
        }
    }

    private fun promptBiometricLogin() {
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(this@LoginActivity, "Biometric authentication successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@LoginActivity, "Biometric authentication failed", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@LoginActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Confirm your identity")
            .setDeviceCredentialAllowed(true) // Allow PIN/password fallback
            .build()

        biometricPrompt.authenticate(promptInfo)
    }



}