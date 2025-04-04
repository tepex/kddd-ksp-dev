package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.domain.demo.NullablePrimitives
import ru.it_arch.clean_ddd.domain.demo.impl.NullablePrimitivesImpl
import ru.it_arch.clean_ddd.domain.demo.impl.nullablePrimitives

// Имена свойств записываются в kebab-case стиле, т.к. такой режим выставлен в настройках Json (с.м. Main.kt)
const val jsonSrc1 = """
{
    "str": "some string",
    "size": 44,
    "bool-value": true,
    "byte-value": 33,
    "char-value": "x",
    "float-value": 2.71828,
    "double-value": 1.4142135623,
    "long-value": 987654321,
    "short-value": -123
}"""

fun testNullables() {
    // Демонстрация сериализации модели
    nullablePrimitives {
        str = "some string for demo"
        size = 55
        boolValue = true
        byteValue = 22
        charValue = 'c'
        floatValue = 3.14f
        doubleValue = 12.3456789
        longValue = -12423423423
        shortValue = 255
    }.apply {
        println("demo: $this")
        json.encodeToString(this).also { println("json: $it") }
    }

    // Демонстрация десериализации модели
    val obj: NullablePrimitives = json.decodeFromString<NullablePrimitivesImpl>(jsonSrc1)
    // Можете пользоваться
    println("deserialize: $obj")
}
