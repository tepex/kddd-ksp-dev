package ru.it_arch.clean_ddd.domain

import ru.it_arch.ddd.ValueObject

public interface ValueObjectSingle<out T : Any> : ValueObject {
    public val value: T
}


public interface MyValueObject : ValueObject {
    public val name: Name
    public val count: Count

    public interface Name : ValueObjectSingle<String> {
        override fun validate() {}
    }

    public interface Count : ValueObjectSingle<Int> {
        override fun validate() {}
    }
}
