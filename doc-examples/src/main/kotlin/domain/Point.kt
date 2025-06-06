package ru.it_arch.kddd.magic.domain

import ru.it_arch.kddd.ValueObject

/**
 * [Регламент/Интерфейс CDT п.1](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-abstraction)
 * [Регламент/Интерфейс CDT п.2](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-kddd)
 * */
interface Point : ValueObject.Data {
    /** [Регламент/Интерфейс CDT п.3](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-immutable) */
    val x: Coordinate
    /** [Регламент/Интерфейс CDT п.3](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-immutable) */
    val y: Coordinate

    // dirty hack 3 варианта
    /** Нейтральный элемент [алгебра]. Необходим, чтобы иметь возможность оперировать объектом [Distance] на уровне
     * абстракций для написания use case.*/
    val distanceIdentity: Distance

    /** [Регламент/Интерфейс CDT п.7](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-validatable) */
    override fun validate() {}

    /** [Регламент/Интерфейс CDT п.8](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-function) */
    operator fun plus(other: Point): Point =
        fork(x + other.x, y + other.y)

    /** [Регламент/Интерфейс CDT п.8](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-function) */
    operator fun minus(other: Point): Point =
        fork(x - other.x, y - other.y)

    /** [Регламент/Интерфейс CDT п.8](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-function) */
    operator fun times(other: Point): Point =
        fork(x * other.x, y * other.y)

    /** [Регламент/Интерфейс CDT п.5.3](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-property-nested)
     * [Регламент/Интерфейс CDT п.6.1](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-property-boxed)
     * */
    interface Coordinate : ValueObject.Value<Int> {
        override fun validate() {}

        operator fun plus(other: Coordinate): Coordinate =
            fork(boxed + other.boxed)

        operator fun minus(other: Coordinate): Coordinate =
            fork(boxed - other.boxed)

        operator fun times(other: Coordinate): Coordinate =
            fork(boxed * other.boxed)
    }

    /** [Регламент/Интерфейс CDT п.5.3](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-property-nested)
     * [Регламент/Интерфейс CDT п.6.1](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-property-boxed)
     * */
    interface Distance : ValueObject.Value<Double> {

        /** [Регламент/Интерфейс CDT п.7](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-iface-validatable) */
        override fun validate() {
            val range = 0.0..300.0
            check(boxed in range) { "Distance not in range $range" }
        }
    }
}
