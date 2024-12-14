package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.IEntity
import ru.it_arch.kddd.ValueObject

public interface MyEntity : IEntity {
    // Surrogate
    override val id: Id
    public val name: Name

    override fun validate() {}

    public interface Id : ValueObject.Boxed<Int> {
        override fun validate() {}
    }

    public interface Name : ValueObject.Boxed<String> {
        override fun validate() {}
    }
}
