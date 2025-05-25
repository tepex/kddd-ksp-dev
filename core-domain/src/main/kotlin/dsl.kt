package ru.it_arch.kddd.domain

import ru.it_arch.kddd.domain.model.CompositeClassName
import ru.it_arch.kddd.domain.model.Context
import ru.it_arch.kddd.domain.model.Options
import ru.it_arch.kddd.domain.model.Property

/**
 *
 * */
public fun property(block: Property.Builder.() -> Unit): Property =
    Property.Builder().apply(block).build()

public fun compositeClassName(block: CompositeClassName.Builder.() -> Unit): CompositeClassName =
    CompositeClassName.Builder().apply(block).build()

/**
 *
 * */
public fun options(block: Options.Builder.() -> Unit): Options =
    Options.Builder().apply(block).build()


context(options: Options)
/**
 *
 * */
public fun kDddContext(block: Context.Builder.() -> Unit): Context =
    Context.Builder().apply(block).build()
