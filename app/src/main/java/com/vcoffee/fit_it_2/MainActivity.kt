package com.vcoffee.fit_it_2

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vcoffee.fit_it_2.ui.theme.FitIt2Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fit_it_data")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitIt2Theme {
                val navController = rememberNavController()
                val viewModel: FoodViewModel = viewModel(
                    factory = FoodViewModelFactory(dataStore)
                )
                AppScaffold(navController, viewModel)
            }
        }
    }
}

@Composable
fun AppScaffold(navController: NavHostController, viewModel: FoodViewModel) {
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            NavigationHost(navController, viewModel)
        }
    }
}

@Composable
fun NavigationHost(navController: NavHostController, viewModel: FoodViewModel) {
    NavHost(
        navController = navController,
        startDestination = Screen.Calendar.route
    ) {
        composable(Screen.Calendar.route) {
            CalendarScreen(viewModel)
        }
        composable(Screen.Storage.route) {
            StorageScreen(viewModel)
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(Screen.Calendar, Screen.Storage)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title, fontSize = 12.sp) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class Day(
    val number: Int,
    val month: Int,
    val year: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean = false
) {
    fun toDateString(): String = "$number.${month + 1}.$year"
    fun toKey(): String = "$year-${month + 1}-$number"
}

@Serializable
data class DailyEntry(
    val date: String,
    val foodIds: List<Int>,
    val waterMl: Int = 0 // Dodane pole dla ilości wody
)

@Serializable
data class FoodItem(
    val id: Int,
    val name: String,
    val calories: Int
)

class FoodViewModel(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {
    private val _foodItems = MutableStateFlow<List<FoodItem>>(emptyList())
    val foodItems: StateFlow<List<FoodItem>> = _foodItems.asStateFlow()

    private val _dailyEntries = MutableStateFlow<Map<String, DailyEntry>>(emptyMap())
    val dailyEntries: StateFlow<Map<String, DailyEntry>> = _dailyEntries.asStateFlow()

    private val FOOD_ITEMS_KEY = stringPreferencesKey("food_items")
    private val DAILY_ENTRIES_KEY = stringPreferencesKey("daily_entries")

    init {
        viewModelScope.launch {
            loadInitialData()
        }
    }

    private suspend fun loadInitialData() {
        try {
            val prefs = dataStore.data.first()

            // Load food items
            val foodJson = prefs[FOOD_ITEMS_KEY] ?: "[]"
            _foodItems.value = try {
                Json.decodeFromString(foodJson)
            } catch (e: Exception) {
                Log.e("FoodVM", "Error parsing food items", e)
                emptyList()
            }

            // Load daily entries
            val entriesJson = prefs[DAILY_ENTRIES_KEY] ?: "[]"
            _dailyEntries.value = try {
                Json.decodeFromString<List<DailyEntry>>(entriesJson)
                    .associateBy { it.date }
            } catch (e: Exception) {
                Log.e("FoodVM", "Error parsing daily entries", e)
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e("FoodVM", "Error loading data", e)
        }
    }

    private suspend fun saveFoodItems() {
        val list = _foodItems.value
        val json = Json.encodeToString(list)
        dataStore.edit { prefs ->
            prefs[FOOD_ITEMS_KEY] = json
        }
    }

    private suspend fun saveDailyEntries() {
        val entriesList = _dailyEntries.value.values.toList()
        val json = Json.encodeToString(entriesList)
        dataStore.edit { prefs ->
            prefs[DAILY_ENTRIES_KEY] = json
        }
    }

    fun addFoodItem(name: String, calories: Int) {
        viewModelScope.launch {
            val newId = (_foodItems.value.maxOfOrNull { it.id } ?: 0) + 1
            val newItem = FoodItem(newId, name, calories)
            _foodItems.value += newItem
            saveFoodItems()
        }
    }

    fun removeFoodItem(item: FoodItem) {
        viewModelScope.launch {
            _foodItems.value = _foodItems.value.filter { it.id != item.id }
            saveFoodItems()
        }
    }

    fun updateDailyEntry(date: String, foodIds: List<Int>, waterMl: Int) {
        viewModelScope.launch {
            val newEntry = DailyEntry(date, foodIds, waterMl)
            _dailyEntries.value = _dailyEntries.value.toMutableMap().apply {
                this[date] = newEntry
            }
            saveDailyEntries()
        }
    }
}

class FoodViewModelFactory(
    private val dataStore: DataStore<Preferences>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FoodViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FoodViewModel(dataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: FoodViewModel) {
    val dailyEntries by viewModel.dailyEntries.collectAsState()
    val foodItems by viewModel.foodItems.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    rememberCoroutineScope()
    val calendar = remember { Calendar.getInstance() }

    var currentMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }

    var showDayDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<Day?>(null) }

    val days = remember(currentMonth, currentYear) {
        generateCalendarDays(currentMonth, currentYear)
    }

    val weeks = remember(days) {
        days.chunked(7)
    }

    val monthNames = listOf(
        "Styczeń", "Luty", "Marzec", "Kwiecień", "Maj", "Czerwiec",
        "Lipiec", "Sierpień", "Wrzesień", "Październik", "Listopad", "Grudzień"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header with navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (currentMonth == Calendar.JANUARY) {
                        currentMonth = Calendar.DECEMBER
                        currentYear--
                    } else {
                        currentMonth--
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Poprzedni miesiąc")
                }

                Text(
                    text = "${monthNames[currentMonth]} $currentYear",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    if (currentMonth == Calendar.DECEMBER) {
                        currentMonth = Calendar.JANUARY
                        currentYear++
                    } else {
                        currentMonth++
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Następny miesiąc")
                }
            }

            // Day names header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd").forEach { dayName ->
                    Text(
                        text = dayName,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Calendar grid
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                items(weeks) { week ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        week.forEach { day ->
                            val dateKey = "${day.year}-${day.month + 1}-${day.number}"
                            val dailyEntry = dailyEntries[dateKey]
                            val hasEntries = dailyEntry?.foodIds?.isNotEmpty() == true || dailyEntry?.waterMl ?: 0 > 0

                            val backgroundColor = if (day.isToday) {
                                MaterialTheme.colorScheme.primary
                            } else if (day.isCurrentMonth) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }

                            val textColor = if (day.isToday) {
                                MaterialTheme.colorScheme.onPrimary
                            } else if (day.isCurrentMonth) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(backgroundColor)
                                    .clickable {
                                        selectedDay = day
                                        showDayDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.number.toString(),
                                    color = textColor,
                                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                                )

                                // Show indicator if day has entries
                                if (hasEntries) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary)
                                    )
                                }

                                // Plus icon for adding entries
                                if (day.isCurrentMonth) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = "Dodaj posiłek",
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.secondary
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

    // Dialog for adding meals to a day
    if (showDayDialog && selectedDay != null) {
        DayMealDialog(
            day = selectedDay!!,
            viewModel = viewModel,
            foodItems = foodItems,
            onDismiss = { showDayDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayMealDialog(
    day: Day,
    viewModel: FoodViewModel,
    foodItems: List<FoodItem>,
    onDismiss: () -> Unit
) {
    val dailyEntries by viewModel.dailyEntries.collectAsState()
    val dateKey = "${day.year}-${day.month + 1}-${day.number}"
    val dailyEntry = dailyEntries[dateKey]

    // Create a mutable list of selected food IDs
    val selectedFoodIds = remember { mutableStateListOf<Int>() }

    // Water tracking
    var waterAmount by remember { mutableStateOf("") }

    // Search functionality
    var searchQuery by remember { mutableStateOf("") }
    val filteredFoodItems = remember(foodItems, searchQuery) {
        if (searchQuery.isBlank()) {
            foodItems
        } else {
            foodItems.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Calculate total calories
    val totalCalories = selectedFoodIds.sumOf { id ->
        foodItems.find { it.id == id }?.calories ?: 0
    }

    // Initialize selectedFoodIds and waterAmount when dialog opens
    LaunchedEffect(dailyEntry) {
        selectedFoodIds.clear()
        dailyEntry?.foodIds?.let { selectedFoodIds.addAll(it) }
        waterAmount = dailyEntry?.waterMl?.toString() ?: ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Posiłki na dzień: ${day.toDateString()}") },
        text = {
            Column {
                // Water intake field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Woda:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = waterAmount,
                        onValueChange = { waterAmount = it.filter { char -> char.isDigit() } },
                        label = { Text("ml") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(120.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Dzisiaj: ${waterAmount.ifBlank { "0" }} ml")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search bar for meals
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Szukaj posiłków") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Szukaj") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredFoodItems.isEmpty()) {
                    Text("Brak posiłków spełniających kryteria",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(filteredFoodItems) { foodItem ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = selectedFoodIds.contains(foodItem.id),
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            selectedFoodIds.add(foodItem.id)
                                        } else {
                                            selectedFoodIds.remove(foodItem.id)
                                        }
                                    }
                                )
                                Text(foodItem.name, modifier = Modifier.padding(start = 8.dp))
                                Spacer(modifier = Modifier.weight(1f))
                                Text("${foodItem.calories} kcal",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Display total calories
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Suma kalorii:", fontWeight = FontWeight.Bold)
                    Text("$totalCalories kcal", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val water = waterAmount.toIntOrNull() ?: 0
                    viewModel.updateDailyEntry(dateKey, selectedFoodIds.toList(), water)
                    onDismiss()
                }
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

private fun generateCalendarDays(month: Int, year: Int): List<Day> {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.MONTH, month)
        set(Calendar.YEAR, year)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    // Get today's date for highlighting
    val today = Calendar.getInstance()
    val isToday = { day: Int, month: Int, year: Int ->
        day == today.get(Calendar.DAY_OF_MONTH) &&
                month == today.get(Calendar.MONTH) &&
                year == today.get(Calendar.YEAR)
    }

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Get first day of week (1 = Sunday, 2 = Monday, etc.)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    // Convert to Monday-first week (Monday = 0, Sunday = 6)
    val offset = (firstDayOfWeek - Calendar.MONDAY + 7) % 7

    val days = mutableListOf<Day>()

    // Previous month days
    val prevMonth = if (month == Calendar.JANUARY) Calendar.DECEMBER else month - 1
    val prevYear = if (month == Calendar.JANUARY) year - 1 else year
    val prevMonthDays = Calendar.getInstance().apply {
        set(Calendar.MONTH, prevMonth)
        set(Calendar.YEAR, prevYear)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    for (i in 0 until offset) {
        val dayNumber = prevMonthDays - offset + i + 1
        days.add(Day(dayNumber, prevMonth, prevYear, false, isToday(dayNumber, prevMonth, prevYear)))
    }

    // Current month days
    for (i in 1..daysInMonth) {
        days.add(Day(i, month, year, true, isToday(i, month, year)))
    }

    // Next month days (to fill the grid)
    val nextMonth = if (month == Calendar.DECEMBER) Calendar.JANUARY else month + 1
    val nextYear = if (month == Calendar.DECEMBER) year + 1 else year
    val remaining = 42 - days.size // 6 weeks * 7 days

    for (i in 1..remaining) {
        days.add(Day(i, nextMonth, nextYear, false, isToday(i, nextMonth, nextYear)))
    }

    return days
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(viewModel: FoodViewModel) {
    val foodItems by viewModel.foodItems.collectAsState(emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var foodName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }

    // Search functionality
    var searchQuery by remember { mutableStateOf("") }
    val filteredFoodItems = remember(foodItems, searchQuery) {
        if (searchQuery.isBlank()) {
            foodItems
        } else {
            foodItems.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(showAddDialog) {
        if (showAddDialog) {
            foodName = ""
            calories = ""
        }
    }

    val isInputValid = remember(foodName, calories) {
        foodName.isNotBlank() && calories.toIntOrNull()?.let { it > 0 } == true
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Dodaj nowy produkt") },
            text = {
                Column {
                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text("Nazwa produktu") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { input -> calories = input.filter { it.isDigit() } },
                        label = { Text("Kalorie na 100g") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val calVal = calories.toIntOrNull() ?: 0
                        Log.d("StorageScreen", "Adding: name='$foodName', kcal=$calVal")
                        if (isInputValid) {
                            viewModel.addFoodItem(foodName, calVal)
                            showAddDialog = false
                        }
                    },
                    enabled = isInputValid
                ) { Text("Dodaj") }
            },
            dismissButton = {
                Button(onClick = { showAddDialog = false }) { Text("Anuluj") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add food")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Szukaj posiłków") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Szukaj") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            if (filteredFoodItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (foodItems.isEmpty()) {
                        Text(
                            text = "Brak produktów",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    } else {
                        Text(
                            text = "Brak wyników wyszukiwania",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredFoodItems) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${item.calories} kcal/100g",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                                Button(
                                    onClick = { viewModel.removeFoodItem(item) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Usuń")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Calendar : Screen("calendar", "Calendar", Icons.Filled.DateRange)
    data object Storage  : Screen("storage",  "Storage",  Icons.Filled.Build)
}