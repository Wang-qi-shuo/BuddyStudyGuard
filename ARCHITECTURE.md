# 弟管严 (BuddyStudyGuard) 架构文档

> 本文档描述项目的整体架构、模块划分、核心流程与关键类，帮助开发者快速理解代码组织与运行机制。

## 1. 项目概述

弟管严是一款面向家庭场景的 Android 监护应用，安装于孩子手机（孩子知情并同意）。应用内分「孩子模式」与「家长模式」：

- **孩子模式**：首页 / 任务 / 课表 / 专注 / 统计 五个底栏 Tab，顶部 AI 助手入口。
- **家长模式**：总览 / 应用控制 / 消息 / 布置任务 / 报告 五个底栏 Tab，通过长按标题 + 数字口令进入。

家长端与学生端通过腾讯云 CloudBase（PostgREST REST API）实时同步限制规则、使用时长、消息、任务与应用清单，实现跨设备互通。应用为侧载分发（非上架），目标设备为 Android 8.0+。

## 2. 技术栈

| 维度 | 选型 |
|------|------|
| 语言 | Kotlin 2.0 |
| 最低版本 | Android 8.0 (API 26)，targetSdk 34 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository |
| 依赖注入 | Hilt（含 Hilt Worker） |
| 本地存储 | Room（唯一本地数据源，version 4） |
| 网络 | Retrofit + OkHttp + Gson |
| 云端 | 腾讯云 CloudBase（PostgREST REST API，Bearer Token 鉴权） |
| 异步 | Coroutines + Flow |
| 后台 | WorkManager + 前台 Service + 无障碍服务 |
| 图表 | Compose Canvas 手绘（无第三方图表库） |
| 测试 | JUnit4 + MockK + kotlinx-coroutines-test + Robolectric |

## 3. 模块结构

应用为单模块（`app`）项目，源码位于 `app/src/main/java/com/buddy/studyguard/`，按功能域分包：

```
com.buddy.studyguard/
├── BuddyStudyGuardApp.kt        # @HiltAndroidApp，初始化 CloudBaseManager、调度同步 Worker
├── MainActivity.kt              # 单 Activity + Compose
├── common/                      # 跨模块共享层
│   ├── cloud/                   # CloudBase 云端同步
│   │   ├── CloudBaseManager.kt      # 单例：登录态/Token 管理、集合常量、设备 ID
│   │   ├── CloudBaseApiService.kt   # Retrofit 接口（Auth + PostgREST CRUD）
│   │   ├── CloudSyncRepository.kt   # 数据同步仓库（消息/任务/时长/限制/应用清单）
│   │   ├── RestrictionSyncWorker.kt # 每 5 分钟拉取限制快照（含空快照保护）
│   │   ├── UsageReportWorker.kt     # 使用时长上报
│   │   ├── InstalledAppReporter.kt  # 学生端应用清单上报
│   │   └── ImageBase64.kt           # 图片压缩编码/解码（云端传输用）
│   ├── data/
│   │   ├── db/                  # AppDatabase + 13 Entity + 12 DAO（Room）
│   │   ├── prefs/ApiKeyPrefs.kt # 豆包 API Key 存储（EncryptedSharedPreferences）
│   │   └── repository/ChatRepository.kt
│   ├── di/DatabaseModule.kt     # Room 数据库 + DAO 提供
│   ├── ui/                      # 主题（PixelTheme）+ 共享组件（ChatImageWithSaveButton 等）
│   └── util/                    # TimeUtil / AppClassifier / PermissionUtil / NetworkUtil / PinHasher / Constants
├── monitor/                     # 监护模块（学生端核心）
│   ├── engine/RestrictionEngine.kt  # 限制决策引擎（纯逻辑，可单测）
│   ├── service/                 # 无障碍服务 + 前台服务 + 全屏提醒 + 开机自启
│   ├── usage/UsageStatsHelper.kt    # 使用情况统计封装
│   ├── worker/DailyResetWorker.kt   # 每日时长重置
│   └── di/
├── ai/                          # AI 学习助手模块
│   ├── data/remote/             # DoubaoApi + Models（豆包大模型）
│   ├── data/local/FaqRepository # 离线 FAQ 兜底
│   ├── data/repository/AiChatRepository
│   └── ui/                      # AiChatScreen + ViewModel
├── study/                       # 孩子模块 UI
│   └── ui/                      # home/task/schedule/focus/stats/usage/chat/login/mode
├── parent/                      # 家长模块 UI
│   ├── ui/                      # entry/pin/dashboard/apps/messages/tasks/reports
│   └── viewmodel/
└── navigation/AppNavGraph.kt    # 顶层导航（含家长/孩子模式切换）
```

### 3.1 数据层（Room）

`AppDatabase`（version 4）包含 **13 个 Entity** 与 **12 个 DAO**：

- 任务：`TaskEntity` / `TaskDao`
- 使用时长：`AppUsageRecordEntity` / `AppUsageRecordDao`
- 限制规则：`AppLimitRuleEntity` / `AppLimitRuleDao`、`AppLockStateEntity` / `AppLockStateDao`
- 禁用时段：`BlockedTimeWindowEntity` + `BlockedTimeWindowAppEntity` / `BlockedTimeWindowDao`
- 消息：`ParentMessageEntity` / `ParentMessageDao`、`ChatMessageEntity` / `ChatMessageDao`
- 学习：`StudySessionEntity` / `StudySessionDao`、`CourseEntity` / `CourseDao`
- AI：`AiMessageEntity` / `AiMessageDao`
- 口令：`ParentPinEntity` / `ParentPinDao`（单行记录，固定 id=1）
- 应用分类：`AppCategoryEntity` / `AppCategoryDao`

数据库首次创建时通过 `SEED_CALLBACK` 写入默认家长口令 `123456` 的 SHA-256 加盐哈希。

### 3.2 云端层（CloudBase）

云端为腾讯云 CloudBase 托管的 PostgreSQL 实例，通过 **PostgREST** 暴露 REST API：

- Base URL：`https://<ENV_ID>.api.tcloudbasegateway.com`
- 鉴权：`Authorization: Bearer <access_token>`（`auth/v1/token` 刷新请求除外）
- 数据操作：`GET/POST/PATCH/DELETE /v1/rdb/rest/{table}`，返回裸 JSON 数组
- 认证：`POST auth/v1/signin`（账号密码 + `x-device-id`）、`POST auth/v1/token`（refresh_token 刷新）

云端集合（表）：`users`、`family_groups`、`messages`、`tasks`、`app_usage`、`app_lock_rules`、`app_limit_rules`、`blocked_time_windows`、`child_apps`。

## 4. 核心流程

### 4.1 限制拦截链路（学生端）

```
无障碍服务监听窗口变化
  └─ AppLimitAccessibilityService.onAccessibilityEvent
       （仅 TYPE_WINDOW_STATE_CHANGED，不读取界面内容）
       └─ RestrictionEngine.evaluate(pkg)   // 纯逻辑决策
            ├─ 1. 即时锁定  AppLockStateDao.get(pkg).locked == true
            ├─ 2. 每日时长  AppLimitRuleDao.get(pkg) 且 当天累计时长 >= dailyLimitMs
            └─ 3. 禁用时段  BlockedTimeWindowDao.getAllEnabled()
                 ├─ 星期掩码 isTodayInMask(daysOfWeek, now)
                 ├─ 分钟区间 isMinuteInWindow(nowMinute, start, end)（支持跨天）
                 └─ appliesToAllApps 或 包名命中 getAppsOf(id)
       └─ 命中 Block → 返回桌面 + 弹出 FullScreenAlertActivity 全屏提醒
```

限制规则来源：家长端操作（锁定/设限/删限）→ `AppControlViewModel` → `CloudSyncRepository.pushRestrictionSnapshot` 推送到云端 → 学生端 `RestrictionSyncWorker` 每 5 分钟拉取覆盖式写入本地 Room → 无障碍服务下次评估时生效。

### 4.2 数据同步链路（双端互通）

```
家长端 / 学生端
  └─ CloudSyncRepository（suspend + Result 优雅降级）
       ├─ 限制规则：pushRestrictionSnapshot / pullRestrictionSnapshot
       ├─ 使用时长：reportAppUsage（覆盖式：先删后插）/ fetchAppUsage / fetchAppUsageSummary
       ├─ 消息：sendMessage（含图片 base64）/ fetchMessages / listenMessages（轮询）
       ├─ 任务：createTask / fetchTasks / markTaskCompleted / deleteTask / listenTasks
       └─ 应用清单：reportInstalledApps（仅学生端）/ fetchChildApps / updateChildAppCategory
  └─ CloudBaseManager.ensureValidToken()   // 过期自动 refresh_token 刷新
```

- 同步周期：限制快照每 5 分钟（`RestrictionSyncWorker`），登录成功后 `runNow` 立即触发一次。
- 图片传输：无对象存储，图片经 `ImageBase64.compressAndEncode` 压缩后以 base64 存入 `messages.image` 字段。
- 失败策略：云端操作失败不影响本地功能（`Result` 包装），Worker 失败返回 `Result.retry()`。

### 4.3 聊天 / 任务互通

- **聊天**：家长端 `ParentMessageViewModel` 与学生端 `ChatViewModel` 通过 `CloudSyncRepository.sendMessage` 发送（文字 + 图片），`listenMessages` 轮询拉取新消息写入本地 `ChatMessageEntity`；接收方将 base64 解码为本地缓存 URI 展示，图片可经 `ImageBase64.saveToGallery` 保存到相册。
- **任务**：家长端 `ParentTaskViewModel` 创建/删除任务，学生端 `TaskBoardViewModel` 通过 `listenTasks` 拉取并展示，学生可标记完成（`markTaskCompleted`）。

## 5. 关键类说明

| 类 | 位置 | 职责 |
|----|------|------|
| `RestrictionEngine` | `monitor/engine/` | 限制决策引擎，按「即时锁定 → 每日时长 → 禁用时段」顺序评估，返回 `RestrictionDecision.Allow/Block`。纯逻辑、依赖注入 4 个 DAO，可单元测试。 |
| `CloudSyncRepository` | `common/cloud/` | 云端数据同步仓库，封装所有 PostgREST 读写，全部 suspend + `Result` 返回，含 family_code 缓存（TTL 60s）。 |
| `CloudBaseManager` | `common/cloud/` | 云端单例：登录态持久化（SharedPreferences）、access_token 过期自动刷新（Mutex 防并发）、设备 ID、集合常量。 |
| `RestrictionSyncWorker` | `common/cloud/` | 每 5 分钟拉取家庭限制快照覆盖式写入本地；含**空快照保护**（连续 2 次云端为空才清空本地，防止误清与保证删除生效）。 |
| `UsageReportWorker` | `common/cloud/` | 使用时长上报（覆盖式：先删后插，避免重复累加）。 |
| `InstalledAppReporter` | `common/cloud/` | 学生端应用清单上报（仅 `IDENTITY_STUDENT` 上报，家长端只读云端）。 |
| `AppLimitAccessibilityService` | `monitor/service/` | 无障碍服务，仅监听窗口状态变化，调用 `RestrictionEngine.evaluate` 决定是否拦截；`onDestroy` 延迟重启前台服务保活。 |
| `AppLimitForegroundService` | `monitor/service/` | 前台服务，每 2 分钟 `runNow` 兜底同步限制；`onTaskRemoved` 直接重启 + `setAndAllowWhileIdle` 抗省电。 |
| `TimeUtil` | `common/util/` | 时间工具：`todayDayString`、`minuteOfDay`、`isTodayInMask`（星期掩码）、`isMinuteInWindow`（支持跨天/空窗口）。 |
| `ImageBase64` | `common/util/` | 图片压缩编码（最长边 1024、质量 75、NO_WRAP）、解码保存、`saveToGallery` 写入相册（API29+ MediaStore / API26-28 DATA 路径）。 |
| `PinHasher` | `common/util/` | 家长口令 SHA-256 加盐哈希，默认口令 `123456`。 |
| `AppClassifier` | `common/util/` | 应用分类（游戏/学习）识别。 |
| `AppNavGraph` | `navigation/` | 顶层导航，家长/孩子模式切换与路由。 |

## 6. 测试说明

单元测试位于 `app/src/test/java/com/buddy/studyguard/`，共 **47 个用例**，覆盖纯逻辑类：

| 测试类 | 位置 | 用例数 | 覆盖内容 |
|--------|------|--------|----------|
| `TimeUtilTest` | `common/util/` | 28 | 时间计算、星期掩码 `isTodayInMask`（含 dow==7→bit0 边界）、`isMinuteInWindow`（含跨天/空窗口/非法参数）、格式化 |
| `RestrictionEngineTest` | `monitor/engine/` | 14 | MockK mock 4 个 DAO + `mockkObject(TimeUtil)`，覆盖即时锁定、每日时长、禁用时段（跨天/星期掩码/指定应用）的评估顺序与边界 |
| `ImageBase64Test` | `common/util/` | 5 | Robolectric 环境，覆盖正常压缩、大图缩放、空输入、ContentResolver 异常、null 流 |

运行方式：

```powershell
$env:JAVA_HOME = "G:\Android\jdk21"
& "C:\Users\DELL\.gradle\wrapper\dists\gradle-8.9-bin\78qddjpeqn5v6yec3xb8kv9ca\gradle-8.9\bin\gradle.bat" :app:testDebugUnitTest --console=plain
```

说明：
- `RestrictionEngineTest` 通过 MockK 隔离 DAO 与时间，不依赖真实数据库与时钟。
- `ImageBase64Test` 依赖 Android 框架（Bitmap/Base64），使用 Robolectric（`@RunWith(RobolectricTestRunner::class)` + `@Config(sdk=[34])`）在 JVM 上运行。
- 测试依赖在 `gradle/libs.versions.toml` 登记（junit/mockk/robolectric），`app/build.gradle.kts` 中 `testImplementation` 引入，并开启 `testOptions.unitTests.isIncludeAndroidResources`。
