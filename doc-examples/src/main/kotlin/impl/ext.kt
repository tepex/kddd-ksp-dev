package ru.it_arch.kddd.magic.impl

import ru.it_arch.kddd.magic.domain.ExampleForDslMode
import ru.it_arch.kddd.magic.domain.Point

/** [Регламент/Имплементация CDT п.II](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-to-builder) */
fun Point.toBuilder(): PointImpl.Builder =
    PointImpl.Builder().apply {
        x = this@toBuilder.x
        y = this@toBuilder.y
    }

/** [Регламент/Имплементация CDT п.II](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-to-builder) */
fun ExampleForDslMode.toBuilder(): ExampleForDslModeImpl.Builder =
    ExampleForDslModeImpl.Builder().apply {
        primitive = this@toBuilder.primitive
        anyUuid = this@toBuilder.anyUuid
        anyFile = this@toBuilder.anyFile
        nested = this@toBuilder.nested
        simpleList = this@toBuilder.simpleList
        simpleMap = this@toBuilder.simpleMap
    }

fun ExampleForDslMode.toDslBuilder(): ExampleForDslModeImpl.DslBuilder =
    ExampleForDslModeImpl.DslBuilder().apply {
        primitive = this@toDslBuilder.primitive.boxed
        anyUuid = this@toDslBuilder.anyUuid.boxed.toString()
        anyFile = this@toDslBuilder.anyFile.boxed
        nested = this@toDslBuilder.nested
        simpleList = this@toDslBuilder.simpleList.map { it.boxed }.toMutableList()
        simpleMap = this@toDslBuilder.simpleMap.entries.associate { it.key.boxed to it.value?.toString() }.toMutableMap()
    }

/** [Регламент/Имплементация CDT п.II](https://github.com/tepex/kddd-ksp-dev/blob/new-arch/docs/kddd.adoc#reg-impl-to-builder) */
fun ExampleForDslMode.SomeNestedType.toBuilder(): ExampleForDslModeImpl.SomeNestedTypeImpl.Builder =
    ExampleForDslModeImpl.SomeNestedTypeImpl.Builder().apply {
        simple = this@toBuilder.simple
        nullableSimple = this@toBuilder.nullableSimple
    }

fun ExampleForDslMode.SomeNestedType.toDslBuilder(): ExampleForDslModeImpl.SomeNestedTypeImpl.DslBuilder =
    ExampleForDslModeImpl.SomeNestedTypeImpl.DslBuilder().apply {
        simple = this@toDslBuilder.simple.boxed
        nullableSimple = this@toDslBuilder.nullableSimple?.boxed
    }

fun exampleForDslMode(block: ExampleForDslModeImpl.DslBuilder.() -> Unit): ExampleForDslMode =
    ExampleForDslModeImpl.DslBuilder().apply(block).build()

fun someNestedType(block: ExampleForDslModeImpl.SomeNestedTypeImpl.DslBuilder.() -> Unit): ExampleForDslMode.SomeNestedType =
    ExampleForDslModeImpl.SomeNestedTypeImpl.DslBuilder().apply(block).build()
