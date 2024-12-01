package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName

internal interface KDValueObjectType {
    object KDValueObject : KDValueObjectType {
        const val CLASSNAME = "ru.it_arch.ddd.ValueObject"
    }

    @ConsistentCopyVisibility
    data class KDValueObjectSingle private constructor(val boxedType: TypeName) : KDValueObjectType {

        override fun toString(): String =
            "KDValueObjectSingle<$boxedType>"

        companion object {
            const val CLASSNAME = "ru.it_arch.ddd.ValueObjectSingle"

            fun create(superInterfaceName: TypeName): KDValueObjectSingle {
                require(superInterfaceName is ParameterizedTypeName && superInterfaceName.typeArguments.size == 1) {
                    "Class name `$superInterfaceName` expected type parameter"
                }
                return KDValueObjectSingle(superInterfaceName.typeArguments.first())
            }
        }
    }
}
