package com.buddy.studyguard.study.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.buddy.studyguard.common.ui.theme.NeonCyan

/**
 * 像素科技风卡片组件。
 *
 * - 固定 8dp 像素化圆角
 * - 1dp 霓虹边框（默认 NeonCyan，可通过 [borderColor] 自定义）
 * - 可选发光效果（[glowColor] 非透明时，在卡片外绘制径向渐变辉光）
 * - 可选像素阴影（[pixelShadow] 为 true 时，右下角添加实色偏置阴影）
 */
@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    borderColor: Color = NeonCyan,
    glowColor: Color = Color.Transparent,
    pixelShadow: Boolean = false,
    glowRadius: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val glowModifier = if (glowColor != Color.Transparent) {
        Modifier.drawBehind {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f + glowRadius.toPx()
            drawCircle(
                color = glowColor.copy(alpha = 0.25f),
                radius = radius,
                center = center
            )
        }
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.then(glowModifier),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (pixelShadow) 4.dp else 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
