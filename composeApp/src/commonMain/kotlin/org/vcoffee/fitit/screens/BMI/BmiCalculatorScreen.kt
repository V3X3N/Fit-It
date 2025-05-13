package org.vcoffee.fitit.screens.BMI

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun BmiCalculatorScreen(viewModel: BmiCalculatorViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "BMI Calculator",
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Select Gender:", style = MaterialTheme.typography.h6)
                Row {
                    RadioButton(
                        selected = viewModel.gender.value == "Man",
                        onClick = { viewModel.gender.value = "Man" }
                    )
                    Text("Man", modifier = Modifier.padding(start = 4.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = viewModel.gender.value == "Woman",
                        onClick = { viewModel.gender.value = "Woman" }
                    )
                    Text("Woman", modifier = Modifier.padding(start = 4.dp))
                }

                OutlinedTextField(
                    value = viewModel.weight.value,
                    onValueChange = { viewModel.weight.value = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.height.value,
                    onValueChange = { viewModel.height.value = it },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { viewModel.calculateBmi() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Calculate BMI")
                }

                if (viewModel.bmiResult.value > 0f) {
                    Text(
                        text = "BMI Result: %.2f".format(viewModel.bmiResult.value),
                        style = MaterialTheme.typography.h5,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}