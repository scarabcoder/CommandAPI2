package com.scarabcoder.commandapi2

import com.google.common.primitives.Ints

fun Int.constrain(min: Int, max: Int): Int {
    return Ints.constrainToRange(this, min, max)
}

fun Int.constrainMax(max: Int): Int {
    return constrain(Integer.MIN_VALUE, max)
}

fun Int.constrainMin(min: Int): Int {
    return constrain(min, Integer.MAX_VALUE)
}