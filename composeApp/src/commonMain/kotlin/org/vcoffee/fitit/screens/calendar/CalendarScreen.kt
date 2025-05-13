package org.vcoffee.fitit.screens.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun CalendarScreen(viewModel: CalendarViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.previousMonth() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
            }
            Text(
                text = "${viewModel.currentYear.value} - ${monthName(viewModel.currentMonth.value)}",
                style = MaterialTheme.typography.h6
            )
            IconButton(onClick = { viewModel.nextMonth() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(text = day, style = MaterialTheme.typography.caption)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val daysInMonth = daysInMonth(viewModel.currentYear.value, viewModel.currentMonth.value)
        val calendar = Calendar.getInstance().apply {
            set(viewModel.currentYear.value, viewModel.currentMonth.value, 1)
        }
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val totalCells = ((firstDayOfWeek - 1) + daysInMonth).let {
            if (it % 7 == 0) it else it + (7 - it % 7)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(totalCells) { index ->
                val dayNumber = index - (firstDayOfWeek - 1) + 1
                if (index < (firstDayOfWeek - 1) || dayNumber > daysInMonth) {
                    Box(modifier = Modifier.size(40.dp))
                } else {
                    val isSelected = dayNumber == viewModel.selectedDay.value
                    Surface(
                        color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(40.dp)
                            .clickable { viewModel.selectDay(dayNumber) },
                        shape = MaterialTheme.shapes.small,
                        elevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = dayNumber.toString(),
                                style = MaterialTheme.typography.body2,
                                color = if (isSelected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun monthName(monthIndex: Int): String {
    val months = listOf(
        "January", "February", "March", "April",
        "May", "June", "July", "August",
        "September", "October", "November", "December"
    )
    return months[monthIndex]
}

fun daysInMonth(year: Int, month: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}
