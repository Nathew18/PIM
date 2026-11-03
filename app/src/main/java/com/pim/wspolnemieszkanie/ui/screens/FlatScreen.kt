package com.pim.wspolnemieszkanie.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
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
        } else {
            FlatDetails(flat)
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

    Text("Utwórz mieszkanie", style = MaterialTheme.typography.headlineMedium)
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Nazwa mieszkania") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { viewModel.createFlat(name) },
        enabled = !viewModel.isCreating,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (viewModel.isCreating) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text("Utwórz")
        }
    }
}

@Composable
private fun FlatDetails(flat: Flat) {
    val qrBitmap = remember(flat.inviteCode) { createQrBitmap(flat.inviteCode) }

    Text(flat.name, style = MaterialTheme.typography.headlineMedium)
    Text("Kod zaproszenia", style = MaterialTheme.typography.titleMedium)
    Image(
        bitmap = qrBitmap.asImageBitmap(),
        contentDescription = "Kod QR zaproszenia ${flat.inviteCode}",
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        modifier = Modifier.size(240.dp),
    )
    Text(
        flat.inviteCode,
        style = MaterialTheme.typography.headlineSmall,
        fontFamily = FontFamily.Monospace,
    )
}

// Kolory QR są stałe, bo w trybie ciemnym odwrócony kod może się nie skanować.
private fun createQrBitmap(text: String, size: Int = 512): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size) { index ->
        if (matrix[index % size, index / size]) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}
