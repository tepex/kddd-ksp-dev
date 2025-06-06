package ru.it_arch.kddd.magic.domain

import ru.it_arch.kddd.ValueObject
import java.math.BigDecimal

interface Price : ValueObject.Value<BigDecimal> {
    override fun validate() {

    }

    operator fun plus(other: Price): Price =
        fork(boxed + other.boxed)

    operator fun minus(other: Price): Price =
        fork(boxed - other.boxed)

    operator fun times(other: Price): Price =
        fork(boxed * other.boxed)

    operator fun div(other: Price): Price =
        fork(boxed / other.boxed)

    operator fun compareTo(other: Price): Int =
        boxed.compareTo(other.boxed)
}
