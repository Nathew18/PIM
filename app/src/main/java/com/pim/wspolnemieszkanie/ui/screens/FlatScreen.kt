package com.pim.wspolnemieszkanie.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.pim.wspolnemieszkanie.data.Flat
import com.pim.wspolnemieszkanie.viewmodel.FlatViewModel

@Composable
fun FlatScreen(viewModel: FlatViewModel, onLogout: () -> Unit) {
    val flat = viewModel.flat

    if (viewModel.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (flat == null) {
            CreateFlatForm(viewModel)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            JoinFlatForm(viewModel)
        } else {
            FlatDetails(flat, viewModel)
        }

        viewModel.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        OutlinedButton(onClick = onLogout) {
            Text("Wyloguj się")
        }
    }
}

@Composable
private fun CreateFlatForm(viewModel: FlatViewModel) {
    var name by rememberSaveable { mutableStateOf("") }

    Text("Utwórz mieszkanie", style = MaterialTheme.typography.headlineSmall)
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Nazwa mieszkania") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { viewModel.createFlat(name) },
        enabled = !viewModel.isSaving,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Utwórz")
    }
}

@Composable
private fun JoinFlatForm(viewModel: FlatViewModel) {
    val context = LocalContext.current
    var code by rememberSaveable { mutableStateOf("") }

    Text("Dołącz do mieszkania", style = MaterialTheme.typography.headlineSmall)
    OutlinedTextField(
        value = code,
        // Bez zamiany na wielkie litery przy pisaniu, bo klawiatura gubi wtedy znaki. Robi to joinFlat.
        onValueChange = { code = it },
        label = { Text("Kod zaproszenia") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { viewModel.joinFlat(code) },
        enabled = !viewModel.isSaving,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Dołącz")
    }
    OutlinedButton(
        onClick = { scanInviteCode(context, viewModel) },
        enabled = !viewModel.isSaving,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Zeskanuj kod QR")
    }
}

@Composable
private fun FlatDetails(flat: Flat, viewModel: FlatViewModel) {
    val qrBitmap = remember(flat.inviteCode) { createQrBitmap(flat.inviteCode) }
    var showLeaveDialog by rememberSaveable { mutableStateOf(false) }

    Text(flat.name, style = MaterialTheme.typography.headlineMedium)
    Text("Kod zaproszenia", style = MaterialTheme.typography.titleMedium)
    Image(
        bitmap = qrBitmap.asImageBitmap(),
        contentDescription = "Kod QR zaproszenia ${flat.inviteCode}",
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        modifier = Modifier.size(200.dp),
    )
    Text(
        flat.inviteCode,
        style = MaterialTheme.typography.headlineSmall,
        fontFamily = FontFamily.Monospace,
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    Text("Współlokatorzy", style = MaterialTheme.typography.titleMedium)
    flat.memberIds.forEach { memberId ->
        val name = flat.memberNames[memberId] ?: "Współlokator"
        Text(if (memberId == viewModel.currentUserId) "$name (Ty)" else name)
    }

    OutlinedButton(
        onClick = { showLeaveDialog = true },
        enabled = !viewModel.isSaving,
    ) {
        Text("Opuść mieszkanie")
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Opuścić mieszkanie?") },
            text = {
                Text(
                    if (flat.memberIds.size == 1) {
                        "Jesteś ostatnią osobą, więc mieszkanie zostanie usunięte."
                    } else {
                        "Aby wrócić, potrzebny będzie kod zaproszenia."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        viewModel.leaveFlat()
                    },
                ) {
                    Text("Opuść")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Anuluj")
                }
            },
        )
    }
}

private fun scanInviteCode(context: Context, viewModel: FlatViewModel) {
    viewModel.clearError()
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    GmsBarcodeScanning.getClient(context, options).startScan()
        .addOnSuccessListener { barcode -> viewModel.joinFlat(barcode.rawValue.orEmpty()) }
        .addOnFailureListener { error ->
            // Przycisk wstecz w skanerze kończy się kodem INTERNAL, a to rezygnacja, nie błąd.
            if ((error as? MlKitException)?.errorCode != MlKitException.INTERNAL) {
                viewModel.showScannerError()
            }
        }
}

// Kolory QR są stałe, bo w trybie ciemnym odwrócony kod może się nie skanować.
private fun createQrBitmap(text: String, size: Int = 512): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size) { index ->
        if (matrix[index % size, index / size]) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}
