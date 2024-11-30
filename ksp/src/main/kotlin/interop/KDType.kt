package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec
import ru.it_arch.ddd.IEntity
import ru.it_arch.ddd.ValueObject
import ru.it_arch.ddd.ValueObjectSingle

internal class KDType private constructor(
    val implName: ImplClassName,
    val builder: TypeSpec.Builder,
    val parameters: Set<KDParameter>,
    val valueObjectType: KDValueObjectType
) : IEntity {

    override val id: ValueObject
        get() = implName

    override fun validate() {}

    private val _innerTypes = mutableSetOf<KDType>()
    val innerTypes: Set<KDType>
        get() = _innerTypes.toSet()

    @JvmInline
    value class ImplClassName private constructor(override val value: ClassName) : ValueObjectSingle<ClassName> {

        val name: String
            get() = value.simpleName

        override fun validate() {}

        override fun toString(): String =
            value.toString()

        companion object {
            fun create(className: ClassName) =
                ImplClassName(className)
        }
    }

    companion object {
        fun create(implName: ClassName, builder: TypeSpec.Builder, parameters: Set<KDParameter>, valueObjectType: KDValueObjectType) =
            KDType(ImplClassName.create(implName), builder, parameters, valueObjectType)
    }
}
