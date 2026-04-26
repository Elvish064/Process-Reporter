# Build Guide

## 环境要求

- JDK 17+
- Android SDK，项目当前 `compileSdk = 36`，`minSdk = 26`
- 使用项目自带 Gradle wrapper
- 推荐 Android Studio Hedgehog 或更新版本

## 构建 Debug APK

在项目根目录执行：

```bash
./gradlew assembleDebug
```

Windows:

```powershell
.\gradlew.bat assembleDebug
```

APK 输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 安装到设备

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

如果已经安装旧版本，建议重新打开一次无障碍服务，让 `AppFocusService` 使用新逻辑重新启动。

## 构建检查

只检查 Kotlin 编译：

```bash
./gradlew :app:compileDebugKotlin
```

Windows:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

## 运行前配置

1. 安装 APK。
2. 在系统设置中开启 Process Reporter 的无障碍服务。
3. 在应用内配置服务端地址和 API Token。
4. 开启监听。
5. 如需要更强后台存活，可开启前台服务；如优先省电，可保持关闭。

## 与当前实现相关的权限

- `BIND_ACCESSIBILITY_SERVICE`：声明无障碍服务，由系统授权绑定。
- `QUERY_ALL_PACKAGES`：Android 11+ 上用于稳定解析第三方应用可读名称。
- launcher `<queries>`：允许查询可启动应用，作为应用 label 解析兜底。
- `FOREGROUND_SERVICE` 和 `FOREGROUND_SERVICE_SPECIAL_USE`：用于可选前台保活服务。
- `POST_NOTIFICATIONS`：Android 13+ 前台服务通知需要。
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`：引导用户关闭电池优化。

## 省电策略

当前版本默认偏省电：

- 心跳范围为 60-300 秒，默认 120 秒。
- WorkManager 要求网络可用且电量不低。
- URL 或 Token 缺失时不继续自调度。
- 前台服务使用 `START_NOT_STICKY`，不会被系统杀掉后强制反复拉起。

## 常见问题

| 问题 | 处理方式 |
| --- | --- |
| 上报仍是包名 | 确认已安装新 APK，并重新开关一次无障碍服务 |
| 第三方应用无法解析名称 | 检查 manifest 是否包含 `QUERY_ALL_PACKAGES` 和 launcher `<queries>` |
| 心跳不立刻执行 | WorkManager 会受网络、电量和系统调度影响，这是省电策略的一部分 |
| 后台被系统杀掉 | 开启前台服务，并按状态页提示关闭电池优化 |

更多架构说明见 [GUIDE.md](GUIDE.md)。
