package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.impl.PointImpl
import ru.it_arch.kddd.ValueObject

public interface Point : ValueObject.Data {
    public val x: X
    public val y: Y

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
}

public operator fun PointImpl.plus(other: PointImpl): PointImpl =
    toBuilder().also { builder ->
        builder.x = x + other.x
        builder.y = y + other.y
    }.build()

public fun String.toPoint(): Point =
    split(":").let { sp ->
        require(sp.size > 1) { "Can't parse $this! Expected '<x>:<y>'" }
        val _x = sp[0].toIntOrNull()
        val _y = sp[1].toIntOrNull()
        requireNotNull(_x) { "x must be integer!" }
        requireNotNull(_y) { "y must be integer!" }
        PointImpl.Builder().apply {
            x = PointImpl.XImpl.create(_x)
            y = PointImpl.YImpl.create(_y)
        }.build()
    }

public fun Point.serialize(): String =
    "$x:$y"
