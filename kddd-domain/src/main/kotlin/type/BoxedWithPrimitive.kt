package ru.it_arch.clean_ddd.domain.type

import ru.it_arch.clean_ddd.domain.type.KdddType.Boxed
import ru.it_arch.clean_ddd.domain.type.KdddType.Generatable
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
public data class BoxedWithPrimitive private constructor(
    private val generatable: GeneratableDelegate,
    public val boxed: PrimitiveClassName
) : Generatable by generatable, Boxed, ValueObject.Data {

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    override fun validate() {}

    public enum class PrimitiveClassName {
        STRING,
        BOOLEAN,
        BYTE,
        CHAR,
        FLOAT,
        DOUBLE,
        INT,
        LONG,
        SHORT
    }

    public companion object {
        public operator fun invoke(gd: GeneratableDelegate, boxed: PrimitiveClassName): BoxedWithPrimitive =
            BoxedWithPrimitive(gd, boxed)
    }
}
