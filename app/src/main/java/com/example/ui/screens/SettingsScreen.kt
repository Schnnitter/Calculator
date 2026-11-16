package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppSettings
import com.example.domain.model.DecimalSeparator
import com.example.domain.model.FractionDisplayPreference
import com.example.domain.model.GroupingStyle
import com.example.domain.model.ThemeMode
import com.example.ui.theme.LocalCalcColors
import com.example.ui.viewmodel.MainAppViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    historyCount: Int,
    mainViewModel: MainAppViewModel,
    onBack: () -> Unit,
    onHistoryCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Налаштування",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsCard(title = "Тема оформлення") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { mainViewModel.setThemeMode(mode) },
                                label = { Text(mode.labelUkr) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("theme_${mode.name.lowercase()}")
                            )
                        }
                    }
                }

                SettingsCard(title = "Точність десяткових результатів") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Знаків після коми",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${settings.decimalPrecision}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.decimalPrecision.toFloat(),
                        onValueChange = { mainViewModel.setDecimalPrecision(it.roundToInt()) },
                        valueRange = 2f..12f,
                        steps = 9,
                        modifier = Modifier.testTag("settings_precision_slider")
                    )
                }

                SettingsCard(title = "Формат відображення чисел") {
                    Text(
                        text = "Десятковий роздільник",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DecimalSeparator.entries.forEach { sep ->
                            FilterChip(
                                selected = settings.decimalSeparator == sep,
                                onClick = { mainViewModel.setDecimalSeparator(sep) },
                                label = { Text(sep.labelUkr) },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Роздільник тисяч",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GroupingStyle.entries.forEach { style ->
                            FilterChip(
                                selected = settings.groupingStyle == style,
                                onClick = { mainViewModel.setGroupingStyle(style) },
                                label = { Text(style.labelUkr) },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Формат результату дробів за замовчуванням",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FractionDisplayPreference.entries.forEach { pref ->
                            FilterChip(
                                selected = settings.fractionDisplayPreference == pref,
                                onClick = { mainViewModel.setFractionDisplayPreference(pref) },
                                label = { Text(pref.labelUkr) },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                }

                SettingsCard(title = "Тактильний і звуковий відгук") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Вібровідгук при натисканні",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "М’яка тактильна реакція клавіш",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.hapticEnabled,
                            onCheckedChange = mainViewModel::setHapticEnabled,
                            modifier = Modifier.testTag("settings_haptic_switch")
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Звуковий відгук",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Системний звук натискання клавіш",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.soundEnabled,
                            onCheckedChange = mainViewModel::setSoundEnabled,
                            modifier = Modifier.testTag("settings_sound_switch")
                        )
                    }
                }

                SettingsCard(title = "Дані та історія") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Збережено записів: $historyCount",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Локальне сховище обчислень",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            enabled = historyCount > 0,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("settings_clear_history")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = null
                            )
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text("Очистити")
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Очистити історію?") },
            text = { Text("Усі збережені обчислення ($historyCount) будуть видалені.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mainViewModel.clearHistory()
                        showClearDialog = false
                        onHistoryCleared()
                    }
                ) {
                    Text("Очистити", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    val calcColors = LocalCalcColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = calcColors.displayCardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}
