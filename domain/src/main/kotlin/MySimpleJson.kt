package ru.it_arch.clean_ddd.domain

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import ru.it_arch.clean_ddd.domain.MySimple.SomeUri
import ru.it_arch.clean_ddd.domain.impl.MySimpleImpl.InnerImpl
import java.io.File
import java.net.URI
import java.util.UUID
import kotlin.ConsistentCopyVisibility
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.jvm.JvmInline
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
@Serializable(with = MySimpleJson.Companion::class)
public data class MySimpleJson private constructor(
    override val nameName: MySimple.Name,
    override val count: MySimple.Count,
    //override val inner: MySimple.Inner,
    override val uri: MySimple.SomeUri,
    override val listUri: List<MySimple.SomeUri>,
    override val nullableListUri: List<MySimple.SomeUri?>,
    override val `file`: MySimple.SomeFile,
    override val uuid: MySimple.SomeUUID?,
    override val mapUUID: Map<MySimple.Name, MySimple.SomeUUID>,
    override val mapUUIDNullable: Map<MySimple.Name, MySimple.SomeUUID?>,
    override val mapUUIDAll: Map<MySimple.SomeUUID, MySimple.SomeUUID>,
    //override val myEnum: MySimple.MyEnum,
    override val nestedList1: List<Set<MySimple.Name>>,
    override val nestedMap: Map<MySimple.Name?, List<MySimple.Name>>,
) : MySimple {

    init {
        validate()
    }

    public fun toBuilder(): Builder {
        val ret = Builder()
        ret.nameName = nameName
        //ret.inner = inner
        ret.uri = uri
        ret.listUri = listUri
        ret.nullableListUri = nullableListUri
        ret.file = file
        ret.uuid = uuid
        ret.mapUUID = mapUUID
        ret.mapUUIDNullable = mapUUIDNullable
        ret.mapUUIDAll = mapUUIDAll
        //ret.myEnum = myEnum
        return ret
    }

    public fun toDslBuilder(): DslBuilder {
        val ret = DslBuilder()
        ret.nameName = nameName.boxed
        //ret.inner = inner
        ret.uri = uri.boxed.toString()
        ret.listUri = listUri.map { it.boxed.toString() }.toMutableList()
        ret.nullableListUri = nullableListUri.map { it?.boxed.toString() }.toMutableList()
        ret.file = file.boxed.toString()
        ret.uuid = uuid?.boxed
        ret.mapUUID = mapUUID.entries.associate { it.key.boxed to it.value.boxed }.toMutableMap()
        ret.mapUUIDNullable = mapUUIDNullable.entries.associate { it.key.boxed to it.value?.boxed }.toMutableMap()
        ret.mapUUIDAll = mapUUIDAll.entries.associate { it.key.boxed to it.value.boxed }.toMutableMap()
        //ret.myEnum = myEnum
        return ret
    }

    @JvmInline
    public value class NameImpl private constructor(
        override val boxed: String,
    ) : MySimple.Name {
        init {
            validate()
        }

        override fun toString(): String = boxed.toString()

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<String>> copy(boxed: String): T = NameImpl(boxed) as T

        public companion object {
            public fun create(boxed: String): NameImpl = NameImpl(boxed)
        }
    }

    @JvmInline
    public value class CountImpl private constructor(override val boxed: Short): MySimple.Count {
        init {
            validate()
        }

        override fun toString(): String =
            boxed.toString()

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<Short>> copy(boxed: Short): T =
            create(boxed) as T

        public companion object {
            public fun create(boxed: Short): CountImpl = CountImpl(boxed)
        }
    }

    @JvmInline
    public value class SomeUriImpl private constructor(
        override val boxed: URI,
    ) : MySimple.SomeUri {
        init {
            validate()
        }

        override fun toString(): String = boxed.toString()

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<URI>> copy(boxed: URI): T = SomeUriImpl(boxed) as T

        public companion object {
            public fun create(boxed: URI): SomeUriImpl = SomeUriImpl(boxed)

            public fun parse(src: String): SomeUriImpl = URI.create(src).let(::create)
        }
    }

    @JvmInline
    public value class SomeFileImpl private constructor(
        override val boxed: File,
    ) : MySimple.SomeFile {
        init {
            validate()
        }

        override fun toString(): String = boxed.toString()

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<File>> copy(boxed: File): T = SomeFileImpl(boxed) as T

        public companion object {
            public fun create(boxed: File): SomeFileImpl = SomeFileImpl(boxed)

            public fun parse(src: String): SomeFileImpl = File(src).let(::create)
        }
    }

    @JvmInline
    public value class SomeUUIDImpl private constructor(
        override val boxed: UUID,
    ) : MySimple.SomeUUID {
        init {
            validate()
        }

        override fun toString(): String = boxed.toString()

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<UUID>> copy(boxed: UUID): T = SomeUUIDImpl(boxed) as T

        public companion object {
            public fun create(boxed: UUID): SomeUUIDImpl = SomeUUIDImpl(boxed)

            public fun parse(src: String): SomeUUIDImpl = UUID.fromString(src).let(::create)
        }
    }

    public class Builder {
        public lateinit var nameName: MySimple.Name

        public lateinit var count: MySimple.Count

        public lateinit var uri: MySimple.SomeUri

        //public lateinit var inner: MySimple.Inner

        public var listUri: List<MySimple.SomeUri> = emptyList()

        public var nullableListUri: List<MySimple.SomeUri?> = emptyList()

        public lateinit var `file`: MySimple.SomeFile

        public var uuid: MySimple.SomeUUID? = null

        public var mapUUID: Map<MySimple.Name, MySimple.SomeUUID> = emptyMap()

        public var mapUUIDNullable: Map<MySimple.Name, MySimple.SomeUUID?> = emptyMap()

        public var mapUUIDAll: Map<MySimple.SomeUUID, MySimple.SomeUUID> = emptyMap()

        //public lateinit var myEnum: MySimple.MyEnum

        public var nestedList1: List<Set<MySimple.Name>> = emptyList()

        public var nestedMap: Map<MySimple.Name?, List<MySimple.Name>> = emptyMap()

        public fun build(): MySimpleJson {
            require(::nameName.isInitialized) { "Property 'MySimpleImpl.name' is not set!" }
            require(::count.isInitialized) { "Property 'MySimpleImpl.count' is not set!" }
            require(::uri.isInitialized) { "Property 'MySimpleImpl.uri' is not set!" }
            require(::`file`.isInitialized) { "Property 'MySimpleImpl.`file`' is not set!" }
            //require(::myEnum.isInitialized) { "Property 'MySimpleImpl.myEnum' is not set!" }
            return MySimpleJson(
                nameName = nameName,
                count = count,
                //inner = inner,
                uri = uri, listUri = listUri,
                nullableListUri = nullableListUri, `file` = `file`, uuid = uuid, mapUUID = mapUUID,
                mapUUIDNullable = mapUUIDNullable, mapUUIDAll = mapUUIDAll,
                //myEnum = myEnum,
                nestedList1 = nestedList1,
                nestedMap = nestedMap,
            )
        }
    }

    public class DslBuilder {
        /** String representation of [MySimple.Name] */
        public var nameName: String? = null
        public var count: Short? = null
        //public var inner: MySimple.Inner? = null
        public var uri: String? = null

        public var listUri: MutableList<String> = mutableListOf()

        public var nullableListUri: MutableList<String?> = mutableListOf()

        public var `file`: String? = null

        public var uuid: UUID? = null

        public var mapUUID: MutableMap<String, UUID> = mutableMapOf()

        public var mapUUIDNullable: MutableMap<String, UUID?> = mutableMapOf()

        public var mapUUIDAll: MutableMap<UUID, UUID> = mutableMapOf()

        //public var myEnum: MySimple.MyEnum? = null

        public var nestedList1: MutableList<MutableSet<String>> = mutableListOf()

        public var nestedMap: MutableMap<String?, MutableList<String>> = mutableMapOf()

        public fun nameName(`value`: String): MySimple.Name = NameImpl.create(`value`)

        public fun count(value: Short): MySimple.Count = CountImpl.create(value)

        public fun someUri(`value`: String): MySimple.SomeUri = SomeUriImpl.parse(`value`)

        public fun someFile(`value`: String): MySimple.SomeFile = SomeFileImpl.parse(`value`)

        public fun someUUID(`value`: UUID): MySimple.SomeUUID = SomeUUIDImpl.create(`value`)

        public fun `inner`(block: context(InnerImpl.DslBuilder) () -> Unit): MySimple.Inner = InnerImpl.DslBuilder().apply(block).build()

        public fun build(): MySimpleJson {
            requireNotNull(nameName) { "Property 'MySimpleImpl.name' is not set!" }
            requireNotNull(count) { "Property 'MySimpleImpl.count' is not set!" }
            requireNotNull(uri) { "Property 'MySimpleImpl.uri' is not set!" }
            requireNotNull(`file`) { "Property 'MySimpleImpl.`file`' is not set!" }
            //requireNotNull(myEnum) { "Property 'MySimpleImpl.myEnum' is not set!" }
            return MySimpleJson(
                nameName = nameName(nameName!!),
                count = count(count!!),
                //inner = inner!!,
                uri = someUri(uri!!),
                listUri = listUri.map { someUri(it) },
                nullableListUri = nullableListUri.map { it?.let(::someUri) },
                `file` = someFile(`file`!!),
                uuid = uuid?.let(::someUUID),
                mapUUID = mapUUID.entries.associate { nameName(it.key) to someUUID(it.value) },
                mapUUIDNullable = mapUUIDNullable.entries.associate { nameName(it.key) to it.value?.let(::someUUID) },
                mapUUIDAll = mapUUIDAll.entries.associate { someUUID(it.key) to someUUID(it.value) },
                //myEnum = myEnum!!,
                nestedList1 = nestedList1.map { it.map { nameName(it) }.toSet() },
                nestedMap = nestedMap.entries.associate { it.key?.let(::nameName) to it.value.map { nameName(it) } },
                )
        }
    }

    // https://stackoverflow.com/questions/65272262/custom-serializer-for-data-class-without-serializable
    @OptIn(ExperimentalSerializationApi::class)
    public companion object : KSerializer<MySimpleJson> {
        //private val builderAction = ClassSerialDescriptorBuilder()
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor(MySimpleJson::class.java.name) {
                element<String>("nameName")
                element<String>("uri")
                element<List<String>>("listUri")
                // nullableListUri: List<SomeUri?>
                element<List<String?>>("nullableListUri")
                element<String>("file")
                element<String?>("uuid", isOptional = true)
                // mapUUID: Map<Name, SomeUUID>
                element<Map<String, String>>("mapUUID")
                element<Map<String, String?>>("mapUUIDNullable")
                element<Map<String, String>>("mapUUIDAll")
                //element<>("myEnum")
                // nestedList1: List<Set<Name>>
                element<List<Set<String>>>("nestedList1")
                element<Map<String?, List<String>>>("nestedMap")
                element<Short>("count")
                //element<String>("inner")
            }

        override fun serialize(encoder: Encoder, value: MySimpleJson) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value.nameName.boxed)
                encodeShortElement(descriptor, 11, value.count.boxed)

                // uri: SomeUri
                encodeStringElement(descriptor, 1, value.uri.boxed.toString()) // serialize from @KDParsable
                // listUri: List<SomeUri>
                encodeSerializableElement(descriptor, 2,
                    ListSerializer(String.serializer()),
                    value.listUri.map { it.boxed.toString() }) // serialize from @KDParsable
                // nullableListUri: List<SomeUri?>
                encodeSerializableElement(descriptor, 3, ListSerializer(String.serializer().nullable),
                    value.nullableListUri.map { it?.boxed?.toString() }) // serialize from @KDParsable
                encodeStringElement(descriptor, 4, value.file.boxed.toString()) // serialize from @KDParsable
                // uuid: SomeUUID?
                encodeNullableSerializableElement(descriptor, 5, String.serializer(), value.uuid?.boxed?.toString()) // serialize from @KDParsable
                // mapUUID: Map<Name, SomeUUID>
                encodeSerializableElement(descriptor, 6, MapSerializer(String.serializer(), String.serializer()),
                    value.mapUUID.entries.associate { it.key.boxed to it.value.boxed.toString() }) // serialize from @KDParsable
                // mapUUIDNullable: Map<Name, SomeUUID?>
                encodeSerializableElement(descriptor, 7, MapSerializer(String.serializer(), String.serializer().nullable),
                    value.mapUUIDNullable.entries.associate { it.key.boxed to it.value?.boxed?.toString() }) // serialize from @KDParsable
                // mapUUIDAll: Map<SomeUUID, SomeUUID>
                encodeSerializableElement(descriptor, 8, MapSerializer(String.serializer(), String.serializer()),
                    value.mapUUIDAll.entries.associate { it.key.boxed.toString() to it.value.boxed.toString() }) // serialize from @KDParsable
                // nestedList1: List<Set<Name>>
                encodeSerializableElement(descriptor, 9, ListSerializer(SetSerializer(String.serializer())),
                    value.nestedList1.map { it.map { it.boxed }.toSet() })
                // nestedMap: Map<Name?, List<Name>>
                encodeSerializableElement(descriptor, 10, MapSerializer(String.serializer().nullable, ListSerializer(String.serializer())),
                    value.nestedMap.entries.associate { it.key?.boxed to it.value.map { it.boxed } })
                // inner
                //encodeSerializableElement()
            }
        }

        override fun deserialize(decoder: Decoder): MySimpleJson =
            decoder.decodeStructure(descriptor) {
                val builder = Builder()
                loop@ while (true) {

                    when (val i = decodeElementIndex(descriptor)) {
                        0 -> builder.nameName = decodeStringElement(descriptor, 0).let(NameImpl::create)
                        11 -> builder.count = decodeShortElement(descriptor, 11).let(CountImpl::create)
                        // uri: SomeUri (as String)
                        1 -> builder.uri = decodeStringElement(descriptor, 1).let(SomeUriImpl::parse) // deserialize from @KDParsable
                        // listUri: List<SomeUri>
                        2 -> builder.listUri = decodeSerializableElement(descriptor, 2, ListSerializer(String.serializer()))
                            .map { it.let(SomeUriImpl::parse) }
                        // nullableListUri: List<SomeUri?>
                        3 -> builder.nullableListUri = decodeSerializableElement(descriptor, 3, ListSerializer(String.serializer().nullable))
                            .map { it?.let(SomeUriImpl::parse) }
                        // file: SomeFile (as String)
                        4 -> builder.file = decodeStringElement(descriptor, 4).let(SomeFileImpl::parse) // deserialize from @KDParsable
                        // uuid: SomeUUID?
                        5 -> builder.uuid = decodeNullableSerializableElement(descriptor, 5, String.serializer().nullable)
                            ?.let(SomeUUIDImpl::parse) // deserialize from @KDParsable
                        // mapUUID: Map<Name, SomeUUID>
                        6 -> builder.mapUUID = decodeSerializableElement(descriptor, 6, MapSerializer(String.serializer(), String.serializer()))
                            .entries.associate { it.key.let(NameImpl::create) to it.value.let(SomeUUIDImpl::parse) }
                        // mapUUIDNullable: Map<Name, SomeUUID?>
                        7 -> builder.mapUUIDNullable = decodeSerializableElement(descriptor, 7, MapSerializer(String.serializer(), String.serializer().nullable))
                            .entries.associate { it.key.let(NameImpl::create) to it.value?.let(SomeUUIDImpl::parse) } // !!! Nullable
                        // mapUUIDAll: Map<SomeUUID, SomeUUID>
                        8 -> builder.mapUUIDAll = decodeSerializableElement(descriptor, 8, MapSerializer(String.serializer(), String.serializer()))
                            .entries.associate { it.key.let(SomeUUIDImpl::parse) to it.value.let(SomeUUIDImpl::parse) }
                        // nestedList1: List<Set<Name>>
                        9 -> builder.nestedList1 = decodeSerializableElement(descriptor, 9, ListSerializer(SetSerializer(String.serializer())))
                            .map { it.map { it.let(NameImpl::create) }.toSet() }
                        // nestedMap: Map<Name?, List<Name>>
                        10 -> builder.nestedMap = decodeSerializableElement(descriptor, 10, MapSerializer(String.serializer().nullable, ListSerializer(String.serializer())))
                            .entries.associate { it.key?.let(NameImpl::create) to it.value.map { it.let(NameImpl::create) }  }
                        DECODE_DONE -> break@loop
                        else -> throw SerializationException("Unexpected index $i")
                    }
                }
                builder.build()
            }
    }
}

public fun mySimpleJson(block: context(MySimpleJson.DslBuilder) () -> Unit): MySimpleJson = MySimpleJson.DslBuilder().apply(block).build()
