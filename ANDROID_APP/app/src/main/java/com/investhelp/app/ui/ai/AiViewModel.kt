package com.investhelp.app.ui.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.investhelp.app.data.local.dao.AiLibraryDao
import com.investhelp.app.data.local.entity.AiLibraryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiLibraryDao: AiLibraryDao
) : ViewModel() {

    val aiLibrary: StateFlow<List<AiLibraryEntity>> =
        aiLibraryDao.getAll()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun buildGeminiIntent(prompt: String): Intent {
        val encoded = Uri.encode(prompt)
        val geminiUri = Uri.parse("https://gemini.google.com/app?text=$encoded")
        return Intent(Intent.ACTION_VIEW, geminiUri)
    }
}
