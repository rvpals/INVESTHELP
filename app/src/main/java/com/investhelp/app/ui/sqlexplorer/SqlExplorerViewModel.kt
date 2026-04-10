package com.investhelp.app.ui.sqlexplorer

import android.content.Context
import android.content.Intent
import android.database.Cursor
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.DatabaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class QueryResult(
    val columns: List<String>,
    val rows: List<List<String>>,
    val rowCount: Int,
    val executionTimeMs: Long,
    val message: String? = null
)

data class ColumnInfo(
    val name: String,
    val type: String,
    val notNull: Boolean,
    val defaultValue: String?,
    val pk: Boolean
)

@HiltViewModel
class SqlExplorerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dbProvider: DatabaseProvider
) : ViewModel() {

    private val _result = MutableStateFlow<QueryResult?>(null)
    val result: StateFlow<QueryResult?> = _result.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _tables = MutableStateFlow<List<String>>(emptyList())
    val tables: StateFlow<List<String>> = _tables.asStateFlow()

    private val _expandedTable = MutableStateFlow<String?>(null)
    val expandedTable: StateFlow<String?> = _expandedTable.asStateFlow()

    private val _tableColumns = MutableStateFlow<List<ColumnInfo>>(emptyList())
    val tableColumns: StateFlow<List<ColumnInfo>> = _tableColumns.asStateFlow()

    init {
        loadTables()
    }

    private fun loadTables() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val db = dbProvider.database.openHelper.readableDatabase
                    val cursor = db.query(
                        "SELECT name FROM sqlite_master WHERE type='table' " +
                            "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%' " +
                            "AND name NOT LIKE 'android_%' ORDER BY name"
                    )
                    val list = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        list.add(cursor.getString(0))
                    }
                    cursor.close()
                    _tables.value = list
                }
            } catch (_: Exception) { }
        }
    }

    fun toggleTable(tableName: String) {
        if (_expandedTable.value == tableName) {
            _expandedTable.value = null
            _tableColumns.value = emptyList()
        } else {
            _expandedTable.value = tableName
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val db = dbProvider.database.openHelper.readableDatabase
                        val cursor = db.query("PRAGMA table_info('$tableName')")
                        val cols = mutableListOf<ColumnInfo>()
                        while (cursor.moveToNext()) {
                            cols.add(
                                ColumnInfo(
                                    name = cursor.getString(1),
                                    type = cursor.getString(2) ?: "",
                                    notNull = cursor.getInt(3) == 1,
                                    defaultValue = if (cursor.isNull(4)) null else cursor.getString(4),
                                    pk = cursor.getInt(5) > 0
                                )
                            )
                        }
                        cursor.close()
                        _tableColumns.value = cols
                    }
                } catch (_: Exception) { }
            }
        }
    }

    fun executeQuery(sql: String) {
        val trimmed = sql.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _isRunning.value = true
            _error.value = null
            _result.value = null

            try {
                val startTime = System.currentTimeMillis()
                val db = dbProvider.database.openHelper.readableDatabase
                val isSelect = trimmed.uppercase().let {
                    it.startsWith("SELECT") || it.startsWith("PRAGMA") || it.startsWith("EXPLAIN")
                }

                if (isSelect) {
                    withContext(Dispatchers.IO) {
                        val cursor: Cursor = db.query(trimmed)
                        try {
                            val columns = (0 until cursor.columnCount).map { cursor.getColumnName(it) }
                            val rows = mutableListOf<List<String>>()
                            while (cursor.moveToNext()) {
                                val row = (0 until cursor.columnCount).map { i ->
                                    if (cursor.isNull(i)) "NULL"
                                    else cursor.getString(i) ?: "NULL"
                                }
                                rows.add(row)
                            }
                            val elapsed = System.currentTimeMillis() - startTime
                            _result.value = QueryResult(
                                columns = columns,
                                rows = rows,
                                rowCount = rows.size,
                                executionTimeMs = elapsed
                            )
                        } finally {
                            cursor.close()
                        }
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        db.execSQL(trimmed)
                    }
                    val elapsed = System.currentTimeMillis() - startTime
                    _result.value = QueryResult(
                        columns = emptyList(),
                        rows = emptyList(),
                        rowCount = 0,
                        executionTimeMs = elapsed,
                        message = "Statement executed successfully"
                    )
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Query failed"
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun exportCsv(): Intent? {
        val result = _result.value ?: return null
        if (result.columns.isEmpty()) return null

        return try {
            val sb = StringBuilder()
            // Header
            sb.appendLine(result.columns.joinToString(",") { escapeCsv(it) })
            // Rows
            result.rows.forEach { row ->
                sb.appendLine(row.joinToString(",") { escapeCsv(it) })
            }

            val file = File(context.cacheDir, "sql_export.csv")
            file.writeText(sb.toString())

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            _error.value = "Export failed: ${e.message}"
            null
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
