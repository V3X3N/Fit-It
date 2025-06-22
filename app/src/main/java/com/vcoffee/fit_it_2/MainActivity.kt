package com.vcoffee.fit_it_2

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fit_it_data")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitIt2Theme {
                val navController = rememberNavController()
                AppScaffold(navController)
            }
        }
    }
}

@Composable
fun AppScaffold(navController: NavHostController) {
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            NavigationHost(navController)
        }
    }
}

@Composable
fun NavigationHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Calendar.route
    ) {
        composable(Screen.Calendar.route) {
            CalendarScreen()
        }
        composable(Screen.Storage.route) {
            val viewModel: FoodViewModel = viewModel(
                factory = FoodViewModelFactory(LocalContext.current.dataStore)
            )
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

@Composable
fun CalendarScreen() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Wyświetlanie dni")
                    }
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Show days")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Calendar Screen",
                style = MaterialTheme.typography.headlineLarge,
            )
        }
    }
}

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

    private val FOOD_ITEMS_KEY = stringPreferencesKey("food_items")

    init {
        viewModelScope.launch {
            loadInitialData()
        }
    }

    private suspend fun loadInitialData() {
        try {
            val prefs = dataStore.data.first()
            val json = prefs[FOOD_ITEMS_KEY] ?: "[]"
            Log.d("FoodVM", "Loaded JSON: $json")
            _foodItems.value = Json.decodeFromString(json)
            Log.d("FoodVM", "Items loaded: ${_foodItems.value.size}")
        } catch (e: Exception) {
            Log.e("FoodVM", "Error loading data", e)
            _foodItems.value = emptyList()
        }
    }

    private suspend fun saveFoodItems() {
        val list = _foodItems.value
        val json = Json.encodeToString(list)
        Log.d("FoodVM", "Saving JSON: $json")
        try {
            dataStore.edit { prefs ->
                prefs[FOOD_ITEMS_KEY] = json
            }
            Log.d("FoodVM", "Saved items: ${list.size}")
        } catch (e: Exception) {
            Log.e("FoodVM", "Error saving data", e)
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

@Composable
fun StorageScreen(viewModel: FoodViewModel) {
    val foodItems by viewModel.foodItems.collectAsState(emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var foodName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (foodItems.isEmpty()) {
                Text(
                    text = "Brak produktów",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(foodItems) { item ->
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