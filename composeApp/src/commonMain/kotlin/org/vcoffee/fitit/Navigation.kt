package org.vcoffee.fitit

import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import org.vcoffee.fitit.screens.BMI.BmiCalculatorScreen
import org.vcoffee.fitit.screens.BMI.BmiCalculatorViewModel
import org.vcoffee.fitit.screens.calendar.CalendarScreen
import org.vcoffee.fitit.screens.calendar.CalendarViewModel

sealed class Screen(val title: String) {
    data object BmiCalculator : Screen("BMI")
    data object Empty : Screen("Other")
}

@Composable
fun AppNavigation() {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.BmiCalculator) }
    val items = listOf(Screen.BmiCalculator, Screen.Empty)

    Scaffold(
        bottomBar = {
            BottomNavigation {
                items.forEach { screen ->
                    BottomNavigationItem(
                        selected = screen == selectedScreen,
                        onClick = { selectedScreen = screen },
                        label = { Text(screen.title) },
                        icon = {}
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedScreen) {
            is Screen.BmiCalculator -> {
                val viewModel = remember { BmiCalculatorViewModel() }
                BmiCalculatorScreen(viewModel, Modifier.padding(innerPadding))
            }
            is Screen.Empty -> {
                val viewModel = remember { CalendarViewModel() }
                CalendarScreen(viewModel, Modifier.padding(innerPadding))
            }
        }
    }
}