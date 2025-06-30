package ru.it_arch.kddd.magic.domain

import ru.it_arch.kddd.ValueObject

interface SandwichRecipe : ValueObject.Data {
    val start: StartNewSandwich
    val add: AddComponent
    val finish: Finish

    override fun validate() {}

    fun interface StartNewSandwich {
        fun doIt(bread: Sandwich.Ingredient.Bread, component: Sandwich.Ingredient.Component): SandwichBody
    }

    fun interface AddComponent {
        fun doIt(sandwichBody: SandwichBody, component: Sandwich.Ingredient.Component): SandwichBody
    }

    fun interface Finish {
        fun doIt(sandwichBody: SandwichBody, mayBeBread: Sandwich.Ingredient.Bread?): Sandwich
    }
}
