package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.domain.demo.CommonTypes
import ru.it_arch.clean_ddd.domain.demo.impl.CommonTypesImpl
import ru.it_arch.clean_ddd.domain.demo.impl.commonTypes
import java.util.UUID

fun testCommonTypes() {
    // Демонстрация сериализации модели
    commonTypes {
        // Т.к. для типа `MyUri` задана аннотация `@KDParsable` с параметром `useStringInDsl = true`,
        // то для инициализации используется строка
        myUri = "https://google.com"
        // Ваш существующий файл
        myFile = "/etc/hosts"
        // а здесь `useStringInDsl` не задан - используется объект
        myUUID = UUID.randomUUID()
    }.apply {
        println("\ncommons demo: $this")
        json.encodeToString(this).also { println("json: $it") }
    }

    // Демонстрация десериализации модели

    """
{
    "my-uri": "https://ya.ru"
    "my-file": "/etc/group"
    "my-uuid": "50d3d60b-b4d7-4fca-a984-d911a3688f99"
}""".apply {
        json.decodeFromString<CommonTypesImpl>(this).also { obj: CommonTypes ->
            // Можете пользоваться
            println("deserialize: $obj")
        }
    }
}
