package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.TypeName
import ru.it_arch.clean_ddd.domain.model.kddd.KdddType
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
internal data class TypeHolder private constructor(
    val kdddType: KdddType,
    val classType: TypeName,
    val propertyHolders: List<PropertyHolder>
) : ValueObject.Data {

    init {
        validate()
    }

    override fun validate() {
        check(propertyHolders.isNotEmpty()) { "propertyHolders must not be empty!" }
    }

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    class Builder {
        var kdddType: KdddType? = null
        var classType: TypeName? = null
        var propertyHolders = emptyList<PropertyHolder>()

        fun build(): TypeHolder {
            checkNotNull(kdddType) { "Property 'kdddType' must be initialized!" }
            checkNotNull(classType) { "Property 'classType' must be initialized!" }
            return TypeHolder(kdddType!!, classType!!, propertyHolders)
        }
    }

    companion object {
        operator fun invoke(
            kdddType: KdddType,
            classType: TypeName,
            propertyHolders: List<PropertyHolder>
        ): TypeHolder = TypeHolder(kdddType, classType, propertyHolders)
    }
}
