package ru.it_arch.kddd.magic.domain

import ru.it_arch.kddd.ValueObject

interface Point : ValueObject.Data {
    val x: Coordinate
    val y: Coordinate

    // dirty hack 3 варианта
    /** Нейтральный элемент [алгебра]. Необходим, чтобы иметь возможность оперировать объектом [Distance] на уровне
     * абстракций для написания use case.*/
    val distanceIdentity: Distance

    override fun validate() {}

    operator fun plus(other: Point): Point =
        fork(x + other.x, y + other.y)

    operator fun minus(other: Point): Point =
        fork(x - other.x, y - other.y)

    operator fun times(other: Point): Point =
        fork(x * other.x, y * other.y)

    interface Coordinate : ValueObject.Value<Int> {
        override fun validate() {}

        operator fun plus(other: Coordinate): Coordinate =
            fork(boxed + other.boxed)

        operator fun minus(other: Coordinate): Coordinate =
            fork(boxed - other.boxed)

        operator fun times(other: Coordinate): Coordinate =
            fork(boxed * other.boxed)
    }

    interface Distance : ValueObject.Value<Double> {

        override fun validate() {
            val range = 0.0..300.0
            check(boxed in range) { "Distance not in range $range" }
        }
    }
}
