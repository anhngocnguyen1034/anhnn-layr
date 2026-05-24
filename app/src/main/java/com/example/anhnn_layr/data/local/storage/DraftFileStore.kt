package com.example.anhnn_layr.data.local.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DraftFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val root: File
        get() = File(context.filesDir, "drafts").apply { if (!exists()) mkdirs() }

    fun saveProcessedPng(draftId: String, bytes: ByteArray): String {
        val file = File(root, "$draftId.png")
        FileOutputStream(file).use { it.write(bytes) }
        return file.absolutePath
    }

    fun saveProcessedPng(draftId: String, bitmap: Bitmap): String {
        val file = File(root, "$draftId.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file.absolutePath
    }

    fun readProcessedBitmap(path: String): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun delete(path: String) {
        runCatching { File(path).delete() }
    }
}
