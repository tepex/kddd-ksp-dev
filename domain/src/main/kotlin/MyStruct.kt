package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.impl.MyStructImpl
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.ValueObject
import java.net.URI


public interface MyStruct : ValueObject.Data {
    public val name: Name
    public val optName: Name?
    public val count: Count
    public val uri: Uri?
    public val names: List<Name>
    public val nullableNames: List<Name?>
    public val indexes: Set<Index>
    public val myMap: Map<Index, Name?>
    public val inner: Inner
    public val nullableInner: Inner?
    public val innerList: List<Inner?> // innerList { a = ... b = ... }
    public val mapInnerValue: Map<Name, Inner>
    public val mapInnerKeyValue: Map<Inner, Inner>
    public val mapInnerKey: Map<Inner, Name>
    public val mapKeyUri: Map<Uri, Inner>

    override fun validate() {
        require(names.isNotEmpty()) { "names size must be > 0" }
    }


    public interface Name : ValueObject.Boxed<String> {
        override fun validate() {}

        public fun upper(): Name =
            copy(boxed.uppercase())
    }

    public interface Count : ValueObject.Boxed<Int> {
        override fun validate() {
            require(boxed > 0 && boxed in (1..15))
        }

        public fun inc(): Count =
            copy(boxed + 1)

        public operator fun plus(i: Int): Count =
            copy(boxed + i)
    }

    public interface Index : ValueObject.Boxed<Int> {
        override fun validate() {
            boxed in (1..10) || boxed in (20..30)
        }
    }

    @KDParsable(deserialization = "create")
    public interface Uri : ValueObject.Boxed<URI>/*, Parsable<URI>*/ {

        override fun validate() {}

        /*
        override fun parse(str: String): URI =
            URI.create(str)*/
    }

    public interface Inner : ValueObject.Data {
        public val innerLong: InnerLong
        public val innerStr: InnerStr

        override fun validate() {}

        public interface InnerLong : ValueObject.Boxed<Long> {
            override fun validate() {}
            /*
            public fun dec(): InnerLong =
                MyStructImpl.InnerImpl.InnerLongImpl.create(boxed-1)*/
        }

        public interface InnerStr : ValueObject.Boxed<String> {
            override fun validate() {}
        }
    }
}

public fun MyStructImpl.updateCount(): MyStructImpl =
    toBuilder().also { it.count = count.inc() }.build()
