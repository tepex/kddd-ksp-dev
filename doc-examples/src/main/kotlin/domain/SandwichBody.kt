package ru.it_arch.kddd.magic.domain

import ru.it_arch.kddd.ValueObject

interface SandwichBody : ValueObject.Data {
    val bread: Sandwich.Ingredient.Bread
    val components: List<Sandwich.Ingredient.Component>
}
