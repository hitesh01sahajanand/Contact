package com.phonecall.dialcontacts.calldialer.utils

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper

class FlashLightUtils(context: Context) {
    private var cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null
    private var isFlashOn = false
    private val handler = Handler(Looper.getMainLooper())
    private var isBlinking = false

    init {
        try {
            cameraId = cameraManager.cameraIdList[0]
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val blinkRunnable = object : Runnable {
        override fun run() {
            if (isBlinking) {
                toggleFlash()
                handler.postDelayed(this, 500)
            }
        }
    }

    private fun toggleFlash() {
        try {
            cameraId?.let {
                isFlashOn = !isFlashOn
                cameraManager.setTorchMode(it, isFlashOn)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startBlinking() {
        if (!isBlinking) {
            isBlinking = true
            handler.post(blinkRunnable)
        }
    }

    fun stopBlinking() {
        isBlinking = false
        handler.removeCallbacks(blinkRunnable)
        try {
            cameraId?.let {
                cameraManager.setTorchMode(it, false)
                isFlashOn = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private var instance: FlashLightUtils? = null

        fun getInstance(context: Context): FlashLightUtils {
            if (instance == null) {
                instance = FlashLightUtils(context.applicationContext)
            }
            return instance!!
        }
    }
}
