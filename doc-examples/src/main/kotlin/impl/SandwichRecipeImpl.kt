package ru.it_arch.kddd.magic.impl

import ru.it_arch.kddd.ValueObject
import ru.it_arch.kddd.magic.domain.Sandwich
import ru.it_arch.kddd.magic.domain.SandwichBody
import ru.it_arch.kddd.magic.domain.SandwichRecipe
import ru.it_arch.kddd.magic.domain.SandwichRecipe.AddComponent
import ru.it_arch.kddd.magic.domain.SandwichRecipe.Finish
import ru.it_arch.kddd.magic.domain.SandwichRecipe.StartNewSandwich

@ConsistentCopyVisibility
data class SandwichRecipeImpl private constructor(
    override val start: StartNewSandwich,
    override val add: AddComponent,
    override val finish: Finish
) : SandwichRecipe {

    init {
        validate()
    }

    /*
    override val neutralSandwich: Sandwich
        get() = SandwichImpl.DEFAULT

    override val neutralSandwichBody: SandwichBody
        get() = SandwichBodyImpl.DEFAULT*/

    @Suppress("UNCHECKED_CAST")
    override fun <T : ValueObject> fork(vararg args: Any?): T =
        Builder().apply {
            start = args[0] as StartNewSandwich
            add = args[1] as AddComponent
            finish = args[2] as Finish
        }.build() as T

    /* Interpreters
    inner class StartNewSandwichImpl : StartNewSandwich {
        override fun doIt(bread: Sandwich.Ingredient.Bread, component: Sandwich.Ingredient.Component): SandwichBody =
            neutralSandwichBody.fork(bread, listOf(component))
    }

    inner class AddComponentImpl : AddComponent {
        override fun doIt(sandwichBody: SandwichBody, component: Sandwich.Ingredient.Component): SandwichBody =
            sandwichBody + component
    }

    inner class FinishImpl : Finish {
        override fun doIt(sandwichBody: SandwichBody, mayBeBread: Sandwich.Ingredient.Bread?): Sandwich =
            neutralSandwich.fork(sandwichBody.bread, mayBeBread, sandwichBody.components)
    }*/

    class Builder {
        var start: StartNewSandwich? = null
        var add: AddComponent? = null
        var finish: Finish? = null

        fun build(): SandwichRecipe {
            checkNotNull(start) { "Property 'SandwichRecipeImpl.start' is not set!" }
            checkNotNull(add) { "Property 'SandwichRecipeImpl.add' is not set!" }
            checkNotNull(finish) { "Property 'SandwichRecipeImpl.finish' is not set!" }

            return SandwichRecipeImpl(start!!, add!!, finish!!)
        }
    }

    companion object {
        val DEFAULT = SandwichRecipeImpl(
            { bread, component -> SandwichBodyImpl.DEFAULT },
            { sandwichBody, component -> SandwichBodyImpl.DEFAULT },
            { sandwichBody, mayBeBread -> SandwichImpl.DEFAULT }
        )
    }
}
