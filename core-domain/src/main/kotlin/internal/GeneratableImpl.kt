package ru.it_arch.kddd.domain.internal

import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.domain.model.CompositeClassName
import ru.it_arch.kddd.domain.model.type.Generatable
import ru.it_arch.kddd.domain.model.type.KdddType

@ConsistentCopyVisibility
internal data class GeneratableImpl private constructor(
    override val kddd: CompositeClassName,
    override val impl: CompositeClassName,
    override val enclosing: KdddType.ModelContainer?
) : Generatable {

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    override fun validate() {}

    class Builder {
        var kddd: CompositeClassName? = null
        var impl: CompositeClassName? = null
        var enclosing: KdddType.ModelContainer? = null

        fun build(): Generatable {
            checkNotNull(kddd) { "Property 'kddd' must be initialized!" }
            checkNotNull(impl) { "Property 'impl' must be initialized!" }
            return GeneratableImpl(kddd!!, impl!!, enclosing)
        }
    }
}
