/**
 * Портирование на Котлин примера из статьи
 * https://github.com/graninas/functional-declarative-design-methodology?tab=readme-ov-file
 * 
 * You can edit, run, and share this code.
 * play.kotlinlang.org
 * https://play.kotlinlang.org/#eyJ2ZXJzaW9uIjoiMi4yLjIwLVJDMiIsInBsYXRmb3JtIjoiamF2YSIsImFyZ3MiOiIiLCJub25lTWFya2VycyI6dHJ1ZSwidGhlbWUiOiJpZGVhIiwiY29kZSI6Ii8qKlxuICog0J/QvtGA0YLQuNGA0L7QstCw0L3QuNC1INC90LAg0JrQvtGC0LvQuNC9INC/0YDQuNC80LXRgNCwINC40Lcg0YHRgtCw0YLRjNC4XG4gKiBodHRwczovL2dpdGh1Yi5jb20vZ3JhbmluYXMvZnVuY3Rpb25hbC1kZWNsYXJhdGl2ZS1kZXNpZ24tbWV0aG9kb2xvZ3k/dGFiPXJlYWRtZS1vdi1maWxlXG4gKiBcbiAqIFlvdSBjYW4gZWRpdCwgcnVuLCBhbmQgc2hhcmUgdGhpcyBjb2RlLlxuICogcGxheS5rb3RsaW5sYW5nLm9yZ1xuICovXG5mdW4gbWFpbigpIHtcbiAgICBwcmludGxuKFwiSGVsbG8sIHdvcmxkISEhXCIpXG59XG5cblxuXG4vLyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4vLyDQnNC+0LTRg9C70YwgYGFwaWAg0YHQvtC00LXRgNC20LDRidC40LkgZURTTCDQuCBVc2UgQ2FzZVxuLy8gLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG5zZWFsZWQgaW50ZXJmYWNlIEluZ3JlZGllbnQge1xuICAgIGVudW0gY2xhc3MgQnJlYWQgOiBJbmdyZWRpZW50IHtcbiAgICAgICAgQkFHVUVUVEUsIFRPQVNUXG4gICAgfVxuICAgIFxuICAgIGVudW0gY2xhc3MgQ29tcG9uZW50IDogSW5ncmVkaWVudCB7XG4gICAgICAgIFNBTFQsIFRPTUFUTywgQ0hFRVNFIFxuICAgIH1cbn1cblxuLyoqIE1vbmFkICovXG5zZWFsZWQgaW50ZXJmYWNlIFNhbmR3aWNoIHtcbiAgICB2YWwgYm90dG9tOiBJbmdyZWRpZW50LkJyZWFkXG4gICAgdmFsIGNvbXBvbmVudHM6IExpc3Q8SW5ncmVkaWVudC5Db21wb25lbnQ+XG4gICAgLy8gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgZGF0YSBjbGFzcyBCb2R5KFxuICAgICAgICBvdmVycmlkZSB2YWwgYm90dG9tOiBJbmdyZWRpZW50LkJyZWFkLFxuICAgICAgICBvdmVycmlkZSB2YWwgY29tcG9uZW50czogTGlzdDxJbmdyZWRpZW50LkNvbXBvbmVudD5cbiAgICApIDogU2FuZHdpY2hcbiAgICBcbiAgICBkYXRhIGNsYXNzIFJlYWR5KFxuICAgICAgICB2YWwgYm9keTogQm9keSxcbiAgICAgICAgdmFsIHRvcDogSW5ncmVkaWVudC5CcmVhZD9cbiAgICApIDogU2FuZHdpY2ggYnkgYm9keVxuICAgIFxuICAgIGNvbXBhbmlvbiBvYmplY3Qge1xuICAgICAgICBmdW4gcHVyZShib3R0b206IEluZ3JlZGllbnQuQnJlYWQsIGNvbXBvbmVudDogSW5ncmVkaWVudC5Db21wb25lbnQpOiBTYW5kd2ljaCA9XG4gICAgICAgICAgICBTYW5kd2ljaC5Cb2R5KGJvdHRvbSwgbGlzdE9mKGNvbXBvbmVudCkpXG4gICAgfVxufVxuXG4vKiogYmluZC9mbGF0TWFwICovXG5pbmZpeCBmdW4gU2FuZHdpY2gubmV4dChvcDogKFNhbmR3aWNoLkJvZHkpIC0+IFNhbmR3aWNoKTogU2FuZHdpY2ggPSB3aGVuKHRoaXMpIHtcbiAgICBpcyBTYW5kd2ljaC5Cb2R5IC0+IG9wKHRoaXMpXG4gICAgZWxzZSAtPiB0aGlzXG59XG5cbi8qKlxuICog0KLQtdGF0L3QvtC70L7Qs9C40YfQtdGB0LrQuNC1INC+0L/QtdGA0LDRhtC40LhcbiAqL1xuc2VhbGVkIGludGVyZmFjZSBPcCB7XG4gICAgZnVuIGludGVyZmFjZSBTdGFydE5ld1NhbmR3aWNoIDogT3Age1xuICAgICAgICBvcGVyYXRvciBmdW4gaW52b2tlKGJvdHRvbTogSW5ncmVkaWVudC5CcmVhZCwgY29tcG9uZW50OiBJbmdyZWRpZW50LkNvbXBvbmVudCk6IFNhbmR3aWNoXG4gICAgfVxuICAgIFxuICAgIGZ1biBpbnRlcmZhY2UgQWRkQ29tcG9uZW50IDogT3Age1xuICAgICAgICBvcGVyYXRvciBmdW4gaW52b2tlKHNhbmR3aWNoOiBTYW5kd2ljaC5Cb2R5LCBjb21wb25lbnQ6IEluZ3JlZGllbnQuQ29tcG9uZW50KTogU2FuZHdpY2hcbiAgICB9XG4gICAgXG4gICAgZnVuIGludGVyZmFjZSBGaW5pc2hTYW5kd2ljaCA6IE9wIHtcbiAgICAgICAgb3BlcmF0b3IgZnVuIGludm9rZShzYW5kd2ljaDogU2FuZHdpY2guQm9keSwgdG9wOiBJbmdyZWRpZW50LkJyZWFkPyk6IFNhbmR3aWNoXG4gICAgfVxufVxuXG4vKiog0J7Qv9C40YHQsNC90LjQtSDRgtC10YXQvdC+0LvQvtCz0LjQuCAqL1xuZGF0YSBjbGFzcyBTYW5kd2ljaFRlY2hub2xvZ3koXG4gICAgdmFsIHN0YXJ0TmV3U2FuZHdpY2g6IE9wLlN0YXJ0TmV3U2FuZHdpY2gsXG4gICAgdmFsIGFkZENvbXBvbmVudDogT3AuQWRkQ29tcG9uZW50LFxuICAgIHZhbCBmaW5pc2hTYW5kd2ljaDogT3AuRmluaXNoU2FuZHdpY2hcbilcblxuLyog0KDQtdGG0LXQv9GCINC80L7QtdCz0L4g0YHRjdC90LTQstC40YfQsDogXG4gKiAxLiDQktC30Y/RgtGMINCy0LjQtCDRhdC70LXQsdCwINGC0L7RgdGCINC4INC60L7QvNC/0L7QvdC10L3RgiDQv9C+0LzQuNC00L7RgCDQutCw0Log0L7RgdC90L7QstGDLlxuICogMi4g0JTQvtCx0LDQstC40YLRjCDRgdGL0YBcbiAqIDMuINCU0L7QsdCw0LLQuNGC0Ywg0YHQvtC70YxcbiAqIDQuINCh0LLQtdGA0YXRgyDRhdC70LXQsSDQvdC1INC00L7QsdCw0LLQu9GP0YLRjCDQuCDQvNC+0Lkg0YHRjdC90LTQstC40Ycg0LPQvtGC0L7Qsi5cbiAqIFxuICog0KHQutGA0LjQv9GCIChVc2UgQ2FzZSkg0L3QsCDRgdC+0LfQtNCw0L3QvdC+0Lwg0LLRi9GI0LUgZURTTC5cbiAqINCY0YHQv9C+0LvRjNC30YPRjtGC0YHRjyDRgtC+0LvRjNC60L4g0LDQsdGB0YLRgNCw0LrRhtC40Lgg0LjQtyDQvNC+0LTRg9C70Y8gYGFwaWAuXG4gKi9cbmZ1biBTYW5kd2ljaFRlY2hub2xvZ3kubXlSZWNpcGUoKTogU2FuZHdpY2ggPSBcbiAgICBzdGFydE5ld1NhbmR3aWNoKEluZ3JlZGllbnQuQnJlYWQuVE9BU1QsIEluZ3JlZGllbnQuQ29tcG9uZW50LlRPTUFUTykgbmV4dFxuICAgIHsgYWRkQ29tcG9uZW50KGl0LCBJbmdyZWRpZW50LkNvbXBvbmVudC5DSEVFU0UpIH0gbmV4dFxuICAgIHsgYWRkQ29tcG9uZW50KGl0LCBJbmdyZWRpZW50LkNvbXBvbmVudC5TQUxUKSB9IG5leHRcbiAgICB7IGZpbmlzaFNhbmR3aWNoKGl0LCBudWxsKSB9XG5cblxuLy8gLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuLy8g0JzQvtC70YPQu9GMINC40LzQv9C70LXQvNC10L3RgtCw0YbQuNC4IGBpbXBsYFxuLy8gLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG4vL0BDb25zaXN0ZW50Q29weVZpc2liaWxpdHlcbiJ9
 */
fun main() {
    println("Hello, world!!!")
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
    ) : Sandwich
    
    data class Ready(
        val body: Body,
        val top: Ingredient.Bread?
    ) : Sandwich by body
    
    companion object {
        fun pure(bottom: Ingredient.Bread, component: Ingredient.Component): Sandwich =
            Sandwich.Body(bottom, listOf(component))
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
fun SandwichTechnology.myRecipe(): Sandwich = 
    startNewSandwich(Ingredient.Bread.TOAST, Ingredient.Component.TOMATO) next
    { addComponent(it, Ingredient.Component.CHEESE) } next
    { addComponent(it, Ingredient.Component.SALT) } next
    { finishSandwich(it, null) }


// ----------------------------
// Молуль имплементации `impl`
// ----------------------------

//@ConsistentCopyVisibility
