package com.smith.screenbot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class CaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            stopSelf()
            return START_NOT_STICKY
        }
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (resultCode == 0 || resultData == null) {
            sendFeedback("Fehler: Bildschirmaufnahme-Freigabe fehlt.")
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            startForegroundCompat()
            val manager = getSystemService(MediaProjectionManager::class.java)
            projection = manager.getMediaProjection(resultCode, resultData)
            val metrics = getScreenMetrics()
            captureThread = HandlerThread("ScreenBotCapture").also { it.start() }
            captureHandler = Handler(captureThread!!.looper)
            imageReader = ImageReader.newInstance(
                metrics.widthPixels.coerceAtLeast(1),
                metrics.heightPixels.coerceAtLeast(1),
                android.graphics.PixelFormat.RGBA_8888,
                2
            )
            imageReader?.setOnImageAvailableListener({ reader ->
                reader.acquireLatestImage()?.close()
            }, captureHandler)

            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopCapture()
                    stopSelf()
                }
            }, Handler(mainLooper))

            virtualDisplay = projection?.createVirtualDisplay(
                "ScreenBotCapture",
                metrics.widthPixels.coerceAtLeast(1),
                metrics.heightPixels.coerceAtLeast(1),
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                captureHandler
            )
            isRunning = true
            sendFeedback("Bildschirmaufnahme aktiv. Testmodus läuft; keine Aktionen werden ausgeführt.")
        } catch (error: Exception) {
            sendFeedback("Start fehlgeschlagen: ${error.message ?: "unbekannter Fehler"}")
            stopCapture()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun getScreenMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return metrics
    }

    private fun startForegroundCompat() {
        val channelId = "screenbot_capture"
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(channelId, "Bildschirmaufnahme", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("ScreenBot")
            .setContentText("Bildschirmaufnahme aktiv")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, notification)
        }
    }

    private fun sendFeedback(message: String) {
        sendBroadcast(Intent(AppEvents.ACTION_FEEDBACK).setPackage(packageName).putExtra(AppEvents.EXTRA_MESSAGE, message))
    }

    private fun stopCapture() {
        isRunning = false
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        captureThread?.quitSafely()
        captureThread = null
        captureHandler = null
        val currentProjection = projection
        projection = null
        currentProjection?.stop()
        sendFeedback("Bildschirmaufnahme gestoppt.")
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_RESULT_CODE = "screenbot_result_code"
        const val EXTRA_RESULT_DATA = "screenbot_result_data"
        private const val ACTION_STOP = "com.smith.screenbot.STOP_CAPTURE"
        @Volatile var isRunning: Boolean = false
            private set
    }
}
