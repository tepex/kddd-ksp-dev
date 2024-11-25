package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.TypeName

internal class KDParameterBase(
    override val name: String,
    override val type: TypeName
) : IKDParameter {

    override fun toString(): String =
        "`$name`: $type"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KDParameterBase) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int =
        name.hashCode()
}
