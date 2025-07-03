package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.k3dm.Generatable
import ru.it_arch.k3dm.Parsable
import ru.it_arch.k3dm.ValueObject
import java.io.File
import java.net.URI
import java.util.UUID

/**
 * Демонстрация использования общих типов в качестве полей.
 *
 * Для типов, которые являются не примитивами и которые оборачиватся в [ValueObject.Value] необходима
 * аннотация [KDParsable] которая определят какими методами этот тип сериализуется/десериализуется.
 *
 * Также необходимо указать способ инициализации типа в DSL/JSON, прописав статический метод в параметре аннотации
 * `deserialization`, отвечающий за создание объекта из строки. Для объектов, создаваемых через конструктор,
 * параметр `deserialization` не определяется.
 * */
@Generatable(json = true)
public interface CommonTypes : ValueObject.Data {
    public val myUri: MyUri
    public val myFile: MyFile
    public val myUUID: MyUUID
    public val myOptionalUUID: MyUUID?

    override fun validate() {}

    // Параметр `deserialization` не указан — объект будет создаваться через конструктор со строковым аргументом со
    // значением из DSL/JSON: `File("<value>")`
    @Parsable(useStringInDsl = true)
    public interface MyFile : ValueObject.Value<File> {
        override fun validate() {
            require(boxed.exists()) { "File `$boxed` not exists!" }
        }
    }

    // Объект будет создаваться через `UUID.fromString("<value>")`
    @Parsable(deserialization = "fromString")
    public interface MyUUID : ValueObject.Value<UUID> {
        override fun validate() {}
    }

    @Parsable(deserialization = "create", useStringInDsl = true)
    public interface MyUri : ValueObject.Value<URI> {
        override fun validate() {}
    }
}
