package com.ageet.filelogprovider

import android.content.Context
import android.os.Process
import android.util.Log

class LogWriter(private val strategy: LogStrategy) {

    var priority: Int = PRIORITY_NONE

    constructor(context: Context, formatter: LogFormatter = LogFormatter.Default(context)) : this(strategy = LogStrategy.RollingFile(context = context, formatter = formatter))

    private fun checkPriority(priority: Int) = this.priority <= priority

    fun crashOnly() {
        priority = PRIORITY_CRASH
    }

    fun disable() {
        priority = PRIORITY_NONE
    }

    fun printLog(record: LogRecord) {
        if (checkPriority(record.priority)) {
            strategy.printLog(record)
        }
    }

    fun printLog(records: List<LogRecord>) {
        strategy.printLog(records.filter { checkPriority(it.priority) })
    }

    internal abstract class CrashHandler(context: Context, private val defaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler?) : Thread.UncaughtExceptionHandler {
        private val processName: String = context.currentProcessName
        @Volatile private var isCrashing = false

        abstract fun log(record: LogRecord)

        override fun uncaughtException(thread: Thread, exception: Throwable) {
            try {
                if (!isCrashing) {
                    isCrashing = true
                    log(LogRecord(PRIORITY_CRASH, CRASH_LOG_TAG, "FATAL EXCEPTION: ${thread.name}\nProcess: $processName, PID: ${Process.myPid()}\n${Log.getStackTraceString(exception)}"))
                }
            } finally {
                defaultUncaughtExceptionHandler?.uncaughtException(thread, exception)
            }
        }
    }

    private var isInstalledCrashHandler = false

    fun installCrashHandler(context: Context) {
        if (!isInstalledCrashHandler) {
            Thread.setDefaultUncaughtExceptionHandler(object : CrashHandler(context, Thread.getDefaultUncaughtExceptionHandler()) {
                override fun log(record: LogRecord) {
                    printLog(record)
                }
            })
            isInstalledCrashHandler = true
        }
    }

    companion object {
        private const val CRASH_LOG_TAG = "Crash"
        internal const val PRIORITY_CRASH = 8
        internal const val PRIORITY_NONE = 9
    }
}
