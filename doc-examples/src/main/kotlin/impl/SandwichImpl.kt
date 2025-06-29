package ru.it_arch.kddd.magic.impl

import ru.it_arch.kddd.ValueObject
import ru.it_arch.kddd.magic.domain.Sandwich

@ConsistentCopyVisibility
data class SandwichImpl private constructor(
    override val bottom: Sandwich.Ingredient.Bread,
    override val top: Sandwich.Ingredient.Bread?,
    override val components: List<Sandwich.Ingredient.Component>
) : Sandwich {

    init {
        validate()
    }

    override fun validate() {}

    @Suppress("UNCHECKED_CAST")
    override fun <T : ValueObject> fork(vararg args: Any?): T =
        Builder().apply {
            bottom = args[0] as Sandwich.Ingredient.Bread
            top = args[1] as Sandwich.Ingredient.Bread?
            components = args[2] as List<Sandwich.Ingredient.Component>
        }.build() as T

    class Builder {
        var bottom: Sandwich.Ingredient.Bread? = null
        var top: Sandwich.Ingredient.Bread? = null
        var components: List<Sandwich.Ingredient.Component> = emptyList()

        fun build(): Sandwich {
            checkNotNull(bottom) { "Property 'SandwichImpl.bottom' is not set!" }

            return SandwichImpl(bottom!!, top, components)
        }
    }

    companion object {
        val DEFAULT: Sandwich = SandwichImpl(Sandwich.Ingredient.Bread.TOAST, null, emptyList())
    }
}
