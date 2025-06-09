package com.example.pocketguard.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pocketguard.R
import com.example.pocketguard.data.SharedPreferencesManager

class PasscodeActivity : AppCompatActivity() {
    private lateinit var etPasscode: EditText
    private lateinit var sharedPreferencesManager: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passcode)

        // Hide the action bar
        supportActionBar?.hide()

        sharedPreferencesManager = SharedPreferencesManager(this)
        etPasscode = findViewById(R.id.etPasscode)

        // Set up numeric keypad buttons
        setupKeypadButtons()
    }

    private fun setupKeypadButtons() {
        // Set up number buttons
        for (i in 0..9) {
            val buttonId = resources.getIdentifier("btn$i", "id", packageName)
            findViewById<Button>(buttonId)?.setOnClickListener {
                if (etPasscode.text.length < 4) {
                    etPasscode.append(i.toString())
                    checkPasscode()
                }
            }
        }

        // Set up clear button
        findViewById<Button>(R.id.btnClear)?.setOnClickListener {
            etPasscode.setText("")
        }

        // Set up delete button
        findViewById<Button>(R.id.btnDelete)?.setOnClickListener {
            val currentText = etPasscode.text.toString()
            if (currentText.isNotEmpty()) {
                etPasscode.setText(currentText.substring(0, currentText.length - 1))
            }
        }
    }

    private fun checkPasscode() {
        if (etPasscode.text.length == 4) {
            if (etPasscode.text.toString() == sharedPreferencesManager.getPasscode()) {
                // Navigate to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Incorrect passcode", Toast.LENGTH_SHORT).show()
                etPasscode.setText("")
            }
        }
    }
} 