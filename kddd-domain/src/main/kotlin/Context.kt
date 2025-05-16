package ru.it_arch.clean_ddd.domain

import ru.it_arch.clean_ddd.domain.core.KdddType
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
public data class Context private constructor(
    val kddd: CompositeClassName,
    val enclosing: KdddType.ModelContainer?,
    val annotations: Set<Annotation>,
    val properties: List<Property>
) : ValueObject.Data {

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    public class Builder {
        public var kddd: CompositeClassName? = null
        public var enclosing: KdddType.ModelContainer? = null
        public var annotations: Set<Annotation> = emptySet()
        public var properties: List<Property>? = null

        context(options: Options)
        public fun build(): Context {
            checkNotNull(kddd) { "Property 'kddd' must be initialized!" }
            checkNotNull(properties) { "Property 'properties' must be initialized!" }

            return Context(
                kddd!!,
                enclosing,
                annotations,
                properties!!
            )
        }
    }
}
