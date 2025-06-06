package ru.it_arch.kddd.magic.domain

infix fun Result<Point>.`distance to`(other: Result<Point>): Result<Point.Distance> =
    mapCatching { src ->
        println("src: $src, other: ${other.getOrThrow()}")
        (src - other.getOrThrow())
            .let { it * it }
            .let { it.x + it.y }
            .let { kotlin.math.sqrt(it.boxed.toDouble()) }
            .let { ret ->
                src.distanceIdentity.fork(ret)
            }
    }
