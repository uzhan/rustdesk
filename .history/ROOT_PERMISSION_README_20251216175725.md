# RustDesk Root 权限支持说明

## 功能概述

为 RustDesk Android 客户端添加了 root 权限支持，可以绕过每次建立连接时的 MediaProjection 权限弹窗。

## 实现原理

通过使用 root 权限执行 `appops` 命令来授予应用 `PROJECT_MEDIA` 权限，这样在请求 MediaProjection 时就不会显示权限对话框。

### 主要修改文件

1. **RootHelper.kt** (新增)
   - 检测设备是否已 root
   - 检查 su 权限是否可用
   - 使用 root 权限授予 MediaProjection 权限
   - 执行 root 命令的工具方法

2. **MainService.kt**
   - 添加 `tryRequestMediaProjectionWithRoot()` 方法
   - 修改 `onStartCommand()` 支持 root 模式启动

3. **MainActivity.kt**
   - 添加 `init_service_root` Flutter 方法通道
   - 添加 `check_root` 和 `check_su_permission` 检查方法
   - 添加 `requestMediaProjectionWithRoot()` 方法

4. **PermissionRequestTransparentActivity.kt**
   - 添加 root 模式支持
   - 在 root 模式下先尝试授予权限再请求

5. **BootReceiver.kt**
   - 支持从开机启动时使用 root 模式

6. **common.kt**
   - 添加 `EXT_USE_ROOT_MODE` 常量

## 使用方法

### 1. 授予应用 Root 权限

首先需要确保：
- 设备已 root（已安装 Magisk 或 SuperSU 等）
- RustDesk 应用已获得 su 权限

### 2. 在 Flutter 层启用 Root 模式

在 Flutter 代码中，可以通过以下方法使用 root 模式：

```dart
// 检查设备是否已 root
bool isRooted = await platform.invokeMethod('check_root');

// 检查 su 权限是否可用
bool hasSuPermission = await platform.invokeMethod('check_su_permission');

// 使用 root 模式初始化服务（不会显示权限弹窗）
if (isRooted && hasSuPermission) {
  await platform.invokeMethod('init_service_root');
} else {
  // 回退到正常模式
  await platform.invokeMethod('init_service');
}
```

### 3. 设置开机启动使用 Root 模式

在 SharedPreferences 中设置 `KEY_USE_ROOT_MODE` 为 true：

```kotlin
val prefs = context.getSharedPreferences(KEY_SHARED_PREFERENCES, Context.MODE_PRIVATE)
prefs.edit().putBoolean("KEY_USE_ROOT_MODE", true).apply()
```

## 技术细节

### Root 命令执行

使用以下命令授予权限：
```bash
appops set <package_name> PROJECT_MEDIA allow
```

### 验证权限

检查权限是否已授予：
```bash
appops get <package_name> PROJECT_MEDIA
```

### 安全性考虑

1. **权限检查**：每次使用前都会检查 root 和 su 权限是否可用
2. **降级策略**：如果 root 模式失败，自动回退到正常的权限请求流程
3. **用户控制**：需要用户主动授予应用 su 权限

## 注意事项

1. **兼容性**：
   - 需要 Android 5.0 (API 21) 及以上版本
   - 不同 ROM 可能对 appops 命令支持不同

2. **Root 风险**：
   - 使用 root 权限会增加安全风险
   - 建议只在受信任的环境中使用

3. **权限持久性**：
   - 通过 appops 授予的权限可能在系统重启后失效
   - 建议配合开机启动功能使用

4. **首次使用**：
   - 首次使用 root 模式时，SuperSU/Magisk 会弹出授权对话框
   - 用户需要授予 RustDesk 永久 root 权限

## 编译说明

直接使用标准的 RustDesk Android 编译流程即可：

```bash
cd flutter
./build_android.sh
```

或者使用 Flutter 命令：

```bash
flutter build apk --release
```

## 测试步骤

1. 确保设备已 root
2. 安装编译后的 APK
3. 首次运行时授予 su 权限
4. 使用 root 模式启动服务
5. 验证不会显示 MediaProjection 权限对话框

## 故障排除

### 问题：root 检测失败
- 检查设备是否真正 root
- 确认 su 二进制文件路径是否正确

### 问题：授权失败
- 检查日志中的错误信息
- 尝试手动执行 appops 命令验证
- 确认应用已获得 su 权限

### 问题：权限仍然弹窗
- 验证 appops 命令是否执行成功
- 检查 `EXT_USE_ROOT_MODE` 参数是否正确传递
- 查看日志确认是否进入 root 模式

## 日志调试

关键日志标签：
- `RootHelper`: Root 权限相关操作
- `permissionRequest`: 权限请求流程
- `mMainActivity`: MainActivity 相关
- `whichService`: MainService 相关

使用 adb 查看日志：
```bash
adb logcat | grep -E "RootHelper|permissionRequest|mMainActivity"
```

## 免责声明

使用 root 权限可能会：
- 影响设备安全性
- 违反某些应用或服务的使用条款
- 导致设备保修失效

请在充分理解风险的情况下使用此功能。
