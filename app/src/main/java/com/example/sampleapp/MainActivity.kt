package com.example.sampleapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private var clickCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val clickButton = findViewById<Button>(R.id.add_user_btn)
        val countText = findViewById<TextView>(R.id.status_text)

        clickButton.setOnClickListener {
            clickCount++
            countText.text = "Button clicked $clickCount times"
            
            if (clickCount % 5 == 0) {
                Toast.makeText(this, "Wow! You've clicked $clickCount times!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
