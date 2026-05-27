package com.saurabh.skipad

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.saurabh.skipad.extension.findActivity
import com.saurabh.skipad.navigation.AppNavigation
import com.saurabh.skipad.ui.theme.SkipAdTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemDark) }
            var permissionDenied by remember { mutableStateOf(false) }
            val requestPermissionLauncher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        permissionDenied = true
                    }
                }
            SkipAdTheme(
                darkTheme = isDarkMode
            ) {
                NotificationPermissionHandler(
                    permissionDenied = permissionDenied,
                    onResetDenied = {
                        permissionDenied = false
                    },
                    onRequestPermission = {
                        requestPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                )
                AppNavigation(
                    isDarkMode = isDarkMode,
                    onThemeToggle = {
                        isDarkMode = it
                    }
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NotificationPermissionHandler(
    permissionDenied: Boolean,
    onResetDenied: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    var showRationale by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
        if (!isGranted) {
            onRequestPermission()
        }
    }
    LaunchedEffect(permissionDenied) {
        if (permissionDenied) {
            val shouldShowRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            if (shouldShowRationale) {
                showRationale = true
            } else {
                showSettings = true
            }
            onResetDenied()
        }
    }

    // Rationale Dialog
    if (showRationale) {
        AlertDialog(
            onDismissRequest = {
                showRationale = false
            },
            title = {
                Text("Allow Notifications")
            },
            text = {
                Text(
                    "We send reminders for membership expiry and trial follow-ups."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        onRequestPermission()
                    }
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Settings Dialog
    if (showSettings) {
        AlertDialog(
            onDismissRequest = {
                showSettings = false
            },
            title = {
                Text("Permission Required")
            },
            text = {
                Text(
                    "Notification permission permanently denied. Please enable it from settings."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSettings = false
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts(
                                    "package",
                                    context.packageName,
                                    null
                                )
                            }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSettings = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}