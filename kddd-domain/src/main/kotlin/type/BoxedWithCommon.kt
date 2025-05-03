package ru.it_arch.clean_ddd.domain.type

import ru.it_arch.clean_ddd.domain.type.KdddType.Boxed
import ru.it_arch.clean_ddd.domain.type.KdddType.Generatable
import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
public data class BoxedWithCommon private constructor(
    private val generatable: GeneratableDelegate,
    public val boxed: CommonClassName,
    public val serializationMethod: SerializationMethodName,
    public val deserializationMethod: DeserializationMethodName,
    public val isStringInDsl: Boolean
) : Generatable by generatable, Boxed, ValueObject.Data {

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    @JvmInline
    public value class CommonClassName private constructor(
        override val boxed: String
    ) : ValueObject.Boxed<String> {

        override fun toString(): String = boxed

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T {
            TODO("Not yet implemented")
        }

        override fun validate() {}

        public companion object {
            public const val FABRIC_PARSE_METHOD: String = "parse"

            public operator fun invoke(value: String): CommonClassName =
                CommonClassName(value)
        }
    }

    @JvmInline
    public value class SerializationMethodName(override val boxed: String) : ValueObject.Boxed<String> {
        override fun toString(): String = boxed

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T {
            TODO("Not yet implemented")
        }

        override fun validate() {}
    }

    @JvmInline
    public value class DeserializationMethodName(override val boxed: String) : ValueObject.Boxed<String> {
        override fun toString(): String = boxed

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T {
            TODO("Not yet implemented")
        }

        override fun validate() {}
    }

    public companion object {
        public operator fun invoke(
            gd: GeneratableDelegate,
            boxed: CommonClassName,
            parsable: KDParsable
        ): BoxedWithCommon = BoxedWithCommon(
            gd,
            boxed,
            SerializationMethodName(parsable.serialization),
            DeserializationMethodName(parsable.deserialization),
            parsable.useStringInDsl
        )
    }
}
