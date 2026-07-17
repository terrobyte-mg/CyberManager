package com.terrobytes.cybermanaver2.ui.composable.dash

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Tokens ──────────────────────────────────────────────────────────
private val BgDeep        = Color(0xFF0B1319)
private val Surface       = Color(0xFF141C24)
private val BorderColor   = Color(0xFF232D38)
private val Accent        = Color(0xFF1A8CFF)
private val AccentBg      = Color(0x221A8CFF)
private val AccentBorder  = Color(0x401A8CFF)
private val Muted         = Color(0xFF8E9297)
private val IconOff       = Color(0xFF5F6368)
private val Danger        = Color(0xFFFF5C5C)
private val DangerBg      = Color(0x18FF5C5C)
private val DangerBorder  = Color(0x33FF5C5C)
private val Warn          = Color(0xFFF59E0B)
private val Ok            = Color(0xFF34D399)
private val OkBg          = Color(0x1834D399)
private val OkBorder      = Color(0x3034D399)
private val White         = Color(0xFFFFFFFF)

// ─── Data ────────────────────────────────────────────────────────────
data class ConnectedClient(
    val id: Int,
    val name: String,
    val ip: String,
    val minutes: Int?,
)

enum class AppTab { DASHBOARD, REVENUS, HISTORIQUE, PARAMETRES }

// ─── Helpers ─────────────────────────────────────────────────────────
private fun timeColor(m: Int?): Color = when {
    m == null -> Muted
    m <= 2    -> Danger
    m <= 8    -> Warn
    else      -> Ok
}

private fun formatTime(m: Int): String {
    if (m < 60) return "${m}m"
    val h = m / 60; val r = m % 60
    return if (r > 0) "${h}h${r}m" else "${h}h"
}

private val previewClients = listOf(
    ConnectedClient(1, "MacBook-Pro",    "192.168.88.10", 47),
    ConnectedClient(2, "iPhone-Thomas",  "192.168.88.14", 5),
    ConnectedClient(3, "DESKTOP-A3F2",   "192.168.88.21", null),
    ConnectedClient(4, "iPad-Lucie",     "192.168.88.27", null),
)

// ═══════════════════════════════════════════════════════════════════════
// Root — app shell with bottom nav
// ═══════════════════════════════════════════════════════════════════════
@Preview(showBackground = true, backgroundColor = 0xFF0B1319, device = "id:pixel_6")
@Composable
fun RouterApp() {
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var clients by remember { mutableStateOf(previewClients) }

    Scaffold(
        containerColor = BgDeep,
        bottomBar = {
            AppBottomNav(
                current = currentTab,
                onSelect = { currentTab = it },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                AppTab.DASHBOARD   -> DashboardScreen(
                    clients    = clients,
                    onAddTime  = { id -> clients = clients.map { if (it.id == id) it.copy(minutes = (it.minutes ?: 0) + 30) else it } },
                    onCutTime  = { id -> clients = clients.map { if (it.id == id) it.copy(minutes = null) else it } },
                    onAddAll   = { clients = clients.map { if (it.minutes == null) it.copy(minutes = 60) else it } },
                )
                AppTab.REVENUS     -> PlaceholderScreen(icon = Icons.Filled.AttachMoney, label = "Revenus")
                AppTab.HISTORIQUE  -> PlaceholderScreen(icon = Icons.Filled.History,     label = "Historique")
                AppTab.PARAMETRES  -> PlaceholderScreen(icon = Icons.Filled.Settings,    label = "Paramètres")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Bottom nav bar
// ═══════════════════════════════════════════════════════════════════════
private data class NavItem(val tab: AppTab, val icon: ImageVector, val label: String)

private val navItems = listOf(
    NavItem(AppTab.DASHBOARD,  Icons.Filled.Home,         "Accueil"),
    NavItem(AppTab.REVENUS,    Icons.Filled.AttachMoney,  "Revenus"),
    NavItem(AppTab.HISTORIQUE, Icons.Filled.History,      "Historique"),
    NavItem(AppTab.PARAMETRES, Icons.Filled.Settings,     "Paramètres"),
)

@Composable
private fun AppBottomNav(current: AppTab, onSelect: (AppTab) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))

        Row(modifier = Modifier.fillMaxWidth()) {
            navItems.forEach { item ->
                val active = current == item.tab
                val tint by animateColorAsState(
                    targetValue = if (active) Accent else IconOff,
                    label = "nav-tint-${item.tab}",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(item.tab) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(20.dp))
                    Text(item.label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Dashboard screen
// ═══════════════════════════════════════════════════════════════════════
@Composable
fun DashboardScreen(
    clients: List<ConnectedClient>,
    onAddTime: (Int) -> Unit,
    onCutTime: (Int) -> Unit,
    onAddAll: () -> Unit,
) {
    val active   = clients.filter { it.minutes != null }
    val inactive = clients.filter { it.minutes == null }
    var passwordVisible by remember { mutableStateOf(false) }
    val password = "Cyb3r\$ecure!"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // ── Header ────────────────────────────────────────────────────
        SurfaceCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(40.dp)
                        .background(AccentBg, RoundedCornerShape(12.dp))
                        .border(1.dp, AccentBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Router, null, tint = Accent, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("MikroTik", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("RouterOS 7.14.3 · hEX S", color = Muted, fontSize = 12.sp, maxLines = 1)
                }
                StatusBadge(online = true)
            }
        }

        // ── Wi-Fi ─────────────────────────────────────────────────────
        SurfaceCard(paddingVertical = 2.dp) {
            Column {
                WifiRow(band = "2.4 GHz", ssid = "CyberNet_Home")
                CardDivider()
                WifiRow(band = "5 GHz", ssid = "CyberNet_5G")
                CardDivider()
                PasswordRow(password = password, visible = passwordVisible, onToggle = { passwordVisible = !passwordVisible })
            }
        }

        // ── Active clients ────────────────────────────────────────────
        SurfaceCard(paddingVertical = 0.dp) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Clients connectés", color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Box(
                        modifier = Modifier
                            .background(AccentBg, RoundedCornerShape(50.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "${active.size} actif${if (active.size != 1) "s" else ""}",
                            color = Accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                CardDivider()
                if (active.isEmpty()) {
                    Text(
                        "Aucun client actif",
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally),
                    )
                } else {
                    active.forEachIndexed { i, client ->
                        ClientRow(client, onAdd = { onAddTime(client.id) }, onCut = { onCutTime(client.id) })
                        if (i < active.lastIndex) CardDivider()
                    }
                }
            }
        }

        // ── Add time CTA ──────────────────────────────────────────────
        if (inactive.isNotEmpty()) {
            AddTimeCta(count = inactive.size, onClick = onAddAll)
        }

        Text(
            "Dernière sync · il y a 12 s",
            color = IconOff,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

// ─── Add time CTA ────────────────────────────────────────────────────
@Composable
private fun AddTimeCta(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentBg, RoundedCornerShape(16.dp))
            .border(1.dp, AccentBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Accent, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PersonAdd, null, tint = White, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Ajouter du temps", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "$count appareil${if (count > 1) "s" else ""} sans session active",
                color = Muted,
                fontSize = 12.sp,
            )
        }
        Icon(Icons.Filled.Add, null, tint = Accent, modifier = Modifier.size(18.dp))
    }
}

// ─── Status badge ────────────────────────────────────────────────────
@Composable
private fun StatusBadge(online: Boolean) {
    val color  = if (online) Ok     else Danger
    val bg     = if (online) OkBg   else DangerBg
    val border = if (online) OkBorder else DangerBorder
    val label  = if (online) "En ligne" else "Hors ligne"

    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50.dp))
            .border(1.dp, border, RoundedCornerShape(50.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Wi-Fi row ───────────────────────────────────────────────────────
@Composable
private fun WifiRow(band: String, ssid: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Wifi, null, tint = Accent, modifier = Modifier.size(14.dp))
        Text(band, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(44.dp))
        Text(ssid, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        CopyIconButton(value = ssid)
    }
}

// ─── Password row ────────────────────────────────────────────────────
@Composable
private fun PasswordRow(password: String, visible: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Lock, null, tint = Muted, modifier = Modifier.size(13.dp))
        Text("Mot de passe", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(80.dp))
        Text(
            text = if (visible) password else "• • • • • • • •",
            color = if (visible) White else Muted,
            fontSize = if (visible) 14.sp else 13.sp,
            letterSpacing = if (visible) 0.sp else 2.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(15.dp),
            )
        }
        if (visible) CopyIconButton(value = password)
    }
}

// ─── Client row ──────────────────────────────────────────────────────
@Composable
private fun ClientRow(
    client: ConnectedClient,
    onAdd: () -> Unit,
    onCut: () -> Unit,
) {
    val tc = timeColor(client.minutes)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(client.name, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(client.ip, color = Muted, fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Filled.AccessTime, null, tint = tc, modifier = Modifier.size(11.dp))
            Text(formatTime(client.minutes!!), color = tc, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        // + button
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(AccentBg, RoundedCornerShape(8.dp))
                .border(1.dp, AccentBorder, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Ajouter du temps", tint = Accent, modifier = Modifier.size(15.dp))
        }
        // cut button
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(DangerBg, RoundedCornerShape(8.dp))
                .border(1.dp, DangerBorder, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onCut),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Couper la connexion", tint = Danger, modifier = Modifier.size(13.dp))
        }
    }
}

// ─── Placeholder screen ──────────────────────────────────────────────
@Composable
private fun PlaceholderScreen(icon: ImageVector, label: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(56.dp)
                .background(Surface, RoundedCornerShape(16.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Muted, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(label, color = Muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Bientôt disponible", color = IconOff, fontSize = 13.sp)
    }
}

// ─── Shared primitives ───────────────────────────────────────────────
@Composable
private fun SurfaceCard(
    paddingVertical: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(16.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = paddingVertical),
        content = content,
    )
}

@Composable
private fun CardDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
}

@Composable
private fun CopyIconButton(value: String) {
    var copied by remember { mutableStateOf(false) }
    val tint by animateColorAsState(targetValue = if (copied) Ok else Muted, label = "copy")
    IconButton(
        onClick = { copied = true },
        modifier = Modifier.size(24.dp),
    ) {
        Icon(
            imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
            contentDescription = if (copied) "Copié" else "Copier",
            tint = tint,
            modifier = Modifier.size(13.dp),
        )
    }
}
