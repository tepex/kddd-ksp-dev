/**
 * Портирование на Котлин примера из статьи
 * https://github.com/graninas/functional-declarative-design-methodology?tab=readme-ov-file
 *
 * https://pl.kotl.in/EUL4SWB0T
 * @version 1.1.1
 * @author Tepex
 * VCS: https://github.com/tepex/kddd-ksp-dev/blob/examples/docs/samples/sandwich.kt
 */

// ***************************************************
// Layer 1. Interfaces (Hierarchical Free Monad eDSLs)
// ***************************************************

// -------------------
// Модуль `api`. eDSL
// -------------------

/** Ресурсы */
sealed interface Ingredient {
    enum class Bread : Ingredient {
        BAGUETTE, TOAST
    }
    
    enum class Component : Ingredient {
        SALT, TOMATO, CHEESE 
    }
}

interface IngredientsStorage {
    operator fun get(ingredient: Ingredient): Result<Ingredient>
}

sealed class IngredientsStorageError(val ingredient: Ingredient, msg: String) : RuntimeException(msg) {
    class NotFound(
        ingredient: Ingredient
    ) : IngredientsStorageError(ingredient, "Ingredient $ingredient not found!")
}

fun notFound(ingredient: Ingredient) =
    IngredientsStorageError.NotFound(ingredient)

/** Объект предметной области. ADT, Monad */
sealed interface Sandwich {
    val bottom: Ingredient.Bread
    val components: List<Ingredient.Component>
    
    interface Body : Sandwich, ValueObject.Data {
        
        override fun validate() {}
    }
    
    interface Ready : Sandwich, ValueObject.Data {
        val body: Body
        val top: Ingredient.Bread?
        
        override fun validate() {}
    }
}

operator fun Sandwich.Body.plus(component: Ingredient.Component): Sandwich.Body =
    fork(bottom, components + component)

/** bind/flatMap. Result<Body> -> Result<Body> */
infix fun Result<Sandwich.Body>.next(op: (Result<Sandwich.Body>) -> Result<Sandwich.Body>): Result<Sandwich.Body> = 
    if (isSuccess) op(this) else this
    
/** bind/flatMap. Result<Body> -> Result<Ready> */
infix fun Result<Sandwich.Body>.finish(op: (Result<Sandwich.Body>) -> Result<Sandwich.Ready>): Result<Sandwich.Ready> = 
    op(this)

operator fun Result<Sandwich.Body>.plus(component: Ingredient.Component): Result<Sandwich.Body> =
    mapCatching { it + component }

/** Технология производства сэндвича */
interface SandwichTechnology {
    val `start new sandwich`: Op.StartNewSandwich
    val `add component`: Op.AddComponent
    val `finish sandwich`: Op.FinishSandwich
    
    /**
     * Технологические операции.
     * 
     * Interpretable Free monadic interfaces.
     */
    sealed interface Op {
        fun interface StartNewSandwich : Op {
            operator fun invoke(bottom: Ingredient.Bread, component: Ingredient.Component): Result<Sandwich.Body>
        }
    
        fun interface AddComponent : Op {
            operator fun invoke(sandwich: Result<Sandwich.Body>, component: Ingredient.Component): Result<Sandwich.Body>
        }
    
        fun interface FinishSandwich : Op {
            operator fun invoke(sandwich: Result<Sandwich.Body>, top: Ingredient.Bread?): Result<Sandwich.Ready>
        }
    }
}

/** Вспомогательный сервис */
interface EventService {
    fun send()
    
    interface MessageId {
        
    }
}

// ********************************
// Layer 2. Domain & Business Logic
// ********************************

// Рецепты. Можно создать библиотеку рецептов.

/* Рецепт моего сэндвича: 
 * 1. Начало: взять вид хлеба тост и компонент помидор как основу.
 * 2. Добавить сыр
 * 3. Добавить соль
 * 4. Завершение: сверху хлеб не добавлять.
 * 
 * Скрипт (Use Case) на созданном в Layer 1 eDSL.
 * Бизнес-логика пишется только на абстракциях из Layer 1.
 */
fun SandwichTechnology.myRecipe(): Result<Sandwich.Ready> = 
    `start new sandwich`(Ingredient.Bread.TOAST, Ingredient.Component.TOMATO) next
    { `add component`(it, Ingredient.Component.CHEESE) } next
    { `add component`(it, Ingredient.Component.SALT) } finish 
    { `finish sandwich`(it, null) }


// *********************************
// Layer 3. Implementation & Runtime
// *********************************

// ----------------------------------
// Модуль `impl`. Имплементации. 
// Генерируются фреймворком KDDD-KSP.
// Подключается в Runtime classpath.
// ----------------------------------

@ConsistentCopyVisibility
data class SandwichBodyImpl private constructor(
    override val bottom: Ingredient.Bread,
    override val components: List<Ingredient.Component>
) : Sandwich.Body {
    
    fun init() {
        validate()
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ValueObject.Data> fork(vararg args: Any?): T =
        Builder().apply {
            bottom = args[0] as Ingredient.Bread
            components = args[1] as List<Ingredient.Component>
        }.build() as T
         
    class Builder {
        var bottom: Ingredient.Bread? = null
        var components: List<Ingredient.Component> = emptyList()
            
        fun build(): Sandwich.Body {
            requireNotNull(bottom) { "`bottom` must be initialized!" }
            require(components.isNotEmpty()) { "`components` must not be empty!" }
            
            return SandwichBodyImpl(bottom!!, components)
        }
    }
}
    
@ConsistentCopyVisibility
data class SandwichReadyImpl private constructor(
    // Вместо наследования применяется композиция (Composition over inheritance)
    override val body: Sandwich.Body,
    override val top: Ingredient.Bread?
) : Sandwich.Ready, Sandwich.Body by body {
        
    fun init() {
        validate()
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ValueObject.Data> fork(vararg args: Any?): T =
        Builder().apply {
            body = args[0] as Sandwich.Body
            top = args[1] as Ingredient.Bread?
        }.build() as T
    
    class Builder {
        var body: Sandwich.Body? = null
        var top: Ingredient.Bread? = null
            
        fun build(): Sandwich.Ready {
            requireNotNull(body) { "`body` must be initialized!" }
            
            return SandwichReadyImpl(body!!, top)
        }
    }
}

@ConsistentCopyVisibility
data class SandwichTechnologyImpl private constructor(
    override val `start new sandwich`: SandwichTechnology.Op.StartNewSandwich,
    override val `add component`: SandwichTechnology.Op.AddComponent,
    override val `finish sandwich`: SandwichTechnology.Op.FinishSandwich
) : SandwichTechnology {
    
    class Builder {
        var startNewSandwich: ((Ingredient.Bread, Ingredient.Component) -> Result<Sandwich.Body>)? = null
        var addComponent: ((Result<Sandwich.Body>, Ingredient.Component) -> Result<Sandwich.Body>)? = null
        var finishSandwich: ((Result<Sandwich.Body>, Ingredient.Bread?) -> Result<Sandwich.Ready>)? = null
        
        fun build(): SandwichTechnology {
            requireNotNull(startNewSandwich) { "`startNewSandwich` must be initialized!" }
            requireNotNull(addComponent) { "`addComponent` must be initialized!" }
            requireNotNull(finishSandwich) { "`finishSandwich` must be initialized!" }
            
            return SandwichTechnologyImpl(startNewSandwich!!, addComponent!!, finishSandwich!!)
        }
    }
}

object IngredientsStorageImpl : IngredientsStorage {
    override operator fun get(ingredient: Ingredient): Result<Ingredient> =
        Result.success(ingredient)
}

fun sandwichBody(block: SandwichBodyImpl.Builder.() -> Unit): Result<Sandwich.Body> =
    SandwichBodyImpl.Builder().apply { block () }.build().let { Result.success(it) }

fun sandwichReady(block: SandwichReadyImpl.Builder.() -> Unit): Sandwich.Ready =
    SandwichReadyImpl.Builder().apply { block () }.build()

fun sandwichTechnology(block: SandwichTechnologyImpl.Builder.() -> Unit): SandwichTechnology =
    SandwichTechnologyImpl.Builder().apply { block() }.build()

// ----------------------
// Модуль `app`. Runtime.
// ----------------------
    
/** Нормальная интерпретация */
fun realInterpreter(): SandwichTechnology =
    sandwichTechnology {
        startNewSandwich = { bread, component -> 
            sandwichBody {
                bottom = bread
                components += component
            }.also { println("Op.StartNewSandwich processed.") }
        }
        addComponent = { body, component ->
            (body + component)
            /* for testing error case
            Result.failure<Sandwich.Body>(notFound(component))
                .also { println("Op.AddComponent processed with error.") }
            */
        }
        finishSandwich = { bodyResult, _top ->
            /* for testing error case
            Result.failure<Sandwich.Ready>(notFound(Ingredient.Bread.TOAST))
                .also { println("Op.FinishSandwich processed with error.") }
            */
            
            bodyResult.mapCatching { _body ->
                sandwichReady {
                    body = _body
                    top = _top
                }
            }
        }  
    }
    
fun crazyInterpreter(): SandwichTechnology =
    sandwichTechnology {
        startNewSandwich = { bread, component -> 
            sandwichBody {
                bottom = bread
                components += component
            }
        }
        addComponent = { body, component -> body + component }
        finishSandwich = { bodyResult, _top ->
            bodyResult.mapCatching { _body ->
                sandwichReady {
                    body = _body
                    top = _top
                }
            }
        }  
    }
    
fun main() {
    realInterpreter()
        .myRecipe()
        .also { println("sandwich: $it") }
}

// ************************************
// External dependency copy-paste from
// https://github.com/itarchdev/k3dm
// ************************************

/**
 * Root of Formal Type System.
 * */
sealed interface Fts {
    /**
     * Вызывается в процессе создания объекта для его валидации.
     *
     * В случае неуспеха должен выкидывать исключение. Предполагается использование методов [require], [requireNotNull], и т.п.
     *
     * @throws IllegalStateException
     * */
    fun validate()
}

sealed interface ValueObject : Fts {
    /** For `data class` */
    interface Data : ValueObject {
        fun <T: Data> fork(vararg args: Any?): T
    }
    
    /** For `value class` */
    interface Value<BOXED : Any> : ValueObject {
        val boxed: BOXED
        
        /**
         * Копирование объекта с новым значением.
         *
         * Имеет тот же смысл, что и метод `copy()` у `data class`. Обусловлен необходимостью создовать объект на
         * уровне абстракции, чтобы иметь возможность писать логику, еще до генерации имплементации.
         * */
        public fun <T : Value<BOXED>> apply(boxed: BOXED): T
    }
    
    /** For `enum class`, `sealed interface` */
    public interface Sealed : ValueObject
}

public interface Entity : Fts {
    public val id: ValueObject
    public var content: ValueObject.Data
}
