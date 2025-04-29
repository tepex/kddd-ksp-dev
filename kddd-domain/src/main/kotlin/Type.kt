package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.KDParsable
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

public sealed interface Type {

    //public val kddd: FullClassName

    public interface Model : Generatable {
    }

    public interface Generatable : Type {
        //public val impl: ClassName
    }

    public class Data private constructor(
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
    ) : Type {

        override fun toString(): String =
            "KDType.Boxed<>"

        public companion object {
            public const val PARAM_NAME: String = "boxed"
            public const val FABRIC_CREATE_METHOD: String = "create"
            public const val FABRIC_PARSE_METHOD: String = "parse"
            public const val CREATE_METHOD: String = "fork"

        }
    }

    @JvmInline
    public value class FullClassName(override val boxed: String) : ValueObject.Boxed<String> {
        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T =
            TODO("Not yet implemented")

        override fun validate() {}

        override fun toString(): String = boxed
    }
}
