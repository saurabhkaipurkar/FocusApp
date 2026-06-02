package com.saurabh.skipad.activity

import android.os.Bundle
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.saurabh.skipad.data.PreferenceManager
import com.saurabh.skipad.ui.theme.SkipAdTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ManageSpaceActivity : ComponentActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkipAdTheme {
                ManageSpaceScreen(
                    onBack = { finish() },
                    onClearCache = { 
                        clearCache()
                        Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
                    },
                    onResetSettings = {
                        preferenceManager.saveSelectedApps(emptySet())
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ManageSpaceActivity, "Settings reset", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    private fun clearCache() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                cacheDir.deleteRecursively()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSpaceScreen(
    onBack: () -> Unit,
    onClearCache: () -> Unit,
    onResetSettings: suspend () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheSize by remember { mutableStateOf("0 B") }

    // Update cache size
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val size = getFolderSize(context.cacheDir)
            cacheSize = Formatter.formatFileSize(context, size)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Information Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Full data clear is disabled to protect your configured focus settings. You can manage temporary files here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Statistics Card
            StorageItem(
                title = "Temporary Cache",
                subtitle = "Includes app icons and temporary data",
                size = cacheSize,
                icon = Icons.Default.DeleteSweep,
                buttonLabel = "Clear Cache",
                onAction = {
                    onClearCache()
                    // Re-calculate size after clearing
                    scope.launch(Dispatchers.IO) {
                        val size = getFolderSize(context.cacheDir)
                        cacheSize = Formatter.formatFileSize(context, size)
                    }
                }
            )

            StorageItem(
                title = "App Settings",
                subtitle = "Reset selected apps and configurations",
                size = "Small",
                icon = Icons.Default.SettingsBackupRestore,
                buttonLabel = "Reset Preferences",
                onAction = {
                    scope.launch {
                        onResetSettings()
                    }
                }
            )

            Spacer(Modifier.weight(1f))
            
            Text(
                text = "Version 1.0.0",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StorageItem(
    title: String,
    subtitle: String,
    size: String,
    icon: ImageVector,
    buttonLabel: String,
    onAction: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = size,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(buttonLabel)
            }
        }
    }
}

private fun getFolderSize(file: File): Long {
    var size: Long = 0
    if (file.exists() && file.isDirectory) {
        file.listFiles()?.forEach {
            size += if (it.isDirectory) getFolderSize(it) else it.length()
        }
    } else if (file.exists()) {
        size = file.length()
    }
    return size
}
