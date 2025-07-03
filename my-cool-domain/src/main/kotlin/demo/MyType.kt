package ru.it_arch.clean_ddd.domain.demo

import ru.it_arch.k3dm.Generatable
import ru.it_arch.k3dm.Parsable
import ru.it_arch.k3dm.ValueObject
import java.io.File
import java.util.UUID

@Generatable(json = true, dsl = true)
public interface MyType : ValueObject.Data {
    public val name: Name
    public val count: Count
    //public val components: Map<MyUuid, NestedType>

    override fun validate() {}

    public interface Name : ValueObject.Value<String> {
        override fun validate() {
            // Полагается бизнес-правило, что имя не должно содержать символы `_, -, .`
            """[_\-.]""".also { require(boxed.contains(it.toRegex()).not()) }
        }
    }

    public interface Count : ValueObject.Value<Int> {
        // Полагается бизнес-правило, что значение счетчика находится в диапазоне 0...100
        override fun validate() {
            require(boxed in 0..100)
        }
    }

    // Объект будет создаваться через `UUID.fromString("<value>")`
    @Parsable(deserialization = "fromString")
    public interface MyUuid : ValueObject.Value<UUID> {
        override fun validate() {}
    }

    @Generatable(json = true)
    public interface NestedType : ValueObject.Data {
        public val myFile: MyFile
        public val desc: Desc

        override fun validate() {}

        // Параметр `deserialization` не указан — объект будет создаваться через конструктор со строковым аргументом со
        // значением из DSL/JSON: `File("<value>")`
        @Parsable(useStringInDsl = true)
        public interface MyFile : ValueObject.Value<File> {
            override fun validate() {}
        }

        public interface Desc : ValueObject.Value<String> {
            override fun validate() {}
        }
    }

    /*
    public companion object {
        public val NAME_RULE: Regex = """[_\-.]""".toRegex()
    }*/
}
