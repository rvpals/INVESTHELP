package com.investhelp.app.ui.ai

import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.ui.components.CollapsibleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTickerScreen(
    ticker: String,
    viewModel: AiViewModel,
    onBack: () -> Unit
) {
    val aiLibrary by viewModel.aiLibrary.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var prompt by rememberSaveable { mutableStateOf("") }
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedName by rememberSaveable { mutableStateOf("") }
    var selectedDescription by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artificial Intelligence for $ticker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                var libraryPinned by rememberSaveable { mutableStateOf(true) }
                CollapsibleCard(
                    title = "AI Library",
                    pinned = libraryPinned,
                    onPinToggle = { libraryPinned = it }
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("Search AI Library") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val filtered = if (searchText.isBlank()) aiLibrary
                    else aiLibrary.filter {
                        it.name.contains(searchText, ignoreCase = true) ||
                        it.description.contains(searchText, ignoreCase = true)
                    }

                    if (filtered.isEmpty()) {
                        Text(
                            "No prompts found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        val altColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        filtered.forEachIndexed { index, entry ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (index % 2 == 1) altColor else Color.Transparent)
                                    .clickable {
                                        prompt = entry.promptText.replace("[TICKER]", "\"$ticker\"")
                                        selectedName = entry.name
                                        selectedDescription = entry.description
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = entry.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = entry.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("AI Prompt") },
                    placeholder = { Text("Type your prompt or select from AI Library above...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 12,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }

            if (selectedName.isNotBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = selectedName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = selectedDescription,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                var showWebView by rememberSaveable { mutableStateOf(false) }
                var geminiUrl by rememberSaveable { mutableStateOf("") }

                Button(
                    onClick = {
                        val encoded = android.net.Uri.encode(prompt)
                        geminiUrl = "https://gemini.google.com/app?text=$encoded"
                        showWebView = true
                    },
                    enabled = prompt.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Send to Gemini")
                }

                if (showWebView && geminiUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webViewClient = WebViewClient()
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                                    loadUrl(geminiUrl)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
