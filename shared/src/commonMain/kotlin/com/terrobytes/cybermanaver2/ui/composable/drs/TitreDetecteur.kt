package com.terrobytes.cybermanaver2.ui.composable.drs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun TitreDetecteur(modifier: Modifier = Modifier) {
    // 1. Conteneur principal Noir/Bleu très foncé
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B1319), shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // 2. Icône Bouclier avec son halo lumineux bleu en arrière-plan
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            // Effet de lueur (Glow/Blur)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .blur(8.dp)
                    .background(Color(0xFF1A8CFF).copy(alpha = 0.6f), shape = RoundedCornerShape(14.dp))
            )
            // Le carré bleu principal
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A8CFF), shape = RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = "Sécurité",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 3. Bloc Textes (Titre + Sous-titre)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "CyberManager",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Détection des routeurs MikroTik",
                color = Color(0xFF00B4D8), // Cyan/Bleu clair
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 4. Badge "Réseau local"
        Row(
            modifier = Modifier
                .background(Color(0xFF00C873).copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Point indicateur vert
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF1A8CFF), shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            // Texte du badge
            Text(
                text = "Réseau local",
                color = Color(0xB71A8CFF),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
