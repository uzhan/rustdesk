package com.carriez.flutter_hbb

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * Root权限辅助类
 * 用于检测和使用root权限来绕过MediaProjection权限弹窗
 */
object RootHelper {
    private const val TAG = "RootHelper"
    
    /**
     * 检查设备是否已root
     */
    fun isDeviceRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
    }
    
    /**
     * 检查su命令是否可用
     */
    private fun checkRootMethod1(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            if (java.io.File(path).exists()) {
                return true
            }
        }
        return false
    }
    
    /**
     * 尝试执行su命令
     */
    private fun checkRootMethod2(): Boolean {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val input = BufferedReader(InputStreamReader(process.inputStream))
            return input.readLine() != null
        } catch (e: Exception) {
            return false
        } finally {
            process?.destroy()
        }
    }
    
    /**
     * 检查Build Tags
     */
    private fun checkRootMethod3(): Boolean {
        val buildTags = android.os.Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
    
    /**
     * 检查su权限是否可用（实际执行su命令）
     */
    fun checkSuPermission(): Boolean {
        return try {
            val result = execRootCommand("id")
            result.contains("uid=0") || result.contains("root")
        } catch (e: Exception) {
            Log.e(TAG, "checkSuPermission failed: ${e.message}")
            false
        }
    }
    
    /**
     * 执行root命令
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    fun execRootCommand(command: String): String {
        var process: Process? = null
        var outputStream: DataOutputStream? = null
        var inputStream: BufferedReader? = null
        val output = StringBuilder()
        
        try {
            process = Runtime.getRuntime().exec("su")
            outputStream = DataOutputStream(process.outputStream)
            inputStream = BufferedReader(InputStreamReader(process.inputStream))
            
            // 写入命令
            outputStream.writeBytes("$command\n")
            outputStream.flush()
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            
            // 读取输出
            var line: String?
            while (inputStream.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            process.waitFor()
            
        } catch (e: Exception) {
            Log.e(TAG, "execRootCommand error: ${e.message}")
            throw e
        } finally {
            try {
                outputStream?.close()
                inputStream?.close()
                process?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Close stream error: ${e.message}")
            }
        }
        
        return output.toString()
    }
    
    /**
     * 使用root权限授予MediaProjection权限
     * 通过执行appops命令来授予PROJECT_MEDIA权限
     * @param packageName 应用包名
     * @return 是否成功
     */
    fun grantMediaProjectionPermission(packageName: String): Boolean {
        return try {
            // 使用appops授予PROJECT_MEDIA权限
            val command = "appops set $packageName PROJECT_MEDIA allow"
            val result = execRootCommand(command)
            Log.d(TAG, "Grant media projection permission result: $result")
            
            // 验证权限是否已授予
            val checkCommand = "appops get $packageName PROJECT_MEDIA"
            val checkResult = execRootCommand(checkCommand)
            Log.d(TAG, "Check permission result: $checkResult")
            
            checkResult.contains("allow") || checkResult.contains("No operations")
        } catch (e: Exception) {
            Log.e(TAG, "grantMediaProjectionPermission failed: ${e.message}")
            false
        }
    }
    
    /**
     * 启动截屏服务而不显示权限对话框
     * 注意：这个方法需要设备已经授予了root权限
     */
    fun startScreenCaptureWithRoot(packageName: String): Boolean {
        if (!checkSuPermission()) {
            Log.w(TAG, "No root permission available")
            return false
        }
        
        if (!grantMediaProjectionPermission(packageName)) {
            Log.w(TAG, "Failed to grant media projection permission")
            return false
        }
        
        Log.d(TAG, "Screen capture permission granted via root")
        return true
    }
}
