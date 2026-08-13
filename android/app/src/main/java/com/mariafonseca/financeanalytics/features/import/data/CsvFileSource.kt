package com.mariafonseca.financeanalytics.features.`import`.data

import android.content.ContentResolver
import android.provider.OpenableColumns
import androidx.core.net.toUri
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.CodingErrorAction

/**
 * Reads a user-selected file through [android.content.ContentResolver]
 * rather than copying it into app storage first — PR-006 says not to
 * permanently copy files unless required, and this pipeline only ever needs
 * one pass over the content.
 *
 * Takes the URI as its string form rather than [android.net.Uri] directly so
 * the ViewModel above it (and its tests) never need a real Uri instance —
 * only this Android-specific implementation parses one, right before the
 * ContentResolver call that actually needs it.
 */
interface CsvFileSource {
    fun fileName(uriString: String): String?
    fun readText(uriString: String): String
}

class ContentResolverCsvFileSource(
    private val contentResolver: ContentResolver,
) : CsvFileSource {

    override fun fileName(uriString: String): String? =
        contentResolver.query(uriString.toUri(), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }

    // Explicitly UTF-8, and strict about it: a non-UTF-8 export (e.g.
    // Windows-1252/Latin-1, common in older European bank exports) throws
    // here rather than silently decoding accented merchant names as
    // mojibake. `MalformedInputException` is an `IOException`, so it
    // surfaces through the same `ImportUiState.Failed(FileReadError)` path
    // as any other read failure.
    override fun readText(uriString: String): String {
        val strictUtf8Decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return contentResolver.openInputStream(uriString.toUri())
            ?.use { InputStreamReader(it, strictUtf8Decoder).readText() }
            ?: throw IOException("Unable to open the selected file")
    }
}
