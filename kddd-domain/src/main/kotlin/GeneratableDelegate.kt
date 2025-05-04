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

    public class Builder {
        public var kdddClassName: String? = null
        public var implClassName: String? = null
        public var enclosing: KdddType.ModelContainer? = null

        public fun build(): GeneratableDelegate {
            checkNotNull(kdddClassName) { "Property 'kdddClassName' must be initialized!" }
            checkNotNull(implClassName) { "Property 'implClassName' must be initialized!" }

            return GeneratableDelegate(
                KdddType.Generatable.KdddClassName(kdddClassName!!),
                KdddType.Generatable.ImplClassName(implClassName!!),
                enclosing
            )
        }
    }
}
