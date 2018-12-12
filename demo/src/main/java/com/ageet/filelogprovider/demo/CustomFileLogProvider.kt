package com.ageet.filelogprovider.demo

import com.ageet.filelogprovider.FileLogProvider
import com.ageet.filelogprovider.LogFormatter

class CustomFileLogProvider : FileLogProvider() {
    override fun createLogFormatter(): LogFormatter = CustomLogFormatter(context)
}
