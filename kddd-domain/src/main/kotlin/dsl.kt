package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.core.Generatable

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

internal fun generatable(block: GeneratableImpl.Builder.() -> Unit): Generatable =
    GeneratableImpl.Builder().apply(block).build()
