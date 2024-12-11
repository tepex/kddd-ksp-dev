package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.impl.PointImpl
import ru.it_arch.ddd.ValueObject
import ru.it_arch.ddd.ValueObjectSingle

public interface Point : ValueObject {
    public val x: X
    public val y: Y

    override fun validate() {   }

    public interface X : ValueObjectSingle<Int> {

        override fun validate() {   }
        public operator fun plus(other: X): X =
            copy(value + other.value)
    }

    public interface Y : ValueObjectSingle<Int> {
        override fun validate() {   }
        public operator fun plus(other: Y): Y =
            copy(value + other.value)
    }
}

public operator fun PointImpl.plus(other: PointImpl): PointImpl =
    toBuilder<PointImpl.Builder>().also { builder ->
        builder.x = x + other.x
        builder.y = y + other.y
    }.build()

