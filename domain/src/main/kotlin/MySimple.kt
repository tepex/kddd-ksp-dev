package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.ValueObject
import java.io.File
import java.net.URI
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

public interface MySimple : ValueObject.Data {
    public val name: Name
    public val uri: SomeUri
    public val listUri: List<SomeUri>
    public val nullableListUri: List<SomeUri?>
    public val file: SomeFile
    public val uuid: SomeUUID?
    public val mapUUID: Map<Name, SomeUUID>
    public val mapUUIDNullable: Map<Name, SomeUUID?>
    public val mapUUIDAll: Map<SomeUUID, SomeUUID>
    //public val kotlinUuid: SomeUuid
    public val myEnum: MyEnum
    //public val point: Point
    //public val empty: List<ValueObject>

    override fun validate() {  }

    public interface Name : ValueObject.Boxed<String> {
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
