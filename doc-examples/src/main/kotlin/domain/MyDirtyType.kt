package ru.it_arch.kddd.magic.domain

import ru.it_arch.kddd.ValueObject
import java.io.File
import java.util.UUID

data class MyDirtyType(
    //
    var name: String,
    var count: Int,
    var components: Map<UUID, NestedType>
) {

    data class NestedType(
        var myFile: File,
        var desc: String
    )
}

val myDirtyType = MyDirtyType(
    "my name",
    1,
    mapOf(
        UUID.randomUUID() to MyDirtyType.NestedType(File("/path/to/file1"), "file 1"),
        UUID.randomUUID() to MyDirtyType.NestedType(File("/path/to/file2"), "file 2"),
    )
)

interface MyType : ValueObject.Data {
    val name: Name
    val count: Count
    val components: Map<MyUuid, NestedType>

    override fun validate() {}

    interface Name : ValueObject.Value<String> {
        override fun validate() {
            // Полагается бизнес-правило, что имя не должно содержать символы `_, -, .`
            require(boxed.contains(NAME_RULE).not())
        }
    }

    interface Count : ValueObject.Value<Int> {
        // Полагается бизнес-правило, что значение счетчика находится в диапазоне 0...100
        override fun validate() {
            require(boxed in 0..100)
        }
    }

    // TODO: add annotation
    interface MyUuid : ValueObject.Value<UUID> {
        override fun validate() {}
    }

    interface NestedType : ValueObject.Data {
        val myFile: MyFile
        val desc: Desc

        override fun validate() {}

        // TODO: add annotation
        interface MyFile : ValueObject.Value<File> {
            override fun validate() {}
        }

        interface Desc : ValueObject.Value<String> {
            override fun validate() {}
        }
    }

    companion object {
        val NAME_RULE = """[_\-.]""".toRegex()
    }
}

// Use cases

fun MyType.Count.increment(): MyType.Count =
    apply(boxed + 1)

fun MyType.incrementCount(): MyType =
    fork(name, count.increment(), components)
