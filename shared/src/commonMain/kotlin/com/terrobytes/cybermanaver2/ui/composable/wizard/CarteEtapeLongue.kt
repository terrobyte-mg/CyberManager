package com.terrobytes.cybermanaver2.ui.composable.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terrobytes.cybermanaver2.ui.composable.colors.*

sealed interface EtapeStatut {
    data object EnAttente : EtapeStatut
    data object EnCours : EtapeStatut
    data object Terminee : EtapeStatut
    data class Echouee(val message: String) : EtapeStatut
}

@Composable
fun CarteEtapeLongue(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    statut: EtapeStatut,
    logs: List<String>,
    onDemarrer: () -> Unit,
    onReessayer: () -> Unit = onDemarrer,
) {
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.background(AccentBg, RoundedCornerShape(12.dp)).padding(10.dp),
            ) {
                Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when (statut) {
                        EtapeStatut.EnAttente -> "En attente"
                        EtapeStatut.EnCours -> "En cours…"
                        EtapeStatut.Terminee -> "Terminé"
                        is EtapeStatut.Echouee -> "Échec"
                    },
                    color = when (statut) {
                        EtapeStatut.Terminee -> Ok
                        is EtapeStatut.Echouee -> {
                            Danger
                        }
                        else -> White
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            when (statut) {
                EtapeStatut.EnCours -> CircularProgressIndicator(color = Accent, modifier = Modifier.padding(2.dp))
                EtapeStatut.Terminee -> Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Ok)
                is EtapeStatut.Echouee -> Icon(Icons.Filled.Error, contentDescription = null, tint = Danger)
                EtapeStatut.EnAttente -> {}
            }
        }

        if (logs.isNotEmpty()) {
            CardDivider()
            Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                logs.forEach { line ->
                    Text(line, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }

        if (statut is EtapeStatut.Echouee) {
            Text(statut.message, color = Danger, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}