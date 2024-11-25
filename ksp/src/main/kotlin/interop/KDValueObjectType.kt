package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

internal interface KDValueObjectType {
    object KDValueObject : KDValueObjectType {
        const val CLASSNAME = "ru.it_arch.ddd.ValueObject"
    }

    class KDValueObjectSingle private constructor(val boxedType: TypeName) : KDValueObjectType {
        companion object {
            const val CLASSNAME = "ru.it_arch.ddd.ValueObjectSingle"
            private val RE = """^ru.it_arch.ddd.ValueObjectSingle<(.+)>$""".toRegex()

            fun create(superInterfaceName: String): KDValueObjectSingle =
                RE.find(superInterfaceName)?.let { KDValueObjectSingle(ClassName.bestGuess(it.groupValues[1])) }
                    ?: throw IllegalArgumentException("Class name `$superInterfaceName` not match RE $RE")
        }
    }
}
