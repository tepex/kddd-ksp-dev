package ru.it_arch.clean_ddd.core.data.model

import com.squareup.kotlinpoet.TypeName
import ru.it_arch.clean_ddd.domain.fullClassName
import ru.it_arch.clean_ddd.domain.model.Property
import ru.it_arch.clean_ddd.domain.model.kddd.KdddType
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
internal data class TypeHolder private constructor(
    val kdddType: KdddType,
    val classType: TypeName,
    val propertyTypes: Map<Property.Name, TypeName>
) : ValueObject.Data {

    init {
        validate()
    }

    override fun validate() {
        when(kdddType) {
            is KdddType.Boxed ->
                check(propertyTypes.size == 1) { "Property types must for ${kdddType.kddd.fullClassName} must contain only one element!" }
            is KdddType.ModelContainer ->
                check(propertyTypes.isNotEmpty()) { "Property types for ${kdddType.kddd.fullClassName} must not be empty!" }
        }
    }

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    class Builder {
        var kdddType: KdddType? = null
        var classType: TypeName? = null
        var propertyTypes = emptyMap<Property.Name, TypeName>()

        fun build(): TypeHolder {
            checkNotNull(kdddType) { "Property 'kdddType' must be initialized!" }
            checkNotNull(classType) { "Property 'classType' must be initialized!" }
            return TypeHolder(kdddType!!, classType!!, propertyTypes)
        }
    }
}
