package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailySchedule
import com.example.data.Gym
import com.example.data.ScheduleParser
import java.time.DayOfWeek
import java.time.LocalTime
import com.example.ui.viewmodels.MainViewModel

@Composable
fun SetupScreen(
    viewModel: MainViewModel,
    gyms: List<Gym>,
    onSetupComplete: () -> Unit
) {
    val currentSettings by viewModel.userSettings.collectAsState()
    
    var schedules by remember { mutableStateOf<List<DailySchedule>>(emptyList()) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(currentSettings) {
        if (!isInitialized && currentSettings != null) {
            schedules = ScheduleParser.parse(currentSettings!!.scheduleCsv)
            isInitialized = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Настройка тренировок",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Text("Выберите залы и добавьте расписание", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            gyms.forEach { gym ->
                GymScheduleCard(
                    gym = gym,
                    gymSchedules = schedules.filter { it.gymId == gym.id },
                    onAddSchedule = { newSchedule -> 
                        schedules = schedules + newSchedule
                    },
                    onRemoveSchedule = { scheduleToRemove ->
                        schedules = schedules.filter { it != scheduleToRemove }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.saveSetup(schedules)
                onSetupComplete()
            },
            enabled = schedules.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Сохранить и продолжить", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GymScheduleCard(
    gym: Gym,
    gymSchedules: List<DailySchedule>,
    onAddSchedule: (DailySchedule) -> Unit,
    onRemoveSchedule: (DailySchedule) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf(DayOfWeek.MONDAY) }
    var classTime by remember { mutableStateOf(LocalTime.of(18, 0)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${gym.name} (${gym.radiusMeters}m)",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
            )
            
            if (gymSchedules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                gymSchedules.forEach { schedule ->
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dayStr = when(schedule.dayOfWeek) {
                            DayOfWeek.MONDAY -> "ПН"
                            DayOfWeek.TUESDAY -> "ВТ"
                            DayOfWeek.WEDNESDAY -> "СР"
                            DayOfWeek.THURSDAY -> "ЧТ"
                            DayOfWeek.FRIDAY -> "ПТ"
                            DayOfWeek.SATURDAY -> "СБ"
                            DayOfWeek.SUNDAY -> "ВС"
                        }
                        Text("$dayStr в ${schedule.startTime.format(formatter)}", style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { onRemoveSchedule(schedule) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Удалить расписание", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Выберите день", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.SpaceBetween) {
                     DayOfWeek.values().take(4).forEach { day ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { 
                                val dayStr = when(day) {
                                    DayOfWeek.MONDAY -> "ПН"
                                    DayOfWeek.TUESDAY -> "ВТ"
                                    DayOfWeek.WEDNESDAY -> "СР"
                                    DayOfWeek.THURSDAY -> "ЧТ"
                                    DayOfWeek.FRIDAY -> "ПТ"
                                    DayOfWeek.SATURDAY -> "СБ"
                                    DayOfWeek.SUNDAY -> "ВС"
                                }
                                Text(dayStr, fontSize = 12.sp) 
                            }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.SpaceBetween) {
                     DayOfWeek.values().drop(4).forEach { day ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { 
                                val dayStr = when(day) {
                                    DayOfWeek.MONDAY -> "ПН"
                                    DayOfWeek.TUESDAY -> "ВТ"
                                    DayOfWeek.WEDNESDAY -> "СР"
                                    DayOfWeek.THURSDAY -> "ЧТ"
                                    DayOfWeek.FRIDAY -> "ПТ"
                                    DayOfWeek.SATURDAY -> "СБ"
                                    DayOfWeek.SUNDAY -> "ВС"
                                }
                                Text(dayStr, fontSize = 12.sp) 
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Время начала тренировки", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { classTime = LocalTime.of(7, 0) }, colors = timeButtonColors(classTime == LocalTime.of(7,0))) { Text("07:00") }
                    Button(onClick = { classTime = LocalTime.of(12, 0) }, colors = timeButtonColors(classTime == LocalTime.of(12,0))) { Text("12:00") }
                    Button(onClick = { classTime = LocalTime.of(18, 0) }, colors = timeButtonColors(classTime == LocalTime.of(18,0))) { Text("18:00") }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val schedule = DailySchedule(selectedDay, classTime, classTime.plusMinutes(90), gym.id)
                        onAddSchedule(schedule)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Filled.Add, "Добавить")
                    Text("Добавить")
                }
            } else if (gymSchedules.isEmpty()) {
                Text("Нажмите, чтобы добавить расписание", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun timeButtonColors(isSelected: Boolean): ButtonColors {
    return ButtonDefaults.buttonColors(
        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inverseOnSurface,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
