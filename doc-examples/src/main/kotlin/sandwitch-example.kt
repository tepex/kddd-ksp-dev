package ru.it_arch.kddd.magic

import ru.it_arch.kddd.magic.domain.buildMyCoolSandwich
import ru.it_arch.kddd.magic.domain.plus
import ru.it_arch.kddd.magic.impl.SandwichBodyImpl
import ru.it_arch.kddd.magic.impl.SandwichImpl
import ru.it_arch.kddd.magic.impl.sandwichRecipe


fun sandwichExample() {
    println("sandwich example ")
    val recipe = sandwichRecipe(
        start = { bread, component -> SandwichBodyImpl.DEFAULT.fork(bread, listOf(component)) },
        add = { sandwichBody, component -> sandwichBody + component },
        finish = { sandwichBody, mayBeBread ->
            SandwichImpl.DEFAULT.fork(sandwichBody.bread, mayBeBread, sandwichBody.components)
        }
    )

    val sandwich = recipe.buildMyCoolSandwich()
    println("sandwich: $sandwich")
}
