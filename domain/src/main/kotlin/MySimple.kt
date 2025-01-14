package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.AATestCollections.Name
import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.KDSerialName
import ru.it_arch.kddd.ValueObject
import java.io.File
import java.net.URI
import java.util.UUID

@KDGeneratable(json = true)
public interface MySimple : ValueObject.Data {
    @KDSerialName("qqq")
    public val nameName: Name
    public val count: Count
    public val inner: Inner
    public val uri: SomeUri // String
    public val listUri: List<SomeUri>
    public val nullableListUri: List<SomeUri?>
    public val file: SomeFile
    public val uuid: SomeUUID?
    public val mapUUID: Map<Name, SomeUUID>
    public val mapUUIDNullable: Map<Name, SomeUUID?>
    public val mapUUIDAll: Map<SomeUUID, SomeUUID>
    //public val kotlinUuid: SomeUuid
    //public val myEnum: MyEnum
    //public val point: Point
    //public val empty: List<ValueObject>
    public val nestedList1: List<Set<Name>>
    public val nestedMap: Map<Name?, List<Name>>

    override fun validate() {  }

    public interface Name : ValueObject.Boxed<String> {
        override fun validate() {}
    }

    public interface Count : ValueObject.Boxed<Short> {
        override fun validate() {}
    }

    @KDParsable(deserialization = "create", useStringInDsl = true)
    public interface SomeUri : ValueObject.Boxed<URI> {
        override fun validate() {}
    }

    @KDParsable(useStringInDsl = true)
    public interface SomeFile : ValueObject.Boxed<File> {
        override fun validate() {}
    }

    @KDParsable(deserialization = "fromString")
    public interface SomeUUID : ValueObject.Boxed<UUID> {
        override fun validate() {}
    }

    public enum class MyEnum : ValueObject.Sealed {
        A, B, C
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

    /*
    public interface MyContainer<A : ValueObject, B : List<ValueObject>, C : ValueObject.Boxed<String>> : ValueObject.Data {
        public val a: A
        public val b: B
        public val c: C
    }*/

    /*
    @OptIn(ExperimentalUuidApi::class)
    public interface SomeUuid : ValueObject.Boxed<Uuid> {
        override fun validate() {}
    }*/
}
