package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.ValueObject

@KDGeneratable(json = false, dsl = true)
public interface Point : ValueObject.Data {
    public val x: Coordinate
    public val y: Coordinate

    override fun validate() { }

    public fun asString(): String =
        "{${x.boxed},${y.boxed}}"

    public operator fun plus(other: Point): Point =
        fork(x + other.x, y + other.y)

    public operator fun minus(other: Point): Point =
        fork(x - other.x, y - other.y)

    public operator fun times(other: Point): Point =
        fork(x * other.x, y * other.y)

    public interface Coordinate : ValueObject.Boxed<Int> {
        override fun validate() {}

        public operator fun plus(other: Coordinate): Coordinate =
            fork(boxed + other.boxed)

        public operator fun minus(other: Coordinate): Coordinate =
            fork(boxed - other.boxed)

        public operator fun times(other: Coordinate): Coordinate =
            fork(boxed * other.boxed)
    }
}
