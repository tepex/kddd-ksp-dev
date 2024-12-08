package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.impl.MyStructImpl
import ru.it_arch.ddd.Parsable
import ru.it_arch.ddd.ValueObject
import ru.it_arch.ddd.ValueObjectSingle
import java.net.URI


/** Операции  */

public interface MyStruct : ValueObject {
    public val name: Name
    public val optName: Name?
    public val count: Count
    public val uri: Uri
    public val names: List<Name>
    public val nullableNames: List<Name?>
    public val indexes: Set<Index>
    public val myMap: Map<Index, Name?>
    public val inner: Inner
    public val nullableInner: Inner?
    public val innerList: List<Inner> // innerList { a = ... b = ... }
    public val mapInnerValue: Map<Name?, Inner>
    public val mapInnerKeyValue: Map<Inner, Inner>
    /*
    public val mapInnerKey: Map<Inner, Name>*/
    //public val str: String

    override fun validate() {
        require(names.size > 0) { "names size must be > 0" }
    }


    public interface Name : ValueObjectSingle<String> {
        override fun validate() {}

        public fun upper(): Name =
            copy(value.uppercase())
    }

    public interface Count : ValueObjectSingle<Int> {
        override fun validate() {
            require(value > 0 && value in (1..15))
        }

        public fun inc(): Count =
            copy(value + 1)

        public operator fun plus(i: Int): Count =
            copy(value + i)
    }

    public interface Index : ValueObjectSingle<Int> {
        override fun validate() {
            value in (1..10) || value in (20..30)
        }
    }

    public interface Uri : ValueObjectSingle<URI>, Parsable<URI> {

        override fun validate() {}

        override fun parse(str: String): URI =
            URI.create(str)
    }

    public interface Inner : ValueObject {
        public val innerLong: InnerLong
        public val innerStr: InnerStr

        override fun validate() {}

        public interface InnerLong : ValueObjectSingle<Long> {
            override fun validate() {}

            public fun dec(): InnerLong =
                MyStructImpl.InnerImpl.InnerLongImpl.create(value-1)
        }

        public interface InnerStr : ValueObjectSingle<String> {
            override fun validate() {}
        }
    }
}
