package com.v2rayops.client.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2rayops.client.designsystem.theme.CyberAccent
import com.v2rayops.client.designsystem.theme.CyberBorder
import com.v2rayops.client.designsystem.theme.CyberCard
import com.v2rayops.client.designsystem.theme.CyberPrimary
import com.v2rayops.client.designsystem.theme.CyberShapes
import com.v2rayops.client.designsystem.theme.CyberTextMuted
import com.v2rayops.client.designsystem.theme.CyberTextPrimary

enum class CyberButtonVariant {
    PRIMARY,
    SECONDARY,
    OUTLINED
}

@Composable
fun CyberButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    variant: CyberButtonVariant = CyberButtonVariant.PRIMARY,
    content: @Composable RowScope.() -> Unit
) {
    val shape = CyberShapes.medium
    val buttonModifier = modifier
        .fillMaxWidth()
        .height(52.dp)

    if (variant == CyberButtonVariant.OUTLINED) {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !isLoading,
            shape = shape,
            border = BorderStroke(1.dp, if (enabled) CyberBorder else CyberBorder.copy(alpha = 0.4f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = CyberCard.copy(alpha = 0.5f),
                contentColor = CyberTextPrimary,
                disabledContentColor = CyberTextMuted
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = CyberAccent
                )
            } else {
                content()
            }
        }
    } else {
        val containerColor = when (variant) {
            CyberButtonVariant.PRIMARY -> CyberPrimary
            CyberButtonVariant.SECONDARY -> CyberAccent
            else -> CyberPrimary
        }

        Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !isLoading,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = Color.White,
                disabledContainerColor = containerColor.copy(alpha = 0.35f),
                disabledContentColor = CyberTextMuted
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                content()
            }
        }
    }
}
