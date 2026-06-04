package com.investhelp.app

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class LogEntry(
    val timestamp: LocalDateTime,
    val message: String
) {
    private companion object {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
    }

    fun formatted(): String = "[${timestamp.format(formatter)}] $message"
}

object AppLog {
    private const val MAX_ENTRIES = 200
    private val _entries = mutableListOf<LogEntry>()
    val entries: List<LogEntry> get() = _entries.toList()

    fun log(message: String) {
        synchronized(_entries) {
            _entries.add(LogEntry(LocalDateTime.now(), message))
            if (_entries.size > MAX_ENTRIES) {
                _entries.removeAt(0)
            }
        }
    }

    fun clear() {
        synchronized(_entries) {
            _entries.clear()
        }
    }
}
