package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
public data class Data private constructor(
    private val generatable: KdddType.Generatable,
    public val properties: List<Property>
) : KdddType.Generatable by generatable, KdddType.ModelContainer, ValueObject.Data {

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
        public const val APPLY_BUILDER: String = "%T().apply(%N).$BUILDER_BUILD_METHOD_NAME()"

        public operator fun invoke(generatable: KdddType.Generatable, properties: List<Property>): Data =
            Data(generatable, properties)
    }
}
