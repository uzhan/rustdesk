@echo off
echo ================================================
echo RustDesk Android 快速编译脚本
echo ================================================
echo.

REM 检查必需工具
echo [1/5] 检查编译环境...

where flutter >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [错误] Flutter 未安装或不在 PATH 中
    echo 请访问: https://flutter.dev/docs/get-started/install/windows
    echo 然后重新运行此脚本
    pause
    exit /b 1
)

where cargo >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [错误] Rust 未安装或不在 PATH 中
    echo 请访问: https://rustup.rs/
    echo 然后重新运行此脚本
    pause
    exit /b 1
)

if not defined ANDROID_NDK_HOME (
    echo [错误] ANDROID_NDK_HOME 环境变量未设置
    echo 请安装 Android NDK 并设置环境变量
    pause
    exit /b 1
)

echo [通过] 编译环境检查完成
echo.

REM 添加 Android 目标
echo [2/5] 添加 Rust Android 目标...
rustup target add aarch64-linux-android armv7-linux-androideabi
echo.

REM 编译 Rust 库
echo [3/5] 编译 Rust 库 (这可能需要 20-30 分钟)...
echo 正在编译 arm64 版本...
cargo build --release --target aarch64-linux-android --features flutter
if %ERRORLEVEL% NEQ 0 (
    echo [错误] arm64 编译失败
    pause
    exit /b 1
)

echo 正在编译 armv7 版本...
cargo build --release --target armv7-linux-androideabi --features flutter
if %ERRORLEVEL% NEQ 0 (
    echo [错误] armv7 编译失败
    pause
    exit /b 1
)
echo.

REM 复制库文件
echo [4/5] 复制库文件到 Flutter 项目...
if not exist "flutter\android\app\src\main\jniLibs\arm64-v8a" mkdir "flutter\android\app\src\main\jniLibs\arm64-v8a"
if not exist "flutter\android\app\src\main\jniLibs\armeabi-v7a" mkdir "flutter\android\app\src\main\jniLibs\armeabi-v7a"

copy /Y "target\aarch64-linux-android\release\librustdesk.so" "flutter\android\app\src\main\jniLibs\arm64-v8a\librustdesk.so"
copy /Y "target\armv7-linux-androideabi\release\librustdesk.so" "flutter\android\app\src\main\jniLibs\armeabi-v7a\librustdesk.so"
echo.

REM 编译 Flutter APK
echo [5/5] 编译 Flutter APK...
cd flutter
call flutter pub get
call flutter build apk --split-per-abi --release
if %ERRORLEVEL% NEQ 0 (
    echo [错误] Flutter APK 编译失败
    cd ..
    pause
    exit /b 1
)
cd ..
echo.

echo ================================================
echo 编译完成！
echo ================================================
echo APK 文件位置:
echo   - flutter\build\app\outputs\flutter-apk\app-arm64-v8a-release.apk
echo   - flutter\build\app\outputs\flutter-apk\app-armeabi-v7a-release.apk
echo.
echo 推荐使用 arm64-v8a 版本
echo ================================================
pause
