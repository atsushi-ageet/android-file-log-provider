package com.ageet.filelogprovider

import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.database.ContentObserver
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.*
import android.util.Log
import java.io.File
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

open class FileLogProvider : ContentProvider() {
    private val sharedPreferences: SharedPreferences get() = context.getSharedPreferences("FileLogProvider", Context.MODE_PRIVATE)
    private lateinit var logWriter: LogWriter
    private lateinit var rollingFile: LogStrategy.RollingFile
    private val logFormatter: LogFormatter by lazy { createLogFormatter() }

    open fun createLogFormatter(): LogFormatter = LogFormatter.Default(context)

    private fun getInternalFilesDir(path: String) = File(context.filesDir, path).also { file ->
        if (!file.exists()) file.mkdirs()
    }

    private fun getLogFileDirPath(path: String): File = when {
        path.startsWith(PREFIX_EXTERNAL_FILES) -> requireNotNull(context.getExternalFilesDir(path.removePrefix(PREFIX_EXTERNAL_FILES)))
        path.isNotBlank() -> getInternalFilesDir(path)
        else -> LogStrategy.RollingFile.getDefaultLogDir(context)
    }

    override fun onCreate(): Boolean {
        val metaData = getMetaData(context)
        val initialPriority = metaData.getInt(MetaData.INITIAL_PRIORITY, LogWriter.PRIORITY_NONE)
        val maxLogFileSize = metaData[MetaData.MAX_LOG_FILE_SIZE_IN_MB]?.let { it as? Float }?.let { it * 1024 * 1024 }?.toLong() ?: LogStrategy.RollingFile.DEFAULT_MAX_LOG_FILE_SIZE
        val maxLogFileBackup = metaData.getInt(MetaData.MAX_LOG_FILE_BACKUP, LogStrategy.RollingFile.DEFAULT_MAX_LOG_FILE_BACKUP)
        val logFileDir = getLogFileDirPath(metaData.getString(MetaData.LOG_FILE_DIR, ""))
        val logFileBaseName = metaData.getString(MetaData.LOG_FILE_BASE_NAME, LogStrategy.RollingFile.DEFAULT_LOG_FILE_BASE_NAME)
        val logFileExt = metaData.getString(MetaData.LOG_FILE_EXT, LogStrategy.RollingFile.DEFAULT_LOG_FILE_EXT)
        rollingFile = LogStrategy.RollingFile(
                context = context,
                formatter = logFormatter,
                maxLogFileSize = maxLogFileSize,
                maxLogFileBackup = maxLogFileBackup,
                logFileDir = logFileDir,
                logFileBaseName = logFileBaseName,
                logFileExt = logFileExt)
        logWriter = LogWriter(rollingFile)
        logWriter.installCrashHandler(context)
        logWriter.priority = sharedPreferences.getInt(Column.PRIORITY, initialPriority)
        Log.i(LOG_TAG,  "Initialize file log provider("
                    + "processName = ${getProviderInfo(context).processName}, "
                    + "initialPriority = $initialPriority, "
                    + "currentPriority = ${logWriter.priority}, "
                    + "maxLogFileSize = $maxLogFileSize, "
                    + "maxLogFileBackup = $maxLogFileBackup, "
                    + "logFileBaseName = $logFileBaseName, "
                    + "logFileExt = $logFileExt"
                    + ")"
        )
        return true
    }

    private fun ContentValues.toLogRecord(): LogRecord = LogRecord(
            priority = getAsInteger(Column.PRIORITY),
            tag = getAsString(Column.TAG),
            message = getAsString(Column.MESSAGE),
            pid = getAsInteger(Column.PID),
            tid = getAsInteger(Column.TID),
            date = Date(getAsLong(Column.DATE))
    )

    override fun insert(uri: Uri?, values: ContentValues?): Uri? {
        if ( uri == null || values == null ) {
            return null
        }
        logWriter.printLog(values.toLogRecord())
        return null
    }

    override fun bulkInsert(uri: Uri?, values: Array<out ContentValues>?): Int {
        if ( uri == null || values == null ) {
            return 0
        }
        val records = values.map { it.toLogRecord() }
        logWriter.printLog(records)
        return records.size
    }

    override fun update(uri: Uri?, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        if (uri == null || values == null) {
            return 0
        }
        when (matcher.match(uri)) {
            Path.PRIORITY.code -> {
                val priority = values.getAsInteger(Column.PRIORITY)
                Log.i(LOG_TAG, "Update priority to $priority")
                logWriter.priority = priority
                sharedPreferences.edit().putInt(Column.PRIORITY, priority).apply()
                context.contentResolver.notifyChange(Path.PRIORITY.getContentUri(context), null)
            }
        }
        return 1
    }

    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        if (uri == null) {
            return null
        }
        return when (matcher.match(uri)) {
            Path.PRIORITY.code -> {
                MatrixCursor(arrayOf(Column.PRIORITY), 1).also { cursor ->
                    cursor.addRow(arrayOf(logWriter.priority))
                }
            }
            Path.FILES.code -> {
                val files = rollingFile.logFileList
                MatrixCursor(arrayOf(Column.FILE), files.size).also { cursor ->
                    files.forEach {
                        cursor.addRow(arrayOf(it.absolutePath))
                    }
                }
            }
            else -> null
        }
    }

    override fun getType(uri: Uri?): String? = null
    override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException("unsupported")

    private val matcher: UriMatcher by lazy { UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(Path.PRIORITY)
        addURI(Path.FILES)
    }}

    private fun UriMatcher.addURI(paths: Path) = addURI(getAuthority(context), paths.path, paths.code)

    private enum class Path(val path: String, val code: Int) {
        PRIORITY("priority", 1),
        FILES("files", 2);
        fun getContentUri(context: Context) = getContentUri(context, path)
    }

    private object Column {
        const val PRIORITY: String = "priority"
        const val TAG: String = "tag"
        const val DATE: String = "date"
        const val PID: String = "pid"
        const val TID: String = "tid"
        const val MESSAGE: String = "message"
        const val FILE: String = "file"
    }

    private object MetaData {
        const val INITIAL_PRIORITY: String = "initialPriority"
        const val MAX_LOG_FILE_SIZE_IN_MB: String = "RollingFile.maxLogFileSizeInMb"
        const val MAX_LOG_FILE_BACKUP: String = "RollingFile.maxLogFileBackup"
        const val LOG_FILE_DIR: String = "RollingFile.logFileDir"
        const val LOG_FILE_BASE_NAME: String = "RollingFile.logFileBaseName"
        const val LOG_FILE_EXT: String = "RollingFile.logFileExt"
    }

    companion object {
        private const val LOG_TAG: String = "FileLogProvider"

        private const val MAX_LOG_ITEM_SIZE_FOR_TRANSACTION: Int = 1000
        private const val MAX_LOG_MESSAGE_LENGTH: Int = 20000
        private const val PREFIX_EXTERNAL_FILES: String = "{external-path}"
        private val executor: Executor = Executors.newSingleThreadExecutor()
        private var contentProviderClient: ContentProviderClient? = null
        private var contentUri: Uri = Uri.EMPTY
        private var contentValues: List<ContentValues> = emptyList()

        private fun getAuthority(context: Context): String = "${context.packageName}.provider.file_log"
        private fun getContentUri(context: Context): Uri = Uri.parse("content://${getAuthority(context)}")
        private fun getContentUri(context: Context, path: String): Uri = getContentUri(context).buildUpon().appendPath(path).build()

        private fun getProviderInfo(context: Context): ProviderInfo =
                context.packageManager.resolveContentProvider(FileLogProvider.getAuthority(context), PackageManager.GET_META_DATA)

        private var isInitialized: Boolean = false

        fun initialize(context: Context) {
            if (!isInitialized) {
                isInitialized = true
                context.contentResolver.registerContentObserver(Path.PRIORITY.getContentUri(context), false, object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        priority = loadPriority(context)
                    }
                })
                if (!isLoggerProcess(context)) {
                    Thread.setDefaultUncaughtExceptionHandler(object : LogWriter.CrashHandler(context, Thread.getDefaultUncaughtExceptionHandler()) {
                        override fun log(record: LogRecord) {
                            val crashLog = newContentValues(record.priority, record.tag, record.message, record.pid, record.tid, record.date)
                            addLogs(listOf(crashLog))
                            sendLog(context, pollLogs())
                        }
                    })
                }
            }
        }

        fun getMetaData(context: Context): Bundle = getProviderInfo(context).metaData ?: Bundle()

        fun isLoggerProcess(context: Context): Boolean = context.currentProcessName == getProviderInfo(context).processName

        fun postLog(context: Context, priority: Int, tag: String, message: String, pid: Int = Process.myPid(), tid: Int = Process.myTid(), date: Date = Date()) {
            if (!isInitialized || !checkPriority(context, priority)) {
                return
            }
            addLogs(message.chunked(MAX_LOG_MESSAGE_LENGTH).map {
                newContentValues(priority, tag, it, pid, tid, date)
            })
            executor.execute {
                val logs = pollLogs()
                if (logs.isEmpty()) {
                    return@execute
                }
                logs.chunked(MAX_LOG_ITEM_SIZE_FOR_TRANSACTION).forEach {
                    sendLog(context, it)
                }
            }
        }

        private fun checkPriority(context: Context, priority: Int) = getPriority(context) <= priority

        private fun addLogs(logs: List<ContentValues>): Unit = synchronized(this) {
            contentValues += logs
        }

        private fun pollLogs(): List<ContentValues> = synchronized(this) {
            val logs = this.contentValues
            this.contentValues = emptyList()
            return logs
        }

        private fun newContentValues(priority: Int, tag: String, message: String, pid: Int, tid: Int, date: Date) = ContentValues().also {
            it.put(Column.PRIORITY, priority)
            it.put(Column.TAG, tag)
            it.put(Column.PID, pid)
            it.put(Column.TID, tid)
            it.put(Column.DATE, date.time)
            it.put(Column.MESSAGE, message)
        }

        private fun initContentProviderClientIfNeeded(context: Context) {
            if (contentProviderClient == null) {
                if (contentUri == Uri.EMPTY) {
                    contentUri = getContentUri(context)
                }
                contentProviderClient = context.contentResolver.acquireUnstableContentProviderClient(contentUri)
            }
        }

        private fun closeContentProviderClient() {
            if (Build.VERSION.SDK_INT >= 24) {
                contentProviderClient?.close()
            } else {
                @Suppress("DEPRECATION")
                contentProviderClient?.release()
            }
            contentProviderClient = null
        }

        private fun sendLog(context: Context, logs: List<ContentValues>, retry: Boolean = false) {
            initContentProviderClientIfNeeded(context)
            try {
                contentProviderClient?.bulkInsert(contentUri, logs.toTypedArray())
            } catch (e: TransactionTooLargeException) {
                Log.w(LOG_TAG, "TransactionTooLargeException ${logs.size}")
                if (logs.size <= 1) {
                    Log.e(LOG_TAG, "Could not write log because transaction too large")
                    return
                }
                logs.chunked(logs.size / 2).forEach {
                    sendLog(context, it, false)
                }
            } catch (e: DeadObjectException) {
                closeContentProviderClient()
                if (!retry) {
                    sendLog(context, logs, true)
                } else {
                    Log.e(LOG_TAG, "Could not write log because dead content provider")
                }
            }
        }

        fun loadFiles(context: Context): List<File> {
            return context.contentResolver.query(Path.FILES.getContentUri(context), null, null, null, null)?.use { cursor ->
                generateSequence { cursor.takeIf { it.moveToNext() } }
                        .map { File(it.getString(cursor.getColumnIndex(Column.FILE))) }
                        .toList()
            } ?: emptyList()
        }

        private var priority: Int = -1

        fun getPriority(context: Context): Int {
            if (priority == -1) {
                priority = loadPriority(context)
            }
            return priority
        }

        fun setPriority(context: Context, priority: Int) {
            FileLogProvider.priority = priority
            savePriority(context, priority)
        }

        fun crashOnly(context: Context) {
            setPriority(context, LogWriter.PRIORITY_CRASH)
        }

        fun disable(context: Context) {
            setPriority(context, LogWriter.PRIORITY_NONE)
        }

        private fun loadPriority(context: Context): Int {
            return context.contentResolver.query(Path.PRIORITY.getContentUri(context), null, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(cursor.getColumnIndex(Column.PRIORITY))
            } ?: LogWriter.PRIORITY_NONE
        }

        private fun savePriority(context: Context, priority: Int) {
            val values = ContentValues()
            values.put(Column.PRIORITY, priority)
            context.contentResolver.update(Path.PRIORITY.getContentUri(context), values, null, null)
        }
    }
}
