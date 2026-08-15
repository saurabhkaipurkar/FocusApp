package com.saurabh.focusapp.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurabh.focusapp.model.InstalledAppGeneral
import com.saurabh.focusapp.ui.component.AppIcon
import com.saurabh.focusapp.ui.component.AppSelectionBottomSheet
import com.saurabh.focusapp.viewmodel.DnsVpnViewModel

/**
 * Optimized DashboardScreen.
 * 
 * Performance Optimizations:
 * 1. Stability: Marked VpnUiState and InstalledAppGeneral with @Immutable to 
 *    enable 'Skippable' recompositions.
 * 2. Stable Lambdas: Using remember { ... } for all callback lambdas passed 
 *    to sub-composables to prevent unnecessary parent-induced recompositions.
 * 3. Lazy List Optimization: Provided keys to items and remembered per-item 
 *    lambdas.
 * 4. Derived State: Used derivedStateOf/remember(key) for computations like 
 *    app counts and color derivations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DnsVpnViewModel = hiltViewModel()) {
    // Performance Metric: Log recompositions to track if optimizations are working
    SideEffect {
        Log.d("DashboardScreen", "DashboardScreen recomposed")
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showBottomSheet by remember { mutableStateOf(false) }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onVpnPermissionGranted()
        }
    }

    // Optimization: Remember static or ViewModel-bound callbacks
    val onStopVpn = remember(viewModel) { { viewModel.stopVpn() } }
    val onShowSheet = remember { { showBottomSheet = true } }
    val onDismissSheet = remember { { showBottomSheet = false } }
    val onConfirmSelection = remember(viewModel) { { viewModel.confirmSelection() } }
    
    // Optimization: Using remember(uiState.selectedApps) to avoid re-calculating size 
    // unless the list instance changes.
    val appCount = remember(uiState.selectedApps) { uiState.selectedApps.size }

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Focus",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Distraction-free browsing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Status card ──
                item {
                    StatusCard(
                        isActive = uiState.isVpnActive,
                        appCount = appCount,
                        onStop = onStopVpn
                    )
                }

                // ── Section header ──
                if (uiState.selectedApps.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Focused apps",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "$appCount selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(
                        items = uiState.selectedApps,
                        key = { it.packageName }
                    ) { app ->
                        // Optimization: Derived state per item
                        val isItemActive = uiState.activePackage == app.packageName
                        
                        // Optimization: Remember item-specific lambda to keep AppRow skippable
                        val onOpenItem = remember(app, viewModel, vpnPermissionLauncher) {
                            {
                                val intent = viewModel.requestVpnFor(app)
                                if (intent != null) vpnPermissionLauncher.launch(intent)
                            }
                        }

                        AppRow(
                            app = app,
                            isActive = isItemActive,
                            onOpen = onOpenItem
                        )
                    }

                    // ── Add more tap area ──
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable(onClick = onShowSheet)
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Add more apps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                } else {
                    item { EmptyState() }
                }
            }

            // ── FAB ──
            ExtendedFloatingActionButton(
                onClick = onShowSheet,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = { Text("Add apps", fontSize = 14.sp) }
            )
        }
    }

    if (showBottomSheet) {
        AppSelectionBottomSheet(
            allApps = uiState.allApps,
            isLoading = uiState.isLoadingApps,
            onToggle = { app, checked -> viewModel.toggleAppSelection(app, checked) },
            onToggleAll = { select ->
                if (select) viewModel.selectAllApps() else viewModel.unselectAllApps()
            },
            onDone = onConfirmSelection,
            onDismiss = onDismissSheet
        )
    }
}

// ══════════════════════════════════════════════════════
// STATUS CARD
// ══════════════════════════════════════════════════════

@Composable
private fun StatusCard(
    isActive: Boolean,
    appCount: Int,
    onStop: () -> Unit
) {
    // Optimization: Remember color derivation
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val cardBg = remember(surfaceVariant) {
        surfaceVariant.copy(alpha = 0.5f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            // ── Icon + status ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Outlined.Security else Icons.Outlined.SecurityUpdateWarning,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column {
                    Text(
                        text = "Focus status",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (isActive) "Active" else "Inactive",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Pills ──
            Row(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill(
                    icon = if (isActive) Icons.Outlined.WifiOff else Icons.Outlined.Wifi,
                    label = if (isActive) "Focus mode on" else "Focus mode off",
                    active = isActive
                )
                StatusPill(
                    icon = Icons.Outlined.Apps,
                    label = "$appCount app${if (appCount != 1) "s" else ""}",
                    active = false
                )
            }

            // ── Stop button ──
            OutlinedButton(
                onClick = onStop,
                enabled = isActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.StopCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Stop service", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun StatusPill(
    icon: ImageVector,
    label: String,
    active: Boolean
) {
    // Optimization: Remember color assignments
    val colorScheme = MaterialTheme.colorScheme
    val bg = remember(active, colorScheme.primaryContainer, colorScheme.surface) {
        if (active) colorScheme.primaryContainer else colorScheme.surface
    }
    val border = remember(active, colorScheme.primary, colorScheme.outlineVariant) {
        if (active) colorScheme.primary else colorScheme.outlineVariant
    }
    val textColor = remember(active, colorScheme.onPrimaryContainer, colorScheme.onSurfaceVariant) {
        if (active) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .border(0.5.dp, border, CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = textColor
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = textColor
        )
    }
}

// ══════════════════════════════════════════════════════
// APP ROW
// ══════════════════════════════════════════════════════

@Composable
private fun AppRow(
    app: InstalledAppGeneral,
    isActive: Boolean,
    onOpen: () -> Unit
) {
    // Optimization: Remember theme-derived properties
    val colorScheme = MaterialTheme.colorScheme
    val bg = remember(isActive, colorScheme.primaryContainer, colorScheme.surface) {
        if (isActive) colorScheme.primaryContainer else colorScheme.surface
    }
    val borderColor = remember(isActive, colorScheme.primary, colorScheme.outlineVariant) {
        if (isActive) colorScheme.primary else colorScheme.outlineVariant
    }
    val iconBg = remember(isActive, colorScheme.primaryContainer, colorScheme.surfaceVariant) {
        if (isActive) colorScheme.primaryContainer else colorScheme.surfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(0.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg)
                    .border(
                        0.5.dp,
                        borderColor,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                AppIcon(
                    packageName = app.packageName,
                    drawable = app.icon,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Name + package
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isActive) "Tap Open to launch in focus mode" else app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Open button
            OutlinedButton(
                onClick = onOpen,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outline
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Open", fontSize = 13.sp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// EMPTY STATE
// ══════════════════════════════════════════════════════

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "No apps selected",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Tap \"Add apps\" to get started",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
