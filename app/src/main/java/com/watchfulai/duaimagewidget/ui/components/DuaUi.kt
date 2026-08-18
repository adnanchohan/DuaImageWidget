package com.watchfulai.duaimagewidget.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.watchfulai.duaimagewidget.ui.theme.Gold300
import com.watchfulai.duaimagewidget.ui.theme.Gold500

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier = modifier.size(size)) {
        drawRoundRect(
            color = primary,
            cornerRadius = CornerRadius(this.size.width * 0.3f),
        )
        val inset = this.size.width * 0.2f
        val cardTop = this.size.height * 0.28f
        val cardBottom = this.size.height * 0.75f
        drawRoundRect(
            color = onPrimary,
            topLeft = Offset(inset, cardTop),
            size = Size(this.size.width - inset * 2, cardBottom - cardTop),
            cornerRadius = CornerRadius(this.size.width * 0.08f),
        )
        drawLine(
            color = Gold300,
            start = Offset(this.size.width * 0.32f, this.size.height * 0.43f),
            end = Offset(this.size.width * 0.68f, this.size.height * 0.43f),
            strokeWidth = this.size.width * 0.055f,
        )
        drawLine(
            color = Gold500,
            start = Offset(this.size.width * 0.38f, this.size.height * 0.57f),
            end = Offset(this.size.width * 0.62f, this.size.height * 0.57f),
            strokeWidth = this.size.width * 0.045f,
        )
        val arch = Path().apply {
            moveTo(this@Canvas.size.width * 0.4f, cardTop)
            quadraticTo(
                this@Canvas.size.width * 0.5f,
                this@Canvas.size.height * 0.12f,
                this@Canvas.size.width * 0.6f,
                cardTop,
            )
        }
        drawPath(arch, Gold300, style = Stroke(width = this.size.width * 0.045f))
    }
}

@Composable
fun DuaIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun DuaSurfaceCard(
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        content = content,
    )
}

@Composable
fun DuaPill(
    text: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun DuaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        leading?.invoke(this)
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
