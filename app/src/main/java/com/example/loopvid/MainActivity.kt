package com.example.loopvid

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.Toast

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            var showDepartmentDialog by remember { mutableStateOf(false) }
            var showClassDialog by remember { mutableStateOf(false) }
            var selectedDepartment by remember { mutableStateOf<String?>(null) }

            val departments = listOf(
                "Engineering & Technology",
                "Biomedical Engineering",
                "Internet of Things"
            )
            val classes = remember { (1..10).map { "A$it" } }

            Box(modifier = Modifier.fillMaxSize()) {
                // Background video layer
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

                // Bottom overlay button
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
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "Select Department",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (showDepartmentDialog) {
                DepartmentDialog(
                    options = departments,
                    onSelect = { dept ->
                        selectedDepartment = dept
                        showDepartmentDialog = false
                        showClassDialog = true
                    },
                    onDismiss = { showDepartmentDialog = false }
                )
            }
            if (showClassDialog) {
                DepartmentDialog(
                    title = "Choose Class",
                    options = classes,
                    onSelect = { cls ->
                        showClassDialog = false
                        Toast.makeText(
                            context,
                            "Selected: $selectedDepartment - $cls",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onDismiss = { showClassDialog = false }
                )
            }
        }
    }
}

@Composable
private fun DepartmentDialog(
    title: String = "Choose Department",
    options: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options) { opt ->
                    Text(
                        text = opt,
                        color = Color.Unspecified,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .clickable { onSelect(opt) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
