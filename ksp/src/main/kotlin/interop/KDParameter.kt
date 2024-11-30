package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.ddd.IEntity
import ru.it_arch.ddd.ValueObject
import ru.it_arch.ddd.ValueObjectSingle

internal class KDParameter private constructor(
    val name: Name,
    val typeReference: KDReference
) : IEntity {

    override val id: ValueObject
        get() = name

    override fun validate() {}

    override fun toString(): String =
        "`$name`: $typeReference"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KDParameter) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int =
        id.hashCode()

    @JvmInline
    value class Name private constructor(override val value: String) : ValueObjectSingle<String> {

        override fun validate() {}

        override fun toString(): String =
            value

        companion object {
            fun name(value: String) = Name(value)
        }
    }

    companion object {
        fun create(property: KSPropertyDeclaration) =
            KDParameter(
                Name.name(property.simpleName.asString()),
                KDReference.create(property.type.toTypeName())
            )

        fun create(name: String, typeName: TypeName) =
            KDParameter(
                Name.name(name),
                KDReference.create(typeName)
            )
    }
}
