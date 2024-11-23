package ru.it_arch.clean_ddd.domain

import ru.it_arch.ddd.ValueObject
import ru.it_arch.ddd.ValueObjectSingle

public interface MyValueObject /*: ValueObject*/ {
    public val name: Name
    public val count: Count

    public interface Name : ValueObjectSingle<String> {
        override fun validate() {}
    }

    public interface Count : ValueObjectSingle<Int> {
        override fun validate() {}
    }
}
