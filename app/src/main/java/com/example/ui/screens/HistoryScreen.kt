package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.math.FractionSerializer
import com.example.domain.math.PowerSerializer
import com.example.domain.model.AppSettings
import com.example.domain.model.HistoryItem
import com.example.domain.model.HistoryMode
import com.example.ui.components.StaticFractionNodesView
import com.example.ui.components.StaticPowerNodesView
import com.example.ui.theme.CalcDisplayFontFamily
import com.example.ui.theme.LocalCalcColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    items: List<HistoryItem>,
    activeFilter: HistoryMode?,
    settings: AppSettings,
    onSelectFilter: (HistoryMode?) -> Unit,
    onReuseResult: (HistoryItem) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onClearAll: () -> Unit,
    onShowCopied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showConfirmClear by remember { mutableStateOf(false) }

    val filtered = remember(items, activeFilter) {
        if (activeFilter == null) items else items.filter { it.mode == activeFilter }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeFilter == null,
                        onClick = { onSelectFilter(null) },
                        label = { Text("Усі") },
                        shape = RoundedCornerShape(50)
                    )
                    HistoryMode.entries.forEach { mode ->
                        FilterChip(
                            selected = activeFilter == mode,
                            onClick = { onSelectFilter(if (activeFilter == mode) null else mode) },
                            label = { Text(mode.labelUkr) },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }

                if (items.isNotEmpty()) {
                    IconButton(
                        onClick = { showConfirmClear = true },
                        modifier = Modifier.testTag("history_clear_all")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = "Очистити історію",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = LocalCalcColors.current.mutedText,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "Історія обчислень порожня",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Натисніть «=» у будь-якому режимі, щоб зберегти результат.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        HistoryItemCard(
                            item = item,
                            settings = settings,
                            onCopy = {
                                copyToClipboard(context, item.resultDisplay)
                                onShowCopied("Скопійовано: ${item.resultDisplay}")
                            },
                            onReuse = { onReuseResult(item) },
                            onDelete = { onDeleteItem(item.id) }
                        )
                    }
                }
            }
        }
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text("Очистити історію?") },
            text = { Text("Усі збережені обчислення будуть видалені без можливості відновлення.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showConfirmClear = false
                    }
                ) {
                    Text("Очистити", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

@Composable
private fun HistoryItemCard(
    item: HistoryItem,
    settings: AppSettings,
    onCopy: () -> Unit,
    onReuse: () -> Unit,
    onDelete: () -> Unit
) {
    val calcColors = LocalCalcColors.current
    val dateLabel = remember(item.timestampMillis) {
        SimpleDateFormat("dd.MM, HH:mm", Locale.forLanguageTag("uk-UA"))
            .format(Date(item.timestampMillis))
    }
    val fractionNodes = remember(item.fractionJson) {
        FractionSerializer.fromJson(item.fractionJson)
    }
    val powerNodes = remember(item.powerJson) {
        PowerSerializer.fromJson(item.powerJson)
    }

    Surface(
        onClick = onReuse,
        shape = RoundedCornerShape(20.dp),
        color = calcColors.displayCardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.mode.labelUkr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = calcColors.mutedText
                )
            }

            if (fractionNodes != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    contentAlignment = Alignment.CenterStart
                ) {
                    StaticFractionNodesView(
                        nodes = fractionNodes,
                        decimalSeparator = settings.decimalSeparator,
                        fontSize = 15.sp,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        accentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else if (powerNodes != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    contentAlignment = Alignment.CenterStart
                ) {
                    StaticPowerNodesView(
                        nodes = powerNodes,
                        decimalSeparator = settings.decimalSeparator,
                        fontSize = 16.sp,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        accentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Text(
                    text = item.expressionDisplay,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = CalcDisplayFontFamily
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "= ${item.resultDisplay}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = CalcDisplayFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onReuse, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Replay,
                            contentDescription = "Використати результат",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Копіювати",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Видалити",
                            tint = calcColors.mutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
