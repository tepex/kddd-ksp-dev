package ru.it_arch.clean_ddd.domain.core

public sealed interface KdddType : Generatable {

    public interface ModelContainer : KdddType {
        /*
        public val hasDsl: Boolean
        public val hasJson: Boolean*/
        public val nestedTypes: Set<KdddType>
        public fun addNestedType(kdddType: KdddType)
    }

    public sealed interface Boxed : KdddType {

        public companion object {
            public const val PARAM_NAME: String = "boxed"
            public const val FORK_METHOD: String = "fork"

            /** `override fun toString(): String { <body> }` */
            public const val TEMPLATE_TO_STRING_BODY: String = "return %N.toString()"
            /** `override fun <T : Boxed<BOXED>> fork(boxed: BOXED): T { <body> }` */
            public const val TEMPLATE_FORK_BODY: String = "return %T(%N) as %T"
            /** `public operator fun invoke(boxed: <BOXED>): T { <body> }` */
            public const val TEMPLATE_COMPANION_INVOKE_BODY: String = "return %T(%N)"
        }
    }
}
