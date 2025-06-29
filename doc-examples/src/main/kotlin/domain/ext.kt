package ru.it_arch.kddd.magic.domain

fun Point.asString(): String =
    "(${x.boxed},${y.boxed})"

operator fun SandwichBody.plus(component: Sandwich.Ingredient.Component): SandwichBody =
    fork(bread, components + component)

inline infix fun SandwichBody.next(fn: (SandwichBody) -> SandwichBody): SandwichBody =
    fn(this)

inline infix fun SandwichBody.finish(fn: (SandwichBody) -> Sandwich): Sandwich =
    fn(this)

// Use case
fun SandwichRecipe.buildMyCoolSandwich(): Sandwich =
    start.doIt(Sandwich.Ingredient.Bread.TOAST, Sandwich.Ingredient.Component.SALT) next
        { add.doIt(it, Sandwich.Ingredient.Component.TOMATO) } next
        { add.doIt(it, Sandwich.Ingredient.Component.CHEESE) } next
        { add.doIt(it, Sandwich.Ingredient.Component.SALT) } finish
        { finish.doIt(it, null) }
