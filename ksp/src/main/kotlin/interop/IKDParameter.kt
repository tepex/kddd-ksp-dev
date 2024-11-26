package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.TypeName
import ru.it_arch.ddd.IEntity
import ru.it_arch.ddd.ValueObject
import ru.it_arch.ddd.ValueObjectSingle

internal interface IKDParameter : IEntity {
    val name: Name
    val type: TypeName

    fun setType(type: TypeName)

    fun replaceParametersType(replacements: Map<WrapperType, BoxedType>)

    override val id: ValueObject
        get() = name

    interface Name : ValueObjectSingle<String> {
        override fun validate() {}
    }
}
