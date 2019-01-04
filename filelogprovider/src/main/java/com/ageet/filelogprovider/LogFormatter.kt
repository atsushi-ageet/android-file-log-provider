package com.ageet.filelogprovider

import android.content.Context
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

interface LogFormatter {
    fun header(): String
    fun format(record: LogRecord): String
    fun format(records: List<LogRecord>): String

    open class Default(val context: Context) : LogFormatter {
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
            return record.message.split('\n').joinToString(separator = "\n") { line ->
                formatLine(priorityText, record.tag, line, record.pid, record.tid, dateText)
            }
        }

        override fun format(records: List<LogRecord>): String {
            return records.joinToString(separator = "\n") { format(it) }
        }

        open fun formatLine(priorityText: String, tag: String, line: String, pid: Int, tid: Int, dateText: String): String {
            return "$dateText $pid-$tid $priorityText/$tag: $line"
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
