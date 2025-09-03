package com.example.loopvid

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
            // Simple screen state management
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
            
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
                        currentScreen = Screen.Timetable(screen.department, screen.year, className)
                    },
                    onBack = { currentScreen = Screen.YearSelection(screen.department) }
                )
                is Screen.Timetable -> TimetableScreen(
                    department = screen.department,
                    year = screen.year,
                    className = screen.className,
                    onBack = { currentScreen = Screen.Home }
                )
            }
        }
    }
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
        else -> listOf(ClassSchedule("Weekend", "No Classes", "N/A", "N/A"))
    }
}

// Get upcoming class
private fun getUpcomingClass(schedule: List<ClassSchedule>): ClassSchedule? {
    return try {
        val currentTime = java.time.LocalTime.now()
        schedule.find { classSchedule ->
            try {
                val timeParts = classSchedule.time.split(" - ")
                if (timeParts.size >= 1) {
                    val classStartTime = java.time.LocalTime.parse(timeParts[0])
                    // Only show as upcoming if the class hasn't started yet
                    currentTime.isBefore(classStartTime)
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    } catch (e: Exception) {
        null
    }
}

// Get time until next class
private fun getTimeUntilNextClass(nextClass: ClassSchedule): String {
    return try {
        val currentTime = java.time.LocalTime.now()
        val timeParts = nextClass.time.split(" - ")
        if (timeParts.size >= 1) {
            val classTime = java.time.LocalTime.parse(timeParts[0])
            val duration = java.time.Duration.between(currentTime, classTime)
            
            val hours = duration.toHours()
            val minutes = duration.toMinutesPart()
            
            when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "Starting now"
            }
        } else {
            "Time not available"
        }
    } catch (e: Exception) {
        "Time not available"
    }
}

// Schedule generators for different departments and classes
private fun getMondaySchedule(department: String, year: String, className: String): List<ClassSchedule> {
    return when (department) {
        "School of Engineering" -> when (year) {
            "Year 2" -> when (className) {
                "A1" -> listOf(
                    ClassSchedule("8:30 - 9:30", "Data Structures", "Room 101", "Dr. Smith", "Theory"),
                    ClassSchedule("9:30 - 10:30", "Mathematics", "Room 102", "Prof. Johnson", "Theory"),
                    ClassSchedule("10:45 - 11:45", "Programming Lab", "Lab 1", "Mr. Davis", "Lab"),
                    ClassSchedule("11:45 - 12:45", "Programming Lab", "Lab 1", "Mr. Davis", "Lab"),
                    ClassSchedule("2:00 - 3:00", "Artificial Intelligence", "Room 103", "Dr. Wilson", "Theory"),
                    ClassSchedule("3:00 - 4:00", "Database Systems", "Room 104", "Ms. Brown", "Theory")
                )
                "A2" -> listOf(
                    ClassSchedule("8:30 - 9:30", "Computer Networks", "Room 201", "Dr. Anderson", "Theory"),
                    ClassSchedule("9:30 - 10:30", "Operating Systems", "Room 202", "Prof. Taylor", "Theory"),
                    ClassSchedule("10:45 - 11:45", "Network Lab", "Lab 2", "Mr. Clark", "Lab"),
                    ClassSchedule("11:45 - 12:45", "Network Lab", "Lab 2", "Mr. Clark", "Lab"),
                    ClassSchedule("2:00 - 3:00", "Software Engineering", "Room 203", "Dr. Lee", "Theory"),
                    ClassSchedule("3:00 - 4:00", "Web Development", "Room 204", "Ms. Garcia", "Theory")
                )
                "A8" -> listOf(
                    ClassSchedule("8:30 - 9:30", "Advanced Algorithms", "Room 301", "Dr. Rodriguez", "Theory"),
                    ClassSchedule("9:30 - 10:30", "Machine Learning", "Room 302", "Prof. Martinez", "Theory"),
                    ClassSchedule("10:45 - 11:45", "ML Lab", "Lab 3", "Mr. Hernandez", "Lab"),
                    ClassSchedule("11:45 - 12:45", "ML Lab", "Lab 3", "Mr. Hernandez", "Lab"),
                    ClassSchedule("2:00 - 3:00", "Deep Learning", "Room 303", "Dr. Lopez", "Theory"),
                    ClassSchedule("3:00 - 4:00", "Data Mining", "Room 304", "Ms. Gonzalez", "Theory")
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
                    ClassSchedule("8:30 - 9:30", "Computer Architecture", "Room 101", "Dr. Thompson", "Theory"),
                    ClassSchedule("9:30 - 10:30", "Digital Logic", "Room 102", "Prof. White", "Theory"),
                    ClassSchedule("10:45 - 11:45", "Architecture Lab", "Lab 1", "Mr. Black", "Lab"),
                    ClassSchedule("11:45 - 12:45", "Architecture Lab", "Lab 1", "Mr. Black", "Lab"),
                    ClassSchedule("2:00 - 3:00", "Computer Graphics", "Room 103", "Dr. Green", "Theory"),
                    ClassSchedule("3:00 - 4:00", "Game Development", "Room 104", "Ms. Blue", "Theory")
                )
                "A2" -> listOf(
                    ClassSchedule("8:30 - 9:30", "Data Science", "Room 201", "Dr. Red", "Theory"),
                    ClassSchedule("9:30 - 10:30", "Statistics", "Room 202", "Prof. Yellow", "Theory"),
                    ClassSchedule("10:45 - 11:45", "Data Lab", "Lab 2", "Mr. Orange", "Lab"),
                    ClassSchedule("11:45 - 12:45", "Data Lab", "Lab 2", "Mr. Orange", "Lab"),
                    ClassSchedule("2:00 - 3:00", "Big Data", "Room 203", "Dr. Purple", "Theory"),
                    ClassSchedule("3:00 - 4:00", "Analytics", "Room 204", "Ms. Pink", "Theory")
                )
                "A8" -> listOf(
                    ClassSchedule("8:30 - 9:30", "Neural Networks", "Room 301", "Dr. Gray", "Theory"),
                    ClassSchedule("9:30 - 10:30", "Computer Vision", "Room 302", "Prof. Silver", "Theory"),
                    ClassSchedule("10:45 - 11:45", "Vision Lab", "Lab 3", "Mr. Gold", "Lab"),
                    ClassSchedule("11:45 - 12:45", "Vision Lab", "Lab 3", "Mr. Gold", "Lab"),
                    ClassSchedule("2:00 - 3:00", "Robotics", "Room 303", "Dr. Bronze", "Theory"),
                    ClassSchedule("3:00 - 4:00", "IoT Systems", "Room 304", "Ms. Copper", "Theory")
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
                    ClassSchedule("8:30 - 9:30", "Software Testing", "Room 101", "Dr. Lewis", "Theory"),
                    ClassSchedule("9:30 - 10:30", "Quality Assurance", "Room 102", "Prof. Hall", "Theory"),
                    ClassSchedule("10:45 - 11:45", "Testing Lab", "Lab 1", "Mr. Allen", "Lab"),
                    ClassSchedule("11:45 - 12:45", "Testing Lab", "Lab 1", "Mr. Allen", "Lab"),
                    ClassSchedule("2:00 - 3:00", "Mobile Development", "Room 103", "Dr. Young", "Theory"),
                    ClassSchedule("3:00 - 4:00", "App Development", "Room 104", "Ms. King", "Theory")
                )
                "A2" -> listOf(
                    ClassSchedule("8:30 - 9:30", "Cloud Computing", "Room 201", "Dr. Wright", "Theory"),
                    ClassSchedule("9:30 - 10:30", "DevOps", "Room 202", "Prof. Scott", "Theory"),
                    ClassSchedule("10:45 - 11:45", "Cloud Lab", "Lab 2", "Mr. Baker", "Lab"),
                    ClassSchedule("11:45 - 12:45", "Cloud Lab", "Lab 2", "Mr. Baker", "Lab"),
                    ClassSchedule("2:00 - 3:00", "Docker & Kubernetes", "Room 203", "Dr. Adams", "Theory"),
                    ClassSchedule("3:00 - 4:00", "Microservices", "Room 204", "Ms. Nelson", "Theory")
                )
                "A8" -> listOf(
                    ClassSchedule("8:30 - 9:30", "Natural Language Processing", "Room 301", "Dr. Carter", "Theory"),
                    ClassSchedule("9:30 - 10:30", "Speech Recognition", "Room 302", "Prof. Mitchell", "Theory"),
                    ClassSchedule("10:45 - 11:45", "NLP Lab", "Lab 3", "Mr. Roberts", "Lab"),
                    ClassSchedule("11:45 - 12:45", "NLP Lab", "Lab 3", "Mr. Roberts", "Lab"),
                    ClassSchedule("2:00 - 3:00", "Chatbots", "Room 303", "Dr. Phillips", "Theory"),
                    ClassSchedule("3:00 - 4:00", "AI Ethics", "Room 304", "Ms. Campbell", "Theory")
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
                    ClassSchedule("8:30 - 9:30", "Cybersecurity", "Room 101", "Dr. Morris", "Theory"),
                    ClassSchedule("9:30 - 10:30", "Network Security", "Room 102", "Prof. Rogers", "Theory"),
                    ClassSchedule("10:45 - 11:45", "Security Lab", "Lab 1", "Mr. Reed", "Lab"),
                    ClassSchedule("11:45 - 12:45", "Security Lab", "Lab 1", "Mr. Reed", "Lab"),
                    ClassSchedule("2:00 - 3:00", "Ethical Hacking", "Room 103", "Dr. Cook", "Theory"),
                    ClassSchedule("3:00 - 4:00", "Digital Forensics", "Room 104", "Ms. Morgan", "Theory")
                )
                "A2" -> listOf(
                    ClassSchedule("8:30 - 9:30", "Blockchain", "Room 201", "Dr. Bell", "Theory"),
                    ClassSchedule("9:30 - 10:30", "Cryptography", "Room 202", "Prof. Murphy", "Theory"),
                    ClassSchedule("10:45 - 11:45", "Blockchain Lab", "Lab 2", "Mr. Richardson", "Lab"),
                    ClassSchedule("11:45 - 12:45", "Blockchain Lab", "Lab 2", "Mr. Richardson", "Lab"),
                    ClassSchedule("2:00 - 3:00", "Smart Contracts", "Room 203", "Dr. Cox", "Theory"),
                    ClassSchedule("3:00 - 4:00", "DeFi", "Room 204", "Ms. Howard", "Theory")
                )
                "A8" -> listOf(
                    ClassSchedule("8:30 - 9:30", "Reinforcement Learning", "Room 301", "Dr. Ward", "Theory"),
                    ClassSchedule("9:30 - 10:30", "Game AI", "Room 302", "Prof. Torres", "Theory"),
                    ClassSchedule("10:45 - 11:45", "RL Lab", "Lab 3", "Mr. Peterson", "Lab"),
                    ClassSchedule("11:45 - 12:45", "RL Lab", "Lab 3", "Mr. Peterson", "Lab"),
                    ClassSchedule("2:00 - 3:00", "Autonomous Systems", "Room 303", "Dr. Gray", "Theory"),
                    ClassSchedule("3:00 - 4:00", "AI Safety", "Room 304", "Ms. Ramirez", "Theory")
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
                    ClassSchedule("8:30 - 9:30", "Project Management", "Room 101", "Dr. Bennett", "Theory"),
                    ClassSchedule("9:30 - 10:30", "Agile Development", "Room 102", "Prof. Wood", "Theory"),
                    ClassSchedule("10:45 - 11:45", "Project Lab", "Lab 1", "Mr. Barnes", "Lab"),
                    ClassSchedule("11:45 - 12:45", "Project Lab", "Lab 1", "Mr. Barnes", "Lab"),
                    ClassSchedule("2:00 - 3:00", "Capstone Project", "Room 103", "Dr. Ross", "Theory"),
                    ClassSchedule("3:00 - 4:00", "Industry Connect", "Room 104", "Ms. Henderson", "Theory")
                )
                "A2" -> listOf(
                    ClassSchedule("8:30 - 9:30", "Data Engineering", "Room 201", "Dr. Coleman", "Theory"),
                    ClassSchedule("9:30 - 10:30", "ETL Processes", "Room 202", "Prof. Jenkins", "Theory"),
                    ClassSchedule("10:45 - 11:45", "Data Lab", "Lab 2", "Mr. Perry", "Lab"),
                    ClassSchedule("11:45 - 12:45", "Data Lab", "Lab 2", "Mr. Perry", "Lab"),
                    ClassSchedule("2:00 - 3:00", "Data Pipeline", "Room 203", "Dr. Powell", "Theory"),
                    ClassSchedule("3:00 - 4:00", "Data Governance", "Room 204", "Ms. Long", "Theory")
                )
                "A8" -> listOf(
                    ClassSchedule("8:30 - 9:30", "AI Research", "Room 301", "Dr. Patterson", "Theory"),
                    ClassSchedule("9:30 - 10:30", "Research Methods", "Room 302", "Prof. Hughes", "Theory"),
                    ClassSchedule("10:45 - 11:45", "Research Lab", "Lab 3", "Mr. Flores", "Lab"),
                    ClassSchedule("11:45 - 12:45", "Research Lab", "Lab 3", "Mr. Flores", "Lab"),
                    ClassSchedule("2:00 - 3:00", "Paper Writing", "Room 303", "Dr. Butler", "Theory"),
                    ClassSchedule("3:00 - 4:00", "Conference Prep", "Room 304", "Ms. Simmons", "Theory")
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
    onBack: () -> Unit
) {
    // Get today's schedule
    val todaySchedule = remember(department, year, className) {
        try {
            getTodaysSchedule(department, year, className)
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
        // Background video (try timtableloop, fallback to ailoop)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    try {
                        // Always use ailoop for now to avoid any video issues
                        val videoUri = Uri.parse("android.resource://${ctx.packageName}/raw/ailoop")
                        setVideoURI(videoUri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(0f, 0f)
                            start()
                        }
                        setOnErrorListener { mp, what, extra ->
                            // Handle video error silently
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
            // Top bar with back button and header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White),
                    modifier = Modifier.size(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(
                        text = "←",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Header info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = department,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$year - $className",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
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

            // Today's schedule
            Text(
                text = "Today's Schedule",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Debug info (temporary)
            Text(
                text = "Debug: Current time is ${java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (todaySchedule.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ClassCard(classSchedule: ClassSchedule) {
    val isCurrentClass = remember(classSchedule) {
        try {
            val currentTime = java.time.LocalTime.now()
            val timeParts = classSchedule.time.split(" - ")
            if (timeParts.size >= 2) {
                val classStartTime = java.time.LocalTime.parse(timeParts[0])
                val classEndTime = java.time.LocalTime.parse(timeParts[1])
                // Check if current time is between start and end time (inclusive of start)
                !currentTime.isBefore(classStartTime) && currentTime.isBefore(classEndTime)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    val isPastClass = remember(classSchedule) {
        try {
            val currentTime = java.time.LocalTime.now()
            val timeParts = classSchedule.time.split(" - ")
            if (timeParts.size >= 2) {
                val classEndTime = java.time.LocalTime.parse(timeParts[1])
                // Check if current time is after the class end time
                currentTime.isAfter(classEndTime)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

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
