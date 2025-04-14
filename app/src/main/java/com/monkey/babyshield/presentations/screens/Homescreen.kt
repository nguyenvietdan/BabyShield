package com.monkey.babyshield.presentations.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.monkey.babyshield.R
import com.monkey.babyshield.presentations.theme.ActiveGreen
import com.monkey.babyshield.presentations.theme.BabyShieldTheme
import com.monkey.babyshield.presentations.theme.InactiveRed
import com.monkey.babyshield.presentations.viewmodel.BabyShieldViewModel

@Composable
fun HomeScreen(viewModel: BabyShieldViewModel = hiltViewModel()) {
    val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsState()
    val overlayActive by viewModel.overlayActive.collectAsState()
    val context = LocalContext.current
    val overlaySettingsLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.checkOverlayPermission()
            Toast.makeText(
                context,
                context.getString(if (viewModel.hasOverlayPermission.value) R.string.permission_granted else R.string.permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }

    LaunchedEffect(Unit) {
        viewModel.checkOverlayPermission() // initial check
    }

    DisposableEffect(Unit) { onDispose { } }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.block_touch_to_screen),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.block_touch_content),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (hasOverlayPermission) {
                Button(
                    onClick = {
                        viewModel.activeOverlay()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (overlayActive) ActiveGreen else InactiveRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = if (overlayActive) stringResource(R.string.disable) else stringResource(
                            R.string.enable
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            ("package:" + context.packageName).toUri()
                        )
                        overlaySettingsLauncher.launch(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InactiveRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = stringResource(R.string.grant_permission),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(
                    if (!hasOverlayPermission) R.string.need_to_grant_permission
                    else if (overlayActive) R.string.touch_blocking_is_active
                    else R.string.touch_blocking_is_not_active
                ),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    BabyShieldTheme {
        HomeScreen()
    }
}