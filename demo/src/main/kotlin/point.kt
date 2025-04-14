package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.app.PointImpl.Builder
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

fun testPoint() {
    println("testing point")
    val p1 = PointImpl.Builder().apply {
        x = PointImpl.CoordinateImpl(10)
        y = PointImpl.CoordinateImpl(5)
    }.build()

    val p2 = PointImpl.Builder().apply {
        x = PointImpl.CoordinateImpl(20)
        y = PointImpl.CoordinateImpl(15)
    }.build()

    //val p3 = p1.to

    println("p1: ${p1.asString()}, p2: ${p2.asString()}")
    println("sum: ${(p1 + p2).asString()}")
}

interface Point : ValueObject.Data {
    val x: Coordinate
    val y: Coordinate

    fun asString(): String =
        "{${x.boxed},${y.boxed}}"

    operator fun plus(other: Point): Point =
        create(x + other.x, y + other.y)

    interface Coordinate : ValueObject.Boxed<Int> {
        override fun validate() {}

        operator fun plus(other: Coordinate): Coordinate =
            create(boxed + other.boxed)
    }
}

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
    override fun <T : Kddd, A : Kddd, B : Kddd> create(p1: A, p2: B): T =
        Builder().apply {
            x = p1 as Point.Coordinate
            y = p2 as Point.Coordinate
        }.build() as T

    @JvmInline
    value class CoordinateImpl private constructor(override val boxed: Int) : Point.Coordinate {
        init {
            validate()
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<Int>> create(boxed: Int): T = CoordinateImpl(boxed) as T

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
