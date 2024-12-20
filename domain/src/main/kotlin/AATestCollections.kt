package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.ValueObject

public interface AATestCollections : ValueObject.Data {
    public val name: Name

    public val list: List<Name>
    //public val nestedList: Set<List<Name>>
    public val nestedList: Set<List<Inner>>
    public val nestedNestedList: List<List<List<List<Name>>>>

    public val nestedMap: Map<Name, List<Name>>
    public val nestedMaps: Map<Map<Name, Inner>, List<List<Inner>>>

    override fun validate() {}

    public interface Name : ValueObject.Boxed<String> {
        override fun validate() {}
    }

    public interface Inner : ValueObject.Data {
        public val innerLong: InnerLong
        public val innerStr: InnerStr

        override fun validate() {}

        public interface InnerLong : ValueObject.Boxed<Long> {
            override fun validate() {}
        }

        public interface InnerStr : ValueObject.Boxed<String> {
            override fun validate() {}
        }
    }
}
