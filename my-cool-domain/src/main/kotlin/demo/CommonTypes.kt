package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.kddd.KDGeneratable
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.ValueObject
import java.io.File
import java.net.URI
import java.util.UUID

/**
 * Демонстрация использования общих типов в качестве полей.
 *
 * Для типов, которые являются не примитивами и которые оборачиватся в [ValueObject.Boxed] необходима
 * аннотация [KDParsable] которая определят какими методами этот тип сериализуется/десериализуется.
 *
 * Также необходимо указать способ инициализации типа в DSL/JSON, прописав статический метод в параметре аннотации
 * `deserialization`, отвечающий за создание объекта из строки. Для объектов, создаваемых через конструктор,
 * параметр `deserialization` не определяется.
 * */
@KDGeneratable(json = true)
public interface CommonTypes : ValueObject.Data {
    public val myUri: MyUri
    public val myFile: MyFile
    public val myUUID: MyUUID
    public val myOptionalUUID: MyUUID?

    override fun validate() {

    }

    // Параметр `deserialization` не указан — объект будет создаваться через констрктор со строковым аргументом со
    // значением из DSL/JSON: `File("<value>")`
    @KDParsable(useStringInDsl = true)
    public interface MyFile : ValueObject.Boxed<File> {
        override fun validate() {
            require(boxed.exists()) { "File `$boxed` not exists!" }
        }
    }

    // Объект будет создаваться через `UUID.fromString("<value>")`
    @KDParsable(deserialization = "fromString")
    public interface MyUUID : ValueObject.Boxed<UUID> {
        override fun validate() {}
    }

    @KDParsable(deserialization = "create", useStringInDsl = true)
    public interface MyUri : ValueObject.Boxed<URI> {
        override fun validate() {}
    }

}
