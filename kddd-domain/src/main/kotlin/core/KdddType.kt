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
        }
    }
}
