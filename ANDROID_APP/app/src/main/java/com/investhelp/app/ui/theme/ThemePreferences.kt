package com.investhelp.app.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemePreferences {
    private const val PREFS_NAME = "invest_help_settings"
    private const val KEY_THEME = "app_theme"

    private val _currentTheme = MutableStateFlow(AppTheme.Default)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString(KEY_THEME, AppTheme.Default.name) ?: AppTheme.Default.name
        _currentTheme.value = AppTheme.fromName(themeName)
    }

    fun setTheme(context: Context, theme: AppTheme) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _currentTheme.value = theme
    }
}
