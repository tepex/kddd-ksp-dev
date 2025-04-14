package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.app.PointImpl.Builder
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject
import kotlin.math.sqrt

fun testPointNoDsl() {
    println("testing point")
    val p1 = PointImpl.Builder().apply {
        x = PointImpl.CoordinateImpl(10)
        y = PointImpl.CoordinateImpl(5)
    }.build()

    val p2 = PointImpl.Builder().apply {
        x = PointImpl.CoordinateImpl(20)
        y = PointImpl.CoordinateImpl(15)
    }.build()

    val p3 = p1.toBuilder().apply { y = PointImpl.CoordinateImpl(100) }.build()

    println("p1: ${p1.asString()}, p2: ${p2.asString()}, p3: ${p3.asString()}")
    println("sum: ${(p1 + p2).asString()} diff: ${(p1 - p2).asString()}")
    println("distance: ${distance(p1, p2)}")
}

interface Point : ValueObject.Data {
    val x: Coordinate
    val y: Coordinate

    fun asString(): String =
        "{${x.boxed},${y.boxed}}"

    operator fun plus(other: Point): Point =
        fork(x + other.x, y + other.y)

    operator fun minus(other: Point): Point =
        fork(x - other.x, y - other.y)

    operator fun times(other: Point): Point =
        fork(x * other.x, y * other.y)

    interface Coordinate : ValueObject.Boxed<Int> {
        override fun validate() {}

        operator fun plus(other: Coordinate): Coordinate =
            fork(boxed + other.boxed)

        operator fun minus(other: Coordinate): Coordinate =
            fork(boxed - other.boxed)

        operator fun times(other: Coordinate): Coordinate =
            fork(boxed * other.boxed)
    }
}

// use case
fun distance(p1: Point, p2: Point): Double =
    (p1 - p2).let { it * it }.let { it.x + it.y }.let { sqrt(it.boxed.toDouble()) }


@ConsistentCopyVisibility
data class PointImpl private constructor(
    override val x: Point.Coordinate,
    override val y: Point.Coordinate
) : Point {

    override fun validate() { }

    init {
        validate()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Kddd, A: Kddd> fork(vararg args: A): T =
        Builder().apply {
            x = args[0] as Point.Coordinate
            y = args[1] as Point.Coordinate
        }.build() as T

    @JvmInline
    value class CoordinateImpl private constructor(override val boxed: Int) : Point.Coordinate {
        init {
            validate()
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<Int>> fork(boxed: Int): T = CoordinateImpl(boxed) as T

        companion object {
            operator fun invoke(boxed: Int): Point.Coordinate = CoordinateImpl(boxed)
        }
    }

    class Builder {
        var x: Point.Coordinate? = null
        var y: Point.Coordinate? = null

        fun build(): Point {
            requireNotNull(x) { "" }
            requireNotNull(y) { "" }

            return PointImpl(x!!, y!!)
        }
    }
}

// Generated
fun Point.toBuilder(): Builder =
    Builder().also { builder ->
        builder.x = x
        builder.y = y
    }
