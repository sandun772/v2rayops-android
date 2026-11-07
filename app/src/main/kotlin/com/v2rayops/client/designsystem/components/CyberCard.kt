package com.v2rayops.client.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberShapes

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CyberCard,
    borderColor: Color = CyberBorder,
    borderWidth: Dp = 1.dp,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = CyberShapes.medium,
            color = backgroundColor,
            border = BorderStroke(borderWidth, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(padding),
                content = content
            )
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = CyberShapes.medium,
            color = backgroundColor,
            border = BorderStroke(borderWidth, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(padding),
                content = content
            )
        }
    }
}
