package ru.it_arch.clean_ddd.domain.type

import ru.it_arch.kddd.ValueObject

public sealed interface KdddType {

    public interface Model : Generatable {
        /*
        public val hasDsl: Boolean
        public val hasJson: Boolean*/
    }

    // Model, Boxed
    public interface Generatable : KdddType {
        public val kddd: KdddClassName
        public val impl: ImplClassName
        public val enclosing: KdddType?

        @JvmInline
        public value class KdddClassName private constructor(override val boxed: String) : ValueObject.Boxed<String> {

            override fun validate() {}

            override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T {
                TODO("Not yet implemented")
            }

            override fun toString(): String = boxed

            public companion object {
                public operator fun invoke(value: String): KdddClassName =
                    KdddClassName(value)
            }
        }

        @JvmInline
        public value class ImplClassName private constructor(override val boxed: String) : ValueObject.Boxed<String> {

            override fun validate() {}

            override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T {
                TODO("Not yet implemented")
            }

            override fun toString(): String = boxed

            public companion object {
                public operator fun invoke(value: String): ImplClassName =
                    ImplClassName(value)
            }
        }
    }

    public sealed interface Boxed : Generatable {

        public companion object {
            public const val PARAM_NAME: String = "boxed"
            public const val FORK_METHOD: String = "fork"
        }
    }
}
