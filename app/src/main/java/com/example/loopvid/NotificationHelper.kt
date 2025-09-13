package com.example.loopvid

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

class NotificationHelper(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "class_reminders"
        const val CHANNEL_NAME = "Class Reminders"
        const val CHANNEL_DESCRIPTION = "Notifications for upcoming classes"
        const val NOTIFICATION_ID_BASE = 1000
        const val REQUEST_CODE_BASE = 2000
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun scheduleClassNotifications(
        department: String,
        year: String,
        className: String,
        schedule: List<ClassSchedule>
    ) {
        // Clear existing notifications first
        clearAllNotifications()
        
        val today = LocalDate.now()
        val currentTime = LocalTime.now()
        
        // Schedule notifications for the next 7 days
        for (dayOffset in 0..6) {
            val targetDate = today.plusDays(dayOffset.toLong())
            val dayOfWeek = targetDate.dayOfWeek
            
            val daySchedule = when (dayOfWeek) {
                DayOfWeek.MONDAY -> getMondaySchedule(department, year, className)
                DayOfWeek.TUESDAY -> getTuesdaySchedule(department, year, className)
                DayOfWeek.WEDNESDAY -> getWednesdaySchedule(department, year, className)
                DayOfWeek.THURSDAY -> getThursdaySchedule(department, year, className)
                DayOfWeek.FRIDAY -> getFridaySchedule(department, year, className)
                DayOfWeek.SATURDAY -> getMondaySchedule(department, year, className) // Saturday uses Monday schedule
                else -> emptyList() // Sunday - no classes
            }
            
            // Schedule notification for each class 15 minutes before
            daySchedule.forEachIndexed { index, classSchedule ->
                val (startTime, _) = parseTimeRange(classSchedule.time) ?: return@forEachIndexed
                
                // Only schedule if the class is in the future
                val classDateTime = LocalDateTime.of(targetDate, startTime)
                val now = LocalDateTime.now()
                
                if (classDateTime.isAfter(now)) {
                    val notificationTime = classDateTime.minusMinutes(15)
                    
                    // Skip if notification time has already passed
                    if (notificationTime.isAfter(now)) {
                        scheduleNotification(
                            classSchedule = classSchedule,
                            department = department,
                            year = year,
                            className = className,
                            notificationTime = notificationTime,
                            dayOffset = dayOffset,
                            classIndex = index
                        )
                    }
                }
            }
        }
    }
    
    private fun scheduleNotification(
        classSchedule: ClassSchedule,
        department: String,
        year: String,
        className: String,
        notificationTime: LocalDateTime,
        dayOffset: Int,
        classIndex: Int
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("subject", classSchedule.subject)
            putExtra("time", classSchedule.time)
            putExtra("room", classSchedule.room)
            putExtra("faculty", classSchedule.faculty)
            putExtra("department", department)
            putExtra("year", year)
            putExtra("className", className)
            putExtra("dayOffset", dayOffset)
            putExtra("classIndex", classIndex)
        }
        
        val requestCode = REQUEST_CODE_BASE + (dayOffset * 100) + classIndex
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar = Calendar.getInstance().apply {
            timeInMillis = notificationTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Handle case where exact alarm permission is not granted
            // Fallback to inexact alarm
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
    
    fun clearAllNotifications() {
        // Cancel all scheduled alarms
        for (dayOffset in 0..6) {
            for (classIndex in 0..9) { // Assuming max 10 classes per day
                val requestCode = REQUEST_CODE_BASE + (dayOffset * 100) + classIndex
                val intent = Intent(context, NotificationReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            }
        }
        
        // Clear any active notifications
        notificationManager.cancelAll()
    }
    
    fun showImmediateNotification(
        subject: String,
        time: String,
        room: String,
        faculty: String,
        department: String,
        year: String,
        className: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Class Reminder: $subject")
            .setContentText("$time • $room • $faculty")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your $subject class is starting in 15 minutes!\n\nTime: $time\nRoom: $room\nFaculty: $faculty\n\n$department - $year - $className")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setLights(0xFF4CAF50.toInt(), 1000, 1000)
            .build()
        
        try {
            notificationManager.notify(NOTIFICATION_ID_BASE, notification)
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
        }
    }
    
    // Helper functions from MainActivity (copied here to avoid circular dependencies)
    private fun parseTimeToLocalTime(raw: String): LocalTime? {
        val cleaned = raw.trim()
        val parts = cleaned.split(":")
        if (parts.size < 2) return null
        
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().take(2).toIntOrNull() ?: return null
        
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(hour, minute)
    }
    
    private fun parseTimeRange(range: String): Pair<LocalTime, LocalTime>? {
        val sections = range.split("-")
        if (sections.size < 2) return null
        
        val start = parseTimeToLocalTime(sections[0]) ?: return null
        val endRaw = sections[1].trim()
        val end = parseTimeToLocalTime(endRaw) ?: return null
        
        // Handle afternoon times (e.g., "12:20 - 01:20" means 12:20 - 13:20)
        val adjustedEnd = if (end.isBefore(start) && start.hour >= 12) {
            end.plusHours(12)
        } else {
            end
        }
        
        return start to adjustedEnd
    }
    
    // Schedule functions (simplified versions from MainActivity)
    private fun getMondaySchedule(department: String, year: String, className: String): List<ClassSchedule> {
        return when (department) {
            "School of Engineering" -> when (year) {
                "Year 2" -> when (className) {
                    "A1" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Data Structures", "Room Lab", "Mrs.M.Sheeba", "Lab"),
                        ClassSchedule("10:00 - 11:00", "Data Structures", "Room Lab", "Mrs.M.Sheeb", "Lab"),
                        ClassSchedule("11:20 - 12:20", "Programming Lab", "Lab 1", "Mr. Davis", "Lab"),
                        ClassSchedule("12:20 - 13:20", "Programming Lab", "Lab 1", "Mr. Davis", "Lab"),
                        ClassSchedule("14:30 - 15:30", "Artificial Intelligence", "Room 103", "Dr. Wilson", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Database Systems", "Room 104", "Ms. Brown", "Theory")
                    )
                    "A2" -> listOf(
                        ClassSchedule("9:00 - 10:00", "OOP Lab", "Lab 3", "Mr.R.Karthikeyan", "Lab"),
                        ClassSchedule("10:00 - 11:00", "OOP Lab", "Lab 3", "Mr.R.Karthikeyan", "Lab"),
                        ClassSchedule("11:20 - 12:20", "Foundations of AI and ML", "Room 709", "Mr.K.Jeeva", "Theory"),
                        ClassSchedule("12:20 - 13:20", "R Programming", "Room 709", "Mr.J.Manivanan", "Theory"),
                        ClassSchedule("14:30 - 15:30", "Data Structures", "Room 709", "Mrs.M.Sheeba", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Probability and Statistics", "Room 709", "Dr.K.Rajakumar", "Theory")
                    )
                    "A3" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Probability and Statistics", "Room 709", "Dr.K.Balamurugan", "Theory"),
                        ClassSchedule("10:00 - 11:00", "R Programming", "Room 709", "Mr.J.Manivanan", "Theory"),
                        ClassSchedule("11:20 - 12:20", "Lateral Thinking", "Room 709", "Mrs.B.Renugadevi", "Theory"),
                        ClassSchedule("12:20 - 13:20", "Foundations of AI and ML", "Room 709", "Mr.K.Jeeva", "Theory"),
                        ClassSchedule("14:30 - 15:30", "Object Oriented Programming", "Room 709", "Mr.V.V.Sabeer", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Data Structures", "Room 709", "Dr.N.Shanmugapriya", "Theory")
                    )
                    "A8" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Lateral Thinking", "Room 301", "Mrs.M.Kanimozhi", "Theory"),
                        ClassSchedule("10:00 - 11:00", "Data Structure", "Room 302", "Mrs.S.Lavanya", "Theory"),
                        ClassSchedule("11:20 - 12:20", "OOP Lab", "Lab 3", "Mr.R.Karthikeyan", "Lab"),
                        ClassSchedule("12:20 - 13:20", "OOP Lab", "Lab 3", "Mr.R.Karthikeyan", "Lab"),
                        ClassSchedule("14:30 - 15:30", "Mathematics", "Room 303", "Dr.E.Ramesh Kumar", "Theory"),
                        ClassSchedule("15:45 - 16:45", "R Programming", "Room 304", "Ms.J.Manivanan", "Theory")
                    )
                    else -> emptyList()
                }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
    
    private fun getTuesdaySchedule(department: String, year: String, className: String): List<ClassSchedule> {
        return when (department) {
            "School of Engineering" -> when (year) {
                "Year 2" -> when (className) {
                    "A1" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Data Structures", "Room 101", "Dr. Smith", "Theory"),
                        ClassSchedule("10:00 - 11:00", "Mathematics", "Room 102", "Prof. Johnson", "Theory"),
                        ClassSchedule("11:20 - 12:20", "Programming Lab", "Lab 1", "Mr. Davis", "Lab"),
                        ClassSchedule("12:20 - 13:20", "Programming Lab", "Lab 1", "Mr. Davis", "Lab"),
                        ClassSchedule("14:30 - 15:30", "Artificial Intelligence", "Room 103", "Dr. Wilson", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Database Systems", "Room 104", "Ms. Brown", "Theory")
                    )
                    "A2" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Object Oriented Programming", "Room 709", "Mr.R.Karthikeyan", "Theory"),
                        ClassSchedule("10:00 - 11:00", "Quantitative Skill Practice", "Room 709", "HRDC", "Theory"),
                        ClassSchedule("11:20 - 12:20", "Probability and Statistics", "Room 709", "Dr.K.Rajakumar", "Theory"),
                        ClassSchedule("12:20 - 13:20", "Lateral Thinking", "Room 709", "Mrs.M.Kanimozhi", "Theory"),
                        ClassSchedule("14:30 - 15:30", "Foundations of AI and ML", "Room 709", "Mr.K.Jeeva", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Data Structures", "Room 709", "Mrs.M.Sheeba", "Theory")
                    )
                    "A3" -> listOf(
                        ClassSchedule("9:00 - 10:00", "R Programming", "Room 709", "Mr.J.Manivanan", "Theory"),
                        ClassSchedule("10:00 - 11:00", "Lateral Thinking", "Room 709", "Mrs.B.Renugadevi", "Theory"),
                        ClassSchedule("11:20 - 12:20", "Data Structures Lab", "Lab 2", "Dr.N.Shanmugapriya", "Lab"),
                        ClassSchedule("12:20 - 13:20", "Data Structures Lab", "Lab 2", "Dr.N.Shanmugapriya", "Lab"),
                        ClassSchedule("14:30 - 15:30", "Probability and Statistics", "Room 709", "Dr.K.Balamurugan", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Quantitative Skill Practice", "Room 709", "HRDC", "Theory")
                    )
                    "A8" -> listOf(
                        ClassSchedule("9:00 - 10:00", "AIML", "Room 709", "Ms.G.Keerthana Sri", "Theory"),
                        ClassSchedule("10:00 - 11:00", "R Programming", "Room 709", "Mr.J.Manivanan", "Theory"),
                        ClassSchedule("11:20 - 12:20", "Data structure", "Room 709", "Mrs.S.Lavanya", "Theory"),
                        ClassSchedule("12:20 - 13:20", "Mathematics", "Room 709", "Dr.E.Ramesh Kumar", "Theory"),
                        ClassSchedule("14:30 - 15:30", "OOP", "Room 709", "Mr.R.Karthikeyan", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Lateral Thinking", "Room 709", "Mrs.M.Kanimozhi", "Theory")
                    )
                    else -> emptyList()
                }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
    
    private fun getWednesdaySchedule(department: String, year: String, className: String): List<ClassSchedule> {
        return when (department) {
            "School of Engineering" -> when (year) {
                "Year 2" -> when (className) {
                    "A1" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Data Structures", "Room 101", "Dr. Smith", "Theory"),
                        ClassSchedule("10:00 - 11:00", "Mathematics", "Room 102", "Prof. Johnson", "Theory"),
                        ClassSchedule("11:20 - 12:20", "Programming Lab", "Lab 1", "Mr. Davis", "Lab"),
                        ClassSchedule("12:20 - 13:20", "Programming Lab", "Lab 1", "Mr. Davis", "Lab"),
                        ClassSchedule("14:30 - 15:30", "Artificial Intelligence", "Room 103", "Dr. Wilson", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Database Systems", "Room 104", "Ms. Brown", "Theory")
                    )
                    "A2" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Foundations of AI and ML", "Room 709", "Mr.K.Jeeva", "Theory"),
                        ClassSchedule("10:00 - 11:00", "Object Oriented Programming", "Room 709", "Mr.R.Karthikeyan", "Theory"),
                        ClassSchedule("11:20 - 12:20", "Data Structures Lab", "Lab 2", "Mrs.M.Sheeba", "Lab"),
                        ClassSchedule("12:20 - 13:20", "Data Structures Lab", "Lab 2", "Mrs.M.Sheeba", "Lab"),
                        ClassSchedule("14:30 - 15:30", "R Programming", "Room 709", "Mr.J.Manivanan", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Probability and Statistics", "Room 709", "Dr.K.Rajakumar", "Theory")
                    )
                    "A3" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Object Oriented Programming", "Room 709", "Mr.V.V.Sabeer", "Theory"),
                        ClassSchedule("10:00 - 11:00", "Lateral Thinking", "Room 709", "Mrs.B.Renugadevi", "Theory"),
                        ClassSchedule("11:20 - 12:20", "Probability and Statistics", "Room 709", "Dr.K.Balamurugan", "Theory"),
                        ClassSchedule("12:20 - 13:20", "R Programming", "Room 709", "Mr.J.Manivanan", "Theory"),
                        ClassSchedule("14:30 - 15:30", "Data Structures", "Room 709", "Dr.N.Shanmugapriya", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Foundations of AI and ML", "Room 709", "Mr.K.Jeeva", "Theory")
                    )
                    "A8" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Mathematics", "Room 709", "Dr.E.Ramesh Kumar", "Theory"),
                        ClassSchedule("10:00 - 11:00", "OOP", "Room 709", "Mr.R.Karthikeyan", "Theory"),
                        ClassSchedule("11:20 - 12:20", "R Programming", "Room 709", "Mr.J.Manivanan", "Theory"),
                        ClassSchedule("12:20 - 13:20", "AIML", "Room 709", "Mr.R.Karthikeyan", "Theory"),
                        ClassSchedule("14:30 - 15:30", "Data Structure", "Room 709", "Mrs.S.Lavanya", "Theory"),
                        ClassSchedule("15:45 - 16:45", "HRDC", "Room 709", "Mrs.S.Ashwantika", "Theory")
                    )
                    else -> emptyList()
                }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
    
    private fun getThursdaySchedule(department: String, year: String, className: String): List<ClassSchedule> {
        return when (department) {
            "School of Engineering" -> when (year) {
                "Year 2" -> when (className) {
                    "A1" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Data Structures", "Room 101", "Dr. Smith", "Theory"),
                        ClassSchedule("10:00 - 11:00", "Mathematics", "Room 102", "Prof. Johnson", "Theory"),
                        ClassSchedule("11:20 - 12:20", "Programming Lab", "Lab 1", "Mr. Davis", "Lab"),
                        ClassSchedule("12:20 - 13:20", "Programming Lab", "Lab 1", "Mr. Davis", "Lab"),
                        ClassSchedule("14:30 - 15:30", "Artificial Intelligence", "Room 103", "Dr. Wilson", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Database Systems", "Room 104", "Ms. Brown", "Theory")
                    )
                    "A2" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Data Structures", "Room 709", "Mrs.M.Sheeba", "Theory"),
                        ClassSchedule("10:00 - 11:00", "R Programming", "Room 709", "Mr.J.Manivanan", "Theory"),
                        ClassSchedule("11:20 - 12:20", "Foundations of AI and ML", "Room 709", "Mr.K.Jeeva", "Theory"),
                        ClassSchedule("12:20 - 13:20", "Object Oriented Programming", "Room 709", "Mr.R.Karthikeyan", "Theory"),
                        ClassSchedule("14:30 - 15:30", "Probability and Statistics", "Room 709", "Dr.K.Rajakumar", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Lateral Thinking", "Room 709", "Mrs.M.Kanimozhi", "Theory")
                    )
                    "A3" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Probability and Statistics", "Room 709", "Dr.K.Balamurugan", "Theory"),
                        ClassSchedule("10:00 - 11:00", "Foundations of AI and ML", "Room 709", "Mr.K.Jeeva", "Theory"),
                        ClassSchedule("11:20 - 12:20", "Object Oriented Programming", "Room 709", "Mr.V.V.Sabeer", "Theory"),
                        ClassSchedule("12:20 - 13:20", "Data Structures Lab", "Lab 2", "Dr.N.Shanmugapriya", "Lab"),
                        ClassSchedule("14:30 - 15:30", "Quantitative Skill Practice", "Room 709", "HRDC", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Lateral Thinking", "Room 709", "Mrs.B.Renugadevi", "Theory")
                    )
                    "A8" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Oop", "Room 709", "Mr.R.Karthikeyan", "Theory"),
                        ClassSchedule("10:00 - 11:00", "Data Structure", "Room 709", "Mrs.S.Lavanya", "Theory"),
                        ClassSchedule("11:20 - 12:20", "HRDC", "Room 709", "Mrs. Aswanthika", "Theory"),
                        ClassSchedule("12:20 - 13:20", "R Programming", "Room 709", "Mr.R.Karthikeyan", "Theory"),
                        ClassSchedule("14:30 - 15:30", "Mathematics", "Room 709", "Dr.E.Ramesh Kumar", "Theory"),
                        ClassSchedule("15:45 - 16:45", "AIML", "Room 709", "Ms.G.Keerthana Sri", "Theory")
                    )
                    else -> emptyList()
                }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
    
    private fun getFridaySchedule(department: String, year: String, className: String): List<ClassSchedule> {
        return when (department) {
            "School of Engineering" -> when (year) {
                "Year 2" -> when (className) {
                    "A1" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Data Structures", "Room 101", "Dr. Smith", "Theory"),
                        ClassSchedule("10:00 - 11:00", "Mathematics", "Room 102", "Prof. Johnson", "Theory"),
                        ClassSchedule("11:20 - 12:20", "Programming Lab", "Lab 1", "Mr. Davis", "Lab"),
                        ClassSchedule("12:20 - 13:20", "Programming Lab", "Lab 1", "Mr. Davis", "Lab"),
                        ClassSchedule("14:30 - 15:30", "Artificial Intelligence", "Room 103", "Dr. Wilson", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Database Systems", "Room 104", "Ms. Brown", "Theory")
                    )
                    "A2" -> listOf(
                        ClassSchedule("9:00 - 10:00", "R Programming", "Room 709", "Mr.J.Manivanan", "Theory"),
                        ClassSchedule("10:00 - 11:00", "Data Structures", "Room 709", "Mrs.M.Sheeba", "Theory"),
                        ClassSchedule("11:20 - 12:20", "Probability and Statistics", "Room 709", "Dr.K.Rajakumar", "Theory"),
                        ClassSchedule("12:20 - 13:20", "Lateral Thinking", "Room 709", "Mrs.M.Kanimozhi", "Theory"),
                        ClassSchedule("14:30 - 15:30", "Quantitative Skill Practice", "Room 709", "HRDC", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Object Oriented Programming", "Room 709", "Mr.R.Karthikeyan", "Theory")
                    )
                    "A3" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Foundations of AI and ML", "Room 709", "Mr.K.Jeeva", "Theory"),
                        ClassSchedule("10:00 - 11:00", "Data Structures", "Room 709", "Dr.N.Shanmugapriya", "Theory"),
                        ClassSchedule("11:20 - 12:20", "OOP Lab", "Lab 3", "Mr.V.V.Sabeer", "Lab"),
                        ClassSchedule("12:20 - 13:20", "OOP Lab", "Lab 3", "Mr.V.V.Sabeer", "Lab"),
                        ClassSchedule("14:30 - 15:30", "R Programming", "Room 709", "Mr.J.Manivanan", "Theory"),
                        ClassSchedule("15:45 - 16:45", "Probability and Statistics", "Room 709", "Dr.K.Balamurugan", "Theory")
                    )
                    "A8" -> listOf(
                        ClassSchedule("9:00 - 10:00", "Data Structure", "Lab 2", "Mrs.S.Lavanya", "Lab"),
                        ClassSchedule("10:00 - 11:00", "Data Structure", "Lab 2", "Mrs.S.Lavanya", "lab"),
                        ClassSchedule("11:20 - 12:20", "AIML", "Room 709", "Ms.G.Keerthana Sri", "Theory"),
                        ClassSchedule("12:20 - 13:20", "Mathematics", "Room 709", "Dr.E.Ramesh Kumar", "Theory"),
                        ClassSchedule("14:30 - 15:30", "Lateral Thinking", "Room 709", "Mrs.M.Kanimozhi", "Theory"),
                        ClassSchedule("15:45 - 16:45", "OOP", "Room 709", "Mr.R.Karthikeyan", "Theory")
                    )
                    else -> emptyList()
                }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
