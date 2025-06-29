package ru.it_arch.kddd.magic.domain

import kotlin.math.sqrt

/** [Регламент/Интерфейс CDT п.8](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-function) */
infix fun Point.`distance to`(other: Point): Point.Distance =
        //println("src: $src, other: ${other.getOrThrow()}")
    (this - other)
        .let { it * it }
        .let { it.x + it.y }
        .let { sqrt(it.boxed.toDouble()) }
        .let { neutralDistance + it }

infix fun Result<Point>.`distance to`(other: Result<Point>): Result<Point.Distance> =
    mapCatching { src ->
        src `distance to` other.getOrThrow()
    }

fun interface DistanceFm {
    fun invoke(p1: Point, p2: Point): Point.Distance
}

class DistanceGeom : DistanceFm {
    override fun invoke(p1: Point, p2: Point): Point.Distance =
        p1 `distance to` p2
}
