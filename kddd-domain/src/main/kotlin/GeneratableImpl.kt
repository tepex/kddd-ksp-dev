package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.Kddd

@ConsistentCopyVisibility
internal data class GeneratableImpl private constructor(
    override val kddd: KdddType.Generatable.KdddClassName,
    override val impl: KdddType.Generatable.ImplClassName,
    override val enclosing: KdddType.ModelContainer?
) : KdddType.Generatable {

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    override fun validate() {}

    class Builder {
        var kdddClassName: String? = null
        var implClassName: String? = null
        var enclosing: KdddType.ModelContainer? = null

        fun build(): KdddType.Generatable {
            checkNotNull(kdddClassName) { "Property 'kdddClassName' must be initialized!" }
            checkNotNull(implClassName) { "Property 'implClassName' must be initialized!" }

            return GeneratableImpl(
                KdddType.Generatable.KdddClassName(kdddClassName!!),
                KdddType.Generatable.ImplClassName(implClassName!!),
                enclosing
            )
        }
    }

    companion object {
        operator fun invoke(
            kddd: KdddType.Generatable.KdddClassName,
            impl: KdddType.Generatable.ImplClassName,
            enclosing: KdddType.ModelContainer?
        ): KdddType.Generatable =
            GeneratableImpl(kddd, impl, enclosing)
    }
}
