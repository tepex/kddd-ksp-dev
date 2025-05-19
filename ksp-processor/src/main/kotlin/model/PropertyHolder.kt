package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.TypeName
import ru.it_arch.clean_ddd.domain.model.Property
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
internal data class PropertyHolder private constructor(
    val property: Property,
    val type: TypeName
) : ValueObject.Data {

    init {
        validate()
    }

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    class Builder {
        var property: Property? = null
        var type: TypeName? = null

        fun build(): PropertyHolder {
            checkNotNull(property) { "Property 'property' must be initialized!" }
            checkNotNull(type) { "Property 'type' must be initialized!" }
            return PropertyHolder(property!!, type!!)
        }
    }
}
