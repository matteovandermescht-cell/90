package com.smith.screenbot

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var feedbackText: TextView
    private var receiverRegistered = false

    private val capturePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val serviceIntent = Intent(this, CaptureService::class.java).apply {
                    putExtra(CaptureService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(CaptureService.EXTRA_RESULT_DATA, result.data)
                }
                ContextCompat.startForegroundService(this, serviceIntent)
                feedbackText.text = "Freigabe erhalten. Bildschirmaufnahme wird gestartet."
            } else {
                feedbackText.text = "Bildschirmaufnahme nicht freigegeben."
            }
        }

    private val feedbackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra(AppEvents.EXTRA_MESSAGE) ?: return
            feedbackText.text = message
            refreshStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)
        feedbackText = findViewById(R.id.feedbackText)

        findViewById<Button>(R.id.accessibilityBtn).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.startBtn).setOnClickListener {
            val manager = getSystemService(MediaProjectionManager::class.java)
            capturePermissionLauncher.launch(manager.createScreenCaptureIntent())
        }
        findViewById<Button>(R.id.stopBtn).setOnClickListener {
            stopService(Intent(this, CaptureService::class.java))
            feedbackText.text = "Stopp angefordert."
            refreshStatus()
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 301)
        }
        refreshStatus()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(AppEvents.ACTION_FEEDBACK)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(feedbackReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(feedbackReceiver, filter)
        }
        receiverRegistered = true
        refreshStatus()
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(feedbackReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        if (!::statusText.isInitialized) return
        val accessibilityEnabled = isOurAccessibilityServiceEnabled()
        val captureActive = CaptureService.isRunning
        val statusColor = if (accessibilityEnabled && captureActive) R.color.status_green else R.color.status_orange
        statusText.setTextColor(ContextCompat.getColor(this, statusColor))
        statusText.text = "Accessibility: ${if (accessibilityEnabled) "aktiv" else "aus"}\nBildschirmaufnahme: ${if (captureActive) "aktiv" else "inaktiv"}"
    }

    private fun isOurAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
        if (!enabled) return false
        val services = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val component = ComponentName(this, TouchService::class.java).flattenToString()
        return services.split(':').any { it.equals(component, ignoreCase = true) }
    }
}
