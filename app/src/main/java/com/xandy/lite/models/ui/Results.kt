package com.xandy.lite.models.ui

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.net.Uri


sealed class InsertResult {
    data object Success : InsertResult()
    data object Failure : InsertResult()
    data object Exists : InsertResult()
}

sealed class UpdateResult {
    data object Success : UpdateResult()
    data object Failure : UpdateResult()
    data class SecurityException(val ex: RecoverableSecurityException) : UpdateResult()
    data class FileException(val request: PendingIntent): UpdateResult()
}

sealed class DeleteResult {
    object Success : DeleteResult()
    data class Partial(
        val deleted: List<Uri>, val failed: List<Uri>
    ) : DeleteResult()

    data class SecurityException(val ex: RecoverableSecurityException) : DeleteResult()
    data object Failure : DeleteResult()
}