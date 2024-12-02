package com.example.tars.notificationlistener

import android.app.Service
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class NotificationListener : NotificationListenerService(), TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isTTSReady: Boolean = false
    private val notificationsQueue: MutableList<String> = mutableListOf()

    override fun onCreate() {
        super.onCreate()
        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification Listener connected.")
        processActiveNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val extras = sbn?.notification?.extras
        val title = extras?.getString("android.title") ?: "No Title"
        val text = extras?.getCharSequence("android.text")?.toString() ?: "No Text"

        val message = "New notification from $title: $text"
        Log.d(TAG, "Notification received: $message")

        // Add to queue and process if TTS is ready
        synchronized(notificationsQueue) {
            notificationsQueue.add(message)
            if (isTTSReady) {
                processNotificationQueue()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "Notification removed: ${sbn?.packageName}")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS Language not supported")
            } else {
                isTTSReady = true
                Log.d(TAG, "TTS initialized successfully.")
                processNotificationQueue()
            }
        } else {
            Log.e(TAG, "TTS initialization failed.")
        }
    }

    private fun processNotificationQueue() {
        synchronized(notificationsQueue) {
            while (notificationsQueue.isNotEmpty()) {
                val message = notificationsQueue.removeAt(0)
                speakNotification(message)
            }
        }
    }

    private fun speakNotification(message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech?.speak(message, TextToSpeech.QUEUE_ADD, null, message.hashCode().toString())
        } else {
            textToSpeech?.speak(message, TextToSpeech.QUEUE_ADD, null)
        }
        Log.d(TAG, "Speaking: $message")
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    private fun processActiveNotifications() {
        val activeNotifications = getActiveNotifications()
        for (sbn in activeNotifications) {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: "No Title"
            val text = extras.getCharSequence("android.text")?.toString() ?: "No Text"

            val message = "Active notification - Title: $title, Text: $text"
            Log.d(TAG, message)

            // Add to queue
            synchronized(notificationsQueue) {
                notificationsQueue.add(message)
                if (isTTSReady) {
                    processNotificationQueue()
                }
            }
        }
    }

    companion object {
        private const val TAG = "NotificationListener"
    }
}
