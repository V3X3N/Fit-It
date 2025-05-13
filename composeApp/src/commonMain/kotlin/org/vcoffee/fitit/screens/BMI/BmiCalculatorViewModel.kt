package org.vcoffee.fitit.screens.BMI

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import org.vcoffee.fitit.utils.calculateBmi

class BmiCalculatorViewModel : ViewModel() {
    var gender = mutableStateOf("Man")
    var weight = mutableStateOf("")
    var height = mutableStateOf("")
    var bmiResult = mutableStateOf(0f)

    fun calculateBmi() {
        val weightVal = weight.value.toFloatOrNull() ?: 0f
        val heightValCm = height.value.toFloatOrNull() ?: 0f
        val heightVal = if (heightValCm > 0) heightValCm / 100f else 0f
        if (heightVal > 0f) {
            bmiResult.value = calculateBmi(weightVal, heightVal)
        }
    }
}
