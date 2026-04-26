# Process Reporter

Process Reporter 是一个适用于Mix-space状态函数 Android 前台状态上报客户端。它通过无障碍服务监听前台窗口变化，解析当前前台应用的可读名称，并按心跳间隔上传应用状态和电量信息。

## 下载

预编译 APK 可从 [GitHub Releases](https://github.com/Elvish064/Process-Reporter/releases) 下载。

## 快速开始

1. 安装最新 APK。
2. 在系统设置中开启本应用的无障碍服务，用于监听前台窗口变化。
3. 配置服务端地址和 API Token。
4. 开启监听后，应用会按配置的心跳间隔上传状态。
5. 如需提高后台存活率，可在设置页开启前台服务，并按状态页提示关闭电池优化。

## 当前实现

### 前台应用识别

- `AppFocusService` 监听 `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`。
- 无障碍事件只用来获取稳定的 `packageName`，不再使用 `event.text` 作为应用名。
- 应用名通过 `PackageManager` 解析：
  - 优先 `getApplicationInfo(packageName, 0).loadLabel(...)`
  - 再通过 launcher intent 查询 `queryIntentActivities(...)` 兜底
  - 全部失败时才回退到包名
- 解析成功的可读名称会缓存在内存中，降低重复查询开销。
- 会过滤 `com.android.systemui` 和本应用自身，避免系统 UI 或自身窗口误报。

Android 11+ 有包可见性限制，因此 `AndroidManifest.xml` 中声明了 `QUERY_ALL_PACKAGES` 和 launcher `<queries>`，否则多数第三方应用可能只能回退为包名。

### 状态上报

- `HeartbeatWorker` 使用自调度 `OneTimeWorkRequest` 周期上报。
- 默认心跳间隔为 120 秒，可配置范围为 60-300 秒。
- WorkManager 约束要求网络可用，并且设备不处于低电量状态。
- URL 或 Token 未配置时不会继续自重排，避免后台空跑。
- 上报内容包含当前前台应用可读名称和电量百分比。

### 后台保活

- 前台服务是可选项，用于提升后台存活率。
- 前台服务返回 `START_NOT_STICKY`，系统杀掉后不会强制反复拉起，以降低耗电。
- 省电优先的默认策略是：较低频心跳 + 低电量暂停 + 不强粘性保活。

## 功能

| 功能 | 说明 |
| --- | --- |
| 前台应用识别 | 通过无障碍服务获取前台包名，再解析为稳定可读的应用名称 |
| 状态上报 | 按 60-300 秒间隔上传当前应用状态和电量 |
| 电量保护 | 低电量时 WorkManager 不主动执行心跳任务 |
| 可选前台服务 | 提升后台存活率，但默认采用较省电的非粘性策略 |
| 调试日志 | 在应用内查看同步、权限和异常日志 |

## 技术栈

- Kotlin
- Jetpack Compose + Material 3
- AccessibilityService
- WorkManager
- DataStore
- EncryptedSharedPreferences
- OkHttp

## 系统要求

- Android 8.0+ (API 26)
- Android 11+ 设备上需要 manifest 包可见性声明才能稳定解析第三方应用名称

## 项目结构

```text
agents/android-app/
├─ app/
│  ├─ build.gradle.kts
│  └─ src/main/
│     ├─ AndroidManifest.xml
│     ├─ res/xml/accessibility_service_config.xml
│     └─ java/com/monika/dashboard/
│        ├─ DashboardApp.kt
│        ├─ MainActivity.kt
│        ├─ data/
│        │  ├─ SettingsStore.kt
│        │  └─ DebugLog.kt
│        ├─ repository/
│        │  └─ AppStatusRepository.kt
│        ├─ network/
│        │  └─ ReportClient.kt
│        ├─ service/
│        │  ├─ AppFocusService.kt
│        │  ├─ AppForegroundService.kt
│        │  └─ HeartbeatWorker.kt
│        └─ ui/screens/
│           ├─ SetupScreen.kt
│           └─ StatusScreen.kt
├─ BUILD.md
├─ GUIDE.md
├─ build.gradle.kts
├─ gradle/
└─ settings.gradle.kts
```

## 构建

```bash
cd agents/android-app
./gradlew assembleDebug
```

Windows:

```powershell
.\gradlew.bat assembleDebug
```

产物路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

更多信息见 [BUILD.md](BUILD.md) 和 [GUIDE.md](GUIDE.md)。

## 许可证

本项目采用 [MIT License](LISENCE)。
---

## 致谢

[Mix-space](https://github.com/mx-space) - 提供状态函数平台和接口规范，参考使用了其图标素材

[live-dashboard](https://github.com/Monika-Dream/live-dashboard/tree/android-source) - 提供了安卓客户端的初始代码和设计思路，重构了 UI 和同步逻辑。