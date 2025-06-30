package ru.it_arch.kddd.magic

import ru.it_arch.kddd.magic.domain.Sandwich
import ru.it_arch.kddd.magic.domain.buildMyCoolSandwich
import ru.it_arch.kddd.magic.domain.plus
import ru.it_arch.kddd.magic.impl.SandwichBodyImpl
import ru.it_arch.kddd.magic.impl.SandwichImpl
import ru.it_arch.kddd.magic.impl.sandwichRecipe
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

fun sandwichExample() {
    println("sandwich example")

    // Имплементация поведения для данной среды выполнения (интерпретации)
    sandwichRecipe(
        start = { bread, component ->
            SandwichBodyImpl.DEFAULT.fork(bread, listOf(component))
        },
        add = { sandwichBody, component ->
            sandwichBody + component
        },
        finish = { sandwichBody, mayBeBread ->
            SandwichImpl.DEFAULT.fork(sandwichBody.bread, mayBeBread, sandwichBody.components)
        }
    ).buildMyCoolSandwich()
        .also { println("My cool sandwich: $it") }

    println("====\nFake interpretation:")
    // Имплементация поведения для какой-нибудь фейковой среды выполнения (интерпретации)
    // Исполнитель (интерпретатор) имитирует бурную деятельность
    sandwichRecipe(
        start = { bread, component ->
            print("start smoking for 5 sec before start... ")
            Thread.sleep(5.seconds.toJavaDuration())
            SandwichBodyImpl.DEFAULT.also {
                println("finish smoking and return default result: $it")
            }
        },
        add = { sandwichBody, component ->
            sandwichBody.also {
                println("expecting to add ${component}, but do nothing and return the same: $it")
            }
        },
        finish = { sandwichBody, mayBeBread ->
            SandwichImpl.DEFAULT.fork<Sandwich>(
                Sandwich.Ingredient.Bread.entries.random(),
                if (Random.nextBoolean()) Sandwich.Ingredient.Bread.entries.random() else null,
                mutableListOf<Sandwich.Ingredient.Component>().apply {
                    (0..Random.nextInt(5)).forEach { Sandwich.Ingredient.Component.entries.random().also(::add) }
                }.toList()
            ).also {
                println("return random result: $it")
            }
        }
    ).buildMyCoolSandwich()
        .also { println("bad, bad sandwich: $it") }
}
