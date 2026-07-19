package com.terrobytes.cybermanaver2.ui.composable.wizard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terrobytes.cybermanaver2.ui.composable.colors.Muted
import com.terrobytes.cybermanaver2.ui.composable.colors.White

@Preview
@Composable
fun TitreWizard(
    modifier: Modifier = Modifier,
    title: String = "Test",
    subtitle: String? = "Subtitle",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            color = White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier
            .height(12.dp)
            .fillMaxWidth()
        )

        if (subtitle != null) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = subtitle,
                color = Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }

    }
}