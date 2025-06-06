package ru.it_arch.kddd.magic.domain

/** [Регламент/Интерфейс CDT п.8](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-function) */
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
