# RustDesk Android APK 编译指南 (Windows)

## 前置要求

### 1. 安装必需的工具

#### 1.1 安装 Flutter
```powershell
# 下载 Flutter SDK
# 访问: https://flutter.dev/docs/get-started/install/windows
# 或直接下载: https://storage.googleapis.com/flutter_infra_release/releases/stable/windows/flutter_windows_3.16.0-stable.zip

# 解压到 C:\flutter
# 添加到 PATH: C:\flutter\bin
```

#### 1.2 安装 Rust
```powershell
# 下载并安装 Rust
# 访问: https://rustup.rs/
# 或直接下载: https://win.rustup.rs/x86_64

# 安装后，添加 Android 目标
rustup target add aarch64-linux-android armv7-linux-androideabi
```

#### 1.3 安装 Android SDK 和 NDK
```powershell
# 方法 1: 通过 Android Studio
# 下载: https://developer.android.com/studio
# 安装后，在 SDK Manager 中安装:
# - Android SDK (API 28+)
# - Android NDK (r23+)
# - CMake
# - Android SDK Platform-Tools

# 方法 2: 通过命令行工具
# 下载 command line tools: https://developer.android.com/studio#command-tools
```

#### 1.4 设置环境变量
```powershell
# 设置 ANDROID_SDK_ROOT
setx ANDROID_SDK_ROOT "C:\Users\YourName\AppData\Local\Android\Sdk"

# 设置 ANDROID_NDK_HOME
setx ANDROID_NDK_HOME "C:\Users\YourName\AppData\Local\Android\Sdk\ndk\25.1.8937393"

# 设置 PATH
setx PATH "%PATH%;%ANDROID_SDK_ROOT%\platform-tools;%ANDROID_SDK_ROOT%\tools"
```

#### 1.5 安装 LLVM (用于 Rust 交叉编译)
```powershell
# 下载: https://github.com/llvm/llvm-project/releases
# 安装后添加到 PATH
```

#### 1.6 安装 vcpkg (RustDesk 依赖)
```powershell
cd C:\
git clone https://github.com/microsoft/vcpkg
cd vcpkg
.\bootstrap-vcpkg.bat
setx VCPKG_ROOT "C:\vcpkg"
```

### 2. 配置 Rust 工具链

创建 `~/.cargo/config.toml` 文件：

```toml
[target.aarch64-linux-android]
ar = "C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk\\ndk\\25.1.8937393\\toolchains\\llvm\\prebuilt\\windows-x86_64\\bin\\llvm-ar.exe"
linker = "C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk\\ndk\\25.1.8937393\\toolchains\\llvm\\prebuilt\\windows-x86_64\\bin\\aarch64-linux-android28-clang.cmd"

[target.armv7-linux-androideabi]
ar = "C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk\\ndk\\25.1.8937393\\toolchains\\llvm\\prebuilt\\windows-x86_64\\bin\\llvm-ar.exe"
linker = "C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk\\ndk\\25.1.8937393\\toolchains\\llvm\\prebuilt\\windows-x86_64\\bin\\armv7a-linux-androideabi28-clang.cmd"
```

## 编译步骤

### 方式 1: 使用 Docker (推荐 - 最简单)

```bash
# 拉取编译镜像
docker pull rustdesk/rustdesk-build-env-android:latest

# 在项目根目录运行
docker run --rm -v ${PWD}:/workspace -w /workspace rustdesk/rustdesk-build-env-android:latest bash -c "cd flutter && ./build_android.sh"

# 编译产物在: flutter/build/app/outputs/flutter-apk/
```

### 方式 2: 本地编译

#### 步骤 1: 编译 Rust 库

```powershell
cd G:\uzhan.net.cn\rustdesk-master

# 编译 arm64 版本
cargo build --release --target aarch64-linux-android --features flutter

# 编译 armv7 版本  
cargo build --release --target armv7-linux-androideabi --features flutter

# 复制到 Flutter 项目
New-Item -ItemType Directory -Force -Path flutter\android\app\src\main\jniLibs\arm64-v8a
New-Item -ItemType Directory -Force -Path flutter\android\app\src\main\jniLibs\armeabi-v7a

Copy-Item target\aarch64-linux-android\release\librustdesk.so flutter\android\app\src\main\jniLibs\arm64-v8a\librustdesk.so
Copy-Item target\armv7-linux-androideabi\release\librustdesk.so flutter\android\app\src\main\jniLibs\armeabi-v7a\librustdesk.so
```

#### 步骤 2: 编译 Flutter APK

```powershell
cd flutter

# 获取 Flutter 依赖
flutter pub get

# 编译 APK (通用版本)
flutter build apk --release

# 或者编译分架构 APK (体积更小)
flutter build apk --split-per-abi --release

# APK 输出路径:
# build/app/outputs/flutter-apk/app-release.apk
# 或
# build/app/outputs/flutter-apk/app-arm64-v8a-release.apk
# build/app/outputs/flutter-apk/app-armeabi-v7a-release.apk
```

### 方式 3: 使用 Python 构建脚本

```powershell
cd G:\uzhan.net.cn\rustdesk-master

# 安装 Python 3
# 下载: https://www.python.org/downloads/

# 运行构建脚本
python build.py --flutter --android
```

## 验证编译环境

```powershell
# 检查 Flutter
flutter doctor

# 检查 Rust
rustc --version
cargo --version

# 检查 Android 工具
adb --version
```

## 常见问题

### 1. Flutter 找不到 Android SDK
```powershell
flutter config --android-sdk C:\Users\YourName\AppData\Local\Android\Sdk
```

### 2. NDK 版本问题
确保使用 NDK r23+ 版本，建议 r25

### 3. 编译失败 "linker not found"
检查 `~/.cargo/config.toml` 中的路径是否正确

### 4. 内存不足
```powershell
# 增加 Gradle 内存
# 编辑 flutter/android/gradle.properties
org.gradle.jvmargs=-Xmx4096m
```

### 5. 签名问题
```powershell
# 生成签名密钥
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias

# 配置签名
# 创建 flutter/android/key.properties
storePassword=your_password
keyPassword=your_password
keyAlias=my-key-alias
storeFile=path/to/my-release-key.jks
```

## 快速编译命令 (Windows PowerShell)

```powershell
# 完整编译流程
cd G:\uzhan.net.cn\rustdesk-master

# 1. 编译 Rust 库
cargo build --release --target aarch64-linux-android --features flutter

# 2. 复制到 Flutter
New-Item -ItemType Directory -Force -Path flutter\android\app\src\main\jniLibs\arm64-v8a
Copy-Item target\aarch64-linux-android\release\librustdesk.so flutter\android\app\src\main\jniLibs\arm64-v8a\

# 3. 编译 APK
cd flutter
flutter pub get
flutter build apk --target-platform android-arm64 --release

# APK 位置: flutter\build\app\outputs\flutter-apk\app-release.apk
```

## 调试版本编译

```powershell
cd flutter
flutter build apk --debug
flutter install  # 安装到连接的设备
```

## 注意事项

1. **首次编译时间**: 首次编译可能需要 30-60 分钟
2. **磁盘空间**: 确保至少有 20GB 可用空间
3. **网络**: 需要良好的网络下载依赖
4. **权限**: Windows Defender 可能会阻止某些操作，需要添加例外

## 输出文件说明

- `app-release.apk`: 通用版本 (包含所有架构，体积较大)
- `app-arm64-v8a-release.apk`: 64位 ARM 设备 (推荐)
- `app-armeabi-v7a-release.apk`: 32位 ARM 设备

推荐使用 arm64-v8a 版本，适配大多数现代 Android 设备。
