package com.ageet.filelogprovider.demo

import android.content.Context
import com.ageet.filelogprovider.LogFormatter

@Suppress("unused")
class CustomLogFormatter(context: Context) : LogFormatter.Default(context) {
    override fun formatLine(priorityText: String, tag: String, line: String, pid: Int, tid: Int, dateText: String): String {
        return "$dateText $priorityText/$tag($pid): $line"
    }
}
