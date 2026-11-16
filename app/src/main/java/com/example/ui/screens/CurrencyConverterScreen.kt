package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.domain.math.NumberFormatter
import com.example.domain.model.AppSettings
import com.example.domain.model.CurrencyCatalog
import com.example.domain.model.CurrencyInfo
import com.example.ui.components.CalcKeyButton
import com.example.ui.components.CalcKeyRole
import com.example.ui.components.rememberFeedbackTrigger
import com.example.ui.theme.CalcDisplayFontFamily
import com.example.ui.theme.LocalCalcColors
import com.example.ui.viewmodel.CurrencyUiState
import com.example.ui.viewmodel.CurrencyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PickerTarget { FROM, TO }

@Composable
fun CurrencyConverterScreen(
    state: CurrencyUiState,
    settings: AppSettings,
    viewModel: CurrencyViewModel,
    onShowCopied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val calcColors = LocalCalcColors.current
    val context = LocalContext.current
    val onFeedback = rememberFeedbackTrigger(settings)

    var activePicker by remember { mutableStateOf<PickerTarget?>(null) }
    var swapRotation by remember { mutableStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = swapRotation,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "swap_rot"
    )

    val fromInfo = remember(state.fromCode) { CurrencyCatalog.getInfo(state.fromCode) }
    val toInfo = remember(state.toCode) { CurrencyCatalog.getInfo(state.toCode) }
    val formattedInput = remember(state.amountInput, settings) {
        NumberFormatter.formatEditableExpression(state.amountInput, settings)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.BottomCenter
    ) {
        val compactHeight = maxHeight < 580.dp

        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(bottom = if (compactHeight) 10.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Currency Cards with overlapping circular swap button
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyCard(
                        label = "З валюти",
                        currency = fromInfo,
                        amountText = formattedInput,
                        isInput = true,
                        onSelectCurrency = { activePicker = PickerTarget.FROM },
                        onCopy = null,
                        modifier = Modifier.testTag("currency_from_card")
                    )

                    CurrencyCard(
                        label = "У валюту",
                        currency = toInfo,
                        amountText = state.convertedFormatted,
                        isInput = false,
                        onSelectCurrency = { activePicker = PickerTarget.TO },
                        onCopy = {
                            if (state.convertedFormatted != "—") {
                                copyToClipboard(context, state.convertedFormatted)
                                onShowCopied("Скопійовано: ${state.convertedFormatted} ${toInfo.code}")
                            }
                        },
                        modifier = Modifier.testTag("currency_to_card")
                    )
                }

                // Circular swap button matching equals circle accent
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 20.dp)
                        .size(42.dp)
                        .graphicsLayer { rotationZ = animatedRotation }
                        .clip(CircleShape)
                        .background(calcColors.equalsKeyBg)
                        .clickable {
                            onFeedback()
                            swapRotation += 180f
                            viewModel.swapCurrencies()
                        }
                        .testTag("currency_swap_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapVert,
                        contentDescription = "Змінити валюти місцями",
                        tint = calcColors.equalsKeyContent
                    )
                }
            }

            // Minimalist Live Rate & Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (state.errorMessage != null && state.rates.isEmpty()) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            calcColors.displayCardBg
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (state.unitRateText.isNotEmpty()) {
                        Text(
                            text = state.unitRateText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = CalcDisplayFontFamily
                        )
                    }
                    if (state.errorMessage != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (state.updatedAtMillis != null) {
                        val formattedDate = remember(state.updatedAtMillis) {
                            SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.forLanguageTag("uk-UA"))
                                .format(Date(state.updatedAtMillis))
                        }
                        val cacheSuffix = if (state.isFromCache) " • Кеш" else " • ${state.sourceLabel}"
                        Text(
                            text = "Оновлено: $formattedDate$cacheSuffix",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    IconButton(
                        onClick = {
                            onFeedback()
                            viewModel.refreshRates()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("currency_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Оновити курси",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Circular 4x4 Numeric Keypad matching the app's unified circular grid
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val gap = if (compactHeight) 10.dp else 14.dp
                val maxW = (maxWidth - gap * 3) / 4
                val maxH = (maxHeight - gap * 3) / 4
                val keySize = min(maxW, maxH).coerceAtMost(84.dp)
                val digitFont = (keySize.value * 0.36f).coerceIn(20f, 30f).sp

                Column(
                    verticalArrangement = Arrangement.spacedBy(gap),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("7", CalcKeyRole.DIGIT, { viewModel.onDigit("7") }, Modifier.size(keySize), "cur_7", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("8", CalcKeyRole.DIGIT, { viewModel.onDigit("8") }, Modifier.size(keySize), "cur_8", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("9", CalcKeyRole.DIGIT, { viewModel.onDigit("9") }, Modifier.size(keySize), "cur_9", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("⌫", CalcKeyRole.FUNCTION, viewModel::onBackspace, Modifier.size(keySize), "cur_backspace", "Стерти", icon = Icons.AutoMirrored.Outlined.Backspace, onFeedback = onFeedback)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("4", CalcKeyRole.DIGIT, { viewModel.onDigit("4") }, Modifier.size(keySize), "cur_4", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("5", CalcKeyRole.DIGIT, { viewModel.onDigit("5") }, Modifier.size(keySize), "cur_5", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("6", CalcKeyRole.DIGIT, { viewModel.onDigit("6") }, Modifier.size(keySize), "cur_6", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("AC", CalcKeyRole.CLEAR, viewModel::onClear, Modifier.size(keySize), "cur_clear", "Очистити", (digitFont.value * 0.78f).sp, onFeedback = onFeedback)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("1", CalcKeyRole.DIGIT, { viewModel.onDigit("1") }, Modifier.size(keySize), "cur_1", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("2", CalcKeyRole.DIGIT, { viewModel.onDigit("2") }, Modifier.size(keySize), "cur_2", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("3", CalcKeyRole.DIGIT, { viewModel.onDigit("3") }, Modifier.size(keySize), "cur_3", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton("⇄", CalcKeyRole.EQUALS, {
                            swapRotation += 180f
                            viewModel.swapCurrencies()
                        }, Modifier.size(keySize), "cur_swap_key", "Змінити валюти місцями", (digitFont.value * 1.05f).sp, onFeedback = onFeedback)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        CalcKeyButton("00", CalcKeyRole.DIGIT, {
                            viewModel.onDigit("0")
                            viewModel.onDigit("0")
                        }, Modifier.size(keySize), "cur_00", "Два нулі", (digitFont.value * 0.9f).sp, onFeedback = onFeedback)
                        CalcKeyButton("0", CalcKeyRole.DIGIT, { viewModel.onDigit("0") }, Modifier.size(keySize), "cur_0", fontSize = digitFont, onFeedback = onFeedback)
                        CalcKeyButton(settings.decimalSeparator.symbol, CalcKeyRole.DIGIT, viewModel::onDecimalPoint, Modifier.size(keySize), "cur_decimal", "Десятковий роздільник", digitFont, onFeedback = onFeedback)
                        CalcKeyButton("↻", CalcKeyRole.FUNCTION, viewModel::refreshRates, Modifier.size(keySize), "cur_refresh_key", "Оновити курси", (digitFont.value * 1.0f).sp, onFeedback = onFeedback)
                    }
                }
            }
        }
    }

    if (activePicker != null) {
        CurrencyPickerSheet(
            currencies = state.availableCurrencies,
            selectedCode = if (activePicker == PickerTarget.FROM) state.fromCode else state.toCode,
            onSelect = { code ->
                if (activePicker == PickerTarget.FROM) {
                    viewModel.selectFromCurrency(code)
                } else {
                    viewModel.selectToCurrency(code)
                }
                activePicker = null
            },
            onDismiss = { activePicker = null }
        )
    }
}

@Composable
private fun CurrencyCard(
    label: String,
    currency: CurrencyInfo,
    amountText: String,
    isInput: Boolean,
    onSelectCurrency: () -> Unit,
    onCopy: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val calcColors = LocalCalcColors.current
    val scroll = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(calcColors.displayCardBg)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onSelectCurrency)
                    .padding(end = 8.dp, top = 2.dp, bottom = 2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = currency.symbol,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = currency.code,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Обрати валюту",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = currency.nameUkr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp)
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scroll),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = amountText,
                        color = if (isInput) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = CalcDisplayFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = if (amountText.length > 10) 24.sp else 30.sp,
                        maxLines = 1
                    )
                }
                if (onCopy != null) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Копіювати суму",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerSheet(
    currencies: List<CurrencyInfo>,
    selectedCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filtered = remember(currencies, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) currencies
        else currencies.filter {
            it.code.lowercase().contains(q) || it.nameUkr.lowercase().contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 540.dp)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Вибір валюти",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onDismiss) {
                    Text("Закрити")
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Пошук валюти (код або назва)...") },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("currency_search_input")
            )

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered, key = { it.code }) { item ->
                    val isSelected = item.code == selectedCode
                    Surface(
                        onClick = { onSelect(item.code) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = item.symbol,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.code,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (item.isCore) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "Основна",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = item.nameUkr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (isSelected) {
                                Text(
                                    text = "Обрано",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        }
    }
}
