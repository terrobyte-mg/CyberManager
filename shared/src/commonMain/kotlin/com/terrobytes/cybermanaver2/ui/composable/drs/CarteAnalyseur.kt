package com.terrobytes.cybermanaver2.ui.composable.drs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun CarteAnalyseur(
    modifier: Modifier = Modifier,
    isScanning: Boolean = false,
    progress: Int = 0,
    total: Int = 0,
    baseIp: String = ""
) {
    val progressRatio = if (total > 0) progress.toFloat() / total.toFloat() else 0f
    val percentage = (progressRatio * 100).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B1319), shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF1A8CFF).copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.Wifi, contentDescription = null, tint = Color(0xFF1A8CFF), modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Analyse du réseau $baseIp.X", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = if (isScanning) "Scan en cours..." else "Scan terminé", color = Color(0xFF8E9297), fontSize = 13.sp)
            }

            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF1A8CFF), strokeWidth = 2.dp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progressRatio },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Color(0xFF1A8CFF),
            trackColor = Color(0xFF141C24),
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Hôtes analysés: $progress/$total", color = Color(0xFF8E9297), fontSize = 13.sp)
            Text(text = "$percentage%", color = Color(0xFF00B4D8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
