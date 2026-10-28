// language: kotlin, file: MainActivity.kt, target: Android 26+
package com.smith.screenbot

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val status = TextView(this).apply {
            textSize = 18f
            setPadding(32, 48, 32, 32)
            text = "ScreenBot — model validation scaffold\n\nBuild setup loaded.\nA compatible model.tflite is not included.\nPlace it in app/src/main/assets/ before running inference."
        }
        setContentView(status)
    }
}
