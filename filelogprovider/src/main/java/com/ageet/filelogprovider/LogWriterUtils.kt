package com.ageet.filelogprovider

import android.app.ActivityManager
import android.content.Context
import android.os.Process

private val Context.activityManager: ActivityManager get() = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
internal fun Context.getProcessName(pid: Int): String = activityManager.runningAppProcesses.firstOrNull { it.pid == pid }?.processName ?: "Process-$pid"
internal fun Context.getProcessShortName(pid: Int): String = getProcessName(pid).substringAfterLast('.').substringAfterLast(':')
internal val Context.currentProcessName: String get() = getProcessName(Process.myPid())
