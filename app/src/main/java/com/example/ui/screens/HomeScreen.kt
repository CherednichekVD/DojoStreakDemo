package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.UserSettings
import com.example.ui.viewmodels.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    userSettings: UserSettings,
    onNavigateToSetup: () -> Unit
) {
    val context = LocalContext.current
    val isTimeValid by viewModel.isTimeValid.collectAsState()
    val isLocationValid by viewModel.isLocationValid.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            viewModel.fetchLocation()
        } else {
            Toast.makeText(context, "Location permission is required to check in.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "DojoStreak",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Streak Flame Visual
        StreakCounterVisual(streak = userSettings.currentStreak)

        Spacer(modifier = Modifier.height(48.dp))

        // Check-in status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Training Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                StatusRow(
                    label = "Scheduled Time Context",
                    isValid = isTimeValid
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow(
                    label = "At Dojo Location",
                    isValid = isLocationValid
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.performCheckIn() },
                    enabled = isTimeValid && isLocationValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Check In to maintain Streak", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        TextButton(onClick = { viewModel.fetchLocation() }) {
            Icon(Icons.Filled.Refresh, contentDescription = "Refresh Location")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh GPS")
        }

        Spacer(modifier = Modifier.weight(1f))
        
        TextButton(onClick = onNavigateToSetup) {
            Text("Edit Dojo & Schedule")
        }
    }
}

@Composable
fun StatusRow(label: String, isValid: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        if (isValid) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Valid", tint = Color(0xFF3DDC84))
        } else {
            Icon(Icons.Filled.Warning, contentDescription = "Invalid", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun StreakCounterVisual(streak: Int) {
    val primaryColor = if (streak > 0) MaterialTheme.colorScheme.primary else Color.DarkGray
    
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = streak.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            Text(
                text = "DAY STREAK",
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 2.sp,
                color = primaryColor
            )
        }
    }
}
