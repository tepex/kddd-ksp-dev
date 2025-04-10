package ru.it_arch.clean_ddd.domain

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlin.ConsistentCopyVisibility
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.jvm.JvmInline
import ru.it_arch.clean_ddd.domain.demo.WithInner
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
@Serializable(with = WithInnerCustomImpl.Companion::class)
public data class WithInnerCustomImpl private constructor(
    override val myInner: WithInner.MyInner,
    override val myOptionalInner: WithInner.MyInner?,
) : WithInner {

    init {
        validate()
    }

    public fun toBuilder(): Builder {
        val ret = Builder()
        ret.myInner = myInner
        ret.myOptionalInner = myOptionalInner
        return ret
    }

    public fun toDslBuilder(): DslBuilder {
        val ret = DslBuilder()
        ret.myInner = myInner
        ret.myOptionalInner = myOptionalInner
        return ret
    }

    @ConsistentCopyVisibility
    @Serializable(with = MyInnerImpl.Companion::class)
    public data class MyInnerImpl private constructor(
        override val innerLong: WithInner.MyInner.InnerLong,
        override val innerStr: WithInner.MyInner.InnerStr,
    ) : WithInner.MyInner {
        init {
            validate()
        }
        public fun toBuilder(): Builder {
            val ret = Builder()
            ret.innerLong = innerLong
            ret.innerStr = innerStr
            return ret
        }

        public fun toDslBuilder(): DslBuilder {
            val ret = DslBuilder()
            ret.innerLong = innerLong.boxed
            ret.innerStr = innerStr.boxed
            return ret
        }

        @JvmInline
        public value class InnerLongImpl private constructor(
            override val boxed: Long,
        ) : WithInner.MyInner.InnerLong {
            init {
                validate()
            }
            override fun toString(): String = boxed.toString()

            @Suppress("UNCHECKED_CAST")
            override fun <T : ValueObject.Boxed<Long>> copy(boxed: Long): T = InnerLongImpl(boxed) as T

            public companion object {
                public fun create(boxed: Long): InnerLongImpl = InnerLongImpl(boxed)
            }
        }

        @JvmInline
        public value class InnerStrImpl private constructor(
            override val boxed: String,
        ) : WithInner.MyInner.InnerStr {
            init {
                validate()
            }
            override fun toString(): String = boxed.toString()

            @Suppress("UNCHECKED_CAST")
            override fun <T : ValueObject.Boxed<String>> copy(boxed: String): T = InnerStrImpl(boxed) as T

            public companion object {
                public fun create(boxed: String): InnerStrImpl = InnerStrImpl(boxed)
            }
        }

        public class Builder {
            public lateinit var innerLong: WithInner.MyInner.InnerLong

            public lateinit var innerStr: WithInner.MyInner.InnerStr

            public fun build(): MyInnerImpl {
                require(::innerLong.isInitialized) { "Property 'MyInnerImpl.innerLong' is not set!" }
                require(::innerStr.isInitialized) { "Property 'MyInnerImpl.innerStr' is not set!" }
                return MyInnerImpl(innerLong = innerLong,innerStr = innerStr,)
            }
        }

        public class DslBuilder {
            public var innerLong: Long? = null

            public var innerStr: String? = null

            public fun innerLong(`value`: Long): WithInner.MyInner.InnerLong = InnerLongImpl.create(`value`)

            public fun innerStr(`value`: String): WithInner.MyInner.InnerStr = InnerStrImpl.create(`value`)

            public fun build(): MyInnerImpl {
                requireNotNull(innerLong) { "Property 'MyInnerImpl.innerLong' is not set!" }
                requireNotNull(innerStr) { "Property 'MyInnerImpl.innerStr' is not set!" }
                return MyInnerImpl(innerLong = innerLong(innerLong!!),innerStr = innerStr(innerStr!!),)
            }
        }

        public companion object : KSerializer<MyInnerImpl> {
            override val descriptor: SerialDescriptor =
                buildClassSerialDescriptor(MyInnerImpl::class.java.name) {
                    element<Long>("innerLong")
                    element<String>("innerStr")
                }


            override fun deserialize(decoder: Decoder): MyInnerImpl =
                decoder.decodeStructure(descriptor) {
                    val builder = Builder()
                    loop@ while (true) {
                        when (val i = decodeElementIndex(descriptor)) {
                            0 -> builder.innerLong = decodeLongElement(descriptor, 0).let(InnerLongImpl::create)
                            1 -> builder.innerStr = decodeStringElement(descriptor, 1).let(InnerStrImpl::create)
                            DECODE_DONE -> break@loop
                            else -> throw SerializationException("Unexpected index $i")
                        }
                    }
                    builder.build()
                }

            override fun serialize(encoder: Encoder, value: MyInnerImpl) {
                encoder.encodeStructure(descriptor) {
                    encodeLongElement(descriptor, 0, value.innerLong.boxed)
                    encodeStringElement(descriptor, 1, value.innerStr.boxed)
                }
            }

        }
    }

    public class Builder {
        public lateinit var myInner: WithInner.MyInner

        public var myOptionalInner: WithInner.MyInner? = null

        public fun build(): WithInnerCustomImpl {
            require(::myInner.isInitialized) { "Property 'WithInnerImpl.myInner' is not set!" }
            return WithInnerCustomImpl(myInner = myInner,myOptionalInner = myOptionalInner,)
        }
    }

    public class DslBuilder {
        public var myInner: WithInner.MyInner? = null

        public var myOptionalInner: WithInner.MyInner? = null

        public fun myInner(block: context(MyInnerImpl.DslBuilder) () -> Unit): WithInner.MyInner = MyInnerImpl.DslBuilder().apply(block).build()

        public fun build(): WithInnerCustomImpl {
            requireNotNull(myInner) { "Property 'WithInnerImpl.myInner' is not set!" }
            return WithInnerCustomImpl(myInner = myInner!!,myOptionalInner = myOptionalInner,)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    public companion object : KSerializer<WithInnerCustomImpl> {
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor(WithInnerCustomImpl::class.java.name) {
                element<WithInner.MyInner>("myInner")
                element<WithInner.MyInner?>("myOptionalInner", isOptional = true)
            }

        override fun serialize(encoder: Encoder, value: WithInnerCustomImpl) {
            encoder.encodeStructure(descriptor) {
                encodeSerializableElement(descriptor, 0, MyInnerImpl.serializer(), value.myInner as MyInnerImpl)
                encodeNullableSerializableElement(descriptor, 1, MyInnerImpl.serializer(), value.myOptionalInner as MyInnerImpl?)
            }
        }

        override fun deserialize(decoder: Decoder): WithInnerCustomImpl =
            decoder.decodeStructure(descriptor) {
                val builder = Builder()
                loop@ while (true) {
                    when (val i = decodeElementIndex(descriptor)) {
                        0 -> builder.myInner = decodeSerializableElement(descriptor, 0, MyInnerImpl.serializer())
                        1 -> builder.myOptionalInner = decodeNullableSerializableElement(descriptor, 1, MyInnerImpl.serializer().nullable)
                        DECODE_DONE -> break@loop
                        else -> throw SerializationException("Unexpected index $i")
                    }
                }
                builder.build()
            }
    }
}

public fun withInnerCustom(block: context(WithInnerCustomImpl.DslBuilder) () -> Unit): WithInnerCustomImpl = WithInnerCustomImpl.DslBuilder().apply(block).build()
