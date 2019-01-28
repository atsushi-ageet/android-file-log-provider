package com.ageet.filelogprovider

import android.content.Context
import android.util.Log
import java.io.*

abstract class LogStrategy(val formatter: LogFormatter) {

    abstract val logFileList: List<File>

    open fun printLog(record: LogRecord) {
        printLog(formatter.header(), formatter.format(record))
    }
    open fun printLog(records: List<LogRecord>) {
        printLog(formatter.header(), formatter.format(records))
    }
    abstract fun printLog(header: String, formattedLog: String)

    class RollingFile(context: Context,
                      formatter: LogFormatter,
                      internal val maxLogFileSize: Long = DEFAULT_MAX_LOG_FILE_SIZE,
                      internal val maxLogFileBackup: Int = DEFAULT_MAX_LOG_FILE_BACKUP,
                      internal val logFileDir: File = getDefaultLogDir(context),
                      internal val logFileBaseName: String = DEFAULT_LOG_FILE_BASE_NAME,
                      internal val logFileExt: String = DEFAULT_LOG_FILE_EXT) : LogStrategy(formatter) {

        private val logFile: File by lazy { getIndexedLogFile(0) }
        private val logFileNameList: List<String> get() = logFileDir.list { _, fileName ->
            fileName.matches("$logFileBaseName(_\\d+)?\\.$logFileExt".toRegex())
        }.toList()
        override val logFileList: List<File> get() = logFileNameList.map { File(logFileDir, it) }

        override fun printLog(header: String, formattedLog: String) {
            rotateIfNeeded()
            if (!printLogWithResult(logFile, header, formattedLog)) {
                retryPrintLog(logFile, header, formattedLog)
            }
        }

        private fun printLogWithResult(file: File, header: String, formattedLog: String): Boolean = try {
            PrintWriter(BufferedWriter(OutputStreamWriter(FileOutputStream(file, true), CHARSET))).use { printWriter ->
                if (file.length() == 0L) {
                    printWriter.println(header)
                }
                printWriter.println(formattedLog)
                !printWriter.checkError()
            }
        } catch (e: IOException) {
            Log.w(LOG_TAG, "printLogWithResult() ${Log.getStackTraceString(e)}")
            false
        }

        private fun retryPrintLog(file: File, header: String, formattedLog: String) {
            (maxLogFileBackup downTo 0).forEach { i ->
                val fileToDelete = getIndexedLogFile(i)
                if (fileToDelete.deleteIfExists()) {
                    Log.d(LOG_TAG, "retryPrintLog() Retry after delete file($fileToDelete)")
                    if (printLogWithResult(file, header, formattedLog)) {
                        Log.d(LOG_TAG, "retryPrintLog() Success")
                        return
                    } else {
                        Log.w(LOG_TAG, "retryPrintLog() Failed")
                    }
                }
            }
        }

        private fun File.deleteIfExists() = if (exists()) {
            delete().also {
                if (!it) Log.w(LOG_TAG, "deleteIfExists() Could not delete file ($this)")
            }
        } else {
            false
        }

        private fun rotateIfNeeded() {
            if (logFile.length() > maxLogFileSize) {
                rotate()
            }
        }

        private fun rotate() {
            Log.d(LOG_TAG, "rotate()")
            deleteUnnecessaryFiles()
            if (maxLogFileBackup <= 0) {
                logFile.deleteIfExists()
                return
            }
            for (i in maxLogFileBackup - 1 downTo 0) {
                var targetFile = getIndexedLogFile(i)
                if (targetFile.exists()) {
                    val nextFile = getIndexedLogFile(i + 1)
                    nextFile.deleteIfExists()
                    targetFile.renameTo(nextFile)
                } else {
                    for (j in i + 2..maxLogFileBackup) {
                        targetFile = getIndexedLogFile(j)
                        if (targetFile.exists()) {
                            targetFile.renameTo(getIndexedLogFile(j - 1))
                        } else {
                            break
                        }
                    }
                }
            }
        }

        private fun deleteUnnecessaryFiles() {
            logFileNameList.mapNotNull { it.removePrefix("${logFileBaseName}_").removeSuffix(".$logFileExt").toIntOrNull() }
                    .filter { it > maxLogFileBackup }
                    .map { getIndexedLogFile(it) }
                    .forEach { unnecessaryFile ->
                        Log.d(LOG_TAG, "Remove unnecessaryFile($unnecessaryFile)")
                        unnecessaryFile.deleteIfExists()
                    }
        }

        private fun getIndexedLogFile(index: Int) = if (index > 0) {
            File(logFileDir, "${logFileBaseName}_$index.$logFileExt")
        } else {
            File(logFileDir, "$logFileBaseName.$logFileExt")
        }

        companion object {
            private const val LOG_TAG = "RollingFile"
            private const val CHARSET = "UTF-8"
            internal const val DEFAULT_MAX_LOG_FILE_SIZE = 5 * 1024 * 1024.toLong()
            internal const val DEFAULT_MAX_LOG_FILE_BACKUP = 10
            internal const val DEFAULT_LOG_FILE_DIR_NAME = "log"
            internal const val DEFAULT_LOG_FILE_BASE_NAME = "application"
            internal const val DEFAULT_LOG_FILE_EXT = "log"

            internal fun getDefaultLogDir(context: Context) = context.getExternalFilesDir(DEFAULT_LOG_FILE_DIR_NAME)
        }
    }
}
