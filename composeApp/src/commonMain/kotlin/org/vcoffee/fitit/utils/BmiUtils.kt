package org.vcoffee.fitit.utils

fun calculateBmi(weight: Float, height: Float): Float {
    return weight / (height * height)
}
