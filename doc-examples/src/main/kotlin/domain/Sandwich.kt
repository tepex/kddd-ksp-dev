package ru.it_arch.kddd.magic.domain

import ru.it_arch.kddd.ValueObject

interface Sandwich : ValueObject.Data {
    val bottom: Ingredient.Bread
    val top: Ingredient.Bread?
    val components: List<Ingredient.Component>

    sealed interface Ingredient {
        enum class Component : Ingredient {
            CHEESE, TOMATO, SALT
        }

        enum class Bread : Ingredient {
            TOAST, BAUGETTE
        }
    }
}
