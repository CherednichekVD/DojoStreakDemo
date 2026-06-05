package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailySchedule
import com.example.data.Gym
import java.time.DayOfWeek
import java.time.LocalTime
import com.example.ui.viewmodels.MainViewModel

@Composable
fun SetupScreen(
    viewModel: MainViewModel,
    gyms: List<Gym>,
    onSetupComplete: () -> Unit
) {
    var selectedGymId by remember { mutableStateOf<String?>(null) }
    // Using a simple set to check days
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }
    var classTime by remember { mutableStateOf(LocalTime.of(18, 0)) } // Default 18:00
    
    val currentSettings by viewModel.userSettings.collectAsState()
    
    LaunchedEffect(currentSettings) {
        if (currentSettings != null && selectedGymId == null) {
            selectedGymId = currentSettings?.selectedGymId
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding()
    ) {
        Text(
            text = "Setup Your Training",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Text("Select Your Dojo", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            items(gyms) { gym ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { selectedGymId = gym.id },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedGymId == gym.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "${gym.name} (${gym.radiusMeters}m)",
                        modifier = Modifier.padding(16.dp),
                        color = if (selectedGymId == gym.id) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Class Schedule (Select Days)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        val allDays = DayOfWeek.values()
        // Simple day toggler
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            allDays.forEach { day ->
                FilterChip(
                    selected = selectedDays.contains(day),
                    onClick = {
                        val newSet = selectedDays.toMutableSet()
                        if (newSet.contains(day)) newSet.remove(day) else newSet.add(day)
                        selectedDays = newSet
                    },
                    label = { Text(day.name.take(3)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Class Start Time (Default 1h 30m)", style = MaterialTheme.typography.titleMedium)
        // Mock time picker - for MVP, just hardcode a button to set common times
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { classTime = LocalTime.of(7, 0) }, colors = timeButtonColors(classTime == LocalTime.of(7,0))) { Text("07:00") }
            Button(onClick = { classTime = LocalTime.of(12, 0) }, colors = timeButtonColors(classTime == LocalTime.of(12,0))) { Text("12:00") }
            Button(onClick = { classTime = LocalTime.of(18, 0) }, colors = timeButtonColors(classTime == LocalTime.of(18,0))) { Text("18:00") }
            Button(onClick = { classTime = LocalTime.of(19, 30) }, colors = timeButtonColors(classTime == LocalTime.of(19,30))) { Text("19:30") }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val schedules = selectedDays.map { day ->
                    DailySchedule(day, classTime, classTime.plusMinutes(90))
                }
                viewModel.saveSetup(selectedGymId!!, schedules)
                onSetupComplete()
            },
            enabled = selectedGymId != null && selectedDays.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Save & Start Streak", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun timeButtonColors(isSelected: Boolean): ButtonColors {
    return ButtonDefaults.buttonColors(
        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
