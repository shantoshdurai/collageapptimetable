package com.example.loopvid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        // Extract class information from intent
        val subject = intent.getStringExtra("subject") ?: "Unknown Subject"
        val time = intent.getStringExtra("time") ?: "Unknown Time"
        val room = intent.getStringExtra("room") ?: "Unknown Room"
        val faculty = intent.getStringExtra("faculty") ?: "Unknown Faculty"
        val department = intent.getStringExtra("department") ?: "Unknown Department"
        val year = intent.getStringExtra("year") ?: "Unknown Year"
        val className = intent.getStringExtra("className") ?: "Unknown Class"
        
        // Show the notification
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showImmediateNotification(
            subject = subject,
            time = time,
            room = room,
            faculty = faculty,
            department = department,
            year = year,
            className = className
        )
    }
}
