package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.SessionManager
import com.example.data.SimChatRepository
import com.example.data.db.AppDatabase
import com.example.data.sms.SmsManagerWrapper

class SimChatApplication : Application() {

    companion object {
        lateinit var instance: SimChatApplication
            private set
    }

    lateinit var database: AppDatabase
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var smsWrapper: SmsManagerWrapper
        private set

    lateinit var repository: SimChatRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Central SQLite Room Database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "simchat_local_database"
        )
        .fallbackToDestructiveMigration() // safe for rapid prototyping and schema iterations
        .build()

        // Session manager
        sessionManager = SessionManager(applicationContext)

        // SMS api abstraction
        smsWrapper = SmsManagerWrapper(applicationContext)

        // Principal master repository
        repository = SimChatRepository(applicationContext, database, sessionManager, smsWrapper)
    }
}
