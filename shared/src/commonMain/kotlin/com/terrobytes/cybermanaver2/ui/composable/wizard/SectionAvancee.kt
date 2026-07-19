package com.terrobytes.cybermanaver2.ui.composable.wizard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terrobytes.cybermanaver2.ui.composable.colors.Muted

/**
 * "Paramètres avancés" repliés par défaut : réseau/CIDR, IP routeur, pool DHCP.
 * Tout ce qui ne concerne pas directement le patron du cyber-café reste caché ici.
 */
@Composable
fun SectionAvancee(
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    SurfaceCard(paddingVertical = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Tune, contentDescription = null, tint = Muted, modifier = Modifier)
                Text("Paramètres avancés", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Muted,
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier.animateContentSize().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CardDivider()
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    content()
                }
            }
        }
    }
}