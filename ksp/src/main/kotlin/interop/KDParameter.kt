package ru.it_arch.clean_ddd.ksp.interop

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName

internal class KDParameter private constructor(
    override val name: IKDParameter.Name,
    type: TypeName
) : IKDParameter {

    override var kdType: IKDParameter.KDType = IKDParameter.KDType.create(type)
        private set

    override fun validate() {}

    /*
    override fun setType(type: TypeName) {
        this.type = typeN.toNullable()
    }*/

    /*
    override fun replaceParametersType(replacements: Map<WrapperType, BoxedType>) {
        (type as? ParameterizedTypeName)?.also { pt ->
            pt.typeArguments.toMutableList().apply {
                forEachIndexed { i, arg ->
                    replacements.getBoxedType(arg)?.also { this[i] = it.toNullable(arg.isNullable) }
                }
                type = pt.copy(typeArguments = this)
            }
        }
    }*/

    override fun toString(): String =
        "`$name`: $kdType"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KDParameter) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int =
        id.hashCode()


    @JvmInline
    private value class NameImpl private constructor(override val value: String) : IKDParameter.Name {

        override fun toString(): String =
            value

        companion object {
            fun name(value: String) = NameImpl(value)
        }
    }

    companion object {
        fun create(property: KSPropertyDeclaration) =
            KDParameter(NameImpl.name(property.simpleName.asString()), property.type.toTypeName())

        fun create(name: String, type: TypeName) =
            KDParameter(NameImpl.name(name), type)
    }
}

