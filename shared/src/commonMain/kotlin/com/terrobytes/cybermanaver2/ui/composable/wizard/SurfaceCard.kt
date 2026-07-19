package com.terrobytes.cybermanaver2.ui.composable.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.terrobytes.cybermanaver2.ui.composable.colors.BorderColor
import com.terrobytes.cybermanaver2.ui.composable.colors.Surface

@Composable
fun SurfaceCard(
    paddingVertical: Dp = 16.dp,
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
fun CardDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
}