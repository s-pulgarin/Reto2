package com.example.app_reto2.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

// Debe coincidir con la authority del <provider> en AndroidManifest.xml.
private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

fun Context.createTempImageUri(): Uri {
    val imagesDir = File(cacheDir, "images").apply { mkdirs() }
    val file = File.createTempFile("kyc_", ".jpg", imagesDir)
    return FileProvider.getUriForFile(this, packageName + FILE_PROVIDER_SUFFIX, file)
}
