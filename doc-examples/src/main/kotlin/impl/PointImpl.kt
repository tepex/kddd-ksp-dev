package ru.it_arch.kddd.magic.impl

import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject
import ru.it_arch.kddd.magic.domain.Point

@ConsistentCopyVisibility
data class PointImpl private constructor(
    override val x: Point.Coordinate,
    override val y: Point.Coordinate
) : Point {
    init {
        validate()
    }

    override val distanceIdentity: Point.Distance
        get() = DistanceImpl.ZERO

    @Suppress("UNCHECKED_CAST")
    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T =
        Builder().apply {
            x = args[0] as Point.Coordinate
            y = args[1] as Point.Coordinate
        }.build() as T

    @JvmInline
    value class CoordinateImpl private constructor(
        override val boxed: Int,
    ) : Point.Coordinate {
        init {
            validate()
        }

        override fun toString(): String =
            boxed.toString()

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Value<Int>> fork(boxed: Int): T =
            CoordinateImpl(boxed) as T

        companion object {
            operator fun invoke(boxed: Int): Point.Coordinate = CoordinateImpl(boxed)
        }
    }

    @JvmInline
    value class DistanceImpl private constructor(
        override val boxed: Double
    ) : Point.Distance {
        init {
            validate()
        }

        override fun toString(): String =
            boxed.toString()

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Value<Double>> fork(boxed: Double): T =
            DistanceImpl(boxed) as T

        companion object {
            val ZERO: Point.Distance = DistanceImpl(0.0)

            operator fun invoke(value: Double): Point.Distance =
                DistanceImpl(value)
        }
    }

    class Builder {
        var x: Point.Coordinate? = null
        var y: Point.Coordinate? = null

        fun build(): Point {
            checkNotNull(x) { "Property 'PointImpl.x' is not set!" }
            checkNotNull(y) { "Property 'PointImpl.y' is not set!" }
            return PointImpl(x!!, y!!)
        }
    }
}
