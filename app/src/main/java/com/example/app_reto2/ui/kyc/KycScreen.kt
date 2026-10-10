package com.example.app_reto2.ui.kyc

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun KycScreen(
    viewModel: KycViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    KycContent(
        state = state,
        onDocumentSelected = viewModel::resetState,
        onUpload = viewModel::uploadDocument,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun KycContent(
    state: KycState,
    onDocumentSelected: () -> Unit,
    onUpload: (Uri) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val fileName = remember(selectedUri) { selectedUri?.let { context.fileName(it) } }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            onDocumentSelected()
        }
    }

    val isLoading = state is KycState.Loading
    val canUpload = selectedUri != null && !isLoading && state !is KycState.Success

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Verificación de identidad",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Sube una foto clara de tu documento de identidad.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        OutlinedButton(
            onClick = {
                pickMedia.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Seleccionar Documento")
        }

        if (selectedUri != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(text = fileName ?: "Documento seleccionado")
            }
        }

        Button(
            onClick = { selectedUri?.let(onUpload) },
            enabled = canUpload,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Subir Documento")
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
        when (state) {
            is KycState.Success -> Text(
                text = "Documento subido correctamente.",
                color = MaterialTheme.colorScheme.primary
            )
            is KycState.Error -> Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error
            )
            else -> Unit
        }

        TextButton(onClick = onBack, enabled = !isLoading) {
            Text("Volver")
        }
    }
}

private fun Context.fileName(uri: Uri): String? =
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: uri.lastPathSegment