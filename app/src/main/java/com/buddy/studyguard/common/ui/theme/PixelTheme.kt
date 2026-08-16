package com.buddy.studyguard.common.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ==========================================================================
   像素风格 Modifier 扩展
   ========================================================================== */

/**
 * 绘制霓虹发光边框。
 * 在组件外缘绘制两层描边：内层为纯色 [color]，外层为同色半透明宽描边模拟辉光。
 */
fun Modifier.neonBorder(color: Color, width: Dp = 1.dp, glowWidth: Dp = 3.dp): Modifier =
    this.drawBehind {
        val innerW = width.toPx()
        val outerW = glowWidth.toPx()
        // 外层辉光
        drawRoundRect(
            color = color.copy(alpha = 0.30f),
            topLeft = Offset(-outerW / 2f, -outerW / 2f),
            size = size.copy(
                width = size.width + outerW,
                height = size.height + outerW
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
            style = Stroke(width = outerW)
        )
        // 内层实线
        drawRoundRect(
            color = color,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
            style = Stroke(width = innerW)
        )
    }

/**
 * 霓虹外发光效果。
 * 在组件外缘绘制径向渐变辉光，模拟 CRT / 霓虹灯管的散射光。
 */
fun Modifier.neonGlow(color: Color, radius: Dp = 16.dp): Modifier =
    this.drawBehind {
        val glowPx = radius.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.35f),
                    color.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = center,
                radius = maxOf(size.width, size.height) / 2f + glowPx
            ),
            radius = maxOf(size.width, size.height) / 2f + glowPx,
            center = center
        )
    }

/**
 * CRT 扫描线效果。
 * 绘制等间隔水平半透明横线，模拟老式 CRT 显示器的扫描线纹理。
 */
fun Modifier.crtScanlines(lineColor: Color = Color.Black.copy(alpha = 0.10f), spacing: Dp = 3.dp): Modifier =
    this.drawBehind {
        val step = spacing.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += step
        }
    }

/* ==========================================================================
   像素风格按钮样式常量
   ========================================================================== */

/** 像素霓虹主按钮颜色配置 */
object PixelButtonStyles {
    /** 霓虹青主色调按钮 */
    val Cyan: androidx.compose.material3.ButtonColors
        @Composable get() = ButtonDefaults.outlinedButtonColors(
            containerColor = NeonCyan.copy(alpha = 0.10f),
            contentColor = NeonCyan,
        )

    /** 霓虹品红次色调按钮 */
    val Magenta: androidx.compose.material3.ButtonColors
        @Composable get() = ButtonDefaults.outlinedButtonColors(
            containerColor = NeonMagenta.copy(alpha = 0.10f),
            contentColor = NeonMagenta,
        )

    /** 霓虹绿确认/通过按钮 */
    val Green: androidx.compose.material3.ButtonColors
        @Composable get() = ButtonDefaults.outlinedButtonColors(
            containerColor = NeonGreen.copy(alpha = 0.10f),
            contentColor = NeonGreen,
        )

    /** 霓虹琥珀警告按钮 */
    val Amber: androidx.compose.material3.ButtonColors
        @Composable get() = ButtonDefaults.outlinedButtonColors(
            containerColor = NeonAmber.copy(alpha = 0.10f),
            contentColor = NeonAmber,
        )

    /** 像素风按钮边框 */
    val Border = BorderStroke(1.dp, NeonCyan)

    /** 像素风按钮形状 */
    val Shape = RoundedCornerShape(8.dp)
}

/**
 * 预设像素风 OutlinedButton（霓虹青主色）。
 * 方便在各 Screen 中直接复用，无需重复设置颜色/形状/字体。
 */
@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String = "",
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PixelButtonStyles.Shape,
        border = PixelButtonStyles.Border,
        colors = PixelButtonStyles.Cyan,
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
