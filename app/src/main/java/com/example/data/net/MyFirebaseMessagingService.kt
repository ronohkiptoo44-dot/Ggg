package com.example.data.net

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.SimChatApplication
import com.example.viewmodel.SimulatedNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "New FCM Token received: $token")
        try {
            val sessionManager = SimChatApplication.instance.sessionManager
            sessionManager.fcmToken = token
        } catch (e: Exception) {
            Log.e("FCMService", "Failed to store FCM Token", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCMService", "FCM Message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "☁️ FCM Push Notification"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "New cloud message received"
        val sender = remoteMessage.data["sender"] ?: "Cloud Sender"
        val category = remoteMessage.data["category"] ?: "Cloud Messages"
        val groupName = remoteMessage.data["groupName"]

        Log.d("FCMService", "Mapped message payload - Title: $title, Body: $body")

        scope.launch {
            try {
                val app = SimChatApplication.instance
                val simNotif = SimulatedNotification(
                    id = "fcm_${System.currentTimeMillis()}",
                    category = category,
                    title = title,
                    senderName = sender,
                    messageText = body,
                    timestamp = "Just Now",
                    carrierSim = "CLOUD", // FCM represents Cloud-based messaging
                    isUnread = true,
                    groupName = groupName
                )
                app.repository.handleIncomingFcmMessage(simNotif)
            } catch (e: Exception) {
                Log.e("FCMService", "Error routing FCM to Repository", e)
            }
        }
    }
}
