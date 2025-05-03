package ru.it_arch.clean_ddd.domain.type

import ru.it_arch.clean_ddd.domain.Property
import ru.it_arch.clean_ddd.domain.type.KdddType.Generatable
import ru.it_arch.clean_ddd.domain.type.KdddType.Model
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
public data class Data private constructor(
    private val generatable: GeneratableDelegate,
    public val properties: List<Property>
) : Generatable by generatable, Model, ValueObject.Data {

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    override fun validate() {}

    public companion object {
        public const val BUILDER_CLASS_NAME: String = "Builder"
        public const val DSL_BUILDER_CLASS_NAME: String = "DslBuilder"
        public const val BUILDER_BUILD_METHOD_NAME: String = "build"
        public const val APPLY_BUILDER: String = "%T().apply(%N).$BUILDER_BUILD_METHOD_NAME()"

        public operator fun invoke(generatable: GeneratableDelegate, properties: List<Property>): Data =
            Data(generatable, properties)
    }
}
