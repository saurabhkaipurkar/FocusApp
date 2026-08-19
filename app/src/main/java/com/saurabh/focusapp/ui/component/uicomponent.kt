package com.saurabh.focusapp.ui.component

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.saurabh.focusapp.model.InstalledAppGeneral
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AppSelectionBottomSheet — visual layer only.
 * onToggle / onToggleAll / onDone / onDismiss semantics and all state
 * derivations (selectedCount, isAllSelected, filtered) are unchanged.
 */

private object SheetPalette {
    val Accent = Color(0xFFE8A33D)
    val AccentSoft = Color(0xFFFCE9C7)
    val OnAccent = Color(0xFF241A05)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionBottomSheet(
    allApps: List<InstalledAppGeneral>,
    isLoading: Boolean,
    onToggle: (InstalledAppGeneral, Boolean) -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(AppFilter.ALL) }

    val filtered = remember(query, activeFilter, allApps) {
        allApps
            .filter { app ->
                when (activeFilter) {
                    AppFilter.ALL -> true
                    AppFilter.SELECTED -> app.isSelected
                }
            }
            .filter { app ->
                query.isBlank() ||
                        app.appName.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true)
            }
    }

    val selectedCount = allApps.count { it.isSelected }
    val isAllSelected = allApps.isNotEmpty() && allApps.all { it.isSelected }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {

            // ── Header ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Select apps",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SheetPalette.AccentSoft
                    ) {
                        Text(
                            text = "$selectedCount selected",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = SheetPalette.OnAccent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Search ──
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = { Text("Search apps") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SheetPalette.Accent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Filter segmented control ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFilter.entries.forEach { filter ->
                    val selected = activeFilter == filter
                    Surface(
                        onClick = { activeFilter = filter },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) SheetPalette.Accent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (!selected) borderStrokeCompat() else null
                    ) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selected) SheetPalette.OnAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // ── List ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 300.dp, max = 500.dp)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = SheetPalette.Accent
                        )
                    }

                    filtered.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No apps found",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Try a different search",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                items = filtered,
                                key = { it.packageName },
                                contentType = { "app_item" }
                            ) { app ->
                                AppCheckboxItem(
                                    app = app,
                                    onToggle = { checked -> onToggle(app, checked) }
                                )
                            }
                        }
                    }
                }
            }

            // ── Bottom actions ──
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onToggleAll(!isAllSelected) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = borderStrokeCompat(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    Text(
                        text = if (isAllSelected) "Unselect all" else "Select all",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    onClick = {
                        onDone()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SheetPalette.Accent,
                        contentColor = SheetPalette.OnAccent
                    )
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Row-level tap and the checkbox drive the same [onToggle] callback exactly once
 * per interaction — fixes the original double-toggle bug where Card. Clickable and
 * Checkbox.onCheckedChange both fired on a checkbox tap. The Checkbox here is
 * visual-only (non-interactive); Row. Selectable is the single source of truth.
 */
@Composable
private fun AppCheckboxItem(
    app: InstalledAppGeneral,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = app.isSelected,
                onClick = { onToggle(!app.isSelected) }
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (app.isSelected) SheetPalette.AccentSoft.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (app.isSelected) SheetPalette.Accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(
                packageName = app.packageName,
                drawable = app.icon,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Visual-only indicator; the Row's selectable() above is what fires onToggle.
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (app.isSelected) SheetPalette.Accent else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (app.isSelected) SheetPalette.Accent else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (app.isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = SheetPalette.OnAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun borderStrokeCompat() = BorderStroke(
    1.dp,
    MaterialTheme.colorScheme.outlineVariant
)

enum class AppFilter(val label: String) {
    ALL("All"),
    SELECTED("Selected")
}

// ── Drawable → Compose painter with Manual Caching ──
@Composable
fun AppIcon(
    packageName: String,
    drawable: Drawable?,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(packageName) {
        mutableStateOf<Bitmap?>(AppIconMemoryCache.cache[packageName])
    }

    if (bitmap == null) {
        val context = LocalContext.current
        LaunchedEffect(packageName) {
            val decoded = withContext(Dispatchers.Default) {
                try {
                    val icon = drawable ?: context.packageManager.getApplicationIcon(packageName)
                    icon.toBitmap(width = 128, height = 128, config = Bitmap.Config.ARGB_8888)
                } catch (_: Exception) {
                    null
                }
            }
            if (decoded != null) {
                AppIconMemoryCache.cache.put(packageName, decoded)
                bitmap = decoded
            }
        }
    }

    Box(modifier = modifier) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.Center,
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(13.dp)
                    )
            )
        }
    }
}

object AppIconMemoryCache {
    private const val MAX_CACHE_SIZE = 20 * 1024 * 1024

    val cache = object : LruCache<String, Bitmap>(MAX_CACHE_SIZE) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }
}