package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.ValueObject

public interface Abstr : ValueObject.Data {

    public val name: Name
    /*
    public val some: ValueObject?
    public val someList: List<ValueObject>
    public val someListNullable: List<ValueObject?>
    public val someMap: Map<Name, ValueObject?>*/
    public val myEnum: MyEnum
    public val enumList: List<MyEnum>
    public val point: Point

    override fun validate() {}

    public interface Name : ValueObject.Boxed<String> {
        override fun validate() {}
    }

    public enum class MyEnum : ValueObject.Sealed {
        A, B, C
    }
}
