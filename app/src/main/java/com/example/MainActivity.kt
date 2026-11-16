package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Superscript
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.CalculatorDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.remote.CurrencyApiService
import com.example.data.repository.CurrencyRepository
import com.example.data.repository.HistoryRepository
import com.example.domain.model.CalculatorMode
import com.example.domain.model.HistoryItem
import com.example.domain.model.HistoryMode
import com.example.domain.model.ThemeMode
import com.example.ui.components.FractionModeIcon
import com.example.ui.components.ScientificToggleIcon
import com.example.ui.components.rememberFeedbackTrigger
import com.example.ui.screens.CurrencyConverterScreen
import com.example.ui.screens.FractionCalculatorScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PhysicsScreen
import com.example.ui.screens.PowerCalculatorScreen
import com.example.ui.screens.ScientificCalculatorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StandardCalculatorScreen
import com.example.ui.theme.CalculatorTheme
import com.example.ui.viewmodel.CalculatorViewModel
import com.example.ui.viewmodel.CurrencyViewModel
import com.example.ui.viewmodel.FractionViewModel
import com.example.ui.viewmodel.MainAppViewModel
import com.example.ui.viewmodel.PhysicsViewModel
import com.example.ui.viewmodel.PowerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = CalculatorDatabase.getInstance(applicationContext)
        val settingsDataStore = SettingsDataStore(applicationContext)
        val historyRepository = HistoryRepository(db.historyDao())
        val currencyRepository = CurrencyRepository(
            apiService = CurrencyApiService.create(),
            currencyDao = db.currencyDao()
        )

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when (modelClass) {
                    MainAppViewModel::class.java -> MainAppViewModel(settingsDataStore, historyRepository) as T
                    CalculatorViewModel::class.java -> CalculatorViewModel(settingsDataStore, historyRepository) as T
                    FractionViewModel::class.java -> FractionViewModel(settingsDataStore, historyRepository) as T
                    PowerViewModel::class.java -> PowerViewModel(settingsDataStore, historyRepository) as T
                    PhysicsViewModel::class.java -> PhysicsViewModel(settingsDataStore, historyRepository) as T
                    CurrencyViewModel::class.java -> CurrencyViewModel(currencyRepository, settingsDataStore) as T
                    else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
                }
            }
        }

        setContent {
            val mainViewModel: MainAppViewModel = viewModel(factory = factory)
            val calculatorViewModel: CalculatorViewModel = viewModel(factory = factory)
            val fractionViewModel: FractionViewModel = viewModel(factory = factory)
            val powerViewModel: PowerViewModel = viewModel(factory = factory)
            val physicsViewModel: PhysicsViewModel = viewModel(factory = factory)
            val currencyViewModel: CurrencyViewModel = viewModel(factory = factory)

            val settings by mainViewModel.settings.collectAsStateWithLifecycle()
            val isDark = when (settings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(isDark) {
                val transparent = Color.Transparent.toArgb()
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    }
                )
            }

            CalculatorTheme(themeMode = settings.themeMode) {
                CalculatorAppRoot(
                    mainViewModel = mainViewModel,
                    calculatorViewModel = calculatorViewModel,
                    fractionViewModel = fractionViewModel,
                    powerViewModel = powerViewModel,
                    physicsViewModel = physicsViewModel,
                    currencyViewModel = currencyViewModel
                )
            }
        }
    }
}

private data class ModeEntry(
    val mode: CalculatorMode,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val testTag: String
)

private val modeEntries = listOf(
    ModeEntry(CalculatorMode.STANDARD, "Звичайний", "Щоденні обчислення", Icons.Outlined.Calculate, "nav_standard"),
    ModeEntry(CalculatorMode.FRACTIONS, "Дроби", "Риска, мішані числа", Icons.Outlined.PieChart, "nav_fractions"),
    ModeEntry(CalculatorMode.POWERS, "Степені", "Множення й ділення aⁿ", Icons.Outlined.Superscript, "nav_powers"),
    ModeEntry(CalculatorMode.SCIENTIFIC, "Науковий", "Тригонометрія, log, √", Icons.Outlined.Functions, "nav_scientific"),
    ModeEntry(CalculatorMode.PHYSICS, "Фізика", "Каталог і пошук формул", Icons.Outlined.Science, "nav_physics"),
    ModeEntry(CalculatorMode.CURRENCY, "Валюти", "Актуальні курси", Icons.Outlined.CurrencyExchange, "nav_currency"),
    ModeEntry(CalculatorMode.HISTORY, "Історія", "Збережені результати", Icons.Outlined.History, "nav_history")
)

@Composable
fun CalculatorAppRoot(
    mainViewModel: MainAppViewModel,
    calculatorViewModel: CalculatorViewModel,
    fractionViewModel: FractionViewModel,
    powerViewModel: PowerViewModel,
    physicsViewModel: PhysicsViewModel,
    currencyViewModel: CurrencyViewModel
) {
    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val currentMode by mainViewModel.currentMode.collectAsStateWithLifecycle()
    val isSettingsOpen by mainViewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val historyItems by mainViewModel.historyItems.collectAsStateWithLifecycle()
    val historyFilter by mainViewModel.historyFilter.collectAsStateWithLifecycle()

    val standardState by calculatorViewModel.standardState.collectAsStateWithLifecycle()
    val scientificState by calculatorViewModel.scientificState.collectAsStateWithLifecycle()
    val angleUnit by calculatorViewModel.angleUnit.collectAsStateWithLifecycle()
    val isSecondMode by calculatorViewModel.isSecondFunctions.collectAsStateWithLifecycle()
    val fractionState by fractionViewModel.uiState.collectAsStateWithLifecycle()
    val powerState by powerViewModel.uiState.collectAsStateWithLifecycle()
    val physicsState by physicsViewModel.uiState.collectAsStateWithLifecycle()
    val currencyState by currencyViewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val onFeedback = rememberFeedbackTrigger(settings)

    var showModesOverlay by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val showMessage: (String) -> Unit = remember {
        { msg ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    val reuseHistoryItem: (HistoryItem) -> Unit = remember {
        { item ->
            when (item.mode) {
                HistoryMode.STANDARD -> {
                    calculatorViewModel.loadResultIntoCalculator(item.rawResult, isScientific = false)
                    mainViewModel.selectMode(CalculatorMode.STANDARD)
                }
                HistoryMode.SCIENTIFIC -> {
                    calculatorViewModel.loadResultIntoCalculator(item.rawResult, isScientific = true)
                    mainViewModel.selectMode(CalculatorMode.SCIENTIFIC)
                }
                HistoryMode.FRACTIONS -> {
                    fractionViewModel.loadFromRawOrJson(item.rawResult, item.fractionJson)
                    mainViewModel.selectMode(CalculatorMode.FRACTIONS)
                }
                HistoryMode.POWERS -> {
                    powerViewModel.loadFromRawOrJson(item.rawResult, item.powerJson)
                    mainViewModel.selectMode(CalculatorMode.POWERS)
                }
                HistoryMode.PHYSICS -> {
                    calculatorViewModel.loadResultIntoCalculator(item.rawResult, isScientific = false)
                    mainViewModel.selectMode(CalculatorMode.STANDARD)
                }
            }
        }
    }

    if (isSettingsOpen) {
        SettingsScreen(
            settings = settings,
            historyCount = historyItems.size,
            mainViewModel = mainViewModel,
            onBack = mainViewModel::closeSettings,
            onHistoryCleared = { showMessage("Історію очищено") }
        )
        return
    }

    if (showModesOverlay) {
        BackHandler { showModesOverlay = false }
    } else if (currentMode != CalculatorMode.STANDARD) {
        BackHandler { mainViewModel.selectMode(CalculatorMode.STANDARD) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                // Minimalist top icon bar matching reference screenshot
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: subtle mode pill/selector
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                onFeedback()
                                showModesOverlay = !showModesOverlay
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("mode_title_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = currentMode.titleUkr,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Обрати режим",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Right: √π/e= (Scientific), a/b (Fractions), ⊞ (Mode Grid), ⋮ (History & Settings)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                onFeedback()
                                val target = if (currentMode == CalculatorMode.SCIENTIFIC) {
                                    CalculatorMode.STANDARD
                                } else {
                                    CalculatorMode.SCIENTIFIC
                                }
                                mainViewModel.selectMode(target)
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (currentMode == CalculatorMode.SCIENTIFIC) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .testTag("quick_scientific_toggle")
                        ) {
                            ScientificToggleIcon(
                                tint = if (currentMode == CalculatorMode.SCIENTIFIC) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        IconButton(
                            onClick = {
                                onFeedback()
                                val target = if (currentMode == CalculatorMode.FRACTIONS) {
                                    CalculatorMode.STANDARD
                                } else {
                                    CalculatorMode.FRACTIONS
                                }
                                mainViewModel.selectMode(target)
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (currentMode == CalculatorMode.FRACTIONS) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .testTag("quick_fractions_toggle")
                        ) {
                            FractionModeIcon(
                                tint = if (currentMode == CalculatorMode.FRACTIONS) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        IconButton(
                            onClick = {
                                onFeedback()
                                showModesOverlay = !showModesOverlay
                            },
                            modifier = Modifier.testTag("open_modes_grid")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GridView,
                                contentDescription = "Усі режими",
                                tint = if (showModesOverlay || currentMode == CalculatorMode.CURRENCY) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        Box {
                            IconButton(
                                onClick = {
                                    onFeedback()
                                    showMoreMenu = true
                                },
                                modifier = Modifier.testTag("more_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "Меню",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Історія обчислень") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.History, contentDescription = null)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        mainViewModel.selectMode(CalculatorMode.HISTORY)
                                    },
                                    modifier = Modifier.testTag("menu_history")
                                )
                                DropdownMenuItem(
                                    text = { Text("Налаштування") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Settings, contentDescription = null)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        mainViewModel.openSettings()
                                    },
                                    modifier = Modifier.testTag("open_settings_button")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentMode,
                    transitionSpec = {
                        (fadeIn(tween(140, easing = FastOutSlowInEasing)) +
                            scaleIn(initialScale = 0.985f, animationSpec = tween(140, easing = FastOutSlowInEasing)))
                            .togetherWith(fadeOut(tween(90)))
                    },
                    label = "fast_mode_switch"
                ) { mode ->
                    when (mode) {
                        CalculatorMode.STANDARD -> StandardCalculatorScreen(
                            state = standardState,
                            settings = settings,
                            latestHistory = historyItems.firstOrNull { it.mode == HistoryMode.STANDARD },
                            viewModel = calculatorViewModel,
                            onShowCopied = showMessage,
                            onReuseLatestHistory = reuseHistoryItem
                        )
                        CalculatorMode.FRACTIONS -> FractionCalculatorScreen(
                            state = fractionState,
                            settings = settings,
                            viewModel = fractionViewModel,
                            onShowCopied = showMessage
                        )
                        CalculatorMode.POWERS -> PowerCalculatorScreen(
                            state = powerState,
                            settings = settings,
                            viewModel = powerViewModel,
                            onShowCopied = showMessage
                        )
                        CalculatorMode.SCIENTIFIC -> ScientificCalculatorScreen(
                            state = scientificState,
                            angleUnit = angleUnit,
                            isSecondMode = isSecondMode,
                            settings = settings,
                            viewModel = calculatorViewModel,
                            onShowCopied = showMessage
                        )
                        CalculatorMode.PHYSICS -> PhysicsScreen(
                            state = physicsState,
                            settings = settings,
                            viewModel = physicsViewModel,
                            onShowCopied = showMessage
                        )
                        CalculatorMode.CURRENCY -> CurrencyConverterScreen(
                            state = currencyState,
                            settings = settings,
                            viewModel = currencyViewModel,
                            onShowCopied = showMessage
                        )
                        CalculatorMode.HISTORY -> HistoryScreen(
                            items = historyItems,
                            activeFilter = historyFilter,
                            settings = settings,
                            onSelectFilter = mainViewModel::setHistoryFilter,
                            onReuseResult = reuseHistoryItem,
                            onDeleteItem = mainViewModel::deleteHistoryItem,
                            onClearAll = {
                                mainViewModel.clearHistory()
                                showMessage("Історію очищено")
                            },
                            onShowCopied = showMessage
                        )
                    }
                }
            }
        }

        // Fast Spring-Animated Mode Switcher Grid Overlay (triggered by ⊞ or mode title)
        AnimatedVisibility(
            visible = showModesOverlay,
            enter = fadeIn(tween(130)) + scaleIn(initialScale = 0.96f, animationSpec = tween(130)),
            exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.96f, animationSpec = tween(100))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showModesOverlay = false },
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .padding(top = 56.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                        .widthIn(max = 460.dp)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Режими калькулятора",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        modeEntries.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { entry ->
                                    val selected = currentMode == entry.mode
                                    Surface(
                                        onClick = {
                                            onFeedback()
                                            mainViewModel.selectMode(entry.mode)
                                            showModesOverlay = false
                                        },
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.65f)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag(entry.testTag)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = entry.icon,
                                                contentDescription = entry.title,
                                                tint = if (selected) {
                                                    MaterialTheme.colorScheme.onBackground
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = entry.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = entry.subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (rowItems.size == 1) {
                                    // Pair the last single mode (Історія) with Settings in a symmetrical 2x4 grid
                                    Surface(
                                        onClick = {
                                            onFeedback()
                                            showModesOverlay = false
                                            mainViewModel.openSettings()
                                        },
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("grid_settings")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Settings,
                                                contentDescription = "Налаштування",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Налаштування",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Тема, точність, звук",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
