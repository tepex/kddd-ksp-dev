package ru.it_arch.clean_ddd.domain.habr.impl

import ru.it_arch.clean_ddd.domain.habr.api.MyType

public fun myType(block: MyTypeImpl.DslBuilder.() -> Unit): MyType = MyTypeImpl.DslBuilder().apply(block).build()
