package com.example.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import com.example.SimChatApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val bundle = intent.extras ?: return
        try {
            val pdus = bundle.get("pdus") as? Array<*> ?: return
            val format = bundle.getString("format")
            val messages = pdus.mapNotNull { pdu ->
                if (pdu is ByteArray) {
                    @Suppress("DEPRECATION")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(pdu, format)
                    } else {
                        SmsMessage.createFromPdu(pdu)
                    }
                } else {
                    null
                }
            }

            if (messages.isEmpty()) return

            // Standardize sender and extract body
            val sender = messages.first().originatingAddress ?: "Unknown"
            val body = messages.joinToString("") { it.messageBody ?: "" }

            Log.d("SmsReceiver", "Real Carrier SMS Received from $sender: $body")

            // Store in Room database via application repository
            scope.launch {
                try {
                    val repository = SimChatApplication.instance.repository
                    repository.handleIncomingCarrierSms(sender, body)
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Failed to store incoming SMS in Room repository", e)
                }
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error parsing incoming SMS PDU format", e)
        }
    }
}
