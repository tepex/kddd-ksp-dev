package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.TypeName

internal interface KDValueObjectType {
    object KDValueObject : KDValueObjectType {
        const val CLASSNAME = "ru.it_arch.ddd.ValueObject"
    }

    class KDValueObjectSingle private constructor(val boxedType: TypeName) : KDValueObjectType {
        companion object {
            const val CLASSNAME = "ru.it_arch.ddd.ValueObjectSingle"

            fun create(superInterfaceName: String): KDValueObjectSingle =
                superInterfaceName.parseClassParameters().firstOrNull()?.let(::KDValueObjectSingle)
                    ?: throw IllegalArgumentException("Class name `$superInterfaceName` expected type parameter")
        }
    }
}
