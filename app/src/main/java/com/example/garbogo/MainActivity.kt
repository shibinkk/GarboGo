package com.example.garbogo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val usernameEditText = findViewById<EditText>(R.id.username)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Check if username and password are correct
            if (username == "user1" && password == "1234") {
                // Start HomeActivity
                val intent = Intent(this, Home::class.java)
                startActivity(intent)

                // Show toast message for successful login
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
            } else {
                // Show toast message for incorrect credentials
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
            }
            if (username == "auth1" && password == "0000") {
                // Start HomeActivity
                val intent = Intent(this, AuthHome::class.java)
                startActivity(intent)

                // Show toast message for successful login
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
            } else {
                // Show toast message for incorrect credentials
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
            }
        }
    }
}