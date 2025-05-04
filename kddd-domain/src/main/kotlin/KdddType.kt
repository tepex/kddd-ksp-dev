package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.ValueObject

public sealed interface KdddType {

    public interface ModelContainer : Generatable {
        /*
        public val hasDsl: Boolean
        public val hasJson: Boolean*/
        public val nestedTypes: Set<KdddType>
        public fun addNestedType(kdddType: KdddType)
    }

    // Model, Boxed
    public interface Generatable : KdddType, ValueObject.Data {
        public val kddd: KdddClassName
        public val impl: ImplClassName
        public val enclosing: ModelContainer?

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
