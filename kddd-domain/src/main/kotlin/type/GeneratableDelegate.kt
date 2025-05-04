package ru.it_arch.clean_ddd.domain.type

import ru.it_arch.clean_ddd.domain.type.KdddType.Generatable
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
public data class GeneratableDelegate private constructor(
    override val kddd: Generatable.KdddClassName,
    override val impl: Generatable.ImplClassName,
    override val enclosing: KdddType?
) : Generatable {

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    override fun validate() {}

    public companion object {
        public operator fun invoke(kddd: String, impl: String, enclosing: KdddType? = null): GeneratableDelegate =
            GeneratableDelegate(Generatable.KdddClassName(kddd), Generatable.ImplClassName(impl), enclosing)
    }
}
