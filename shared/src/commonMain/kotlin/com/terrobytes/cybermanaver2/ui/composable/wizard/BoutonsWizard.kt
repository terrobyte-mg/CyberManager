package com.terrobytes.cybermanaver2.ui.composable.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terrobytes.cybermanaver2.ui.composable.colors.Accent
import com.terrobytes.cybermanaver2.ui.composable.colors.AccentBg
import com.terrobytes.cybermanaver2.ui.composable.colors.BorderColor
import com.terrobytes.cybermanaver2.ui.composable.colors.Muted
import com.terrobytes.cybermanaver2.ui.composable.colors.Surface
import com.terrobytes.cybermanaver2.ui.composable.colors.White

/**
 * Barre d'actions commune à toutes les étapes du wizard.
 * - [onBack] null => bouton retour caché (première étape)
 * - [onSkip] null => pas de bouton "passer" (étapes obligatoires : backup/injection/verify)
 * - [continueEnabled] désactive visuellement "Continuer" sans le cacher (ex: validation en cours)
 */
@Composable
fun BarreActionsWizard(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)?,
    onContinue: () -> Unit,
    continueLabel: String = "Continuer",
    continueEnabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface)
            .navigationBarsPadding(),
    ) {
        CardDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                GhostButton(label = "Retour", onClick = onBack, modifier = Modifier.weight(1f))
            }
            AccentButton(
                label = continueLabel,
                onClick = onContinue,
                enabled = continueEnabled,
                modifier = Modifier.weight(if (onBack == null) 1f else 1.4f),
            )
        }
    }
}

@Composable
private fun GhostButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BorderColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(label, color = Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun AccentButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Accent else AccentBg, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.6f),
    ) {
        Text(
            label,
            color = if (enabled) White else Accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}