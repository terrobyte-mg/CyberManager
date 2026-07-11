package com.terrobytes.cybermanaver2.ui.composable.drs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terrobytes.cybermanaver2.models.RouterDiscoveryMethod
import com.terrobytes.cybermanaver2.models.Routeur

@Preview
@Composable
fun CarteRouteur(
    modifier: Modifier = Modifier,
    routeur: Routeur = Routeur(ipAddress = "127.0.0.1", name = "Cyber Manaver", source = RouterDiscoveryMethod.SCAN_WIFI, networkTarget = null),
    isSelected: Boolean = false,
    onSelectClick: () -> Unit = {}
) {
    val borderColor = if (isSelected) Color(0xFF1A8CFF) else Color.Transparent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B1319), shape = RoundedCornerShape(24.dp))
            .border(2.dp, borderColor, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable { onSelectClick() }
            .padding(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(Color(0xFF141C24), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.Router, contentDescription = null, tint = Color(0xFF5F6368), modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = routeur.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = routeur.ipAddress, color = Color(0xFF00B4D8), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            // Bouton Radio d'état de sélection
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(if (isSelected) Color(0xFF1A8CFF) else Color.Transparent, CircleShape)
                    .border(2.dp, if (isSelected) Color(0xFF1A8CFF) else Color(0xFF5F6368), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(modifier = Modifier.size(10.dp).background(Color.White, CircleShape))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = Color(0xFF5F6368), modifier = Modifier.size(16.dp))
            Text(text = routeur.source.toString(), color = Color(0xFF8E9297), fontSize = 14.sp)
        }
    }
}
