package com.buddy.studyguard.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.buddy.studyguard.R
import com.buddy.studyguard.ai.ui.AiChatScreen
import com.buddy.studyguard.common.cloud.CloudBaseManager
import com.buddy.studyguard.common.ui.components.PixelDots
import com.buddy.studyguard.common.ui.theme.BgDeepest
import com.buddy.studyguard.common.ui.theme.NeonAmber
import com.buddy.studyguard.common.ui.theme.NeonCyan
import com.buddy.studyguard.common.util.PermissionUtil
import com.buddy.studyguard.parent.ui.apps.AppControlScreen
import com.buddy.studyguard.parent.ui.dashboard.ParentDashboardScreen
import com.buddy.studyguard.parent.ui.entry.ParentEntryScreen
import com.buddy.studyguard.parent.ui.messages.ParentMessageScreen
import com.buddy.studyguard.parent.ui.pin.PinManageScreen
import com.buddy.studyguard.parent.ui.reports.ParentReportScreen
import com.buddy.studyguard.parent.ui.tasks.ParentTaskScreen
import com.buddy.studyguard.study.ui.chat.ChatScreen
import com.buddy.studyguard.study.ui.focus.FocusTimerScreen
import com.buddy.studyguard.study.ui.home.HomeScreen
import com.buddy.studyguard.study.ui.login.IdentityBindingScreen
import com.buddy.studyguard.study.ui.login.LoginScreen
import com.buddy.studyguard.study.ui.mode.ModeSelectionScreen
import com.buddy.studyguard.study.ui.schedule.ScheduleScreen
import com.buddy.studyguard.study.ui.stats.StudyStatsScreen
import com.buddy.studyguard.study.ui.task.TaskBoardScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 顶层路由。 */
object Route {
    const val AUTH_CHECK = "auth_check"
    const val LOGIN = "login"
    const val IDENTITY_BINDING = "identity_binding"
    const val MODE_SELECTION = "mode_selection"
    const val CHILD = "child"
    const val AI = "ai"
    const val PARENT_ENTRY = "parent_entry"
    const val PARENT = "parent"
}

/**
 * 应用根导航：
 *
 * 启动 → 检查登录 → 未登录 LoginScreen → 登录后检查身份 →
 * 无身份 IdentityBindingScreen → 有身份直接进对应主页。
 */
@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = Route.AUTH_CHECK) {
        // ── 登录状态检查 ──
        composable(Route.AUTH_CHECK) {
            AuthCheckScreen(
                onNotLoggedIn = {
                    navController.navigate(Route.LOGIN) {
                        popUpTo(Route.AUTH_CHECK) { inclusive = true }
                    }
                },
                onHasIdentity = { identity ->
                    navController.navigate("${Route.MODE_SELECTION}/$identity") {
                        popUpTo(Route.AUTH_CHECK) { inclusive = true }
                    }
                },
                onNoIdentity = {
                    navController.navigate(Route.IDENTITY_BINDING) {
                        popUpTo(Route.AUTH_CHECK) { inclusive = true }
                    }
                }
            )
        }

        // ── 手机号登录 ──
        composable(Route.LOGIN) {
            LoginScreen(
                onLoginSuccess = { hasIdentity ->
                    if (hasIdentity) {
                        // 查询身份后导航
                        navController.navigate(Route.AUTH_CHECK) {
                            popUpTo(Route.LOGIN) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Route.IDENTITY_BINDING) {
                            popUpTo(Route.LOGIN) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ── 身份绑定 ──
        composable(Route.IDENTITY_BINDING) {
            IdentityBindingScreen(
                onBindingComplete = {
                    navController.navigate(Route.AUTH_CHECK) {
                        popUpTo(Route.IDENTITY_BINDING) { inclusive = true }
                    }
                }
            )
        }

        // ── 模式选择（根据身份直接导航） ──
        composable("${Route.MODE_SELECTION}/{identity}") { backStackEntry ->
            val identity = backStackEntry.arguments?.getString("identity") ?: "student"
            ModeSelectionScreen(
                identity = identity,
                onChildMode = {
                    navController.navigate(Route.CHILD) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onParentMode = {
                    navController.navigate(Route.PARENT) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.CHILD) {
            ChildSection(
                onEnterParent = { navController.navigate(Route.PARENT_ENTRY) },
                onOpenAi = { navController.navigate(Route.AI) },
                onLogout = {
                    CloudBaseManager.logout()
                    navController.navigate(Route.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.AI) {
            AiChatScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.PARENT_ENTRY) {
            ParentEntryScreen(
                onSuccess = {
                    navController.navigate(Route.PARENT) {
                        popUpTo(Route.PARENT_ENTRY) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.PARENT) {
            ParentSection(
                onExit = {
                    navController.popBackStack(Route.CHILD, inclusive = false)
                },
                onLogout = {
                    CloudBaseManager.logout()
                    navController.navigate(Route.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
 * 启动时检查 CloudBase 登录状态和用户身份。
 */
@Composable
private fun AuthCheckScreen(
    onNotLoggedIn: () -> Unit,
    onHasIdentity: (identity: String) -> Unit,
    onNoIdentity: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (!CloudBaseManager.isLoggedIn()) {
            onNotLoggedIn()
            return@LaunchedEffect
        }
        val uid = CloudBaseManager.currentUserId()
        if (uid == null) {
            onNotLoggedIn()
            return@LaunchedEffect
        }

        val identity: String? = try {
            // 确保 access_token 有效；刷新失败说明会话已失效，回到登录页重新登录
            if (!CloudBaseManager.ensureValidToken()) {
                CloudBaseManager.logout()
                onNotLoggedIn()
                return@LaunchedEffect
            }
            val result = withContext(Dispatchers.IO) {
                CloudBaseManager.api.query(
                    CloudBaseManager.COLL_USERS,
                    filters = mapOf("uid" to "eq.$uid")
                )
            }
            if (result.isNotEmpty()) {
                result.first()["identity"] as? String ?: "student"
            } else {
                null
            }
        } catch (_: Exception) {
            // 查询失败（网络/认证异常）不能判定为"无身份"，回到登录页重新登录
            CloudBaseManager.logout()
            onNotLoggedIn()
            return@LaunchedEffect
        }

        if (identity != null) {
            onHasIdentity(identity)
        } else {
            onNoIdentity()
        }
    }
    // 加载中指示
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = NeonCyan)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ChildSection(
    onEnterParent: () -> Unit,
    onOpenAi: () -> Unit,
    onLogout: () -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val labels = listOf(
        stringResource(R.string.nav_home),
        stringResource(R.string.nav_chat),
        stringResource(R.string.nav_tasks),
        stringResource(R.string.nav_schedule),
        stringResource(R.string.nav_focus),
        stringResource(R.string.nav_stats)
    )
    val icons = listOf(
        Icons.Default.Home,
        Icons.Default.Chat,
        Icons.Default.Assignment,
        Icons.Default.CalendarMonth,
        Icons.Default.Timer,
        Icons.Default.BarChart
    )

    // ── 无障碍服务检测与引导 ──
    // 进入学生端时检测无障碍服务是否开启；未开启则弹窗引导跳转系统无障碍设置页，
    // 并在顶部常驻横幅持续提示（所有 tab 可见），从系统设置返回后自动刷新状态。
    val context = LocalContext.current
    var hasAccessibility by remember { mutableStateOf(PermissionUtil.isAccessibilityEnabled(context)) }
    var showA11yDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccessibility = PermissionUtil.isAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // 首次进入检测：无障碍未开启则弹引导
    LaunchedEffect(Unit) {
        if (!PermissionUtil.isAccessibilityEnabled(context)) {
            showA11yDialog = true
        }
    }

    if (showA11yDialog) {
        AlertDialog(
            onDismissRequest = { showA11yDialog = false },
            title = { Text("无障碍服务未开启", fontFamily = FontFamily.Monospace) },
            text = {
                Text(
                    "监护限制功能需要无障碍服务才能生效。\n请前往系统设置开启「弟管严」的无障碍服务。",
                    fontFamily = FontFamily.Monospace
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showA11yDialog = false
                    context.startActivity(PermissionUtil.accessibilitySettingsIntent())
                }) {
                    Text("去开启", fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showA11yDialog = false }) {
                    Text("稍后", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = onEnterParent
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onOpenAi) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_ai),
                            contentDescription = stringResource(R.string.nav_ai)
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "登出"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                labels.forEachIndexed { i, label ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                when (i) {
                                    0 -> Icon(painter = painterResource(id = R.drawable.ic_nav_home), contentDescription = label)
                                    5 -> Icon(painter = painterResource(id = R.drawable.ic_nav_stats), contentDescription = label)
                                    else -> Icon(icons[i], contentDescription = label)
                                }
                                if (tab == i) {
                                    PixelDots(
                                        color = NeonCyan,
                                        dotCount = 6,
                                        dotSize = 2.dp,
                                        modifier = Modifier.size(width = 20.dp, height = 6.dp)
                                    )
                                }
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { inner ->
        Column(Modifier.padding(inner)) {
            // 无障碍未开启时的常驻引导横幅（所有 tab 可见）
            if (!hasAccessibility) {
                Surface(
                    color = NeonAmber.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { context.startActivity(PermissionUtil.accessibilitySettingsIntent()) }
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "无障碍服务未开启，限制功能无法生效",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = NeonAmber,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "去开启 >",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = NeonAmber
                        )
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> HomeScreen()
                    1 -> ChatScreen(isParentMode = false)
                    2 -> TaskBoardScreen()
                    3 -> ScheduleScreen()
                    4 -> FocusTimerScreen()
                    5 -> StudyStatsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentSection(onExit: () -> Unit, onLogout: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showPin by rememberSaveable { mutableStateOf(false) }
    var showChat by rememberSaveable { mutableStateOf(false) }
    val labels = listOf(
        stringResource(R.string.nav_parent_dashboard),
        stringResource(R.string.nav_parent_apps),
        stringResource(R.string.nav_parent_messages),
        stringResource(R.string.nav_parent_tasks),
        stringResource(R.string.nav_parent_reports)
    )
    val icons = listOf(
        Icons.Default.Dashboard,
        Icons.Default.Apps,
        Icons.Default.Email,
        Icons.Default.Assignment,
        Icons.Default.Assessment
    )

    if (showPin) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("口令管理", fontFamily = FontFamily.Monospace) },
                    navigationIcon = {
                        IconButton(onClick = { showPin = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
            }
        ) { inner ->
            Box(Modifier.padding(inner)) { PinManageScreen() }
        }
        return
    }

    if (showChat) {
        ChatScreen(isParentMode = true, onBack = { showChat = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.role_parent), fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "登出"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                labels.forEachIndexed { i, label ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                when (i) {
                                    0 -> Icon(painter = painterResource(id = R.drawable.ic_nav_home), contentDescription = label)
                                    4 -> Icon(painter = painterResource(id = R.drawable.ic_nav_stats), contentDescription = label)
                                    else -> Icon(icons[i], contentDescription = label)
                                }
                                if (tab == i) {
                                    PixelDots(
                                        color = NeonCyan,
                                        dotCount = 6,
                                        dotSize = 2.dp,
                                        modifier = Modifier.size(width = 20.dp, height = 6.dp)
                                    )
                                }
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner)) {
            when (tab) {
                0 -> ParentDashboardScreen(
                    onNavigate = { route ->
                        when (route) {
                            "parent_pin" -> showPin = true
                            "parent_chat" -> showChat = true
                            "parent_apps" -> tab = 1
                            "parent_messages" -> tab = 2
                            "parent_tasks" -> tab = 3
                            "parent_reports" -> tab = 4
                        }
                    }
                )
                1 -> AppControlScreen()
                2 -> ParentMessageScreen()
                3 -> ParentTaskScreen()
                4 -> ParentReportScreen()
            }
        }
    }
}
