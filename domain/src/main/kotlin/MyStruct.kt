package ru.it_arch.clean_ddd.domain

import ru.it_arch.ddd.Parsable
import ru.it_arch.ddd.ValueObject
import ru.it_arch.ddd.ValueObjectSingle
import java.net.URI

public interface MyStruct : ValueObject {
    public val name: Name
    public val optName: Name?
    public val count: Count
    public val uri: Uri
    public val names: List<Name>
    public val indexes: Set<Index>
    //public val myMap: Map<Index, Name?>
    public val inner: Inner

    override fun validate() {
        require(names.size > 2) { "names size must be > 2" }
    }

    public interface Name : ValueObjectSingle<String> {
        override fun validate() {}
    }

    public interface Count : ValueObjectSingle<Int> {
        override fun validate() {
            require(value > 0 && value in (1..15))
        }
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
        }

        public interface InnerStr : ValueObjectSingle<String> {
            override fun validate() {}
        }
    }
}
