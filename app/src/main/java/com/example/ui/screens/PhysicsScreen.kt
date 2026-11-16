package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppSettings
import com.example.domain.physics.PhysicsCategory
import com.example.domain.physics.PhysicsFormula
import com.example.domain.physics.PhysicsVar
import com.example.ui.components.rememberFeedbackTrigger
import com.example.ui.theme.CalcDisplayFontFamily
import com.example.ui.theme.LocalCalcColors
import com.example.ui.viewmodel.PhysicsUiState
import com.example.ui.viewmodel.PhysicsViewModel

private val quickSearchTags = listOf("швидкість", "час", "сила", "енергія", "Ом", "тиск")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicsScreen(
    state: PhysicsUiState,
    settings: AppSettings,
    viewModel: PhysicsViewModel,
    onShowCopied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val calcColors = LocalCalcColors.current
    val context = LocalContext.current
    val onFeedback = rememberFeedbackTrigger(settings)

    if (state.activeFormula != null) {
        BackHandler { viewModel.closeFormula() }
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
                .padding(horizontal = 16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("physics_search_input"),
                placeholder = {
                    Text(
                        text = "Пошук: швидкість, час, сила, v=s/t...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Пошук формул"
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onSearchQueryChange("") },
                            modifier = Modifier.testTag("physics_search_clear")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Очистити пошук"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = calcColors.displayCardBg,
                    unfocusedContainerColor = calcColors.displayCardBg,
                    focusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                    unfocusedBorderColor = calcColors.subtleBorder.copy(alpha = 0.5f)
                )
            )

            Spacer(Modifier.height(8.dp))

            // Quick keyword chips when search is empty
            AnimatedVisibility(
                visible = state.searchQuery.isEmpty(),
                enter = fadeIn(tween(120)),
                exit = fadeOut(tween(80))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickSearchTags.forEach { tag ->
                        Surface(
                            onClick = {
                                onFeedback()
                                viewModel.onSearchQueryChange(tag)
                            },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "# $tag",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // Category & Favorites Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedCategory == null && !state.favoritesOnly,
                    onClick = {
                        onFeedback()
                        viewModel.clearFilters()
                    },
                    label = { Text("Усі") },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("physics_cat_all")
                )
                FilterChip(
                    selected = state.favoritesOnly,
                    onClick = {
                        onFeedback()
                        viewModel.onToggleFavoritesOnly()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (state.favoritesOnly) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Обране") },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("physics_cat_favorites")
                )
                PhysicsCategory.entries.forEach { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = {
                            onFeedback()
                            viewModel.onSelectCategory(category)
                        },
                        label = { Text(category.labelUkr) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("physics_cat_${category.name.lowercase()}")
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Знайдено формул: ${state.filteredFormulas.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.searchQuery.isNotEmpty() || state.selectedCategory != null || state.favoritesOnly) {
                    TextButton(
                        onClick = { viewModel.clearFilters() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Скинути")
                    }
                }
            }

            if (state.filteredFormulas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Science,
                            contentDescription = null,
                            tint = calcColors.mutedText,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Формул за цим запитом не знайдено",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Спробуйте інше слово або позначення, наприклад: «швидкість», «час», «сила», «Ом».",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { viewModel.clearFilters() },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Показати всі формули")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("physics_formula_list"),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filteredFormulas, key = { it.id }) { formula ->
                        PhysicsFormulaCard(
                            formula = formula,
                            isFavorite = formula.id in state.favoriteIds,
                            onClick = {
                                onFeedback()
                                viewModel.openFormula(formula)
                            },
                            onToggleFavorite = {
                                onFeedback()
                                viewModel.onToggleFavorite(formula.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Interactive Formula Solver Sheet
    state.activeFormula?.let { formula ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeFormula() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            PhysicsFormulaSolverContent(
                formula = formula,
                state = state,
                viewModel = viewModel,
                onFeedback = onFeedback,
                onCopyResult = { text ->
                    copyToClipboard(context, text)
                    onShowCopied("Скопійовано: $text")
                },
                onSavedToHistory = {
                    onShowCopied("Збережено в історію")
                }
            )
        }
    }
}

@Composable
private fun PhysicsFormulaCard(
    formula: PhysicsFormula,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val calcColors = LocalCalcColors.current

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = calcColors.displayCardBg,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("physics_formula_${formula.id}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = formula.category.labelUkr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
                        .testTag("physics_fav_${formula.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (isFavorite) "Прибрати з обраного" else "Додати в обране",
                        tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = formula.titleUkr,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = formula.formulaDisplay,
                fontFamily = CalcDisplayFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary
            )

            Text(
                text = formula.descriptionUkr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                formula.variables.forEach { v ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${v.symbol} — ${v.nameUkr}",
                            style = MaterialTheme.typography.labelSmall,
                            color = calcColors.mutedText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhysicsFormulaSolverContent(
    formula: PhysicsFormula,
    state: PhysicsUiState,
    viewModel: PhysicsViewModel,
    onFeedback: () -> Unit,
    onCopyResult: (String) -> Unit,
    onSavedToHistory: () -> Unit
) {
    val calcColors = LocalCalcColors.current
    val activeTarget = formula.solveTargets[state.selectedTargetIndex.coerceIn(0, formula.solveTargets.lastIndex)]
    val targetVar = formula.varById(activeTarget.targetVarId)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formula.category.labelUkr,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = formula.titleUkr,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = { viewModel.closeFormula() },
                modifier = Modifier.testTag("physics_solver_close")
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Закрити")
            }
        }

        // Target Selector: "Що шукаємо?"
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Що потрібно знайти?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                formula.solveTargets.forEachIndexed { idx, target ->
                    FilterChip(
                        selected = idx == state.selectedTargetIndex,
                        onClick = {
                            onFeedback()
                            viewModel.selectTarget(idx)
                        },
                        label = { Text(target.targetLabelUkr) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("physics_target_${target.targetVarId}")
                    )
                }
            }
        }

        // Active Equation Display Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = calcColors.displayCardBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = activeTarget.equationDisplay,
                    fontFamily = CalcDisplayFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.testTag("physics_active_equation")
                )
                if (formula.constantsNote != null) {
                    Text(
                        text = formula.constantsNote,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Required Variable Inputs
        activeTarget.requiredVarIds.forEach { varId ->
            val variable = formula.varById(varId)
            val inputVal = state.inputTexts[varId].orEmpty()
            val selectedUnitIdx = (state.selectedUnitIndices[varId] ?: 0)
                .coerceIn(0, variable.units.lastIndex)
            PhysicsVariableInputRow(
                variable = variable,
                value = inputVal,
                selectedUnitIndex = selectedUnitIdx,
                onValueChange = { viewModel.onInputChange(varId, it) },
                onUnitSelected = { viewModel.onUnitChange(varId, it) }
            )
        }

        HorizontalDivider(color = calcColors.subtleBorder.copy(alpha = 0.5f))

        // Output Unit Selector (if multiple units exist) + Result Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Результат (${targetVar.symbol})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val outUnitIdx = (state.selectedUnitIndices[targetVar.id] ?: 0)
                .coerceIn(0, targetVar.units.lastIndex)
            UnitSelectorDropdown(
                variable = targetVar,
                selectedIndex = outUnitIdx,
                onSelect = { viewModel.onUnitChange(targetVar.id, it) },
                testTag = "physics_output_unit"
            )
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (state.errorMessage != null) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                calcColors.displayCardBg
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    state.errorMessage != null -> {
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.testTag("physics_error_text")
                        )
                    }
                    state.formattedResult != null -> {
                        val unitDisplay = if (state.resultUnitLabel.isNullOrEmpty() || state.resultUnitLabel == "—") {
                            ""
                        } else {
                            " ${state.resultUnitLabel}"
                        }
                        val fullResult = "${state.formattedResult}$unitDisplay"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${targetVar.symbol} = $fullResult",
                                fontFamily = CalcDisplayFontFamily,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("physics_result_text")
                            )
                            IconButton(
                                onClick = {
                                    onFeedback()
                                    onCopyResult(fullResult)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Копіювати результат",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!state.substitutionText.isNullOrEmpty()) {
                            Text(
                                text = state.substitutionText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Calculate,
                                contentDescription = null,
                                tint = calcColors.mutedText
                            )
                            Text(
                                text = "Введіть відомі величини вище для миттєвого обчислення",
                                style = MaterialTheme.typography.bodyMedium,
                                color = calcColors.mutedText
                            )
                        }
                    }
                }
            }
        }

        // Actions: Clear & Save to History
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onFeedback()
                    viewModel.clearInputs()
                },
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f)
            ) {
                Text("Очистити")
            }
            Button(
                onClick = {
                    onFeedback()
                    if (viewModel.saveToHistory()) {
                        onSavedToHistory()
                    }
                },
                enabled = state.formattedResult != null && state.errorMessage == null,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = calcColors.equalsKeyBg,
                    contentColor = calcColors.equalsKeyContent
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("physics_save_history")
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("В історію")
            }
        }
    }
}

@Composable
private fun PhysicsVariableInputRow(
    variable: PhysicsVar,
    value: String,
    selectedUnitIndex: Int,
    onValueChange: (String) -> Unit,
    onUnitSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = variable.symbol,
                fontFamily = CalcDisplayFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(variable.nameUkr, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = if (variable.hint.isNotEmpty()) {
                { Text("Напр. ${variable.hint}") }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("physics_input_${variable.id}")
        )

        UnitSelectorDropdown(
            variable = variable,
            selectedIndex = selectedUnitIndex,
            onSelect = onUnitSelected,
            testTag = "physics_unit_${variable.id}"
        )
    }
}

@Composable
private fun UnitSelectorDropdown(
    variable: PhysicsVar,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    testTag: String
) {
    var expanded by remember { mutableStateOf(false) }
    val currentUnit = variable.units[selectedIndex.coerceIn(0, variable.units.lastIndex)]

    Box {
        Surface(
            onClick = { if (variable.units.size > 1) expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag(testTag)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = currentUnit.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (variable.units.size > 1) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Обрати одиницю вимірювання",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            variable.units.forEachIndexed { idx, option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onSelect(idx)
                    }
                )
            }
        }
    }
}
