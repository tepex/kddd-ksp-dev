package ru.it_arch.kddd.magic.domain

fun Point.asString(): String =
    "(${x.boxed},${y.boxed})"
