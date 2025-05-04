package ru.it_arch.clean_ddd.domain

/**
 *
 * */
public fun property(block: Property.Builder.() -> Unit): Property =
    Property.Builder().apply(block).build()

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

internal fun generatable(block: GeneratableDelegate.Builder.() -> Unit): GeneratableDelegate =
    GeneratableDelegate.Builder().apply(block).build()



/**
 *
 * */
/*
public fun String.type(): Type? = when(this) {
    Type.Data::class.java.simpleName -> Type.Data()
}*/

