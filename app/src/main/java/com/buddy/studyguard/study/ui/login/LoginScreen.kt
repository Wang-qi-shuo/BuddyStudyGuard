package com.buddy.studyguard.study.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.buddy.studyguard.R
import com.buddy.studyguard.common.ui.components.PixelStar
import com.buddy.studyguard.common.ui.components.SweepLightText
import com.buddy.studyguard.common.ui.theme.BgDeepest
import com.buddy.studyguard.common.ui.theme.NeonCyan
import com.buddy.studyguard.common.ui.theme.NeonMagenta
import com.buddy.studyguard.common.ui.theme.TextDisabled
import com.buddy.studyguard.common.ui.theme.TextPrimary
import com.buddy.studyguard.common.ui.theme.TextSecondary
import com.buddy.studyguard.common.ui.theme.neonGlow

/**
 * 账号密码登录页面 — 像素科技风。
 *
 * 星空背景 + 标题 + 账号 / 密码输入 + 登录按钮。
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (hasIdentity: Boolean) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val loginResult by viewModel.loginResult.collectAsState()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    // 监听登录结果
    LaunchedEffect(loginResult) {
        if (loginResult != null) {
            onLoginSuccess(loginResult!!)
            viewModel.resetLoginResult()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeepest)
    ) {
        // 半透明星空背景
        Image(
            painter = painterResource(id = R.drawable.bg_pixel_starfield),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.18f)
        )

        // 前景内容
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp)
        ) {
            // ── 标题区 ──
            PixelStar(size = 28.dp, color = NeonMagenta)
            Spacer(Modifier.height(8.dp))

            SweepLightText(
                text = "BuddyStudyGuard",
                baseColor = NeonCyan,
                sweepColor = Color.White,
                fontSize = 28,
                modifier = Modifier.neonGlow(NeonCyan, radius = 20.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "账号登录",
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
            Spacer(Modifier.height(40.dp))

            // ── 账号输入 ──
            OutlinedTextField(
                value = username,
                onValueChange = { viewModel.onUsernameChange(it) },
                label = { Text("账号", fontFamily = FontFamily.Monospace) },
                placeholder = { Text("请输入账号", fontFamily = FontFamily.Monospace) },
                singleLine = true,
                enabled = !loading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = TextDisabled,
                    focusedLabelColor = NeonCyan,
                    cursorColor = NeonCyan,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // ── 密码输入 ──
            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("密码", fontFamily = FontFamily.Monospace) },
                placeholder = { Text("请输入密码", fontFamily = FontFamily.Monospace) },
                singleLine = true,
                enabled = !loading,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                            tint = TextSecondary
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.login()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = TextDisabled,
                    focusedLabelColor = NeonCyan,
                    cursorColor = NeonCyan,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // ── 错误提示 ──
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NeonMagenta,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── 登录按钮 ──
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.login()
                },
                enabled = username.isNotBlank() && password.isNotEmpty() && !loading,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = NeonCyan.copy(alpha = 0.12f),
                    contentColor = NeonCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .neonGlow(NeonCyan.copy(alpha = 0.3f), radius = 16.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = NeonCyan,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "登 录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
