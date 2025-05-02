package ru.it_arch.clean_ddd.domain

public val Type.Boxed.boxed: Property
    get() = properties.first()
