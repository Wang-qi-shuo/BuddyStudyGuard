---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: 061a0cddaa82373f1653d85953832b66_b72330c1994311f1a98a525400f8a581
    ReservedCode1: Q6TVqs98gCwj2fuXSwh8zoJEkh4QJIrqNrpPzPIzlLeua6CTXOxBqSVCql6hjL5JVmUvuwkRMHTk1utN4phAGa1jpi8ga6n9GCXCZjXjp5WWzhrek2gb34ST4u0pg4IZmWssH5T4WuoW+O6mXMmYgydQTrnf6hBlFi+ymasykFF7qgUfl1HolpsfHEI=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: 061a0cddaa82373f1653d85953832b66_b72330c1994311f1a98a525400f8a581
    ReservedCode2: Q6TVqs98gCwj2fuXSwh8zoJEkh4QJIrqNrpPzPIzlLeua6CTXOxBqSVCql6hjL5JVmUvuwkRMHTk1utN4phAGa1jpi8ga6n9GCXCZjXjp5WWzhrek2gb34ST4u0pg4IZmWssH5T4WuoW+O6mXMmYgydQTrnf6hBlFi+ymasykFF7qgUfl1HolpsfHEI=
---

# 弟管严 (BuddyStudyGuard)

一款为弟弟开发的学习与监护 Android 应用，安装在他自己的手机上（弟弟完全知情并同意）。应用内分「孩子模式」和「家长模式」。数据以本地 Room 为唯一数据源，并已接入腾讯云 CloudBase 云端同步：限制规则、使用时长、消息、任务、应用清单等可在多台设备间跨设备互通；AI 助手需联网调用豆包大模型 API。

## 技术栈

| 维度 | 选型 |
|------|------|
| 语言 | Kotlin |
| 最低版本 | Android 8.0 (API 26) |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository |
| 依赖注入 | Hilt |
| 本地存储 | Room（唯一本地数据源） |
| 网络 | Retrofit + OkHttp + Gson（AI 模块 + CloudBase 云端同步） |
| 异步 | Coroutines + Flow |
| 后台 | WorkManager + 前台 Service |
| 图表 | Compose Canvas 手绘（无第三方图表库） |

## 项目结构

```
BuddyStudyGuard/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml          # 版本目录
├── app/
│   ├── build.gradle.kts               # 依赖 + manifestPlaceholders 注入豆包 API Key
│   └── src/main/
│       ├── AndroidManifest.xml        # 权限 + 服务 + Activity 注册
│       ├── java/com/buddy/studyguard/
│       │   ├── BuddyStudyGuardApp.kt  # @HiltAndroidApp + WorkManager Configuration
│       │   ├── MainActivity.kt        # 单 Activity + Compose
│       │   ├── common/
│       │   │   ├── cloud/             # CloudBase 云端同步（CloudBaseManager + CloudBaseApi + CloudSyncRepository）
│       │   │   ├── di/                # DatabaseModule
│       │   │   ├── data/db/           # AppDatabase + 12 Entity + 11 DAO
│       │   │   ├── ui/theme/          # 像素+科技风主题（PixelCyan/NeonPurple/AmberWarn）
│       │   │   └── util/              # Constants/TimeUtil/AppClassifier/PermissionUtil/NetworkUtil/PinHasher
│       │   ├── monitor/               # 监护模块（离线）
│       │   │   ├── usage/UsageStatsHelper
│       │   │   ├── engine/RestrictionEngine
│       │   │   ├── service/           # 前台服务 + 无障碍服务 + 全屏提醒 + 开机自启
│       │   │   └── worker/DailyResetWorker
│       │   ├── ai/                    # AI 助手模块（在线 + 离线兜底）
│       │   │   ├── data/remote/       # DoubaoApi + Models
│       │   │   ├── data/local/FaqRepository   # 20+ 离线学习 FAQ
│       │   │   ├── data/repository/AiChatRepository
│       │   │   └── ui/                # AiChatScreen + ViewModel
│       │   ├── study/                 # 孩子模块 UI（离线）
│       │   │   └── ui/                # Home/TaskBoard/Schedule/FocusTimer/Stats/Usage
│       │   ├── parent/                # 家长模块 UI（离线）
│       │   │   └── ui/                # Entry/Pin/Dashboard/AppControl/Message/Task/Report
│       │   └── navigation/AppNavGraph # 顶层导航
│       └── res/                       # 主题/字符串/图标/无障碍配置
```

## 编译运行

1. 用 **Android Studio Hedgehog 或更高版本**打开 `BuddyStudyGuard` 目录。
2. 等待 Gradle Sync 完成（首次会下载依赖）。
3. 连接 Android 8.0+ 真机，点 Run。

> 命令行编译：项目未内置 `gradlew.bat`，请使用本机 Gradle 8.9 的 `gradle.bat` 或直接用 Android Studio 构建。示例（PowerShell）：
>
> ```powershell
> $env:JAVA_HOME = "G:\Android\jdk21"
> & "C:\Users\DELL\.gradle\wrapper\dists\gradle-8.9-bin\78qddjpeqn5v6yec3xb8kv9ca\gradle-8.9\bin\gradle.bat" :app:assembleDebug --console=plain
> ```

## 豆包 API Key 配置

AI 助手需要豆包大模型 API Key。编辑 `app/build.gradle.kts`：

```kotlin
manifestPlaceholders["DOUBAO_API_KEY"] = "你的豆包 API Key"
```

Key 申请：火山引擎控制台 → 方舟大模型 → 创建 API Key。模型 ID 默认 `doubao-pro-32k`，可在 `common/util/Constants.kt` 的 `DOUBAO_DEFAULT_MODEL` 修改。

未配置 Key 时应用其余功能正常，AI 对话会返回 401 错误（有重试提示）。

## 权限说明

首次使用相关功能时会引导授权，每项权限都有通俗解释：

| 权限 | 用途 | 触发时机 |
|------|------|----------|
| PACKAGE_USAGE_STATS | 统计各应用前台时长 | 进入应用使用统计 |
| SYSTEM_ALERT_WINDOW | 超时全屏提醒 | 设置时长限制后 |
| BIND_ACCESSIBILITY_SERVICE | 命中限制时返回桌面（不读取界面内容） | 启用应用限制 |
| POST_NOTIFICATIONS | 学习提醒与任务通知 | 首次启动 (API 33+) |
| FOREGROUND_SERVICE | 保持后台监护运行 | 启动监护服务 |
| RECEIVE_BOOT_COMPLETED | 重启后恢复监护服务 | 开机 |

无障碍服务配置见 `res/xml/accessibility_service_config.xml`：`canRetrieveWindowContent="false"`，仅监听窗口状态变化，不读取任何界面内容。

## 模式切换

- **孩子模式**（默认）：首页 / 任务 / 课表 / 专注 / 统计 五个底栏 Tab，顶部 AI 助手入口图标。
- **进入家长模式**：在顶部标题「弟管严」上**长按**，弹出数字口令输入框，默认口令 `123456`（首次启动自动写入数据库的 SHA-256 加盐哈希，安装后建议立即修改）。
- 家长模式底栏：总览 / 应用控制 / 消息 / 布置任务 / 报告；口令管理从总览页按钮进入。
- 修改口令：家长模式 → 总览 → 口令管理。

## 模块说明

### monitor（监护，离线）
- `UsageStatsHelper`：封装 UsageStatsManager，取每日/每周应用时长，自动识别游戏并落库。
- `RestrictionEngine`：按「即时锁定 → 单应用时长上限 → 禁用时段」顺序评估是否拦截。
- `AppLimitAccessibilityService`：监听窗口变化，命中限制时返回桌面并弹出全屏提醒。
- `AppLimitForegroundService`：前台服务每 60 秒刷新当日使用记录。
- `DailyResetWorker`：每日清理 30 天前的使用记录。

### ai（AI 助手，在线 + 离线兜底）
- 调用豆包 OpenAI 兼容接口，多轮上下文。
- 无网络或调用异常时降级到本地 20+ 学习 FAQ。
- 系统提示限定只讨论学习话题。

### study（孩子模式，离线）
- 首页：家长消息卡片 + 今日任务 + 今日课程。
- 任务板：合并自建与家长布置任务，按截止排序，标记完成。
- 课表：周视图，当天高亮，可编辑。
- 专注计时：番茄钟 / 正计时，记录学习时长到统计。
- 学习统计：今日/本周时长，按科目条形图。

### parent（家长模式，离线）
- 总览：今日游戏/学习时长，应用排行，快捷入口。
- 应用控制：锁定开关、每日时长限额、禁用时段。
- 消息：发文字给孩子（孩子首页卡片展示 + 振动）。
- 布置任务：创建任务推到孩子任务板。
- 报告：学习时长分布、游戏时长、任务完成度。
- 口令管理：修改进入口令。

## 数据安全

- 本地 Room 数据库为唯一数据源；已接入 CloudBase 云端同步（限制规则、使用时长、消息、任务、应用清单等跨设备互通），云端数据仅用于多设备同步，不对外公开。
- 无障碍服务不读取界面内容，仅用于执行「返回桌面」动作。
- 家长口令以 SHA-256 加盐哈希存储，不存明文。
- 应用不包含任何监控通讯内容的功能。

## 已知限制 / 后续优化

- `UsageOverviewScreen`（孩子应用使用概览）已实现，导航入口待接入（可从首页或统计页加入口）。
- 在线题库接口为预留，当前版本未实现。
*（内容由AI生成，仅供参考）*
