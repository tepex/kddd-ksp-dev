package ru.it_arch.kddd.magic

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import ru.it_arch.kddd.magic.domain.asString
import ru.it_arch.kddd.magic.domain.`distance to`
import ru.it_arch.kddd.magic.impl.PointImpl

fun examplePoint() {
    val point1 = runCatching {
        PointImpl.Builder().apply {
            x = PointImpl.CoordinateImpl(10)
            y = PointImpl.CoordinateImpl(14)
        }.build()
    }

    val point2 = runCatching {
        PointImpl.Builder().apply {
            x = PointImpl.CoordinateImpl(100)
            y = PointImpl.CoordinateImpl(200)
        }.build()
    }

    runCatching {
        point1.getOrThrow().asString() to point2.getOrThrow().asString()
    }
        .onSuccess { println("point1: ${it.first}, point2: ${it.second}") }
        .onFailure { println("error: $it") }

    (point1 `distance to` point2)
        .onSuccess { println("distance: ${it.boxed shouldBe 206.63.plusOrMinus(0.001)}") }
        .onFailure { println("error: $it") }
}
