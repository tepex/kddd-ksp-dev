package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.TypeName

internal class KDParameter private constructor(
    val base: KDParameterBase,
    val isNullable: Boolean = false
) : IKDParameter by base {

    private val _boxedTypes = mutableListOf<TypeName>()
    val boxedTypes: List<TypeName>
        get() = _boxedTypes.toList()

    fun addBoxedType(typeName: TypeName) {
        _boxedTypes.add(typeName)
    }

    fun getClassParameters(): List<TypeName> {
        return emptyList()
    }

    override fun toString(): String =
        "{$base, boxed types: $boxedTypes}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KDParameter) return false

        if (base != other.base) return false

        return true
    }

    override fun hashCode(): Int =
        base.hashCode()

    companion object {
        fun create(name: String, type: TypeName, isNullable: Boolean) =
            KDParameter(KDParameterBase(name, type), isNullable)
    }
}
