package com.example.loopvid

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
            
            // Check for saved preferences on startup
            val savedDepartment = prefs.getString("saved_department", null)
            val savedYear = prefs.getString("saved_year", null)
            val savedClass = prefs.getString("saved_class", null)
            
            // Simple screen state management
            var currentScreen by remember { 
                mutableStateOf<Screen>(
                    if (savedDepartment != null && savedYear != null && savedClass != null) {
                        Screen.Timetable(savedDepartment, savedYear, savedClass)
                    } else {
                        Screen.Home
                    }
                )
            }
            
            // Show the appropriate screen based on state
            when (val screen = currentScreen) {
                is Screen.Home -> HomeScreen(
                    onDepartmentSelected = { dept ->
                        currentScreen = Screen.YearSelection(dept)
                    }
                )
                is Screen.YearSelection -> YearSelectionScreen(
                    department = screen.department,
                    onYearSelected = { year ->
                        currentScreen = Screen.ClassSelection(screen.department, year)
                    },
                    onBack = { currentScreen = Screen.Home }
                )
                is Screen.ClassSelection -> ClassSelectionScreen(
                    department = screen.department,
                    year = screen.year,
                    onClassSelected = { className ->
                        // Save user preferences when class is selected
                        saveUserPreferences(prefs, screen.department, screen.year, className)
                        currentScreen = Screen.Timetable(screen.department, screen.year, className)
                    },
                    onBack = { currentScreen = Screen.YearSelection(screen.department) }
                )
                is Screen.Timetable -> TimetableScreen(
                    department = screen.department,
                    year = screen.year,
                    className = screen.className,
                    onBack = { currentScreen = Screen.Home },
                    onClearPreferences = {
                        clearUserPreferences(prefs)
                        currentScreen = Screen.Home
                    }
                )
            }
        }
    }
}

// Helper functions for SharedPreferences
private fun saveUserPreferences(prefs: SharedPreferences, department: String, year: String, className: String) {
    prefs.edit().apply {
        putString("saved_department", department)
        putString("saved_year", year)
        putString("saved_class", className)
        apply()
    }
}

private fun clearUserPreferences(prefs: SharedPreferences) {
    prefs.edit().clear().apply()
}

// Simple screen navigation model
private sealed class Screen {
    data object Home : Screen()
    data class YearSelection(val department: String) : Screen()
    data class ClassSelection(val department: String, val year: String) : Screen()
    data class Timetable(val department: String, val year: String, val className: String) : Screen()
}

// Data classes for timetable
data class ClassSchedule(
    val time: String,
    val subject: String,
    val room: String,
    val faculty: String,
    val type: String = "Theory" // Theory, Lab, Tutorial
)

// Function to get today's schedule based on department, year, and class
private fun getTodaysSchedule(department: String, year: String, className: String): List<ClassSchedule> {
    val today = LocalDate.now()
    val dayOfWeek = today.dayOfWeek
    
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> getMondaySchedule(department, year, className)
        DayOfWeek.TUESDAY -> getTuesdaySchedule(department, year, className)
        DayOfWeek.WEDNESDAY -> getWednesdaySchedule(department, year, className)
        DayOfWeek.THURSDAY -> getThursdaySchedule(department, year, className)
        DayOfWeek.FRIDAY -> getFridaySchedule(department, year, className)
        DayOfWeek.SATURDAY -> getMondaySchedule(department, year, className)
        else -> listOf(ClassSchedule("Weekend", "No Classes", "N/A", "N/A"))
    }
}

// Get upcoming class
private fun getUpcomingClass(schedule: List<ClassSchedule>): ClassSchedule? {
    return try {
        val now = java.time.LocalTime.now()
        schedule.find { classSchedule ->
            val (start, _) = parseTimeRange(classSchedule.time) ?: return@find false
            now.isBefore(start)
        }
    } catch (e: Exception) {
        null
    }
}

// Get time until next class
private fun getTimeUntilNextClass(nextClass: ClassSchedule): String {
    return try {
        val now = java.time.LocalTime.now()
        val (start, _) = parseTimeRange(nextClass.time) ?: return "Time not available"
        val duration = java.time.Duration.between(now, start)
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            duration.isNegative || duration.isZero -> "Starting now"
            else -> "${minutes}m"
        }
    } catch (e: Exception) {
        "Time not available"
    }
}

// Enhanced time parsing that handles all time formats correctly
private fun parseTimeToLocalTime(raw: String): java.time.LocalTime? {
    val cleaned = raw.trim()
    val parts = cleaned.split(":")
    if (parts.size < 2) return null
    
    val hour = parts[0].trim().toIntOrNull() ?: return null
    val minute = parts[1].trim().take(2).toIntOrNull() ?: return null
    
    if (hour !in 0..23 || minute !in 0..59) return null
    return java.time.LocalTime.of(hour, minute)
}

private fun parseTimeRange(range: String): Pair<java.time.LocalTime, java.time.LocalTime>? {
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

// =============================================================================
// TIMETABLE FRAMEWORK - HOW TO ADD NEW CLASSES
// =============================================================================
// 
// To add a new class (e.g., A1, A2, etc.), follow this pattern:
// 
// 1. Copy the A8 schedule structure below
// 2. Replace "A8" with your class name (e.g., "A1")
// 3. Update the class details (subjects, rooms, faculty, times)
// 4. Repeat for all 5 days (Monday to Friday)
// 5. Make sure time format is 24-hour (e.g., "14:30" not "2:30 PM")
//
// EXAMPLE FOR A1 CLASS:
// "A1" -> listOf(
//     ClassSchedule("9:00 - 10:00", "Subject Name", "Room Number", "Faculty Name", "Theory/Lab"),
//     ClassSchedule("10:00 - 11:00", "Subject Name", "Room Number", "Faculty Name", "Theory/Lab"),
//     // ... add more classes
// )
//
// =============================================================================

// Schedule generators for different departments and classes
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
                else -> getDefaultSchedule()
            }
            else -> getDefaultSchedule()
        }
        "School of Medicine" -> listOf(
            ClassSchedule("8:00 - 9:00", "Anatomy", "Lecture Hall 1", "Dr. Williams", "Theory"),
            ClassSchedule("9:00 - 10:00", "Physiology", "Lecture Hall 2", "Dr. Miller", "Theory"),
            ClassSchedule("10:15 - 11:15", "Anatomy Lab", "Lab A", "Dr. Davis", "Lab"),
            ClassSchedule("11:15 - 12:15", "Anatomy Lab", "Lab A", "Dr. Davis", "Lab"),
            ClassSchedule("2:00 - 3:00", "Biochemistry", "Lecture Hall 3", "Dr. Johnson", "Theory"),
            ClassSchedule("3:00 - 4:00", "Clinical Skills", "Skills Lab", "Dr. Brown", "Practical")
        )
        else -> getDefaultSchedule()
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
                else -> getDefaultSchedule()
            }
            else -> getDefaultSchedule()
        }
        "School of Medicine" -> listOf(
            ClassSchedule("8:00 - 9:00", "Pathology", "Lecture Hall 1", "Dr. Wilson", "Theory"),
            ClassSchedule("9:00 - 10:00", "Microbiology", "Lecture Hall 2", "Dr. Moore", "Theory"),
            ClassSchedule("10:15 - 11:15", "Pathology Lab", "Lab B", "Dr. Taylor", "Lab"),
            ClassSchedule("11:15 - 12:15", "Pathology Lab", "Lab B", "Dr. Taylor", "Lab"),
            ClassSchedule("2:00 - 3:00", "Pharmacology", "Lecture Hall 3", "Dr. Anderson", "Theory"),
            ClassSchedule("3:00 - 4:00", "Patient Care", "Clinical Lab", "Dr. Thomas", "Practical")
        )
        else -> getDefaultSchedule()
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
                else -> getDefaultSchedule()
            }
            else -> getDefaultSchedule()
        }
        "School of Medicine" -> listOf(
            ClassSchedule("8:00 - 9:00", "Clinical Medicine", "Lecture Hall 1", "Dr. Parker", "Theory"),
            ClassSchedule("9:00 - 10:00", "Diagnostics", "Lecture Hall 2", "Dr. Evans", "Theory"),
            ClassSchedule("10:15 - 11:15", "Clinical Lab", "Lab C", "Dr. Edwards", "Lab"),
            ClassSchedule("11:15 - 12:15", "Clinical Lab", "Lab C", "Dr. Edwards", "Lab"),
            ClassSchedule("2:00 - 3:00", "Medical Ethics", "Lecture Hall 3", "Dr. Collins", "Theory"),
            ClassSchedule("3:00 - 4:00", "Research Methods", "Skills Lab", "Dr. Stewart", "Practical")
        )
        else -> getDefaultSchedule()
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
                "A9" -> listOf(
                    ClassSchedule("9:00 - 10:00", "Oop", "Room 709", "Mr.R.Karthikeyan", "Theory"),
                    ClassSchedule("10:00 - 11:00", "Data Structure", "Room 709", "Mrs.S.Lavanya", "Theory"),
                    ClassSchedule("11:20 - 12:20", "HRDC", "Room 709", "Mrs. Aswanthika", "Theory"),
                    ClassSchedule("12:20 - 13:20", "R Programming", "Room 709", "Mr.R.Karthikeyan", "Theory"),
                    ClassSchedule("14:30 - 15:30", "Mathematics", "Room 709", "Dr.E.Ramesh Kumar", "Theory"),
                    ClassSchedule("15:45 - 16:45", "AIML", "Room 709", "Ms.G.Keerthana Sri", "Theory")
                )
                else -> getDefaultSchedule()
            }
            else -> getDefaultSchedule()
        }
        "School of Medicine" -> listOf(
            ClassSchedule("8:00 - 9:00", "Surgery", "Lecture Hall 1", "Dr. Watson", "Theory"),
            ClassSchedule("9:00 - 10:00", "Emergency Medicine", "Lecture Hall 2", "Dr. Brooks", "Theory"),
            ClassSchedule("10:15 - 11:15", "Surgery Lab", "Lab D", "Dr. Kelly", "Lab"),
            ClassSchedule("11:15 - 12:15", "Surgery Lab", "Lab D", "Dr. Kelly", "Lab"),
            ClassSchedule("2:00 - 3:00", "Medical Imaging", "Lecture Hall 3", "Dr. Sanders", "Theory"),
            ClassSchedule("3:00 - 4:00", "Simulation Lab", "Skills Lab", "Dr. Price", "Practical")
        )
        else -> getDefaultSchedule()
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
                else -> getDefaultSchedule()
            }
            else -> getDefaultSchedule()
        }
        "School of Medicine" -> listOf(
            ClassSchedule("8:00 - 9:00", "Medical Research", "Lecture Hall 1", "Dr. Foster", "Theory"),
            ClassSchedule("9:00 - 10:00", "Evidence-Based Medicine", "Lecture Hall 2", "Dr. Gonzales", "Theory"),
            ClassSchedule("10:15 - 11:15", "Research Lab", "Lab E", "Dr. Bryant", "Lab"),
            ClassSchedule("11:15 - 12:15", "Research Lab", "Lab E", "Dr. Bryant", "Lab"),
            ClassSchedule("2:00 - 3:00", "Medical Writing", "Lecture Hall 3", "Dr. Alexander", "Theory"),
            ClassSchedule("3:00 - 4:00", "Case Studies", "Skills Lab", "Dr. Russell", "Practical")
        )
        else -> getDefaultSchedule()
    }
}

private fun getDefaultSchedule(): List<ClassSchedule> {
    return listOf(
        ClassSchedule("8:30 - 9:30", "General Studies", "Room 101", "Prof. General", "Theory"),
        ClassSchedule("9:30 - 10:30", "Core Subject", "Room 102", "Dr. Core", "Theory"),
        ClassSchedule("10:45 - 11:45", "Practical Session", "Lab 1", "Mr. Practical", "Lab"),
        ClassSchedule("11:45 - 12:45", "Practical Session", "Lab 1", "Mr. Practical", "Lab"),
        ClassSchedule("2:00 - 3:00", "Advanced Topics", "Room 103", "Dr. Advanced", "Theory"),
        ClassSchedule("3:00 - 4:00", "Specialized Course", "Room 104", "Ms. Special", "Theory")
    )
}

@Composable
private fun HomeScreen(
    onDepartmentSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var showDepartmentDialog by remember { mutableStateOf(false) }

    val departments = listOf(
        "School of Medicine",
        "School of Engineering",
        "School of AHS",
        "School of Agriculture Science",
        "School of Physiotherapy",
        "School of Pharmacy",
        "College of Nursing",
        "School of Architecture",
        "School of Management",
        "School of Arts and Science",
        "School of Law",
        "Centre for Research"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background video
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    val videoUri = Uri.parse("android.resource://${ctx.packageName}/raw/ailoop")
                    setVideoURI(videoUri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f)
                        start()
                    }
                }
            }
        )

        // Bottom button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { showDepartmentDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Select Department",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Department selection dialog
    if (showDepartmentDialog) {
        SelectionDialog(
            title = "Select Department",
            items = departments,
            onItemSelected = { dept ->
                showDepartmentDialog = false
                onDepartmentSelected(dept)
            },
            onDismiss = { showDepartmentDialog = false }
        )
    }
}

@Composable
private fun YearSelectionScreen(
    department: String,
    onYearSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    var showYearDialog by remember { mutableStateOf(true) }

    val years = listOf("Year 1", "Year 2", "Year 3", "Year 4")

    Box(modifier = Modifier.fillMaxSize()) {
        // Background video
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    val videoUri = Uri.parse("android.resource://${ctx.packageName}/raw/ailoop")
                    setVideoURI(videoUri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f)
                        start()
                    }
                }
            }
        )

        // Header
        Text(
            text = department,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        )

        // Back button
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            ),
            border = BorderStroke(1.dp, Color.White),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text("Back to Departments")
        }
    }

    // Year selection dialog
    if (showYearDialog) {
        SelectionDialog(
            title = "Select Year",
            items = years,
            onItemSelected = { year ->
                showYearDialog = false
                onYearSelected(year)
            },
            onDismiss = { 
                showYearDialog = false
                onBack()
            }
        )
    }
}

@Composable
private fun ClassSelectionScreen(
    department: String,
    year: String,
    onClassSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    var showClassDialog by remember { mutableStateOf(true) }

    val classes = (1..10).map { "A$it" }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background video
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    val videoUri = Uri.parse("android.resource://${ctx.packageName}/raw/ailoop")
                    setVideoURI(videoUri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f)
                        start()
                    }
                }
            }
        )

        // Header
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = department,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = year,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Back button
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            ),
            border = BorderStroke(1.dp, Color.White),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text("Back to Years")
        }
    }

    // Class selection dialog
    if (showClassDialog) {
        SelectionDialog(
            title = "Select Class",
            items = classes,
            onItemSelected = { className ->
                showClassDialog = false
                onClassSelected(className)
            },
            onDismiss = { 
                showClassDialog = false
                onBack()
            }
        )
    }
}

@Composable
private fun TimetableScreen(
    department: String,
    year: String,
    className: String,
    onBack: () -> Unit,
    onClearPreferences: () -> Unit
) {
    val context = LocalContext.current
    // Get today's schedule
    val todaySchedule = remember(department, year, className) {
        try {
            val today = LocalDate.now()
            val dayOfWeek = today.dayOfWeek
            val schedule = when (dayOfWeek) {
                DayOfWeek.MONDAY -> getMondaySchedule(department, year, className)
                DayOfWeek.TUESDAY -> getTuesdaySchedule(department, year, className)
                DayOfWeek.WEDNESDAY -> getWednesdaySchedule(department, year, className)
                DayOfWeek.THURSDAY -> getThursdaySchedule(department, year, className)
                DayOfWeek.FRIDAY -> getFridaySchedule(department, year, className)
                else -> listOf(ClassSchedule("Weekend", "No Classes", "N/A", "N/A"))
            }
            schedule
        } catch (e: Exception) {
            // Fallback to default schedule if there's an error
            listOf(
                ClassSchedule("8:30 - 9:30", "General Studies", "Room 101", "Prof. General", "Theory"),
                ClassSchedule("9:30 - 10:30", "Core Subject", "Room 102", "Dr. Core", "Theory"),
                ClassSchedule("10:45 - 11:45", "Practical Session", "Lab 1", "Mr. Practical", "Lab"),
                ClassSchedule("11:45 - 12:45", "Practical Session", "Lab 1", "Mr. Practical", "Lab"),
                ClassSchedule("2:00 - 3:00", "Advanced Topics", "Room 103", "Dr. Advanced", "Theory"),
                ClassSchedule("3:00 - 4:00", "Specialized Course", "Room 104", "Ms. Special", "Theory")
            )
        }
    }

    // Get upcoming class
    val upcomingClass = remember(todaySchedule) {
        try {
            getUpcomingClass(todaySchedule)
        } catch (e: Exception) {
            null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background video - use timtableloop for timetable screen
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    try {
                        // Use timtableloop video for the timetable screen
                        val videoUri = Uri.parse("android.resource://${ctx.packageName}/raw/timtableloop")
                        setVideoURI(videoUri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(0f, 0f)
                            start()
                        }
                        setOnErrorListener { mp, what, extra ->
                            // Fallback to ailoop if timtableloop fails
                            try {
                                val fallbackUri = Uri.parse("android.resource://${ctx.packageName}/raw/ailoop")
                                setVideoURI(fallbackUri)
                                setOnPreparedListener { fallbackMp ->
                                    fallbackMp.isLooping = true
                                    fallbackMp.setVolume(0f, 0f)
                                    fallbackMp.start()
                                }
                            } catch (e: Exception) {
                                // Handle fallback error silently
                            }
                            true
                        }
                    } catch (e: Exception) {
                        // Handle any video setup errors silently
                    }
                }
            }
        )

        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Clean top bar: title + class info + time (no action labels)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = department,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$year - $className",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                // Current time indicator
                Text(
                    text = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Subtle divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )

            

            // Upcoming class highlight
            upcomingClass?.let { nextClass ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NEXT CLASS",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = nextClass.subject,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${nextClass.time} • ${nextClass.room}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "Starts in ${getTimeUntilNextClass(nextClass)}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Welcome message
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2196F3).copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Welcome back! 👋",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Showing your saved class: $department - $year - $className",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Today's schedule
            Text(
                text = "Today's Schedule",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (todaySchedule.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp)
                ) {
                    items(todaySchedule) { classSchedule ->
                        ClassCard(classSchedule = classSchedule)
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "No classes scheduled for today.",
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Fixed bottom action buttons inside layout flow, placed at the end
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Back",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onClearPreferences,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Change Class",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            // Leave space so content doesn't get hidden behind the bottom bar
            Spacer(modifier = Modifier.height(120.dp))
        }
        
        // Bottom action bar anchored to the screen bottom (always visible)
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.65f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        // If School of Engineering, Year 2, Class A8 → open Notion screen.
                        // Else open placeholder notes screen.
                        val target = if (department == "School of Engineering" && year == "Year 2" && className == "A8") {
                            NotesActivity::class.java
                        } else {
                            PlaceholderNotesActivity::class.java
                        }
                        val intent = Intent(context, target)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.8f),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Text(
                        text = "Notes",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Button(
                    onClick = onClearPreferences,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Text(
                        text = "Change Class",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassCard(classSchedule: ClassSchedule) {
    // Add real-time updates every minute
    var currentTime by remember { mutableStateOf(java.time.LocalTime.now()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60000) // Update every minute
            currentTime = java.time.LocalTime.now()
        }
    }
    
    val timeStatus = remember(classSchedule, currentTime) {
        try {
            val timeRange = parseTimeRange(classSchedule.time)
            if (timeRange != null) {
                val (startTime, endTime) = timeRange
                when {
                    currentTime.isBefore(startTime) -> "UPCOMING"
                    currentTime.isAfter(endTime) -> "COMPLETED"
                    else -> "CURRENT"
                }
            } else {
                "UNKNOWN"
            }
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }
    
    val isCurrentClass = timeStatus == "CURRENT"
    val isPastClass = timeStatus == "COMPLETED"

    val cardColor = when {
        isCurrentClass -> Color(0xFF2196F3).copy(alpha = 0.9f) // Blue for current
        isPastClass -> Color.Gray.copy(alpha = 0.6f) // Gray for past
        else -> Color.Black.copy(alpha = 0.7f) // Default for upcoming
    }

    val statusText = when {
        isCurrentClass -> "CURRENT"
        isPastClass -> "COMPLETED"
        else -> "UPCOMING"
    }

    val statusColor = when {
        isCurrentClass -> Color(0xFF4CAF50) // Green for current
        isPastClass -> Color.Gray // Gray for completed
        else -> Color(0xFFFF9800) // Orange for upcoming
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Class details
            Text(
                text = classSchedule.subject,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = classSchedule.time,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = classSchedule.room,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = classSchedule.faculty,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = classSchedule.type,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    items: List<String>,
    onItemSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        // Blurred background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 8.dp)
                .background(Color.Black.copy(alpha = 0.3f))
        )
        
        // Dialog content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.85f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Title
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Items list
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items) { item: String ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onItemSelected(item) }
                                .padding(vertical = 12.dp, horizontal = 16.dp)
                        ) {
                            Text(
                                text = item,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            
        }
    }
}
