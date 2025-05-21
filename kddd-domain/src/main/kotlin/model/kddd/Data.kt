package ru.it_arch.clean_ddd.domain.model.kddd

import ru.it_arch.clean_ddd.domain.model.Property
import ru.it_arch.kddd.Kddd

@ConsistentCopyVisibility
public data class Data private constructor(
    private val generatable: Generatable,
    // List, not Set!
    public val properties: List<Property>,
    override val hasDsl: Boolean
) : Generatable by generatable, KdddType.ModelContainer {

    private val _nestedTypes = mutableSetOf<KdddType>()
    override val nestedTypes: Set<KdddType>
        get() = _nestedTypes.toSet()

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    override fun validate() {}

    override fun addNestedType(kdddType: KdddType) {
        _nestedTypes += kdddType
    }

    public companion object {
        public const val BUILDER_CLASS_NAME: String = "Builder"
        public const val DSL_BUILDER_CLASS_NAME: String = "DslBuilder"
        public const val BUILDER_BUILD_METHOD_NAME: String = "build"
        public const val TO_BUILDER_FUN: String = "toBuilder"
        public const val TO_DSL_BUILDER_FUN: String = "toDslBuilder"
        public const val APPLY_BUILDER: String = "%T().apply(%N).$BUILDER_BUILD_METHOD_NAME()"

        public operator fun invoke(generatable: Generatable, properties: List<Property>, hasDsl: Boolean): Data =
            Data(generatable, properties, hasDsl)
    }
}
