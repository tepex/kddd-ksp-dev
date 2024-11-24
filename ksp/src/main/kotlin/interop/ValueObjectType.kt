package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

internal interface ValueObjectType {
    object ValueObject : ValueObjectType {
        const val CLASSNAME = "ru.it_arch.ddd.ValueObject"
    }

    class ValueObjectSingle private constructor(val boxedType: TypeName) : ValueObjectType {
        companion object {
            const val CLASSNAME = "ru.it_arch.ddd.ValueObjectSingle"
            private val RE = """^ru.it_arch.ddd.ValueObjectSingle<(.+)>$""".toRegex()

            fun create(superInterfaceName: String): ValueObjectSingle =
                RE.find(superInterfaceName)?.let { ValueObjectSingle(ClassName.bestGuess(it.groupValues[1])) }
                    ?: throw IllegalArgumentException("Class name `$superInterfaceName` not match RE $RE")
        }
    }
}
