package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.Kddd

@ConsistentCopyVisibility
public data class BoxedWithPrimitive private constructor(
    private val generatable: GeneratableDelegate,
    public val boxed: PrimitiveClassName
) : KdddType.Generatable by generatable, KdddType.Boxed {

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
