package com.ageet.filelogprovider.demo

import android.app.Application
import android.content.Context
import com.ageet.filelogprovider.FileLogProvider
import timber.log.Timber

class DemoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree(), FileTree(this))
    }

    class FileTree(val context: Context) : Timber.DebugTree() {
        init {
            FileLogProvider.initialize(context)
        }
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            FileLogProvider.postLog(context, priority, tag.orEmpty(), message)
        }
    }
}
