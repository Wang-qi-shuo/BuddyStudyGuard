package com.buddy.studyguard.common.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 像素科技风暗色主题。
 *
 * 仅提供 darkColorScheme，无 light mode 切换。
 * colorScheme 映射：
 *   primary       → NeonCyan
 *   secondary     → NeonMagenta
 *   tertiary      → NeonGreen
 *   background    → BgPrimary
 *   surface       → BgCard
 *   onPrimary     → BgDeepest
 *   onBackground  → TextPrimary
 *   onSurface     → TextPrimary
 */

private val PixelDarkColors = darkColorScheme(
    primary = NeonCyan,
    onPrimary = BgDeepest,
    primaryContainer = NeonCyan.copy(alpha = 0.15f),
    onPrimaryContainer = NeonCyan,
    secondary = NeonMagenta,
    onSecondary = BgDeepest,
    secondaryContainer = NeonMagenta.copy(alpha = 0.15f),
    onSecondaryContainer = NeonMagenta,
    tertiary = NeonGreen,
    onTertiary = BgDeepest,
    tertiaryContainer = NeonGreen.copy(alpha = 0.15f),
    onTertiaryContainer = NeonGreen,
    error = NeonMagenta,
    onError = BgDeepest,
    errorContainer = NeonMagenta.copy(alpha = 0.15f),
    onErrorContainer = NeonMagenta,
    background = BgPrimary,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TextDisabled,
    outlineVariant = BgSurfaceVariant,
    inverseSurface = TextPrimary,
    inverseOnSurface = BgPrimary,
    inversePrimary = NeonCyan,
    scrim = BgDeepest.copy(alpha = 0.8f),
)

/**
 * 像素科技风主题入口。
 *
 * 始终使用暗色配色，不受系统深/浅色模式影响。
 * 自动将状态栏设为透明并同步背景色。
 */
@Composable
fun BuddyStudyGuardTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PixelDarkColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PixelTypography,
        content = content
    )
}
