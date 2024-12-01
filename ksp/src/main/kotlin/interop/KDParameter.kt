package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.ddd.ValueObjectSingle

@ConsistentCopyVisibility
internal data class KDParameter private constructor(
    val name: Name,
    val typeReference: KDReference
) {

    @JvmInline
    value class Name private constructor(override val value: String) : ValueObjectSingle<String> {

        override fun validate() {}

        override fun toString(): String =
            value

        companion object {
            fun create(value: String) = Name(value)
        }
    }

    companion object {
        fun create(property: KSPropertyDeclaration) =
            KDParameter(
                Name.create(property.simpleName.asString()),
                KDReference.create(property.type.toTypeName())
            )

        fun create(name: String, typeName: TypeName) =
            KDParameter(
                Name.create(name),
                KDReference.create(typeName)
            )
    }
}
