package ru.it_arch.clean_ddd.domain

import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
public data class Context private constructor(
    val kddd: KdddType.Generatable.KdddClassName,
    val impl: KdddType.Generatable.ImplClassName,
    val parent: KdddType.ModelContainer?,
    val annotations: List<Annotation>,
    val properties: List<Property>
) : ValueObject.Data {

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    public class Builder {
        public var kdddClassName: String? = null
        public var parent: KdddType.ModelContainer? = null
        public var annotations: List<Annotation> = emptyList()
        public var properties: List<Property>? = null

        context(options: Options)
        public fun build(): Context {
            checkNotNull(kdddClassName) { "Property 'kdddClassName' must be initialized!" }
            checkNotNull(properties) { "Property 'properties' must be initialized!" }

            return Context(
                KdddType.Generatable.KdddClassName(kdddClassName!!),
                kdddClassName!! `to implementation class name with @KDGeneratable annotation in` annotations,
                parent,
                annotations,
                properties!!
            )
        }
    }
}
