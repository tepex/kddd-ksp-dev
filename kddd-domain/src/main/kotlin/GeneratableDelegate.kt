package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.Kddd

@ConsistentCopyVisibility
public data class GeneratableDelegate private constructor(
    override val kddd: KdddType.Generatable.KdddClassName,
    override val impl: KdddType.Generatable.ImplClassName,
    override val enclosing: KdddType.ModelContainer?
) : KdddType.Generatable {

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    override fun validate() {}

    public companion object {
        public operator fun invoke(kddd: String, impl: String, enclosing: KdddType.ModelContainer? = null): GeneratableDelegate =
            GeneratableDelegate(KdddType.Generatable.KdddClassName(kddd), KdddType.Generatable.ImplClassName(impl), enclosing)
    }
}
