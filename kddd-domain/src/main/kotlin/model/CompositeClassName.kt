package ru.it_arch.clean_ddd.domain.model

import ru.it_arch.clean_ddd.domain.shortName
import ru.it_arch.kddd.Kddd
import ru.it_arch.kddd.ValueObject

@ConsistentCopyVisibility
public data class CompositeClassName private constructor(
    public val packageName: PackageName,
    public val className: ClassName
) : ValueObject.Data {

    override fun validate() {}

    override fun <T : Kddd, A : Kddd> fork(vararg args: A): T {
        TODO("Not yet implemented")
    }

    @JvmInline
    public value class PackageName private constructor(override val boxed: String) : ValueObject.Boxed<String> {

        override fun validate() {}

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T {
            TODO("Not yet implemented")
        }

        override fun toString(): String =
            boxed

        public operator fun plus(subpackage: Options.Subpackage): PackageName =
            PackageName(subpackage.takeUnless { it.isEmpty() }?.let { "$boxed.$subpackage" } ?: boxed)

        public companion object {
            public operator fun invoke(value: String): PackageName =
                PackageName(value)
        }
    }

    @JvmInline
    public value class ClassName private constructor(override val boxed: String) : ValueObject.Boxed<String> {

        override fun validate() {}

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T {
            TODO("Not yet implemented")
        }

        override fun toString(): String =
            boxed

        public operator fun plus(other: ClassName): ClassName =
            ClassName("$boxed.${other.shortName}")

        public companion object {
            public operator fun invoke(value: String): ClassName =
                ClassName(value)
        }
    }

    @JvmInline
    public value class FullClassName private constructor(override val boxed: String) : ValueObject.Boxed<String> {

        override fun validate() {}

        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T {
            TODO("Not yet implemented")
        }

        override fun toString(): String =
            boxed

        public companion object {
            public operator fun invoke(value: String): FullClassName =
                FullClassName(value)
        }
    }

    public class Builder {
        public var packageName: PackageName? = null
        public var fullClassName: String? = null

        public fun build(): CompositeClassName {
            checkNotNull(packageName) { "Property 'packageName' must be initialized!" }
            checkNotNull(fullClassName) { "Property 'fullClassName' must be initialized!" }
            return CompositeClassName(
                packageName!!,
                ClassName(fullClassName!!.substringAfterLast("$packageName."))
            )
        }
    }

    public companion object {
        public operator fun invoke(packageName: PackageName, className: ClassName): CompositeClassName =
            CompositeClassName(packageName, className)
    }
}
