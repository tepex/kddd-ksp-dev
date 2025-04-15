package ru.it_arch.clean_ddd.app

import ru.it_arch.clean_ddd.domain.demo.NullablePrimitives
import ru.it_arch.clean_ddd.domain.demo.Primitives
import ru.it_arch.clean_ddd.domain.demo.impl.NullablePrimitivesImpl
import ru.it_arch.clean_ddd.domain.demo.impl.PrimitivesImpl
import ru.it_arch.clean_ddd.domain.demo.impl.json
import ru.it_arch.clean_ddd.domain.demo.impl.primitives

fun testPrimitives() {
    // Демонстрация сериализации модели
    primitives {
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
        println("\nprimitives demo: $this")
        println("json: ${toJson()}")
    }

    // Демонстрация десериализации модели

    // Имена свойств записываются в kebab-case стиле, т.к. такой режим выставлен в настройках Json (с.м. Main.kt)
    """
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
}""".apply {
        println("deserialize: ${fromJson()}")
    }
}

fun Primitives.toJson(): String =
    json.encodeToString(this as PrimitivesImpl)

fun String.fromJson(): Primitives =
    json.decodeFromString<PrimitivesImpl>(this)

fun String.fomJson(): NullablePrimitives =
    json.decodeFromString<NullablePrimitivesImpl>(this)
