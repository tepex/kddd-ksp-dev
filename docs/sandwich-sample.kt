/**
 * You can edit, run, and share this code.
 * play.kotlinlang.org
 */
fun main() {
    println("Hello, world!!!")
}

sealed interface Op {
    fun interface First : Op {
        operator fun invoke(bottom: Sandwich.Bread, component: Sandwich.Component): Sandwich.InProgress
    }
    
    fun interface AddComponent : Op {
        operator fun invoke(sandwich: Sandwich.InProgress, component: Sandwich.Component): Sandwich.InProgress
    }
    
    fun interface Last : Op {
        operator fun invoke(sandwich: Sandwich.InProgress, top: Sandwich.Bread?): Sandwich.Ready
    }
}

sealed interface Sandwich {
    val bottom: Bread
    val components: List<Component>
    //                                     
    data class InProgress(
        override val bottom: Bread,
        override val components: List<Component>
    ) : Sandwich
    
    data class Ready(
        val inProgress: InProgress,
        val top: Bread?
    ) : Sandwich by inProgress

    enum class Bread {
        BAGUETTE, TOAST
    }
    
    enum class Component {
        SALAD, TOMATO, CHEESE 
    }
}

interface SandwichTechnology {
    val first: Op.First
    val add: Op.AddComponent
    val last: Op.Last
}

/*
fun start(f: () -> Sandwich.InProgress): Sandwich.InProgress =
    f()*/

infix fun Sandwich.InProgress.next(f: (Sandwich.InProgress) -> Sandwich.InProgress): Sandwich.InProgress =
    f(this)
    
infix fun Sandwich.InProgress.finish(f: (Sandwich.InProgress) -> Sandwich.Ready): Sandwich.Ready =
    f(this)

fun SandwichTechnology.myRecipe(): Sandwich.Ready = 
    first(Sandwich.Bread.BAGUETTE, Sandwich.Component.CHEESE) next
    { add(it, Sandwich.Component.TOMATO) } next
    { add(it, Sandwich.Component.SALAD) } finish
    { last(it, Sandwich.Bread.TOAST) }

// _--__------------------------------------------------------------------__---------+------------
@ConsistentCopyVisibility
data class SandwichTechnologyImpl private constructor(
    override val first: Op.First,
    override val add: Op.AddComponent,
    override val last: Op.Last
) : SandwichTechnology {
    
    class Builder {
        var first: Op.First? = null
        var add: Op.AddComponent? = null
        var last: Op.Last? = null
        
        fun build(): SandwichTechnology {
            requireNotNull(first) { "`first` must be initialized!" }
            requireNotNull(add) { "`add` must be initialized!" }
            requireNotNull(last) { "`last` must be initialized!" }
            return SandwichTechnologyImpl(first!!, add!!, last!!)
        }
    }
    
    companion object {
        
    }
}