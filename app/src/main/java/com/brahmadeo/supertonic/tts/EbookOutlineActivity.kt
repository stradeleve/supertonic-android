package com.brahmadeo.supertonic.tts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brahmadeo.supertonic.tts.ui.theme.SupertonicTheme
import com.brahmadeo.supertonic.tts.utils.EbookManager
import com.brahmadeo.supertonic.tts.utils.EbookParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication

class EbookOutlineActivity : ComponentActivity() {

    private lateinit var ebookParser: EbookParser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ebookParser = EbookParser(this)

        val ebookUriString = intent.getStringExtra(EXTRA_URI)
        if (ebookUriString == null) {
            finish()
            return
        }

        val ebookUri = Uri.parse(ebookUriString)

        setContent {
            SupertonicTheme {
                OutlineScreen(
                    ebookUri = ebookUri,
                    onChapterSelected = { text ->
                        val resultIntent = Intent()
                        resultIntent.putExtra(EXTRA_TEXT, text)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun OutlineScreen(
        ebookUri: Uri,
        onChapterSelected: (String) -> Unit,
        onBack: () -> Unit
    ) {
        var publication by remember { mutableStateOf<Publication?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var isExtracting by remember { mutableStateOf(false) }

        LaunchedEffect(ebookUri) {
            val result = ebookParser.openPublication(ebookUri)
            publication = result.getOrNull()
            isLoading = false
            if (publication == null) {
                Toast.makeText(this@EbookOutlineActivity, "Failed to open ebook", Toast.LENGTH_SHORT).show()
                onBack()
            } else {
                // Save to recent books
                val title = publication?.metadata?.title ?: "Unknown Title"
                EbookManager.addBook(this@EbookOutlineActivity, title, ebookUri.toString())
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(publication?.metadata?.title ?: "Ebook Outline") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    val toc = publication?.tableOfContents ?: emptyList()
                    if (toc.isEmpty()) {
                        // Fallback to reading order if TOC is empty
                        val readingOrder = publication?.readingOrder ?: emptyList()
                        ChapterList(readingOrder, publication!!, onChapterSelected) { isExtracting = it }
                    } else {
                        ChapterList(toc, publication!!, onChapterSelected) { isExtracting = it }
                    }
                }

                if (isExtracting) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Extracting text...")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ChapterList(
        links: List<Link>,
        publication: Publication,
        onChapterSelected: (String) -> Unit,
        setExtracting: (Boolean) -> Unit
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(links) { link ->
                ChapterItem(link, publication, onChapterSelected, setExtracting)
                // Nested children if any
                link.children.forEach { child ->
                    ChapterItem(child, publication, onChapterSelected, setExtracting, level = 1)
                }
            }
        }
    }

    @Composable
    fun ChapterItem(
        link: Link,
        publication: Publication,
        onChapterSelected: (String) -> Unit,
        setExtracting: (Boolean) -> Unit,
        level: Int = 0
    ) {
        ListItem(
            headlineContent = { 
                Text(
                    text = link.title ?: link.href.toString(), 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = (level * 16).dp)
                ) 
            },
            modifier = Modifier.clickable {
                setExtracting(true)
                CoroutineScope(Dispatchers.Main).launch {
                    val result = withContext(Dispatchers.IO) {
                        ebookParser.extractText(publication, link)
                    }
                    setExtracting(false)
                    result.onSuccess { text ->
                        onChapterSelected(text)
                    }.onFailure { e ->
                        Toast.makeText(this@EbookOutlineActivity, "Extraction failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    companion object {
        const val EXTRA_URI = "ebook_uri"
        const val EXTRA_TEXT = "extracted_text"
    }
}
