package com.example.loopvid

import android.app.Service
import android.content.Intent
import android.os.IBinder

class NotificationService : Service() {
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // This service can be used for background notification management
        // Currently, most functionality is handled by NotificationHelper and NotificationReceiver
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up any resources if needed
    }
}
