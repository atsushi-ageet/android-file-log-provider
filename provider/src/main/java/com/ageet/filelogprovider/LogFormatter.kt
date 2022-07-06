package com.ageet.filelogprovider

import android.content.Context
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

abstract class  LogFormatter(val context: Context) {
    abstract fun header(): String
    abstract fun format(record: LogRecord): String
    abstract fun format(records: List<LogRecord>): String

    open class Default(context: Context) : LogFormatter(context) {
        private val header: String by lazy {
            val packageManager = context.packageManager
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "AppName: ${context.applicationInfo.loadLabel(packageManager)}\n" +
                    "VersionCode: ${packageInfo.versionCode}\n" +
                    "VersionName: ${packageInfo.versionName}\n" +
                    "OS Version: ${Build.VERSION.RELEASE}\n" +
                    "Device: ${Build.DEVICE}\n" +
                    "Model: ${Build.MODEL}\n" +
                    "Product: ${Build.PRODUCT}\n"
        }

        override fun header(): String = header

        override fun format(record: LogRecord): String {
            val dateText = formatDate(record.date)
            val priorityText = formatPriority(record.priority)
            return record.message.lineSequence().joinToString(prefix = formatPrefix(priorityText, record.tag, record.pid, record.tid, dateText), separator = lineSeparator)
        }

        open val lineSeparator: String = "\n    "

        override fun format(records: List<LogRecord>): String {
            return records.joinToString(separator = "\n") { format(it) }
        }

        open fun formatPrefix(priorityText: String, tag: String, pid: Int, tid: Int, dateText: String): String {
            return "$dateText $pid-$tid $priorityText/$tag: "
        }

        open fun formatDate(date: Date): String = DATE_FORMAT.format(date)

        open fun formatPriority(priority: Int) = when (priority) {
            Log.ASSERT -> "A"
            Log.ERROR -> "E"
            Log.WARN -> "W"
            Log.INFO -> "I"
            Log.DEBUG -> "D"
            Log.VERBOSE -> "V"
            else -> "E"
        }

        companion object {
            private val DATE_FORMAT = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
        }
    }
}
