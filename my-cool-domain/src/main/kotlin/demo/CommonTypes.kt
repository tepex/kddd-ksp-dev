package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.ValueObject
import java.io.File
import java.net.URI
import java.util.UUID

/**
 * Демонстрация использования общих типов в качестве полей.
 * */
@KDGeneratable(json = true)
public interface CommonTypes : ValueObject.Data {
    public val myUri: MyUri
    public val myFile: MyFile
    public val myUUID: MyUUID

    override fun validate() {

    }

    @KDParsable(deserialization = "create", useStringInDsl = true)
    public interface MyUri : ValueObject.Boxed<URI> {
        override fun validate() {}
    }

    @KDParsable(useStringInDsl = true)
    public interface MyFile : ValueObject.Boxed<File> {
        override fun validate() {}
    }

    @KDParsable(deserialization = "fromString")
    public interface MyUUID : ValueObject.Boxed<UUID> {
        override fun validate() {}
    }
}
