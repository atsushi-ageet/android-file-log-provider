package com.ageet.filelogprovider

import android.os.Parcel
import android.os.Parcelable
import android.os.Process
import java.util.*

class LogRecord(val priority: Int, val tag: String, val message: String, val pid: Int = Process.myPid(), val tid: Int = Process.myTid(), val date: Date = Date())
