package com.ageet.filelogprovider

import android.content.Context
import android.os.Process
import android.util.Log

abstract class CrashHandler(context: Context, private val defaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler?) : Thread.UncaughtExceptionHandler {
    private val processName: String = context.currentProcessName
    @Volatile private var isCrashing = false

    abstract fun log(record: LogRecord)

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            if (!isCrashing) {
                isCrashing = true
                log(LogRecord(Log.ERROR, CRASH_LOG_TAG, "FATAL EXCEPTION: ${thread.name}\nProcess: $processName, PID: ${Process.myPid()}\n${Log.getStackTraceString(exception)}"))
            }
        } finally {
            defaultUncaughtExceptionHandler?.uncaughtException(thread, exception)
        }
    }

    companion object {
        private const val CRASH_LOG_TAG = "Crash"
    }
}
