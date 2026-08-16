package com.buddy.studyguard.common.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// â”€â”€â”€ PixelDivider â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * åƒç´ é£æ ¼æ°´å¹³åˆ†éš”çº¿ï¼Œç”±é‡å¤çš„ â–€ å­—ç¬¦æ„æˆã€‚
 *
 * @param color åˆ†éš”çº¿é¢œè‰²ï¼Œé»˜è®¤ä½¿ç”¨ NeonCyanã€‚
 * @param modifier å¯é€‰çš„ Modifierã€‚
 */
@Composable
fun PixelDivider(
    color: Color = Color(0xFF00FFFF),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "â–€".repeat(48),
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = color.copy(alpha = 0.4f),
            maxLines = 1,
        )
    }
}

// â”€â”€â”€ PixelCorner â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * åƒç´ é£æ ¼å››è§’è£…é¥°ï¼šå››ä¸ª L å½¢éœ“è™¹è‰²å—ï¼ˆå·¦ä¸Šã€å³ä¸Šã€å·¦ä¸‹ã€å³ä¸‹ï¼‰ã€‚
 *
 * @param color è£…é¥°é¢œè‰²ã€‚
 * @param armLength æ¯ä¸ª L å½¢è‡‚çš„é•¿åº¦ã€‚
 * @param thickness çº¿æ¡ç²—ç»†ã€‚
 * @param modifier å¯é€‰çš„ Modifierã€‚
 */
@Composable
fun PixelCorner(
    color: Color = Color(0xFFFF00FF),
    armLength: Dp = 16.dp,
    thickness: Dp = 2.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(Modifier.matchParentSize()) {
            val t = thickness.toPx()
            val len = armLength.toPx()
            // å·¦ä¸Š
            drawLine(color, Offset(0f, 0f), Offset(len, 0f), t)
            drawLine(color, Offset(0f, 0f), Offset(0f, len), t)
            // å³ä¸‹
            val w = size.width
            val h = size.height
            drawLine(color, Offset(w - len, h), Offset(w, h), t)
            drawLine(color, Offset(w, h - len), Offset(w, h), t)
            // å³ä¸Š
            drawLine(color, Offset(w - len, 0f), Offset(w, 0f), t)
            drawLine(color, Offset(w, 0f), Offset(w, len), t)
            // å·¦ä¸‹
            drawLine(color, Offset(0f, h - len), Offset(0f, h), t)
            drawLine(color, Offset(0f, h), Offset(len, h), t)
        }
    }
}

// â”€â”€â”€ PixelDots â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * åƒç´ æ•£ç‚¹èƒŒæ™¯ï¼šéšæœºåˆ†å¸ƒçš„å°è‰²å—ã€‚
 *
 * @param color è‰²å—é¢œè‰²ã€‚
 * @param dotCount æ•£ç‚¹æ•°é‡ã€‚
 * @param dotSize æ¯ä¸ªè‰²å—çš„å¤§å°ã€‚
 * @param modifier å¯é€‰çš„ Modifierã€‚
 */
@Composable
fun PixelDots(
    color: Color = Color(0xFFFF00FF),
    dotCount: Int = 24,
    dotSize: Dp = 3.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val d = dotSize.toPx()
        val w = size.width
        val h = size.height
        val seed = 42L
        repeat(dotCount) { i ->
            val x = ((seed * (i + 1) * 7919) % 10007).toFloat() / 10007f * (w - d)
            val y = ((seed * (i + 1) * 6271) % 10007).toFloat() / 10007f * (h - d)
            drawRect(
                color = color.copy(alpha = 0.15f),
                topLeft = Offset(x, y),
                size = Size(d, d)
            )
        }
    }
}

// â”€â”€â”€ ScanlineOverlay â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * CRT æ‰«æçº¿è¦†ç›–å±‚ï¼šåŠé€æ˜æ°´å¹³æ¡çº¹ï¼Œå¯å¸¦å‘¼å¸åŠ¨ç”»ã€‚
 *
 * @param color æ‰«æçº¿é¢œè‰²ã€‚
 * @param alpha é™æ€é€æ˜åº¦ï¼ˆanimated=false æ—¶ç”Ÿæ•ˆï¼‰ã€‚
 * @param animatedAlphaMin åŠ¨ç”»å‘¼å¸æœ€ä½é€æ˜åº¦ã€‚
 * @param animatedAlphaMax åŠ¨ç”»å‘¼å¸æœ€é«˜é€æ˜åº¦ã€‚
 * @param lineHeight æ¯æ ¹æ‰«æçº¿é«˜åº¦ã€‚
 * @param spacing æ‰«æçº¿é—´è·ã€‚
 * @param animated æ˜¯å¦å¯ç”¨å‘¼å¸åŠ¨ç”»ã€‚
 * @param modifier å¯é€‰çš„ Modifierã€‚
 */
@Composable
fun ScanlineOverlay(
    color: Color = Color(0xFFFFFFFF),
    alpha: Float = 0.05f,
    animatedAlphaMin: Float = 0.03f,
    animatedAlphaMax: Float = 0.08f,
    lineHeight: Dp = 1.dp,
    spacing: Dp = 3.dp,
    animated: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animAlpha by if (animated) {
        val transition = rememberInfiniteTransition(label = "scanline")
        transition.animateFloat(
            initialValue = animatedAlphaMin,
            targetValue = animatedAlphaMax,
            animationSpec = infiniteRepeatable(
                animation = tween(3000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scanlineAlpha"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(alpha) }
    }

    val baseAlpha = animAlpha

    Canvas(modifier = modifier) {
        val lh = lineHeight.toPx()
        val sp = spacing.toPx()
        val pitch = lh + sp
        var y = 0f
        while (y < size.height) {
            drawRect(
                color = color.copy(alpha = baseAlpha),
                topLeft = Offset(0f, y),
                size = Size(size.width, lh)
            )
            y += pitch
        }
    }
}

// â”€â”€â”€ PixelStarBurst â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * å››è§’æ˜Ÿå½¢éœ“è™¹é—ªå…‰è£…é¥°ã€‚
 */
@Composable
fun PixelStarBurst(
    color: Color = Color(0xFF00FFFF),
    size: Dp = 40.dp,
    rayCount: Int = 8,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = size.toPx()
            val h = size.toPx()
            val cx = w / 2
            val cy = h / 2
            val inner = w * 0.2f
            drawRect(
                color = color,
                topLeft = Offset(cx - inner / 2, cy - inner / 2),
                size = Size(inner, inner)
            )
            val outer = w / 2
            val half = inner / 2
            drawLine(color, Offset(cx, cy - half), Offset(cx, cy - outer), 3f)
            drawLine(color, Offset(cx, cy + half), Offset(cx, cy + outer), 3f)
            drawLine(color, Offset(cx - half, cy), Offset(cx - outer, cy), 3f)
            drawLine(color, Offset(cx + half, cy), Offset(cx + outer, cy), 3f)
            val diag = outer * 0.7f
            drawLine(color, Offset(cx - half, cy - half), Offset(cx - diag, cy - diag), 2f)
            drawLine(color, Offset(cx + half, cy - half), Offset(cx + diag, cy - diag), 2f)
            drawLine(color, Offset(cx - half, cy + half), Offset(cx - diag, cy + diag), 2f)
            drawLine(color, Offset(cx + half, cy + half), Offset(cx + diag, cy + diag), 2f)
        }
    }
}

// â”€â”€â”€ NeonGlowBorder â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * å¸¦å‘¼å¸åŠ¨ç”»çš„éœ“è™¹å‘å…‰è¾¹æ¡†å®¹å™¨ã€‚
 * å†…å±‚ç»˜åˆ¶å®çº¿è¾¹æ¡†ï¼Œå¤–å±‚æ˜¯å‘¨æœŸæ€§è„‰åŠ¨çš„è¾‰å…‰ã€‚
 *
 * @param borderColor è¾¹æ¡†é¢œè‰²ã€‚
 * @param glowColor è¾‰å…‰é¢œè‰²ï¼ˆé»˜è®¤åŒ borderColorï¼‰ã€‚
 * @param modifier å¯é€‰çš„ Modifierã€‚
 * @param breathPeriodMs å‘¼å¸å‘¨æœŸï¼ˆæ¯«ç§’ï¼‰ï¼Œé»˜è®¤ 2000ã€‚
 * @param borderWidth å†…å±‚è¾¹æ¡†å®½åº¦ã€‚
 * @param glowWidth è¾‰å…‰å®½åº¦ã€‚
 * @param cornerRadius åœ†è§’ã€‚
 * @param content åŒ…è£¹çš„å†…å®¹ã€‚
 */
@Composable
fun NeonGlowBorder(
    borderColor: Color,
    glowColor: Color = borderColor,
    modifier: Modifier = Modifier,
    breathPeriodMs: Int = 2000,
    borderWidth: Dp = 2.dp,
    glowWidth: Dp = 6.dp,
    cornerRadius: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "neonGlowBorder")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(breathPeriodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier.drawBehind {
            val bW = borderWidth.toPx()
            val gW = glowWidth.toPx()
            val cr = cornerRadius.toPx()

            // å¤–å±‚è¾‰å…‰ - è„‰åŠ¨
            drawRoundRect(
                color = glowColor.copy(alpha = glowAlpha),
                topLeft = Offset(-gW / 2f, -gW / 2f),
                size = Size(size.width + gW, size.height + gW),
                cornerRadius = CornerRadius(cr),
                style = Stroke(width = gW)
            )
            // å†…å±‚å®çº¿
            drawRoundRect(
                color = borderColor,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = CornerRadius(cr),
                style = Stroke(width = bW)
            )
        }
    ) {
        content()
    }
}

// â”€â”€â”€ PixelProgressBar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * åƒç´ é£æ ¼è¿›åº¦æ¡ï¼šä½¿ç”¨ â–ˆ å­—ç¬¦å—ï¼Œæ”¯æŒéœ“è™¹è‰²æ¸å˜ã€‚
 *
 * @param progress è¿›åº¦å€¼ 0f..1fã€‚
 * @param color è¿›åº¦æ¡åŸºç¡€é¢œè‰²ã€‚
 * @param modifier å¯é€‰çš„ Modifierã€‚
 * @param height è¿›åº¦æ¡é«˜åº¦ã€‚
 * @param totalBlocks æ€»å­—ç¬¦å—æ•°ï¼ˆæ§åˆ¶ç²¾åº¦ï¼‰ã€‚
 */
@Composable
fun PixelProgressBar(
    progress: Float,
    color: Color = Color(0xFF00FFFF),
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    totalBlocks: Int = 32
) {
    val filledCount = (progress.coerceIn(0f, 1f) * totalBlocks).toInt()

    Row(
        modifier = modifier.fillMaxWidth().height(height),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // å·²å¡«å……å— â€” éœ“è™¹æ¸å˜ï¼ˆä»äº®åˆ°ç¨æš—ï¼‰
        val filledChar = "â–ˆ"
        repeat(filledCount) { i ->
            val ratio = i.toFloat() / totalBlocks
            val blockColor = Color(
                red = (color.red * (0.6f + 0.4f * (1f - ratio))).coerceIn(0f, 1f),
                green = (color.green * (0.6f + 0.4f * (1f - ratio))).coerceIn(0f, 1f),
                blue = (color.blue * (0.6f + 0.4f * (1f - ratio))).coerceIn(0f, 1f),
                alpha = 1f
            )
            Text(
                text = filledChar,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = blockColor,
            )
        }
        // æœªå¡«å……å—
        val emptyCount = totalBlocks - filledCount
        if (emptyCount > 0) {
            Text(
                text = "â–‘".repeat(emptyCount),
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = color.copy(alpha = 0.2f),
            )
        }
    }
}

// â”€â”€â”€ PixelBadge â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * åƒç´ å°æ ‡ç­¾ç»„ä»¶ã€‚
 *
 * @param text æ ‡ç­¾æ–‡å­—ã€‚
 * @param color æ ‡ç­¾é¢œè‰²ã€‚
 * @param modifier å¯é€‰çš„ Modifierã€‚
 */
@Composable
fun PixelBadge(
    text: String,
    color: Color = Color(0xFFFF00FF),
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier.drawBehind {
            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(4f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// â”€â”€â”€ SweepLightText â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * æ‰«å…‰åŠ¨ç”»æ–‡å­—ï¼šéœ“è™¹å…‰çº¿ä»å·¦åˆ°å³æ‰«è¿‡æ–‡å­—è¡¨é¢ã€‚
 *
 * @param text æ˜¾ç¤ºçš„æ–‡å­—ã€‚
 * @param baseColor åŸºç¡€é¢œè‰²ã€‚
 * @param sweepColor æ‰«å…‰é¢œè‰²ï¼ˆé«˜äº®ï¼‰ã€‚
 * @param fontSize å­—ä½“å¤§å°ã€‚
 * @param modifier å¯é€‰çš„ Modifierã€‚
 */
@Composable
fun SweepLightText(
    text: String,
    baseColor: Color = Color(0xFF00FFFF),
    sweepColor: Color = Color.White,
    fontSize: Int = 32,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "sweepLight")
    val sweepPos by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepPos"
    )

    val textBrush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            baseColor,
            sweepColor,
            baseColor,
            baseColor
        ),
        start = Offset(sweepPos * 1000f, 0f),
        end = Offset(sweepPos * 1000f + 200f, 0f)
    )

    Text(
        text = text,
        fontSize = fontSize.sp,
        fontFamily = FontFamily.Monospace,
        style = androidx.compose.ui.text.TextStyle(
            brush = textBrush
        ),
        modifier = modifier
    )
}

// â”€â”€â”€ PixelCloudDecoration â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * åº•éƒ¨åƒç´ äº‘æœµè£…é¥°ï¼šç”± â–„â–€â–ˆâ– ç­‰å­—ç¬¦æ‹¼æˆçš„åƒç´ äº‘å›¾æ¡ˆã€‚
 *
 * @param color äº‘æœµé¢œè‰²ã€‚
 * @param modifier å¯é€‰çš„ Modifierã€‚
 */
@Composable
fun PixelCloudDecoration(
    color: Color = Color(0xFF00FFFF),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "  â–„â–„â–„â–„â–„      â–„â–„â–„â–„  ",
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = color.copy(alpha = 0.2f),
        )
        Text(
            text = " â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–„  â–„â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–„ ",
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = color.copy(alpha = 0.3f),
        )
        Text(
            text = "â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆ",
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = color.copy(alpha = 0.4f),
        )
    }
}

// â”€â”€â”€ PixelEmptyState â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * åƒç´ é£æ ¼ç©ºçŠ¶æ€æ’ç”»æç¤ºã€‚
 *
 * @param icon å­—ç¬¦å›¾æ ‡ï¼ˆå¦‚ "[  ]", "{?}", "[!]" ç­‰ï¼‰ã€‚
 * @param title ä¸»æç¤ºæ–‡å­—ã€‚
 * @param subtitle å‰¯æç¤ºæ–‡å­—ã€‚
 * @param color é¢œè‰²ã€‚
 * @param modifier å¯é€‰çš„ Modifierã€‚
 */
@Composable
fun PixelEmptyState(
    icon: String = "[  ]",
    title: String = "æš‚æ— å†…å®¹",
    subtitle: String = "",
    color: Color = Color(0xFF8888AA),
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = icon,
            fontFamily = FontFamily.Monospace,
            fontSize = 48.sp,
            color = color.copy(alpha = 0.3f),
        )
        Text(
            text = title,
            fontFamily = FontFamily.Monospace,
            color = color,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                fontFamily = FontFamily.Monospace,
                color = color.copy(alpha = 0.5f),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T
// ÏñËØÍ¼±ê¿â ¡ª Compose Canvas 16x16 Íø¸ñ»æÖÆ
// ¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T¨T

/** ÔÚ Canvas ÉÏ»æÖÆ 16x16 ÏñËØÍø¸ñ¡£cellSize = canvasSize / 16¡£ */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelGrid(
    cells: List<Pair<Int, Int>>,
    color: Color
) {
    val cellW = size.width / 16f
    val cellH = size.height / 16f
    for ((cx, cy) in cells) {
        drawRect(
            color = color,
            topLeft = Offset(cx * cellW, cy * cellH),
            size = Size(cellW, cellH)
        )
    }
}

// ©¤©¤©¤ PixelShieldSmall ©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤

/** 16¡Á16 ÏñËØ¶ÜÅÆ¡£ */
private val SHIELD_CELLS = listOf(
    4,5, 5,5, 6,5, 7,5, 8,5, 9,5, 10,5,
    3,6, 4,6, 5,6, 6,6, 7,6, 8,6, 9,6, 10,6, 11,6,
    2,7, 3,7, 4,7, 5,7, 6,7, 7,7, 8,7, 9,7, 10,7, 11,7, 12,7,
    1,8, 2,8, 3,8, 4,8, 5,8, 6,8, 7,8, 8,8, 9,8, 10,8, 11,8, 12,8, 13,8,
    0,9, 1,9, 2,9, 3,9, 4,9, 5,9, 6,9, 7,9, 8,9, 9,9, 10,9, 11,9, 12,9, 13,9, 14,9,
    0,10, 1,10, 2,10, 3,10, 4,10, 5,10, 6,10, 7,10, 8,10, 9,10, 10,10, 11,10, 12,10, 13,10, 14,10,
    1,11, 2,11, 3,11, 4,11, 5,11, 6,11, 7,11, 8,11, 9,11, 10,11, 11,11, 12,11, 13,11,
    2,12, 3,12, 4,12, 5,12, 6,12, 7,12, 8,12, 9,12, 10,12, 11,12, 12,12,
    3,13, 4,13, 5,13, 6,13, 7,13, 8,13, 9,13, 10,13, 11,13,
    4,14, 5,14, 6,14, 7,14, 8,14, 9,14, 10,14,
    5,15, 6,15, 7,15, 8,15, 9,15
).chunked(2).map { it[0] to it[1] }

@Composable
fun PixelShieldSmall(
    color: Color = Color(0xFF00FFFF),
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val cells = remember { SHIELD_CELLS }
    Canvas(modifier = modifier.size(size)) {
        drawPixelGrid(cells, color)
    }
}

// ©¤©¤©¤ PixelStar ©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤

/** 16¡Á16 ÏñËØÎå½ÇĞÇ¡£ */
private val STAR_CELLS = listOf(
    7,0, 8,0,
    6,1, 7,1, 8,1, 9,1,
    6,2, 7,2, 8,2, 9,2, 10,2,
    5,3, 6,3, 7,3, 8,3, 9,3, 10,3,
    4,4, 5,4, 6,4, 7,4, 8,4, 9,4, 10,4, 11,4,
    4,5, 5,5, 6,5, 7,5, 8,5, 9,5, 10,5, 11,5,
    3,6, 4,6, 5,6, 6,6, 7,6, 8,6, 9,6, 10,6,
    0,7, 1,7, 2,7, 3,7, 4,7, 5,7, 6,7, 7,7, 8,7, 9,7, 10,7, 11,7, 12,7, 13,7, 14,7,
    1,8, 2,8, 3,8, 4,8, 5,8, 6,8, 7,8, 8,8, 9,8, 10,8, 11,8, 12,8, 13,8,
    2,9, 3,9, 5,9, 6,9, 7,9, 8,9, 9,9, 10,9, 11,9,
    3,10, 4,10, 6,10, 7,10, 8,10, 9,10, 10,10,
    3,11, 4,11, 5,11, 7,11, 8,11, 9,11,
    2,12, 3,12, 4,12, 8,12, 9,12,
    1,13, 2,13, 8,13, 9,13,
    1,14, 2,14, 3,14, 7,14, 8,14,
    0,15, 7,15
).chunked(2).map { it[0] to it[1] }

@Composable
fun PixelStar(
    color: Color = Color(0xFF00FFFF),
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val cells = remember { STAR_CELLS }
    Canvas(modifier = modifier.size(size)) {
        drawPixelGrid(cells, color)
    }
}

// ©¤©¤©¤ PixelCrown ©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤

/** 16¡Á16 ÏñËØ»Ê¹Ú£¨¼Ò³¤±êÊ¶£©¡£ */
private val CROWN_CELLS = listOf(
    2,0, 3,0, 4,0, 5,0, 6,0, 7,0, 8,0, 9,0, 10,0, 11,0, 12,0, 13,0,
    1,1, 2,1, 3,1, 4,1, 5,1, 6,1, 7,1, 8,1, 9,1, 10,1, 11,1, 12,1, 13,1, 14,1,
    2,2, 3,2, 4,2, 5,2, 6,2, 7,2, 8,2, 9,2, 10,2, 11,2, 12,2, 13,2,
    1,3, 2,3, 4,3, 5,3, 6,3, 7,3, 8,3, 9,3, 10,3, 11,3, 13,3, 14,3,
    0,4, 2,4, 3,4, 5,4, 7,4, 8,4, 10,4, 12,4, 13,4, 15,4,
    0,5, 2,5, 6,5, 9,5, 13,5, 15,5,
    0,6, 1,6, 2,6, 5,6, 6,6, 7,6, 8,6, 9,6, 10,6, 13,6, 14,6, 15,6,
    0,7, 1,7, 2,7, 7,7, 8,7, 13,7, 14,7, 15,7,
    0,8, 1,8, 2,8, 3,8, 4,8, 5,8, 6,8, 7,8, 8,8, 9,8, 10,8, 11,8, 12,8, 13,8, 14,8, 15,8,
    1,9, 2,9, 3,9, 4,9, 5,9, 6,9, 7,9, 8,9, 9,9, 10,9, 11,9, 12,9, 13,9, 14,9,
    2,10, 3,10, 4,10, 5,10, 6,10, 7,10, 8,10, 9,10, 10,10, 11,10, 12,10, 13,10
).chunked(2).map { it[0] to it[1] }

@Composable
fun PixelCrown(
    color: Color = Color(0xFFFFD700),
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val cells = remember { CROWN_CELLS }
    Canvas(modifier = modifier.size(size)) {
        drawPixelGrid(cells, color)
    }
}

// ©¤©¤©¤ PixelSword ©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤

/** 16¡Á16 ÏñËØ½££¨×°ÊÎÓÃ£©¡£ */
private val SWORD_CELLS = listOf(
    7,0, 8,0,
    6,1, 7,1, 8,1, 9,1,
    6,2, 7,2, 8,2, 9,2,
    6,3, 7,3, 8,3, 9,3,
    5,4, 6,4, 7,4, 8,4, 9,4, 10,4,
    5,5, 6,5, 7,5, 8,5, 9,5, 10,5,
    4,6, 5,6, 6,6, 7,6, 8,6, 9,6, 10,6, 11,6,
    5,7, 6,7, 7,7, 8,7, 9,7, 10,7,
    6,8, 7,8, 8,8, 9,8,
    6,9, 7,9, 8,9, 9,9,
    6,10, 7,10, 8,10, 9,10,
    5,11, 6,11, 7,11, 8,11, 9,11, 10,11,
    5,12, 6,12, 7,12, 8,12, 9,12,
    5,13, 6,13, 7,13, 8,13,
    6,14, 7,14, 8,14,
    7,15
).chunked(2).map { it[0] to it[1] }

@Composable
fun PixelSword(
    color: Color = Color(0xFF0080FF),
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val cells = remember { SWORD_CELLS }
    Canvas(modifier = modifier.size(size)) {
        drawPixelGrid(cells, color)
    }
}

// ©¤©¤©¤ PixelBook ©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤

/** 16¡Á16 ÏñËØÊé±¾¡£ */
private val BOOK_CELLS = listOf(
    2,1, 3,1, 4,1, 5,1, 6,1, 7,1, 8,1, 9,1, 10,1, 11,1,
    2,2, 3,2, 11,2, 12,2, 13,2,
    2,3, 3,3, 11,3, 12,3, 13,3,
    2,4, 3,4, 8,4, 9,4, 10,4, 11,4, 12,4, 13,4,
    2,5, 3,5, 8,5, 9,5, 10,5, 11,5, 12,5, 13,5,
    2,6, 3,6, 11,6, 12,6, 13,6,
    2,7, 3,7, 11,7, 12,7, 13,7,
    2,8, 3,8, 4,8, 5,8, 6,8, 7,8, 8,8, 9,8, 10,8, 11,8, 12,8, 13,8,
    2,9, 3,9, 4,9, 5,9, 6,9, 7,9, 8,9, 9,9, 10,9, 11,9, 12,9, 13,9,
    1,10, 2,10, 3,10, 4,10, 5,10, 6,10, 7,10, 8,10, 9,10, 10,10, 11,10, 12,10,
    0,11, 1,11, 2,11, 3,11, 4,11, 5,11, 6,11, 7,11, 8,11, 9,11, 10,11, 11,11,
    0,12, 1,12, 2,12, 3,12, 4,12, 5,12, 6,12, 7,12, 8,12, 9,12, 10,12,
    0,13, 1,13, 2,13, 3,13, 4,13, 5,13, 6,13, 7,13, 8,13, 9,13,
    0,14, 1,14, 2,14, 3,14, 4,14, 5,14, 6,14, 7,14, 8,14
).chunked(2).map { it[0] to it[1] }

@Composable
fun PixelBook(
    color: Color = Color(0xFF39FF14),
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val cells = remember { BOOK_CELLS }
    Canvas(modifier = modifier.size(size)) {
        drawPixelGrid(cells, color)
    }
}

// ©¤©¤©¤ PixelBell ©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤

/** 16¡Á16 ÏñËØÁåîõ£¨Í¨ÖªÓÃ£©¡£ */
private val BELL_CELLS = listOf(
    6,0, 7,0, 8,0, 9,0,
    5,1, 6,1, 7,1, 8,1, 9,1, 10,1,
    5,2, 6,2, 7,2, 8,2, 9,2, 10,2,
    4,3, 5,3, 6,3, 7,3, 8,3, 9,3, 10,3, 11,3,
    3,4, 4,4, 5,4, 6,4, 7,4, 8,4, 9,4, 10,4, 11,4, 12,4,
    3,5, 4,5, 5,5, 6,5, 7,5, 8,5, 9,5, 10,5, 11,5, 12,5,
    4,6, 5,6, 7,6, 8,6, 10,6, 11,6,
    5,7, 6,7, 7,7, 8,7, 9,7, 10,7,
    5,8, 6,8, 7,8, 8,8, 9,8, 10,8,
    5,9, 6,9, 7,9, 8,9, 9,9, 10,9,
    4,10, 5,10, 6,10, 7,10, 8,10, 9,10, 10,10, 11,10,
    4,11, 6,11, 7,11, 8,11, 9,11, 11,11,
    5,12, 7,12, 8,12, 10,12,
    6,13, 7,13, 8,13, 9,13,
    7,14, 8,14
).chunked(2).map { it[0] to it[1] }

@Composable
fun PixelBell(
    color: Color = Color(0xFFFFD700),
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val cells = remember { BELL_CELLS }
    Canvas(modifier = modifier.size(size)) {
        drawPixelGrid(cells, color)
    }
}

// ©¤©¤©¤ PixelHeart ©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤

/** 16¡Á16 ÏñËØ°®ĞÄ¡£ */
private val HEART_CELLS = listOf(
    3,0, 4,0, 11,0, 12,0,
    2,1, 3,1, 4,1, 5,1, 10,1, 11,1, 12,1, 13,1,
    2,2, 3,2, 4,2, 5,2, 10,2, 11,2, 12,2, 13,2,
    2,3, 3,3, 4,3, 5,3, 10,3, 11,3, 12,3, 13,3,
    3,4, 4,4, 5,4, 10,4, 11,4, 12,4,
    3,5, 4,5, 5,5, 6,5, 9,5, 10,5, 11,5, 12,5,
    4,6, 5,6, 6,6, 7,6, 8,6, 9,6, 10,6, 11,6,
    5,7, 6,7, 7,7, 8,7, 9,7, 10,7,
    6,8, 7,8, 8,8, 9,8,
    6,9, 7,9, 8,9, 9,9,
    7,10, 8,10,
    7,11, 8,11
).chunked(2).map { it[0] to it[1] }

@Composable
fun PixelHeart(
    color: Color = Color(0xFFFF00FF),
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val cells = remember { HEART_CELLS }
    Canvas(modifier = modifier.size(size)) {
        drawPixelGrid(cells, color)
    }
}

// ©¤©¤©¤ PixelCloud ©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤©¤

/** 16¡Á16 ÏñËØÔÆ¶ä£¨3 ÖÖ±äÌå£©¡£ */
private val CLOUD1_CELLS = listOf(
    6,1, 7,1,
    5,2, 6,2, 7,2, 8,2, 9,2,
    4,3, 5,3, 6,3, 7,3, 8,3, 9,3, 10,3,
    3,4, 4,4, 5,4, 6,4, 7,4, 8,4, 9,4, 10,4, 11,4,
    3,5, 4,5, 5,5, 6,5, 7,5, 8,5, 9,5, 10,5,
    2,6, 3,6, 4,6, 5,6, 6,6, 7,6, 8,6, 9,6, 10,6, 11,6, 12,6,
    2,7, 3,7, 4,7, 5,7, 6,7, 7,7, 8,7, 9,7, 10,7, 11,7, 12,7, 13,7,
    3,8, 4,8, 5,8, 6,8, 7,8, 8,8, 9,8, 10,8, 11,8, 12,8,
    4,9, 5,9, 6,9, 7,9, 8,9, 9,9, 10,9, 11,9,
    7,10, 8,10, 9,10
).chunked(2).map { it[0] to it[1] }

private val CLOUD2_CELLS = listOf(
    3,2, 4,2,
    2,3, 3,3, 4,3, 5,3,
    1,4, 2,4, 3,4, 4,4, 5,4, 6,4,
    0,5, 1,5, 2,5, 3,5, 4,5, 5,5, 6,5,
    0,6, 1,6, 2,6, 3,6, 4,6, 5,6, 6,6, 7,6,
    1,7, 2,7, 3,7, 4,7, 5,7, 6,7, 7,7, 8,7,
    2,8, 3,8, 4,8, 5,8, 6,8, 7,8,
    4,9, 5,9, 6,9
).chunked(2).map { it[0] to it[1] }

private val CLOUD3_CELLS = listOf(
    6,3, 7,3, 8,3,
    5,4, 6,4, 7,4, 8,4, 9,4,
    4,5, 5,5, 6,5, 7,5, 8,5, 9,5,
    3,6, 4,6, 5,6, 6,6, 7,6, 8,6, 9,6, 10,6,
    2,7, 3,7, 4,7, 5,7, 6,7, 7,7, 8,7, 9,7, 10,7, 11,7,
    3,8, 4,8, 5,8, 6,8, 7,8, 8,8, 9,8, 10,8,
    5,9, 6,9, 7,9, 8,9, 9,9,
    6,10, 7,10
).chunked(2).map { it[0] to it[1] }

private val CLOUD_VARIANTS = listOf(CLOUD1_CELLS, CLOUD2_CELLS, CLOUD3_CELLS)

@Composable
fun PixelCloud(
    color: Color = Color(0xFF00FFFF),
    size: Dp = 48.dp,
    variant: Int = 0,
    modifier: Modifier = Modifier
) {
    val cells = remember(variant) {
        CLOUD_VARIANTS[variant.coerceIn(0, CLOUD_VARIANTS.size - 1)]
    }
    Canvas(modifier = modifier.size(size)) {
        drawPixelGrid(cells, color)
    }
}
