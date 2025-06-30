package ru.it_arch.kddd.magic

import ru.it_arch.kddd.magic.domain.Sandwich
import ru.it_arch.kddd.magic.domain.SandwichBody
import ru.it_arch.kddd.magic.domain.plus
import ru.it_arch.kddd.magic.impl.SandwichBodyImpl
import ru.it_arch.kddd.magic.impl.SandwichImpl

class SandwichFsm(initialState: State = State.Idle) {
    var state: State = initialState
        private set

    fun doIt(event: Event) {
        val current = state
        state = when(event) {
            is Event.Start ->
                if (state == State.Idle)
                    SandwichBodyImpl.DEFAULT.fork<SandwichBody>(event.bread, listOf(event.component))
                        .let(State::InProgress)
                else state
            is Event.AddComponent ->
                if (current is State.InProgress) State.InProgress(current.sandwichBody + event.component) else state
            is Event.Finish ->
                if (current is State.InProgress)
                    SandwichImpl.DEFAULT.fork<Sandwich>(
                        current.sandwichBody.bread,
                        event.mayBeBread,
                        current.sandwichBody.components
                    ).let(State::Ready)
                else state
        }
    }

    sealed interface State {
        data object Idle : State
        data class InProgress(val sandwichBody: SandwichBody) : State
        data class Ready(val sandwich: Sandwich) : State
    }

    sealed interface Event {
        data class Start(val bread: Sandwich.Ingredient.Bread, val component: Sandwich.Ingredient.Component) : Event
        data class AddComponent(val component: Sandwich.Ingredient.Component) : Event
        data class Finish(val mayBeBread: Sandwich.Ingredient.Bread? = null) : Event
    }
}

fun sandwichFsmExample() {
    println("FSM sandwich example")
    SandwichFsm().apply {
        SandwichFsm.Event.Start(Sandwich.Ingredient.Bread.TOAST, Sandwich.Ingredient.Component.SALT).also(::doIt)
        SandwichFsm.Event.AddComponent(Sandwich.Ingredient.Component.TOMATO).also(::doIt)
        SandwichFsm.Event.AddComponent(Sandwich.Ingredient.Component.CHEESE).also(::doIt)
        SandwichFsm.Event.AddComponent(Sandwich.Ingredient.Component.SALT).also(::doIt)
        SandwichFsm.Event.Finish().also(::doIt)
    }.also { fsm ->
        when(val state = fsm.state) {
            is SandwichFsm.State.Ready -> println("My cool sandwich: ${state.sandwich}")
            else -> println("Wrong state: $state")
        }
    }
}
