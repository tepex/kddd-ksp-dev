package ru.it_arch.kddd.magic.domain

import java.text.NumberFormat
import java.util.Locale

fun Point.asString(): String =
    "(${x.boxed},${y.boxed})"
