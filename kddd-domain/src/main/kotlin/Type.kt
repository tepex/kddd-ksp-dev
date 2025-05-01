package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.ValueObject

public sealed interface Type {

    public val kddd: ClassName

    public interface Model : Generatable {
    }

    public interface Generatable : Type {
        public val impl: ClassName
        public val properties: List<Property>
    }

    public open class Data protected constructor(
        override val kddd: ClassName,
        override val impl: ClassName,
        override val properties: List<Property>
    ) : Model {

        public companion object {
            public const val BUILDER_CLASS_NAME: String = "Builder"
            public const val DSL_BUILDER_CLASS_NAME: String = "DslBuilder"
            public const val BUILDER_BUILD_METHOD_NAME: String = "build"
            public const val APPLY_BUILDER: String = "%T().apply(%N).$BUILDER_BUILD_METHOD_NAME()"

        }
    }

    public class IEntity private constructor(private val data: Data) : Model by data {

        public companion object {
            public const val ID_NAME: String = "id"
        }
    }

    public class Boxed private constructor(
        kddd: ClassName,
        impl: ClassName,
        boxed: Property
    ) : Data(kddd, impl, listOf(boxed)) {

        override fun toString(): String =
            "KDType.Boxed<>"

        public companion object {
            public const val PARAM_NAME: String = "boxed"
            public const val FABRIC_CREATE_METHOD: String = "create"
            public const val FABRIC_PARSE_METHOD: String = "parse"
            public const val CREATE_METHOD: String = "fork"

            public operator fun invoke(kddd: ClassName, impl: ClassName, boxedType: String): Boxed =
                Property.DslBuilder().apply {
                    name = PARAM_NAME
                    serialName = PARAM_NAME
                    type = boxedType
                }.build().let { Boxed(kddd, impl, it) }
        }
    }
}
