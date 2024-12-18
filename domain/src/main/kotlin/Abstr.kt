package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.ValueObject

public interface Abstr : ValueObject.Data {

    public val name: Name
    public val some: ValueObject

    override fun validate() {}

    public interface Name : ValueObject.Boxed<String> {
        override fun validate() {}
    }
}
