package ru.it_arch.clean_ddd.domain.demo.sub

import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.ValueObject

@KDGeneratable(json = false, dsl = true)
public interface Point : ValueObject.Data {
    public val a: Coordinate
    public val b: Coordinate

    override fun validate() { }

    public fun asString(): String =
        "{${a.boxed},${b.boxed}}"

    public operator fun plus(other: Point): Point =
        fork(a + other.a, b + other.b)

    public operator fun minus(other: Point): Point =
        fork(a - other.a, b - other.b)

    public operator fun times(other: Point): Point =
        fork(a * other.a, b * other.b)

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
