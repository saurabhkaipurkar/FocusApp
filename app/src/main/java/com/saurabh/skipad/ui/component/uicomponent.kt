package com.saurabh.skipad.ui.component

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.saurabh.skipad.model.InstalledAppGeneral

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionBottomSheet(
    allApps: List<InstalledAppGeneral>,
    isLoading: Boolean,
    onToggle: (InstalledAppGeneral, Boolean) -> Unit,
    onSelectAll: () -> Unit,
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {

            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Select Apps",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Search ──
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search apps...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Filter chips ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { activeFilter = filter },
                        label = { Text(filter.label) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── List ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 300.dp, max = 500.dp)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    filtered.isEmpty() -> {
                        Text(
                            text = "No apps found",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = filtered,
                                key = { it.packageName }
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
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSelectAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Select All")
                }
                Button(
                    onClick = {
                        onDone()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun AppCheckboxItem(
    app: InstalledAppGeneral,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!app.isSelected) },
        colors = CardDefaults.cardColors(
            containerColor = if (app.isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(
                drawable = app.icon,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Checkbox(
                checked = app.isSelected,
                onCheckedChange = onToggle
            )
        }
    }
}

enum class AppFilter(val label: String) {
    ALL("All"),
    SELECTED("Selected")
}

// ── Drawable → Compose painter without Coil dependency ──
@Composable
fun AppIcon(drawable: Drawable?, modifier: Modifier = Modifier) {
    if (drawable != null) {
        val bitmap = remember(drawable) { drawable.toBitmap().asImageBitmap() }
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier)
    } else {
        Box(
            modifier = modifier.background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp)
            )
        )
    }
}