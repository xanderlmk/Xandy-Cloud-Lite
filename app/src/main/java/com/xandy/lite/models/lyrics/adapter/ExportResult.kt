package com.xandy.lite.models.lyrics.adapter

import android.net.Uri

sealed class ExportResult {
    data class Success(val uri: Uri?): ExportResult()
    data object Exists: ExportResult()
    data object Failed: ExportResult()
}