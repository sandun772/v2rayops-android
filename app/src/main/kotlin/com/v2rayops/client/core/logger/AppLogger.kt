package com.v2rayops.client.core.logger

import android.util.Log

object AppLogger {
    private const val TAG = "V2RayOps"

    fun d(tag: String = TAG, message: String) {
        // Safe logging: no sensitive tokens or passwords in logs
        Log.d(tag, message)
    }

    fun i(tag: String = TAG, message: String) {
        Log.i(tag, message)
    }

    fun w(tag: String = TAG, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
    }

    fun e(tag: String = TAG, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
}
