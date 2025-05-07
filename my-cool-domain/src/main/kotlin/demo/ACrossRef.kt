package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.kddd.ValueObject
import ru.it_arch.clean_ddd.domain.demo.sub.Point as Point1

public interface ACrossRef : ValueObject.Data {

    public val myType: MyCustomInnerType
    public val point: Point
    public val point1: Point1
    public val x: Point1.Coordinate

    override fun validate() {
    }

    public interface MyCustomInnerType : ValueObject.Boxed<Int> {
        override fun validate() {

        }
    }
}
