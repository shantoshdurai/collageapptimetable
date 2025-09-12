package com.example.loopvid

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

class NotesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
		// Try to open Notion in Chrome first, then any browser. Fall back to WebView if unavailable.
		val notionUrl = "https://www.notion.so/Study-Notes-Class-A8-24b05735a36e809e95dfe9f6119ba6fd?source=copy_link"
		try {
			val chromeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(notionUrl)).apply {
				addCategory(Intent.CATEGORY_BROWSABLE)
				setPackage("com.android.chrome")
			}
			startActivity(chromeIntent)
			finish()
			return
		} catch (_: ActivityNotFoundException) {
			try {
				val anyBrowserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(notionUrl)).apply {
					addCategory(Intent.CATEGORY_BROWSABLE)
				}
				startActivity(anyBrowserIntent)
				finish()
				return
			} catch (_: Exception) { /* fall through to WebView */ }
		}

		setContent {
			NotesScreen(
				onBackPressed = { finish() }
			)
		}
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun NotesScreen(
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBackPressed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                modifier = Modifier.size(40.dp)
            ) {
                Text(
                    text = "←",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = "Study Notes",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading Study Notes...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // WebView
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        // Spoof a standard mobile Chrome UA for better Notion compatibility
                        userAgentString =
                            "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
                    }
                    // Allow cookies so Notion can authenticate/load content
                    CookieManager.getInstance().setAcceptCookie(true)
                    try {
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    } catch (_: Throwable) { }
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }
                        // Keep navigation inside WebView. Translate deep links to https and load here.
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            if (url == null) return false
                            return try {
                                val uri = Uri.parse(url)
                                when (uri.scheme) {
                                    "http", "https" -> false
                                    "notion" -> {
                                        val httpsUrl = url.replaceFirst("notion://", "https://")
                                        view?.loadUrl(httpsUrl)
                                        true
                                    }
                                    "intent" -> {
                                        val intent = try { Intent.parseUri(url, 0) } catch (_: Exception) { null }
                                        val dataUrl = intent?.dataString
                                        if (dataUrl != null) {
                                            view?.loadUrl(dataUrl)
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    else -> false
                                }
                            } catch (_: Exception) {
                                false
                            }
                        }
                    }
                    
                    // Fallback: load Notion inside the WebView if no external browser is available
                    loadUrl("https://www.notion.so/Study-Notes-Class-A8-24b05735a36e809e95dfe9f6119ba6fd?source=copy_link")
                }
            }
        )
    }
}
