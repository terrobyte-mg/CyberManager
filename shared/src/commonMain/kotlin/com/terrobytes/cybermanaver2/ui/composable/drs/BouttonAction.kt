package com.terrobytes.cybermanaver2.ui.composable.drs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BouttonAction(
    modifier            : Modifier = Modifier,
    onConnectClick      : () -> Unit = {},
    onRefreshClick      : () -> Unit = {},
    onManualConnectClick: () -> Unit = {},
    isConnecting        : Boolean = false,
    hasSelection        : Boolean = false,
    refreshEnabled      : Boolean,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Bouton Connexion principal ─────────────────────────────────
        val connectEnabled = hasSelection && !isConnecting
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .alpha(if (connectEnabled) 1f else 0.5f)
                .background(Color(0xFF1A8CFF), shape = RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .then(if (connectEnabled) Modifier.clickable { onConnectClick() } else Modifier),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connexion…", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(imageVector = Icons.Filled.Power, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasSelection) "Connexion" else "Sélectionnez un routeur",
                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── Boutons secondaires ────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .weight(1f).height(64.dp)
                    .background(Color(0xFF141C24), shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        refreshEnabled
                        onRefreshClick()
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Actualiser", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Row(
                modifier = Modifier
                    .weight(1f).height(64.dp)
                    .background(Color(0xFF141C24), shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onManualConnectClick() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Filled.Hub, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connexion\nmanuelle", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start, lineHeight = 18.sp)
            }
        }
    }
}