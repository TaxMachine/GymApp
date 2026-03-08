package dev.taxmachine.gymapp.utils

import android.util.Log
import dev.taxmachine.gymapp.db.AppLogEntity
import dev.taxmachine.gymapp.db.AppLogLevel
import dev.taxmachine.gymapp.db.GymDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Logger {
    private var dao: GymDao? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun init(gymDao: GymDao) {
        dao = gymDao
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        saveLog(AppLogLevel.DEBUG, tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        saveLog(AppLogLevel.INFO, tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        saveLog(AppLogLevel.WARNING, tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) "$message\n${Log.getStackTraceString(throwable)}" else message
        Log.e(tag, fullMessage)
        saveLog(AppLogLevel.ERROR, tag, fullMessage)
    }

    private fun saveLog(level: AppLogLevel, tag: String, message: String) {
        dao?.let {
            scope.launch {
                try {
                    it.insertAppLog(AppLogEntity(level = level, tag = tag, message = message))
                } catch (e: Exception) {
                    Log.e("Logger", "Failed to save log to DB", e)
                }
            }
        }
    }
}
