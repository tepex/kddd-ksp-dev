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

/**
 *
 * */
/*
public fun String.type(): Type? = when(this) {
    Type.Data::class.java.simpleName -> Type.Data()
}*/
