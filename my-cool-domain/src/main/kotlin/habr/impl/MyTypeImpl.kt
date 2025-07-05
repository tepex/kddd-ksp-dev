//
// AUTO-GENERATED FILE. DO NOT MODIFY.
// This file generated automatically by KDDD framework.
// Author: Tepex <tepex@mail.ru>, Telegram: @Tepex
//

package ru.it_arch.clean_ddd.domain.habr.impl

import java.io.File
import java.util.UUID
import kotlin.Any
import kotlin.ConsistentCopyVisibility
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.Map
import kotlin.collections.MutableMap
import kotlin.jvm.JvmInline
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import ru.it_arch.clean_ddd.domain.habr.api.MyType
import kotlinx.serialization.ExperimentalSerializationApi
import ru.it_arch.k3dm.ValueObject

@ConsistentCopyVisibility
@Serializable(with = MyTypeImpl.Companion::class)
public data class MyTypeImpl private constructor(
    override val name: MyType.Name,
    override val count: MyType.Count,
    override val components: Map<MyType.MyUuid, MyType.Name>,
) : MyType {
    init {
        validate()
    }
    @Suppress("UNCHECKED_CAST")
    override fun <T : ValueObject.Data> fork(vararg args: Any?): T {
        val ret = Builder().apply {

            name = args[0] as MyType.Name
            count = args[1] as MyType.Count
            components = args[2] as Map<MyType.MyUuid, MyType.Name>
        }.build() as T
        return ret
    }

    public fun MyType.toBuilder(): MyTypeImpl.Builder {
        val ret = MyTypeImpl.Builder()
        ret.name = name
        ret.count = count
        ret.components = components
        return ret
    }

    public fun MyType.toDslBuilder(): MyTypeImpl.DslBuilder {
        val ret = MyTypeImpl.DslBuilder()
        ret.name = name.boxed
        ret.count = count.boxed
        ret.components = components.entries.associate { it.key.boxed to it.value.boxed }.toMutableMap()
        return ret
    }

    @JvmInline
    public value class NameImpl private constructor(
        override val boxed: String,
    ) : MyType.Name {
        init {
            validate()
        }
        override fun toString(): String = boxed.toString()

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Value<String>> apply(boxed: String): T = NameImpl(boxed) as T

        public companion object {
            public operator fun invoke(boxed: String): MyType.Name = NameImpl(boxed)
        }
    }

    @JvmInline
    public value class CountImpl private constructor(
        override val boxed: Int,
    ) : MyType.Count {
        init {
            validate()
        }
        override fun toString(): String = boxed.toString()

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Value<Int>> apply(boxed: Int): T = CountImpl(boxed) as T

        public companion object {
            public operator fun invoke(boxed: Int): MyType.Count = CountImpl(boxed)
        }
    }

    @JvmInline
    public value class MyUuidImpl private constructor(
        override val boxed: UUID,
    ) : MyType.MyUuid {
        init {
            validate()
        }
        override fun toString(): String = boxed.toString()

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Value<UUID>> apply(boxed: UUID): T = MyUuidImpl(boxed) as T

        public companion object {
            public operator fun invoke(boxed: UUID): MyType.MyUuid = MyUuidImpl(boxed)

            public fun parse(src: String): MyUuidImpl = UUID.fromString(src).let(::MyUuidImpl)
        }
    }

    @ConsistentCopyVisibility
    @Serializable(with = NestedTypeImpl.Companion::class)
    public data class NestedTypeImpl private constructor(
        override val myFile: MyType.NestedType.MyFile,
        override val desc: MyType.NestedType.Desc,
    ) : MyType.NestedType {
        init {
            validate()
        }
        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Data> fork(vararg args: Any?): T {
            val ret = Builder().apply {

                myFile = args[0] as MyType.NestedType.MyFile
                desc = args[1] as MyType.NestedType.Desc
            }.build() as T
            return ret
        }

        public fun MyType.NestedType.toBuilder(): NestedTypeImpl.Builder {
            val ret = NestedTypeImpl.Builder()
            ret.myFile = myFile
            ret.desc = desc
            return ret
        }

        public fun MyType.NestedType.toDslBuilder(): NestedTypeImpl.DslBuilder {
            val ret = NestedTypeImpl.DslBuilder()
            ret.myFile = myFile.boxed.toString()
            ret.desc = desc.boxed
            return ret
        }

        @JvmInline
        public value class MyFileImpl private constructor(
            override val boxed: File,
        ) : MyType.NestedType.MyFile {
            init {
                validate()
            }
            override fun toString(): String = boxed.toString()

            @Suppress("UNCHECKED_CAST")
            override fun <T : ValueObject.Value<File>> apply(boxed: File): T = MyFileImpl(boxed) as T

            public companion object {
                public operator fun invoke(boxed: File): MyType.NestedType.MyFile = MyFileImpl(boxed)

                public fun parse(src: String): MyFileImpl = File(src).let(::MyFileImpl)
            }
        }

        @JvmInline
        public value class DescImpl private constructor(
            override val boxed: String,
        ) : MyType.NestedType.Desc {
            init {
                validate()
            }
            override fun toString(): String = boxed.toString()

            @Suppress("UNCHECKED_CAST")
            override fun <T : ValueObject.Value<String>> apply(boxed: String): T = DescImpl(boxed) as T

            public companion object {
                public operator fun invoke(boxed: String): MyType.NestedType.Desc = DescImpl(boxed)
            }
        }

        public class Builder {
            public lateinit var myFile: MyType.NestedType.MyFile

            public lateinit var desc: MyType.NestedType.Desc

            public fun build(): NestedTypeImpl {
                require(::myFile.isInitialized) { "Property 'NestedTypeImpl.myFile' is not set!" }
                require(::desc.isInitialized) { "Property 'NestedTypeImpl.desc' is not set!" }
                return NestedTypeImpl(myFile = myFile,desc = desc,)
            }
        }

        public class DslBuilder {
            public var myFile: String? = null

            public var desc: String? = null

            public fun build(): NestedTypeImpl {
                requireNotNull(myFile) { "Property 'NestedTypeImpl.myFile' is not set!" }
                requireNotNull(desc) { "Property 'NestedTypeImpl.desc' is not set!" }
                return NestedTypeImpl(myFile = MyFileImpl.parse(myFile!!), desc = DescImpl(desc!!), )
            }
        }

        /**
         * https://stackoverflow.com/questions/65272262/custom-serializer-for-data-class-without-serializable
         */
        @OptIn(ExperimentalSerializationApi::class)
        public companion object : KSerializer<NestedTypeImpl> {
            override val descriptor: SerialDescriptor =
                buildClassSerialDescriptor(NestedTypeImpl::class.java.name) {
                    element<String>("myFile")
                    element<String>("desc")
                }

            override fun serialize(encoder: Encoder, `value`: NestedTypeImpl) {
                encoder.encodeStructure(descriptor) {
                    encodeStringElement(descriptor, 0, `value`.myFile.boxed.toString())
                    encodeStringElement(descriptor, 1, `value`.desc.boxed)
                }
            }

            override fun deserialize(decoder: Decoder): NestedTypeImpl {
                val ret = decoder.decodeStructure(descriptor) {
                    val builder = Builder()
                    loop@ while (true) {
                        when (val i = decodeElementIndex(descriptor)) {
                            0 -> builder.myFile = decodeStringElement(descriptor, 0).let(MyFileImpl::parse)
                            1 -> builder.desc = decodeStringElement(descriptor, 1).let { DescImpl(it) }
                            DECODE_DONE -> break@loop
                            else -> throw SerializationException("Unexpected index $i")
                        }
                    }
                    builder.build()
                }
                return ret
            }
        }
    }

    public class Builder {
        public lateinit var name: MyType.Name

        public lateinit var count: MyType.Count

        public var components: Map<MyType.MyUuid, MyType.Name> = emptyMap()

        public fun build(): MyTypeImpl {
            require(::name.isInitialized) { "Property 'MyTypeImpl.name' is not set!" }
            require(::count.isInitialized) { "Property 'MyTypeImpl.count' is not set!" }
            return MyTypeImpl(name = name,count = count,components = components, )
        }
    }

    public class DslBuilder {
        public var name: String? = null

        public var count: Int? = null

        public var components: MutableMap<UUID, String> = mutableMapOf()

        public fun build(): MyTypeImpl {
            requireNotNull(name) { "Property 'MyTypeImpl.name' is not set!" }
            requireNotNull(count) { "Property 'MyTypeImpl.count' is not set!" }
            return MyTypeImpl(name = NameImpl(name!!), count = CountImpl(count!!), components = components.entries.associate { MyUuidImpl(it.key) to NameImpl(it.value) }.toMap(),
            )
        }
    }

    /**
     * https://stackoverflow.com/questions/65272262/custom-serializer-for-data-class-without-serializable
     */
    @OptIn(ExperimentalSerializationApi::class)
    public companion object : KSerializer<MyTypeImpl> {
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor(MyTypeImpl::class.java.name) {
                element<String>("name")
                element<Int>("count")
                element<Map<String, String>>("components")
            }

        override fun serialize(encoder: Encoder, `value`: MyTypeImpl) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, `value`.name.boxed)
                encodeIntElement(descriptor, 1, `value`.count.boxed)
                encodeSerializableElement(descriptor, 2, MapSerializer(String.serializer(), String.serializer()), `value`.components.entries.associate { it.key.boxed.toString() to it.value.boxed }.toMap())
            }
        }

        override fun deserialize(decoder: Decoder): MyTypeImpl {
            val ret = decoder.decodeStructure(descriptor) {
                val builder = Builder()
                loop@ while (true) {
                    when (val i = decodeElementIndex(descriptor)) {
                        0 -> builder.name = decodeStringElement(descriptor, 0).let { NameImpl(it) }
                        1 -> builder.count = decodeIntElement(descriptor, 1).let { CountImpl(it) }
                        2 -> builder.components = decodeSerializableElement(descriptor, 2, MapSerializer(String.serializer(), String.serializer())).entries.associate { it.key.let(MyUuidImpl::parse) to it.value.let { NameImpl(it) } }.toMap()
                        DECODE_DONE -> break@loop
                        else -> throw SerializationException("Unexpected index $i")
                    }
                }
                builder.build()
            }
            return ret
        }
    }
}
