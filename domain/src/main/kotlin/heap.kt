package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.ValueObject

public interface A : ValueObject.Data {
    public val name: Name
    //public val b: B
    public val point: Point

    override fun validate() {}

    public interface Name : ValueObject.Boxed<String> {
        override fun validate() {}
    }
}

public interface B : ValueObject.Data {
    public val name: Name
    //public val aName: A.Name

    override fun validate() {}

    public interface Name : ValueObject.Boxed<String> {
        override fun validate() {}
    }
}
