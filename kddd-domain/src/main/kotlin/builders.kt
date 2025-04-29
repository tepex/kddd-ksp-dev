package ru.it_arch.clean_ddd.domain

public fun className(block: (ClassName.DslBuilder) -> Unit): ClassName =
    ClassName.DslBuilder().apply(block).build()


public fun property(block: (Property.DslBuilder) -> Unit): Property =
    Property.DslBuilder().apply(block).build()
