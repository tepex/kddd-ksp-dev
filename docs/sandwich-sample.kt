/**
 * Портирование на Котлин примера из статьи
 * https://github.com/graninas/functional-declarative-design-methodology?tab=readme-ov-file
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
        { bottom, component -> Sandwich.body(bottom, component) },
        { body, component -> body + component },
        { body, bread -> Sandwich.ready(body, bread) }
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

/** Monad */
sealed interface Sandwich {
    val bottom: Ingredient.Bread
    val components: List<Ingredient.Component>
    //                                     
    data class Body(
        override val bottom: Ingredient.Bread,
        override val components: List<Ingredient.Component>
    ) : Sandwich {
        operator fun plus(component: Ingredient.Component): Body =
            copy(components = components + component)
    }
    
    data class Ready(
        val body: Body,
        val top: Ingredient.Bread?
    ) : Sandwich by body
    
    companion object {
        /** pure */
        fun body(bottom: Ingredient.Bread, component: Ingredient.Component) =
            Sandwich.Body(bottom, listOf(component))
        
        fun update(body: Sandwich.Body, component: Ingredient.Component) =
            body.copy()
            
        fun ready(body: Sandwich.Body, top: Ingredient.Bread?) =
            Sandwich.Ready(body, top)
    }
}

/** bind/flatMap */
infix fun Sandwich.next(op: (Sandwich.Body) -> Sandwich): Sandwich = when(this) {
    is Sandwich.Body -> op(this)
    else -> this
}

/**
 * Технологические операции
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
 * Используются только абстракции из модуля `api`.
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
