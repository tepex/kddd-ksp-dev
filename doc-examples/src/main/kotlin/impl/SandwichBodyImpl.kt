package ru.it_arch.kddd.magic.impl

import ru.it_arch.kddd.ValueObject
import ru.it_arch.kddd.magic.domain.Sandwich
import ru.it_arch.kddd.magic.domain.SandwichBody

@ConsistentCopyVisibility
data class SandwichBodyImpl private constructor(
    override val bread: Sandwich.Ingredient.Bread,
    override val components: List<Sandwich.Ingredient.Component>
) : SandwichBody {

    init {
        validate()
    }

    override fun validate() {}

    @Suppress("UNCHECKED_CAST")
    override fun <T : ValueObject> fork(vararg args: Any?): T =
        Builder().apply {
            bread = args[0] as Sandwich.Ingredient.Bread
            components = args[1] as List<Sandwich.Ingredient.Component>
        }.build() as T

    class Builder {
        var bread: Sandwich.Ingredient.Bread? = null
        var components: List<Sandwich.Ingredient.Component> = emptyList()

        fun build(): SandwichBody {
            checkNotNull(bread) { "Property 'SandwichBodyImpl.bread' is not set!" }

            return SandwichBodyImpl(bread!!, components)
        }
    }

    companion object {
        val DEFAULT: SandwichBody = SandwichBodyImpl(Sandwich.Ingredient.Bread.TOAST, emptyList())
    }
}
