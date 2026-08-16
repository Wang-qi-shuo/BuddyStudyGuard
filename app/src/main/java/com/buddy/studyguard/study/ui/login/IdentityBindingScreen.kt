package com.buddy.studyguard.study.ui.login

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.buddy.studyguard.R
import com.buddy.studyguard.common.ui.components.PixelStar
import com.buddy.studyguard.common.ui.theme.BgDeepest
import com.buddy.studyguard.common.ui.theme.BgCard
import com.buddy.studyguard.common.ui.theme.BgSurfaceVariant
import com.buddy.studyguard.common.ui.theme.NeonAmber
import com.buddy.studyguard.common.ui.theme.NeonBlue
import com.buddy.studyguard.common.ui.theme.NeonCyan
import com.buddy.studyguard.common.ui.theme.NeonGreen
import com.buddy.studyguard.common.ui.theme.NeonMagenta
import com.buddy.studyguard.common.ui.theme.TextDisabled
import com.buddy.studyguard.common.ui.theme.TextPrimary
import com.buddy.studyguard.common.ui.theme.TextSecondary
import com.buddy.studyguard.common.ui.theme.neonBorder
import com.buddy.studyguard.common.ui.theme.neonGlow

/**
 * 身份绑定页面 — 像素科技风。
 *
 * 选择「学生」或「家长」身份，完成家庭绑定。
 */
@Composable
fun IdentityBindingScreen(
    onBindingComplete: () -> Unit,
    viewModel: IdentityBindingViewModel = hiltViewModel()
) {
    val loading by viewModel.loading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val generatedFamilyCode by viewModel.generatedFamilyCode.collectAsState()
    val parentFamilyCode by viewModel.parentFamilyCode.collectAsState()
    val nickname by viewModel.nickname.collectAsState()
    val bindingComplete by viewModel.bindingComplete.collectAsState()
    var selectedRole by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(bindingComplete) {
        if (bindingComplete) onBindingComplete()
    }

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
                .alpha(0.15f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
        ) {
            // ── 标题 ──
            PixelStar(size = 24.dp, color = NeonCyan)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "选择你的身份",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = NeonCyan,
                modifier = Modifier.neonGlow(NeonCyan, radius = 16.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "首次使用，请绑定家庭身份",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
            Spacer(Modifier.height(32.dp))

            // ── 身份选择卡片 ──
            if (selectedRole == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IdentityCard(
                        title = "我是弟弟",
                        icon = Icons.Default.Gamepad,
                        description = "需要使用设备\n进行学习",
                        borderColor = NeonBlue,
                        onClick = { selectedRole = "student" },
                        modifier = Modifier.weight(1f)
                    )
                    IdentityCard(
                        title = "我是家长",
                        icon = Icons.Default.Lock,
                        description = "监管孩子设备\n使用情况",
                        borderColor = NeonAmber,
                        onClick = { selectedRole = "parent" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── 学生端：生成家庭码 ──
            if (selectedRole == "student") {
                StudentBindingPanel(
                    generatedCode = generatedFamilyCode,
                    loading = loading,
                    errorMessage = errorMessage,
                    onGenerateCode = { viewModel.bindAsStudent() },
                    onBack = { selectedRole = null }
                )
            }

            // ── 家长端：输入家庭码绑定 ──
            if (selectedRole == "parent") {
                ParentBindingPanel(
                    currentUsername = viewModel.currentUsername,
                    familyCode = parentFamilyCode,
                    nickname = nickname,
                    loading = loading,
                    errorMessage = errorMessage,
                    onCodeChange = { viewModel.onParentCodeChange(it) },
                    onNicknameChange = { viewModel.onNicknameChange(it) },
                    onBind = { viewModel.bindAsParent() },
                    onBack = { selectedRole = null }
                )
            }
        }
    }
}

/**
 * 身份选择大卡片。
 */
@Composable
private fun IdentityCard(
    title: String,
    icon: ImageVector,
    description: String,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(180.dp)
            .neonBorder(borderColor, width = 1.dp, glowWidth = 4.dp),
        shape = RoundedCornerShape(8.dp),
        border = null,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = borderColor.copy(alpha = 0.06f),
            contentColor = borderColor
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = borderColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = borderColor
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 学生绑定面板：生成家庭码并展示。
 */
@Composable
private fun StudentBindingPanel(
    generatedCode: String?,
    loading: Boolean,
    errorMessage: String?,
    onGenerateCode: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (generatedCode == null) {
            Text(
                text = "生成专属家庭码\n让家长凭码绑定你的设备",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = onGenerateCode,
                enabled = !loading,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = NeonBlue.copy(alpha = 0.12f),
                    contentColor = NeonBlue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .neonGlow(NeonBlue.copy(alpha = 0.3f), radius = 16.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(color = NeonBlue, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("生成家庭码", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        } else {
            // 已生成，展示家庭码
            Text(
                text = "你的家庭码",
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .background(BgCard, RoundedCornerShape(8.dp))
                    .neonBorder(NeonGreen, width = 2.dp, glowWidth = 6.dp)
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(
                    text = generatedCode,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = NeonGreen
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "将此码发给家长，完成绑定",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = TextDisabled
            )
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = errorMessage,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = NeonMagenta
            )
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = TextSecondary
            )
        ) {
            Text("返回选择", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
    }
}

/**
 * 家长绑定面板：输入家庭码和称呼。
 */
@Composable
private fun ParentBindingPanel(
    currentUsername: String?,
    familyCode: String,
    nickname: String,
    loading: Boolean,
    errorMessage: String?,
    onCodeChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onBind: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "输入孩子提供的家庭码\n完成设备绑定",
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "当前登录账号：${currentUsername ?: "未登录"}（已通过账号密码登录）",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = NeonGreen,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            label = { Text("你的称呼", fontFamily = FontFamily.Monospace) },
            placeholder = { Text("如：爸爸、妈妈、爷爷…", fontFamily = FontFamily.Monospace) },
            singleLine = true,
            enabled = !loading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonAmber,
                unfocusedBorderColor = TextDisabled,
                focusedLabelColor = NeonAmber,
                cursorColor = NeonAmber,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = familyCode,
            onValueChange = onCodeChange,
            label = { Text("家庭绑定码", fontFamily = FontFamily.Monospace) },
            placeholder = { Text("6 位数字", fontFamily = FontFamily.Monospace) },
            singleLine = true,
            enabled = !loading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonAmber,
                unfocusedBorderColor = TextDisabled,
                focusedLabelColor = NeonAmber,
                cursorColor = NeonAmber,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onBind,
            enabled = familyCode.length == 6 && !loading,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = NeonAmber.copy(alpha = 0.12f),
                contentColor = NeonAmber
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .neonGlow(NeonAmber.copy(alpha = 0.3f), radius = 16.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(color = NeonAmber, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text("绑定设备", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = errorMessage,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = NeonMagenta,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = TextSecondary
            )
        ) {
            Text("返回选择", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
    }
}
