package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.core.Generatable
import ru.it_arch.clean_ddd.domain.core.KdddType
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
internal data class GeneratableImpl private constructor(
    override val kddd: CompositeClassName,
    override val implClassName: CompositeClassName.ClassName,
    override val implPackageName: CompositeClassName.PackageName,
    override val enclosing: KdddType.ModelContainer?
) : Generatable {

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    override fun validate() {}

    class Builder {
        var kddd: CompositeClassName? = null
        var implClassName: CompositeClassName.ClassName? = null
        var implPackageName: CompositeClassName.PackageName? = null
        var enclosing: KdddType.ModelContainer? = null

        fun build(): Generatable {
            checkNotNull(kddd) { "Property 'kddd' must be initialized!" }
            checkNotNull(implClassName) { "Property 'implClassName' must be initialized!" }
            checkNotNull(implPackageName) { "Property 'implPackageName' must be initialized!" }

            return GeneratableImpl(kddd!!, implClassName!!, implPackageName!!, enclosing)
        }
    }
}
