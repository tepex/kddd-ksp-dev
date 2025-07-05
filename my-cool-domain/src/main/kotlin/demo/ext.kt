package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.clean_ddd.domain.habr.api.MyType

public fun MyType.Count.increment(): MyType.Count =
    apply(boxed + 1)


public fun MyType.incrementCount(): MyType =
    fork(name, count.increment(), components)
