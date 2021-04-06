package com.ageet.filelogprovider

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process

private val Context.activityManager: ActivityManager get() = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
internal fun Context.getProcessName(pid: Int): String = activityManager.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName ?: "Process-$pid"
internal val Context.currentProcessName: String get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Application.getProcessName() else getProcessName(Process.myPid())
