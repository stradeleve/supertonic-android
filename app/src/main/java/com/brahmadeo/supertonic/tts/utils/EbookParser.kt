package com.brahmadeo.supertonic.tts.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.toAbsoluteUrl
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.content.content
import org.readium.r2.shared.publication.services.content.Content
import java.io.File

class EbookParser(private val context: Context) {

    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
    private val publicationParser = DefaultPublicationParser(context, httpClient, assetRetriever, null)
    private val publicationOpener = PublicationOpener(publicationParser)

    suspend fun openPublication(uri: Uri): Result<Publication> = withContext(Dispatchers.IO) {
        try {
            val url = uri.toAbsoluteUrl()
                ?: return@withContext Result.failure<Publication>(Exception("Failed to convert URI to Readium URL"))

            val asset = assetRetriever.retrieve(url).getOrElse { error: Error ->
                return@withContext Result.failure<Publication>(Exception("Failed to retrieve asset: ${error.message}"))
            }

            val publication = publicationOpener.open(asset, allowUserInteraction = false).getOrElse { error: Error ->
                return@withContext Result.failure<Publication>(Exception("Failed to open publication: ${error.message}"))
            }

            Result.success(publication)
        } catch (e: Exception) {
            Result.failure<Publication>(e)
        }
    }

    suspend fun extractText(publication: Publication, link: Link? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val locator = link?.let { Locator(href = it.url(), mediaType = it.mediaType ?: org.readium.r2.shared.util.mediatype.MediaType.BINARY) }
            val content = publication.content(locator)
            if (content == null) {
                return@withContext Result.failure<String>(Exception("This publication does not support content extraction."))
            }

            val text = if (link == null) {
                // Use the built-in text() method from the Content interface
                content.text()
            } else {
                val chapterText = StringBuilder()
                val elements = content.elements()
                for (element in elements) {
                    // Stop when we move to a different resource (next chapter)
                    if (element.locator.href != link.url()) break
                    
                    if (element is Content.TextualElement) {
                        val elementText = element.text
                        if (elementText != null) {
                            chapterText.append(elementText).append("\n\n")
                        }
                    }
                }
                chapterText.toString()
            }

            if (text == null || text.isBlank()) {
                return@withContext Result.failure<String>(Exception("No text content could be extracted."))
            }

            Result.success(text)
        } catch (e: Exception) {
            Result.failure<String>(e)
        }
    }
}
