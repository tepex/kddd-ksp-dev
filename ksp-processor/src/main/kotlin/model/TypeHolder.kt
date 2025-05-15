package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.TypeName
import ru.it_arch.clean_ddd.ksp.PropertyHolders
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
internal data class TypeHolder private constructor(
    val classType: TypeName,
    val properties: PropertyHolders
) : ValueObject.Data {

    init {
        validate()
    }

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    companion object {
        operator fun invoke(classType: TypeName, properties: PropertyHolders): TypeHolder =
            TypeHolder(classType, properties)
    }
}
