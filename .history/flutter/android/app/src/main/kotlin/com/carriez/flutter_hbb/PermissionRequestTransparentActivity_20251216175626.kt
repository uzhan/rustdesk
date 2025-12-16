package com.carriez.flutter_hbb

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log

class PermissionRequestTransparentActivity: Activity() {
    private val logTag = "permissionRequest"
    private var useRootMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(logTag, "onCreate PermissionRequestTransparentActivity: intent.action: ${intent.action}")

        useRootMode = intent.getBooleanExtra(EXT_USE_ROOT_MODE, false)
        
        when (intent.action) {
            ACT_REQUEST_MEDIA_PROJECTION -> {
                if (useRootMode) {
                    // 尝试使用root权限授予MediaProjection权限
                    tryGrantPermissionWithRoot()
                } else {
                    // 正常流程，显示权限对话框
                    requestMediaProjectionPermission()
                }
            }
            else -> finish()
        }
    }

    private fun requestMediaProjectionPermission() {
        val mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(intent, REQ_REQUEST_MEDIA_PROJECTION)
    }

    private fun tryGrantPermissionWithRoot() {
        try {
            // 检查root权限
            if (!RootHelper.isDeviceRooted()) {
                Log.w(logTag, "Device not rooted, fallback to normal mode")
                requestMediaProjectionPermission()
                return
            }
            
            if (!RootHelper.checkSuPermission()) {
                Log.w(logTag, "No su permission, fallback to normal mode")
                requestMediaProjectionPermission()
                return
            }
            
            // 使用root权限授予MediaProjection权限
            val packageName = applicationContext.packageName
            if (RootHelper.startScreenCaptureWithRoot(packageName)) {
                Log.d(logTag, "Successfully granted permission via root")
                // 权限已通过root授予，现在仍需要创建MediaProjection对象
                // 但此时应该不会显示对话框
                requestMediaProjectionPermission()
            } else {
                Log.w(logTag, "Root permission grant failed, fallback to normal mode")
                requestMediaProjectionPermission()
            }
        } catch (e: Exception) {
            Log.e(logTag, "Root mode error: ${e.message}, fallback to normal mode")
            requestMediaProjectionPermission()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                launchService(data)
            } else {
                setResult(RES_FAILED)
            }
        }

        finish()
    }

    private fun launchService(mediaProjectionResultIntent: Intent) {
        Log.d(logTag, "Launch MainService")
        val serviceIntent = Intent(this, MainService::class.java)
        serviceIntent.action = ACT_INIT_MEDIA_PROJECTION_AND_SERVICE
        serviceIntent.putExtra(EXT_MEDIA_PROJECTION_RES_INTENT, mediaProjectionResultIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

}