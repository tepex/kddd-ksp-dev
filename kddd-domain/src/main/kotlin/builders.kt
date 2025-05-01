package ru.it_arch.clean_ddd.domain

/**
 *
 * */
public fun className(block: ClassName.DslBuilder.() -> Unit): ClassName =
    ClassName.DslBuilder().apply(block).build()

/**
 *
 * */
public fun property(block: Property.DslBuilder.() -> Unit): Property =
    Property.DslBuilder().apply(block).build()

/**
 *
 * */
public fun options(block: Options.DslBuilder.() -> Unit): Options =
    Options.DslBuilder().apply(block).build()

/**
 *
 * */
public fun String.type(): Type? = when(this) {
    Type.Data::class.java.simpleName -> Type.Data()
}
