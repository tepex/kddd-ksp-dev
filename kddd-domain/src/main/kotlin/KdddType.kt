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
        public val kddd: CompositeClassName.ClassName
        public val impl: CompositeClassName.ClassName
        public val enclosing: ModelContainer?
    }

    public sealed interface Boxed : Generatable {

        public companion object {
            public const val PARAM_NAME: String = "boxed"
            public const val FORK_METHOD: String = "fork"
        }
    }
}
