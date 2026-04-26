# Process Reporter Android App 代码指南

> 更新：2026-04-26

## 模块概览

- `AppFocusService`：无障碍服务，监听前台窗口变化并解析当前应用可读名称。
- `AppStatusRepository`：保存当前前台应用名称和自定义 emoji，并根据设置启动或停止前台服务。
- `HeartbeatWorker`：后台心跳任务，按间隔上传当前状态。
- `AppForegroundService`：可选前台保活服务。
- `SettingsStore`：DataStore 配置和加密 Token 存储。
- `ReportClient`：HTTP 上报客户端。
- `SetupScreen` / `StatusScreen`：配置、权限状态和日志 UI。

## 前台应用名称获取逻辑

当前实现不再把 `AccessibilityEvent.text` 当成应用名。该字段表示窗口事件文本，可能是 Activity 标题、页面文本、空值或系统返回的其它内容，因此不稳定。

实际流程：

1. `AppFocusService.onAccessibilityEvent()` 只处理 `TYPE_WINDOW_STATE_CHANGED`。
2. 从事件中读取 `event.packageName`。
3. 过滤 `com.android.systemui` 和本应用自身包名。
4. 使用 `PackageManager.getApplicationInfo(packageName, 0).loadLabel(...)` 解析应用显示名。
5. 如果失败，通过 launcher intent 查询对应启动入口并读取 label。
6. 如果仍失败，才回退到包名。
7. 只有解析出非包名的可读名称时才写入内存缓存，避免把一次失败永久缓存成包名。
8. 最终写入 `AppStatusRepository.currentForegroundApp`，由心跳任务读取并上报。

Android 11+ 的包可见性会影响 `PackageManager` 查询结果。项目在 `AndroidManifest.xml` 中声明了：

```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />

<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

如果删除这些声明，大多数第三方应用可能只能上报包名。

## 心跳上报流程

1. 用户在 `SetupScreen` 开启监听。
2. `HeartbeatWorker.schedule(context, interval)` 创建一个延迟执行的 `OneTimeWorkRequest`。
3. Worker 执行时读取监听开关、服务端地址、Token、当前前台应用名称和电量信息。
4. `ReportClient.reportApp()` 发送：

```json
{
  "key": "<API Token>",
  "emoji": "<自定义 emoji>",
  "desc": "<当前前台应用名> | 🔋<电量百分比>%",
  "ttl": 120
}
```

5. 上报完成后，Worker 自调度下一次任务。
6. 如果监听关闭，Worker 直接结束。
7. 如果 URL 或 Token 未配置，Worker 直接结束且不再自重排，避免后台空跑。

## 省电策略

当前版本的后台策略以省电为默认目标：

- 心跳最小间隔 60 秒，最大 300 秒，默认 120 秒。
- 即使旧任务携带旧间隔，Worker 执行时也会强制校正到当前范围。
- WorkManager 约束要求网络可用和 `batteryNotLow`。
- 前台服务只在用户开启且监听开启时启动。
- `AppForegroundService` 返回 `START_NOT_STICKY`，避免被系统杀掉后反复强拉起。
- 配置缺失时停止自调度，等待用户保存配置或重新开启监听。

## 关键接口和数据流

```text
AccessibilityEvent
    -> AppFocusService
    -> PackageManager label resolver
    -> AppStatusRepository.currentForegroundApp
    -> HeartbeatWorker
    -> ReportClient.reportApp()
    -> server /api/report
```

配置流：

```text
SetupScreen
    -> SettingsStore
    -> DataStore / EncryptedSharedPreferences
    -> AppStatusRepository / HeartbeatWorker
```

## 常见问题

| 症状 | 原因 | 解决 |
| --- | --- | --- |
| 大多数应用上报包名 | Android 11+ 包可见性不足，或 APK 未更新 | 确认 manifest 包含查询声明，重新安装并重启无障碍服务 |
| 设置和 Launcher 能显示名称，其它应用不能 | 系统应用和 launcher 天然更容易被查询到 | 使用 `QUERY_ALL_PACKAGES` 和 launcher `<queries>` |
| 后台耗电偏高 | 心跳间隔过短或前台服务常开 | 使用 120 秒以上间隔，低电量约束保持开启，按需关闭前台服务 |
| 未配置服务端仍有后台唤醒 | 旧版本会继续自调度 | 新版本 URL/Token 缺失时不再自重排 |
| 无障碍服务没有更新行为 | 系统仍运行旧服务实例 | 重新安装后关闭再打开无障碍服务 |

## 构建

```bash
./gradlew assembleDebug
./gradlew :app:compileDebugKotlin
```

Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat :app:compileDebugKotlin
```
