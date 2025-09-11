/**
 * Портирование на Котлин примера из статьи
 * https://github.com/graninas/functional-declarative-design-methodology?tab=readme-ov-file
 * 
 * https://pl.kotl.in/6Iq5aucmf
 *
 * You can edit, run, and share this code.
 * play.kotlinlang.org
 */

fun main() {
    interpreter()
}

/** Нормальная интерпретация */
fun interpreter() {
    SandwichTechnology(
        { b, c -> 
            sandwichBody {
                bottom = b
                components += c
            }
        },
        { b, c -> b + c },
        { b, t -> 
            sandwichReady {
                body = b
                top = t
            }
        }  
    ).myRecipe
    .also { println("sandwich: $it") }
}

// ----------------------------------------
// Модуль `api` содержащий eDSL и Use Case
// ----------------------------------------

sealed interface Ingredient {
    enum class Bread : Ingredient {
        BAGUETTE, TOAST
    }
    
    enum class Component : Ingredient {
        SALT, TOMATO, CHEESE 
    }
}

/** Объект предметной области. ADT, Monad */
sealed interface Sandwich {
    val bottom: Ingredient.Bread
    val components: List<Ingredient.Component>
    
    @ConsistentCopyVisibility
    data class Body private constructor(
        override val bottom: Ingredient.Bread,
        override val components: List<Ingredient.Component>
    ) : Sandwich {
        
        class Builder {
            var bottom: Ingredient.Bread? = null
            var components: List<Ingredient.Component> = emptyList()
            
            fun build(): Body {
                requireNotNull(bottom) { "`bottom` must be initialized!" }
                require(components.isNotEmpty()) { "`components` must not be empty!" }
                return Body(bottom!!, components)
            }
        }
        
        operator fun plus(component: Ingredient.Component): Body =
            copy(components = components + component)
    }
    
    @ConsistentCopyVisibility
    data class Ready private constructor(
        val body: Body,
        val top: Ingredient.Bread?
    ) : Sandwich by body {
        
        class Builder {
            var body: Body? = null
            var top: Ingredient.Bread? = null
            
            fun build(): Ready {
                requireNotNull(body) { "`body` must be initialized!" }
                return Ready(body!!, top)
            }
        }
    }
}

fun sandwichBody(block: Sandwich.Body.Builder.() -> Unit): Sandwich.Body =
    Sandwich.Body.Builder().apply { block () }.build()
    
fun sandwichReady(block: Sandwich.Ready.Builder.() -> Unit): Sandwich.Ready =
    Sandwich.Ready.Builder().apply { block () }.build()
    
/** bind/flatMap */
infix fun Sandwich.next(op: (Sandwich.Body) -> Sandwich): Sandwich = when(this) {
    is Sandwich.Body -> op(this)
    else -> this
}

/**
 * Технологические операции.
 * 
 * Interpretable Free Monadic Interfaces.
 */
sealed interface Op {
    fun interface StartNewSandwich : Op {
        operator fun invoke(bottom: Ingredient.Bread, component: Ingredient.Component): Sandwich
    }
    
    fun interface AddComponent : Op {
        operator fun invoke(sandwich: Sandwich.Body, component: Ingredient.Component): Sandwich
    }
    
    fun interface FinishSandwich : Op {
        operator fun invoke(sandwich: Sandwich.Body, top: Ingredient.Bread?): Sandwich
    }
}

/** Описание технологии */
data class SandwichTechnology(
    val startNewSandwich: Op.StartNewSandwich,
    val addComponent: Op.AddComponent,
    val finishSandwich: Op.FinishSandwich
)

/* Рецепт моего сэндвича: 
 * 1. Взять вид хлеба тост и компонент помидор как основу.
 * 2. Добавить сыр
 * 3. Добавить соль
 * 4. Сверху хлеб не добавлять и мой сэндвич готов.
 * 
 * Скрипт (Use Case) на созданном выше eDSL.
 * Бизнес-логика пишется только на абстракциях из модуля `api`.
 */
val SandwichTechnology.myRecipe: Sandwich get() = 
    startNewSandwich(Ingredient.Bread.TOAST, Ingredient.Component.TOMATO) next
    { addComponent(it, Ingredient.Component.CHEESE) } next
    { addComponent(it, Ingredient.Component.SALT) } next
    { finishSandwich(it, null) }

// ----------------------------
// Молуль имплементации `impl`
// ----------------------------

//@ConsistentCopyVisibility
