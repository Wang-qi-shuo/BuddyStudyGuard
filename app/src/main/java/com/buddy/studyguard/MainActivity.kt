package com.buddy.studyguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.buddy.studyguard.common.ui.theme.BuddyStudyGuardTheme
import com.buddy.studyguard.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint

/**
 * 单 Activity 入口：启动屏 -> Compose 导航。
 * 所有界面由 [AppNavGraph] 驱动，依赖由 Hilt 注入。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuddyStudyGuardTheme {
                AppNavGraph()
            }
        }
    }
}
