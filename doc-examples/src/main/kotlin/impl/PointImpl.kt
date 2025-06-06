package ru.it_arch.kddd.magic.impl

import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject
import ru.it_arch.kddd.magic.domain.Point

/**
 * [Регламент/Имплементация CDT п.I.1](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-kddd)
 * [Регламент/Имплементация CDT п.I.2](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-private)
 * [Регламент/Имплементация CDT п.I.6.1](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-data-class)
 * */
@ConsistentCopyVisibility
data class PointImpl private constructor(
    /** [Регламент/Имплементация CDT п.I.3](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-params) */
    override val x: Point.Coordinate,
    /** [Регламент/Имплементация CDT п.I.4](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-val) */
    override val y: Point.Coordinate
) : Point {

    /** [Регламент/Имплементация CDT п.I.5](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-init) */
    init {
        validate()
    }

    override val distanceIdentity: Point.Distance
        get() = DistanceImpl.ZERO

    /** [Регламент/Имплементация CDT п.I.6.3](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-data-fork) */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T =
        Builder().apply {
            x = args[0] as Point.Coordinate
            y = args[1] as Point.Coordinate
        }.build() as T

    /**
     * [Регламент/Имплементация CDT п.I.7.1](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-value-class)
     * [Регламент/Имплементация CDT п.I.2](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-private)
     * */
    @JvmInline
    value class CoordinateImpl private constructor(
        /** [Регламент/Имплементация CDT п.I.7.2](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-boxed) */
        override val boxed: Int,
    ) : Point.Coordinate {
        /** [Регламент/Имплементация CDT п.I.5](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-init) */
        init {
            validate()
        }

        override fun toString(): String =
            boxed.toString()

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Value<Int>> fork(boxed: Int): T =
            CoordinateImpl(boxed) as T

        companion object {
            /** [Регламент/Имплементация CDT п.I.7.3](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-value-builder) */
            operator fun invoke(boxed: Int): Point.Coordinate = CoordinateImpl(boxed)
        }
    }

    /** [Регламент/Имплементация CDT п.I.7.1](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-value-class) */
    @JvmInline
    value class DistanceImpl private constructor(
        /** [Регламент/Имплементация CDT п.I.7.2](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-boxed) */
        override val boxed: Double
    ) : Point.Distance {
        /** [Регламент/Имплементация CDT п.I.5](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-init) */
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

            /** [Регламент/Имплементация CDT п.I.7.3](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-value-builder) */
            operator fun invoke(value: Double): Point.Distance =
                DistanceImpl(value)
        }
    }

    /** [Регламент/Имплементация CDT п.I.6.2](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-data-builder) */
    class Builder {
        /**
         * [Регламент/Имплементация CDT п.I.6.2.1](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-data-builder-properties)
         * [Регламент/Имплементация CDT п.I.6.2.2](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-data-builder-properties-var)
         * */
        var x: Point.Coordinate? = null
        /**
         * [Регламент/Имплементация CDT п.I.6.2.1](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-data-builder-properties)
         * [Регламент/Имплементация CDT п.I.6.2.2](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-data-builder-properties-var)
         * */
        var y: Point.Coordinate? = null

        /** [Регламент/Имплементация CDT п.I.6.2.3](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-data-builder-build) */
        fun build(): Point {
            /** [Регламент/Имплементация CDT п.I.6.2.4](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-data-builder-check) */
            checkNotNull(x) { "Property 'PointImpl.x' is not set!" }
            checkNotNull(y) { "Property 'PointImpl.y' is not set!" }
            return PointImpl(x!!, y!!)
        }
    }
}
