package ru.it_arch.kddd.magic.impl

import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject
import ru.it_arch.kddd.magic.domain.ExampleForDslMode
import java.io.File
import java.util.UUID

@ConsistentCopyVisibility
data class ExampleForDslModeImpl private constructor(
    override val primitive: ExampleForDslMode.Primitive,
    override val anyUuid: ExampleForDslMode.CommonUuid,
    override val anyFile: ExampleForDslMode.CommonFile,
    override val nested: ExampleForDslMode.SomeNestedType,
    override val simpleList: List<ExampleForDslMode.Primitive>,
    override val simpleMap: Map<ExampleForDslMode.Primitive, ExampleForDslMode.CommonUuid?>
) : ExampleForDslMode {

    init {
        validate()
    }

    override fun validate() {}

    @Suppress("UNCHECKED_CAST")
    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T =
        Builder().apply {
            primitive = args[0] as ExampleForDslMode.Primitive
            anyUuid = args[1] as ExampleForDslMode.CommonUuid
            anyFile = args[2] as ExampleForDslMode.CommonFile
            nested = args[3] as ExampleForDslMode.SomeNestedType
            simpleList = args[4] as List<ExampleForDslMode.Primitive>
            simpleMap = args[5] as Map<ExampleForDslMode.Primitive, ExampleForDslMode.CommonUuid?>
        }.build() as T

    @JvmInline
    value class PrimitiveImpl private constructor(override val boxed: Int) : ExampleForDslMode.Primitive {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Value<Int>> fork(boxed: Int): T =
            PrimitiveImpl(boxed) as T

        override fun toString(): String =
            boxed.toString()

        companion object {
            operator fun invoke(value: Int): ExampleForDslMode.Primitive =
                PrimitiveImpl(value)
        }
    }

    @JvmInline
    value class CommonUuidImpl private constructor(override val boxed: UUID): ExampleForDslMode.CommonUuid {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Value<UUID>> fork(boxed: UUID): T =
            CommonUuidImpl(boxed) as T

        override fun toString(): String =
            boxed.toString()

        companion object {
            operator fun invoke(value: UUID): ExampleForDslMode.CommonUuid =
                CommonUuidImpl(value)

            fun parse(src: String): ExampleForDslMode.CommonUuid =
                UUID.fromString(src).let(::CommonUuidImpl)
        }
    }

    @JvmInline
    value class CommonFileImpl private constructor(override val boxed: File): ExampleForDslMode.CommonFile {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Value<File>> fork(boxed: File): T =
            CommonFileImpl(boxed) as T

        override fun toString(): String =
            boxed.toString()

        companion object {
            operator fun invoke(value: File): ExampleForDslMode.CommonFile =
                CommonFileImpl(value)
        }
    }

    @ConsistentCopyVisibility
    data class SomeNestedTypeImpl private constructor(
        override val simple: ExampleForDslMode.SomeNestedType.SimpleType,
        override val nullableSimple: ExampleForDslMode.SomeNestedType.SimpleType?
    ): ExampleForDslMode.SomeNestedType {

        init {
            validate()
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Kddd, A : Kddd> fork(vararg args: A): T = Builder().apply {
            simple = args[0] as ExampleForDslMode.SomeNestedType.SimpleType
            nullableSimple = args[1] as ExampleForDslMode.SomeNestedType.SimpleType?
        }.build() as T

        @JvmInline
        value class SimpleTypeImpl private constructor(
            override val boxed: String
        ) : ExampleForDslMode.SomeNestedType.SimpleType {

            init {
                validate()
            }

            override fun toString(): String =
                boxed

            @Suppress("UNCHECKED_CAST")
            override fun <T : ValueObject.Value<String>> fork(boxed: String): T =
                SimpleTypeImpl(boxed) as T

            companion object {
                operator fun invoke(value: String): ExampleForDslMode.SomeNestedType.SimpleType =
                    SimpleTypeImpl(value)
            }
        }

        class Builder {
            var simple: ExampleForDslMode.SomeNestedType.SimpleType? = null
            var nullableSimple: ExampleForDslMode.SomeNestedType.SimpleType? = null

            fun build(): ExampleForDslMode.SomeNestedType {
                checkNotNull(simple) { "Property SomeNestedTypeImpl.simple must be initialized!" }
                return SomeNestedTypeImpl(simple!!, nullableSimple)
            }
        }

        class DslBuilder {
            var simple: String? = null
            var nullableSimple: String? = null

            fun build(): ExampleForDslMode.SomeNestedType {
                checkNotNull(simple) { "Property SomeNestedTypeImpl.simple must be initialized!" }
                return SomeNestedTypeImpl(SimpleTypeImpl(simple!!), nullableSimple?.let { SimpleTypeImpl(it) })
            }
        }
    }

    class Builder {
        var primitive: ExampleForDslMode.Primitive? = null
        var anyUuid: ExampleForDslMode.CommonUuid? = null
        var anyFile: ExampleForDslMode.CommonFile? = null
        var nested: ExampleForDslMode.SomeNestedType? = null
        var simpleList: List<ExampleForDslMode.Primitive> = emptyList()
        var simpleMap: Map<ExampleForDslMode.Primitive, ExampleForDslMode.CommonUuid?> = emptyMap()

        fun build(): ExampleForDslMode {
            checkNotNull(primitive) { "Property ExampleForDslModeImpl.primitive must be initialized!" }
            checkNotNull(anyUuid) { "Property ExampleForDslModeImpl.anyUuid must be initialized!" }
            checkNotNull(anyFile) { "Property ExampleForDslModeImpl.anyFile must be initialized!" }
            checkNotNull(nested) { "Property ExampleForDslModeImpl.nested must be initialized!" }
            return ExampleForDslModeImpl(primitive!!, anyUuid!!, anyFile!!, nested!!, simpleList, simpleMap)
        }
    }

    class DslBuilder {
        var primitive: Int? = null
        var anyUuid: String? = null
        var anyFile: File? = null
        var nested: ExampleForDslMode.SomeNestedType? = null
        var simpleList: MutableList<Int> = mutableListOf()
        var simpleMap: MutableMap<Int, String?> = mutableMapOf()

        fun build(): ExampleForDslMode {
            checkNotNull(primitive) { "Property ExampleForDslModeImpl.primitive must be initialized!" }
            checkNotNull(anyUuid) { "Property ExampleForDslModeImpl.anyUuid must be initialized!" }
            checkNotNull(anyFile) { "Property ExampleForDslModeImpl.anyFile must be initialized!" }
            checkNotNull(nested) { "Property ExampleForDslModeImpl.nested must be initialized!" }
            return ExampleForDslModeImpl(
                PrimitiveImpl(primitive!!),
                CommonUuidImpl.parse(anyUuid!!),
                CommonFileImpl(anyFile!!),
                nested!!,
                simpleList.map { PrimitiveImpl(it) },
                simpleMap.entries.associate { PrimitiveImpl(it.key) to it.value?.let { CommonUuidImpl.parse(it) } }
            )
        }
    }
}
