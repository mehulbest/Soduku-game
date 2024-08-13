// src/main/java/com/example/sudokugame/StartActivity.kt
package com.example.sudokugame

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val startButton: Button = findViewById(R.id.startButton)
        val exitButton: Button = findViewById(R.id.exitButton)

        startButton.setOnClickListener {
            // Start the main Sudoku game activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close the start screen so it doesn't appear when pressing back
        }

        exitButton.setOnClickListener {
            // Exit the app
            finishAffinity() // Closes the entire app
        }
    }
}
