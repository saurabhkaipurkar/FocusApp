package com.saurabh.focusapp.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurabh.focusapp.BuildConfig
import com.saurabh.focusapp.model.InstalledAppGeneral
import com.saurabh.focusapp.ui.component.AppIcon
import com.saurabh.focusapp.ui.component.AppSelectionBottomSheet
import com.saurabh.focusapp.viewmodel.DnsVpnViewModel

/**
 * DashboardScreen — visual layer only.
 * All ViewModel calls, state reads, callback wiring, and navigation are
 * unchanged from the original implementation. Only composable structure,
 * styling, and layout have been reworked.
 */

// ══════════════════════════════════════════════════════
// DESIGN TOKENS — accent palette (slate + amber signal color)
// ══════════════════════════════════════════════════════
private object Palette {
    val Accent = Color(0xFFE8A33D)       // warm amber — primary action / active signal
    val AccentSoft = Color(0xFFFCE9C7)
    val InkSurface = Color(0xFF14161A)   // near-black card surface
    val InkSurfaceAlt = Color(0xFF1D2026)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DnsVpnViewModel = hiltViewModel()) {
    if (BuildConfig.DEBUG) {
        SideEffect { Log.d("DashboardScreen", "DashboardScreen recomposed") }
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

    val onStopVpn = remember(viewModel) { { viewModel.stopVpn() } }
    val onShowSheet = remember { { showBottomSheet = true } }
    val onDismissSheet = remember { { showBottomSheet = false } }
    val onConfirmSelection = remember(viewModel) { { viewModel.confirmSelection() } }
    val appCount = remember(uiState.selectedApps) { uiState.selectedApps.size }

    Scaffold(
        topBar = { DashboardTopBar(isActive = uiState.isVpnActive) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                item {
                    HeroStatusCard(
                        isActive = uiState.isVpnActive,
                        appCount = appCount,
                        onStop = onStopVpn
                    )
                }

                if (uiState.selectedApps.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Focused apps",
                            trailing = "$appCount selected"
                        )
                    }

                    items(
                        items = uiState.selectedApps,
                        key = { it.packageName }
                    ) { app ->
                        val isItemActive = uiState.activePackage == app.packageName

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

                    item {
                        AddMoreCard(onClick = onShowSheet)
                    }

                    item { Spacer(Modifier.height(88.dp)) }
                } else {
                    item { EmptyState() }
                }
            }

            ExtendedFloatingActionButton(
                onClick = onShowSheet,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(18.dp), clip = false),
                containerColor = Palette.Accent,
                contentColor = Color(0xFF241A05),
                shape = RoundedCornerShape(18.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = { Text("Add apps", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
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
// TOP BAR
// ══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(isActive: Boolean) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Focus",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Distraction-free browsing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // live status dot, purely visual, driven by existing isActive state
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Palette.Accent else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// HERO STATUS CARD
// ══════════════════════════════════════════════════════

@Composable
private fun HeroStatusCard(
    isActive: Boolean,
    appCount: Int,
    onStop: () -> Unit
) {
    val bgBrush = remember(isActive) {
        if (isActive) {
            Brush.linearGradient(listOf(Palette.InkSurface, Palette.InkSurfaceAlt))
        } else {
            Brush.linearGradient(listOf(Palette.InkSurfaceAlt, Palette.InkSurface))
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(28.dp), clip = false),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(bgBrush)
                .padding(22.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isActive) Palette.Accent.copy(alpha = 0.16f) else Color.White.copy(
                                    alpha = 0.06f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Outlined.Security else Icons.Outlined.SecurityUpdateWarning,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (isActive) Palette.Accent else Color.White.copy(alpha = 0.55f)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (isActive) "Focus mode active" else "Focus mode off",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = if (isActive) "Selected apps route through a filtered DNS/VPN" else "Turn on to start blocking distractions",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StatMetric(
                        label = "Status",
                        value = if (isActive) "On" else "Off",
                        accent = isActive
                    )
                    StatMetric(label = "Apps", value = "$appCount", accent = false)
                }

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = onStop,
                    enabled = isActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.10f),
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.04f),
                        disabledContentColor = Color.White.copy(alpha = 0.25f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.StopCircle,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Stop service", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StatMetric(label: String, value: String, accent: Boolean) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f),
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = if (accent) Palette.Accent else Color.White
        )
    }
}

// ══════════════════════════════════════════════════════
// SECTION HEADER
// ══════════════════════════════════════════════════════

@Composable
private fun SectionHeader(title: String, trailing: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
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
    val colorScheme = MaterialTheme.colorScheme
    val elevation by animateDpAsState(if (isActive) 3.dp else 0.dp, label = "rowElevation")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = elevation, shape = RoundedCornerShape(20.dp), clip = false),
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) Palette.AccentSoft.copy(alpha = 0.4f) else colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isActive) Palette.Accent.copy(alpha = 0.5f) else colorScheme.outlineVariant.copy(
                alpha = 0.6f
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colorScheme.surfaceVariant)
                    .border(
                        1.dp,
                        colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                AppIcon(
                    packageName = app.packageName,
                    drawable = app.icon,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colorScheme.onBackground
                )
                Text(
                    text = if (isActive) "Running in focus mode" else app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isActive) Palette.Accent else colorScheme.onSurfaceVariant
                )
            }

            Surface(
                onClick = onOpen,
                shape = CircleShape,
                color = if (isActive) Palette.Accent else colorScheme.surfaceVariant,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Open",
                        modifier = Modifier.size(18.dp),
                        tint = if (isActive) Color(0xFF241A05) else colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// ADD MORE CARD
// ══════════════════════════════════════════════════════

@Composable
private fun AddMoreCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Palette.Accent
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Add more apps",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Palette.Accent
            )
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
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No apps selected",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Tap \"Add apps\" below to get started",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}