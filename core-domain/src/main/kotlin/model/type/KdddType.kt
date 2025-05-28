package ru.it_arch.kddd.domain.model.type

public sealed interface KdddType : Generatable {

    // TODO: Дурное название. Переименовать.
    public interface DataClass : KdddType {
        public val hasDsl: Boolean
        /*public val hasJson: Boolean*/
        public val nestedTypes: Set<KdddType>
        public fun addNestedType(kdddType: KdddType)
    }

    public sealed interface ValueClass : KdddType {

        public companion object {
            public const val PARAM_NAME: String = "boxed"
            public const val FORK_METHOD: String = "fork"

            /** `override fun toString(): String { <body> }` */
            public const val TEMPLATE_TO_STRING_BODY: String = "return %N.toString()"
            /** `override fun <T : Boxed<BOXED>> fork(boxed: BOXED): T { <body> }` */
            public const val TEMPLATE_FORK_BODY: String = "return %N(%N) as %T"
            /** `public operator fun invoke(boxed: <BOXED>): T { <body> }` */
            public const val TEMPLATE_COMPANION_INVOKE_BODY: String = "return %N(%N)"
        }
    }
}
