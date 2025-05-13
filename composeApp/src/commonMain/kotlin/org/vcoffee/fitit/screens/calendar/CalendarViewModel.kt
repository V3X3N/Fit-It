package org.vcoffee.fitit.screens.calendar

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.util.Calendar

class CalendarViewModel : ViewModel() {
    private val calendarInstance: Calendar = Calendar.getInstance()

    val currentYear = mutableStateOf(calendarInstance.get(Calendar.YEAR))
    val currentMonth = mutableStateOf(calendarInstance.get(Calendar.MONTH))
    val selectedDay = mutableStateOf(calendarInstance.get(Calendar.DAY_OF_MONTH))

    fun previousMonth() {
        if (currentMonth.value == 0) {
            currentMonth.value = 11
            currentYear.value -= 1
        } else {
            currentMonth.value -= 1
        }
    }

    fun nextMonth() {
        if (currentMonth.value == 11) {
            currentMonth.value = 0
            currentYear.value += 1
        } else {
            currentMonth.value += 1
        }
    }

    fun selectDay(day: Int) {
        selectedDay.value = day
    }
}
