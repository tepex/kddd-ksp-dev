package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.core.Generatable
import ru.it_arch.clean_ddd.domain.core.KdddType
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
internal data class GeneratableImpl private constructor(
    override val kddd: CompositeClassName,
    override val impl: CompositeClassName.ClassName,
    override val enclosing: KdddType.ModelContainer?
) : Generatable {

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    override fun validate() {}

    class Builder {
        var kddd: CompositeClassName? = null
        var impl: CompositeClassName.ClassName? = null
        var enclosing: KdddType.ModelContainer? = null

        fun build(): Generatable {
            checkNotNull(kddd) { "Property 'kddd' must be initialized!" }
            checkNotNull(impl) { "Property 'impl' must be initialized!" }

            return GeneratableImpl(kddd!!, impl!!, enclosing)
        }
    }
}
