package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.impl.PointImpl
import ru.it_arch.kddd.ValueObject

public interface Point : ValueObject.Data {
    public val x: X
    public val y: Y
    public val en: MyEnum

    override fun validate() {   }

    public interface X : ValueObject.Boxed<Int> {

        override fun validate() {   }
        public operator fun plus(other: X): X =
            copy(boxed + other.boxed)
    }

    public interface Y : ValueObject.Boxed<Int> {
        override fun validate() {   }
        public operator fun plus(other: Y): Y =
            copy(boxed + other.boxed)
    }

    public enum class MyEnum : ValueObject.Sealed {
        A, B, C
    }
}

public operator fun PointImpl.plus(other: PointImpl): PointImpl =
    toBuilder().also { builder ->
        builder.x = x + other.x
        builder.y = y + other.y
    }.build()
