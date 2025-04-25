package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.clean_ddd.domain.demo.impl.ACrossRefImpl
import ru.it_arch.kddd.ValueObject
import ru.it_arch.clean_ddd.domain.demo.sub.Point as Point1

public interface ACrossRef : ValueObject.Data {

    public val myType: MyCustomInnerType
    public val point: Point
    public val point1: Point1

    override fun validate() {
    }

    public interface MyCustomInnerType : ValueObject.Boxed<Int> {
        override fun validate() {

        }
    }
}

public val crossRef: ACrossRef = ACrossRefImpl.DslBuilder().apply {
    myType = 33

}.build()
