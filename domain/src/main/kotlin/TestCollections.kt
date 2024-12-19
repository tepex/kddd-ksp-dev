package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.ValueObject

public interface TestCollections : ValueObject.Data {
    //public val name: Name
    public val list: List<Name>
    public val nestedList: List<List<Name>>
    public val nestedMap: Map<Name, List<Name>>
    public val nestedMaps: Map<Map<Name, Name>, List<List<Name>>>

    override fun validate() {}

    public interface Name : ValueObject.Boxed<String> {
        override fun validate() {}
    }
}
