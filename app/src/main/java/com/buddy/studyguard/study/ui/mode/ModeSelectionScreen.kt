package com.buddy.studyguard.study.ui.mode

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buddy.studyguard.R
import com.buddy.studyguard.common.ui.components.PixelCloudDecoration
import com.buddy.studyguard.common.ui.components.PixelCrown
import com.buddy.studyguard.common.ui.components.PixelShieldSmall
import com.buddy.studyguard.common.ui.components.PixelStar
import com.buddy.studyguard.common.ui.components.PixelSword
import com.buddy.studyguard.common.ui.components.SweepLightText
import com.buddy.studyguard.common.ui.theme.BgDeepest
import com.buddy.studyguard.common.ui.theme.NeonAmber
import com.buddy.studyguard.common.ui.theme.NeonBlue
import com.buddy.studyguard.common.ui.theme.NeonCyan
import com.buddy.studyguard.common.ui.theme.TextDisabled
import com.buddy.studyguard.common.ui.theme.TextSecondary
import com.buddy.studyguard.common.ui.theme.neonBorder
import com.buddy.studyguard.common.ui.theme.neonGlow

/**
 * 模式选择启动页 — 像素科技风。
 *
 * 星空背景 + 扫光标题 + 霓虹脉冲模式卡片 + 底部像素云朵。
 * 根据已绑定的身份直接跳转到对应主页。
 */
@Composable
fun ModeSelectionScreen(
    identity: String,
    onChildMode: () -> Unit,
    onParentMode: () -> Unit
) {
    // 根据身份自动跳转
    LaunchedEffect(identity) {
        when (identity) {
            "parent" -> onParentMode()
            else -> onChildMode()
        }
    }

    // 过渡展示（跳转前短暂显示）
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeepest)
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_pixel_starfield),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.18f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Spacer(Modifier.weight(0.15f))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                PixelShieldSmall(size = 32.dp, color = NeonBlue)
                PixelStar(size = 28.dp, color = NeonCyan)
            }

            Spacer(Modifier.height(8.dp))

            SweepLightText(
                text = "弟管严",
                baseColor = NeonCyan,
                sweepColor = Color.White,
                fontSize = 36,
                modifier = Modifier.neonGlow(NeonCyan, radius = 24.dp)
            )
            Text(
                text = "BuddyStudyGuard",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = NeonCyan,
                textAlign = TextAlign.Center,
                modifier = Modifier.neonGlow(NeonCyan, radius = 20.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "v1.0 · PIXEL EDITION",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextDisabled,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = "正在进入${if (identity == "parent") "家长" else "弟弟"}模式...",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )

            Spacer(Modifier.weight(0.5f))

            PixelCloudDecoration(color = NeonCyan)
        }
    }
}
